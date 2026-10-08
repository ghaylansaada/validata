/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ghaylan.validata.engine

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.internal.ReflectionUtils
import io.ghaylan.validata.internal.TypeInfo
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.runtime.SchemaNotFoundException
import io.ghaylan.validata.schema.shape.DynamicShape
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.schema.spi.ObjectSchemaModule
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * Spring-free runtime cache for static endpoint schemas, generated [ObjectSchema] lookups, and
 * the validator catalog.
 *
 * Lives in this module so the walker and registry share a jar with **zero** Spring imports.
 * Boot population (validators and static schemas) is performed by the host registry initializer
 * via [registerValidators] and [registerStaticSchemas], then [freeze].
 *
 * Object graphs resolve **only** through [GeneratedSchemas] (KSP / hand-written
 * [ObjectSchemaModule] SPI modules). Safe to share across threads
 * after [freeze]; schema resolution caches are concurrent for read-mostly lookups.
 *
 * ### Startup contract
 *
 * Call [registerValidators] / [registerStaticSchemas] once during host bootstrap **before** sharing
 * the registry with [ValidatorEngine] or concurrent readers, then call [freeze]. After freeze,
 * further registration throws [IllegalStateException]. Readers use [validatorCatalog],
 * [staticSchemas], and [getSchemaByRequest] only.
 *
 * ### Example
 *
 * ```kotlin
 * val registry = ValidationRegistry()
 * registry.registerValidators(builtFromCatalog)
 * registry.registerStaticSchemas(endpointSchemas)
 * registry.freeze()
 *
 * val schema = registry.resolveObjectSchemaByClass(UserRequest::class.java)
 * val endpoint = registry.getSchemaByRequest(method.getUniqueIdentifier())
 * val nested = registry.tryResolveObjectSchema(Address::class.java) // null when absent
 * ```
 *
 * Prefer [tryResolveObjectSchema] on optional cascade paths ([DynamicShape]) so misses do not allocate
 * [SchemaNotFoundException] stack traces.
 *
 * ### Cache bounds
 *
 * Dynamic schema caches ([dynamicSchemas], [schemaByRawClass]) are unbounded under the **static
 * schema** product contract: generated / SPI schemas are JVM-lifetime singletons keyed by class.
 * Hosts that mint unique schemas or classes per request must not share a long-lived registry
 * without an external bound.*
 * 
 * @author Ghaylan Saada
 */
open class ValidationRegistry {

	/**
	 * Validator catalog: metadata class → (compatible [TypeInfo] → validator instance).
	 *
	 * Replaced/merged by [registerValidators]; not concurrent — populate at startup before [freeze].
	 */
	private val validators =
		HashMap<KClass<out ConstraintMetadata>, Map<TypeInfo, ConstraintValidator<*, *>>>()

	/**
	 * Immutable snapshot of [validators] after [freeze]; `null` until frozen.
	 */
	@Volatile
	private var frozenValidators: Map<KClass<out ConstraintMetadata>, Map<TypeInfo, ConstraintValidator<*, *>>>? =
		null

	/**
	 * Immutable snapshot of static endpoint schemas after [freeze]; `null` until frozen.
	 */
	@Volatile
	private var frozenStaticSchemas: Map<String, EndpointSchema>? = null

	/**
	 * Cache of resolved [ObjectSchema] keyed by the KSP / SPI type identity.
	 *
	 * Filled on first successful [GeneratedSchemas] hit; concurrent for read-mostly lookups.
	 */
	private val dynamicSchemas = ConcurrentHashMap<Class<*>, ObjectSchema>()

	/**
	 * Memoization of [resolveObjectSchemaByClass] keyed by the **exact** runtime [Class] argument.
	 *
	 * The reflective [ReflectionUtils.infoFromClass] walk is a pure function of that class and must
	 * not run on every [ValidatorEngine.validate] call. This map sits in front of [dynamicSchemas]
	 * (keyed by the KSP-resolved type) so a repeated lookup for the same concrete class returns in
	 * one ConcurrentHashMap get.
	 *
	 * Only successful resolutions are stored — missing schemas keep throwing
	 * [SchemaNotFoundException] without negative caching (a later SPI registration must remain
	 * discoverable until freeze; after freeze SPI is fixed for the JVM). Hot class redefinition is
	 * not a supported scenario for this library.
	 */
	private val schemaByRawClass = ConcurrentHashMap<Class<*>, ObjectSchema>()

	/**
	 * Endpoint id → [EndpointSchema] map populated by the host at startup via [registerStaticSchemas].
	 */
	private val staticSchemasInternal: ConcurrentHashMap<String, EndpointSchema> = ConcurrentHashMap()

	/**
	 * Whether [freeze] has completed.
	 */
	private val frozen = AtomicBoolean(false)

	/**
	 * Returns whether this registry has been [freeze]d.
	 */
	fun isFrozen(): Boolean = frozen.get()

	/**
	 * Freezes the validator catalog and static endpoint map.
	 *
	 * After this call, [registerValidators] / [registerStaticSchemas] throw.
	 * [validatorCatalog] / [staticSchemas] return deep-unmodifiable snapshots.
	 * Idempotent: a second call is a no-op.
	 */
	fun freeze() {
		if (!frozen.compareAndSet(false, true)) return

		val validatorsSnap = HashMap<KClass<out ConstraintMetadata>, Map<TypeInfo, ConstraintValidator<*, *>>>(validators.size)
		for (validator in validators) {
			validatorsSnap[validator.key] = Collections.unmodifiableMap(HashMap(validator.value))
		}
		frozenValidators = Collections.unmodifiableMap(validatorsSnap)
		frozenStaticSchemas = Collections.unmodifiableMap(HashMap(staticSchemasInternal))
		validators.clear()
		staticSchemasInternal.clear()
	}

	/**
	 * Merges [built] into the validator catalog (per-key overwrite).
	 *
	 * Mutates the private catalog. Intended for startup population by the Spring host; not
	 * concurrent-safe with readers until population finishes and [freeze] is called.
	 *
	 * @param built Metadata class → (compatible [TypeInfo] → validator instance).
	 * @throws IllegalStateException When the registry is already frozen.
	 */
	fun registerValidators(
		built: Map<KClass<out ConstraintMetadata>, Map<TypeInfo, ConstraintValidator<*, *>>>,
	) {
		checkNotFrozen("registerValidators")
		built.forEach { validators[it.key] = it.value }
	}

	/**
	 * Merges [schemas] into the private static endpoint map (endpoint id → [EndpointSchema]).
	 *
	 * Mutates via `putAll`. Prefer this over any direct map mutation.
	 *
	 * @param schemas Schemas from the Spring host's static schema builder / KSP endpoint modules.
	 * @throws IllegalStateException When the registry is already frozen.
	 */
	fun registerStaticSchemas(schemas: Map<String, EndpointSchema>) {
		checkNotFrozen("registerStaticSchemas")
		staticSchemasInternal.putAll(schemas)
	}

	/**
	 * Snapshot of the validator catalog currently registered with this instance.
	 *
	 * Before [freeze], returns an unmodifiable view of the live map (inner maps remain the
	 * registered instances — do not mutate them). After [freeze], returns a deep-unmodifiable
	 * snapshot.
	 *
	 * @return Metadata class → (compatible [TypeInfo] → validator); exposed read-only.
	 */
	fun validatorCatalog(): Map<KClass<out ConstraintMetadata>, Map<TypeInfo, ConstraintValidator<*, *>>> =
		frozenValidators ?: Collections.unmodifiableMap(validators)

	/**
	 * Read-only view of registered endpoint schemas (endpoint id → [EndpointSchema]).
	 *
	 * Populated only via [registerStaticSchemas]. Safe for concurrent readers after [freeze].
	 *
	 * @return Unmodifiable live view (pre-freeze) or frozen snapshot (post-freeze).
	 */
	fun staticSchemas(): Map<String, EndpointSchema> =
		frozenStaticSchemas ?: Collections.unmodifiableMap(staticSchemasInternal)

	/**
	 * Looks up a pre-registered endpoint schema by coordinate id.
	 *
	 * No mutation.
	 *
	 * @param id Endpoint id matching KSP / method unique identifier.
	 * @return Schema, or `null` when [registerStaticSchemas] never stored [id].
	 */
	fun getSchemaByRequest(id: String): EndpointSchema? =
		frozenStaticSchemas?.get(id) ?: staticSchemasInternal[id]

	/**
	 * Resolves an [ObjectSchema] for [clazz] from generated modules only.
	 *
	 * Hits [schemaByRawClass] first so reflective [TypeInfo] construction runs at most once per
	 * distinct runtime class for the life of this registry. On miss, may write [dynamicSchemas]
	 * and always writes [schemaByRawClass] on success.
	 *
	 * @param clazz Runtime class to resolve.
	 * @return Cached or freshly looked-up schema.
	 * @throws SchemaNotFoundException When no generated / SPI schema exists for [clazz].
	 */
	fun resolveObjectSchemaByClass(clazz: Class<*>): ObjectSchema {
		schemaByRawClass[clazz]?.let { return it }

		return schemaByRawClass.computeIfAbsent(clazz) {
			val type = ReflectionUtils.infoFromClass(clazz).resolveType.java
			dynamicSchemas[type]
				?: GeneratedSchemas.get(type)?.also { dynamicSchemas[type] = it }
				?: throw SchemaNotFoundException.forMissingSchema(type)
		}
	}

	/**
	 * Same as [resolveObjectSchemaByClass] but returns `null` instead of throwing.
	 *
	 * Prefer this on optional cascade paths ([DynamicShape]) where absence means “no nested schema”.
	 * Avoids `runCatching` so a miss does not allocate a [SchemaNotFoundException] + stack trace
	 * on every unresolved DynamicShape cascade. On success, may write [dynamicSchemas] and
	 * [schemaByRawClass].
	 *
	 * @param clazz Runtime class to resolve.
	 * @return Schema, or `null` when missing.
	 */
	fun tryResolveObjectSchema(clazz: Class<*>): ObjectSchema? {
		schemaByRawClass[clazz]?.let { return it }

		val type = ReflectionUtils.infoFromClass(clazz).resolveType.java
		
		val schema = dynamicSchemas[type]
			?: GeneratedSchemas.get(type)?.also { dynamicSchemas[type] = it }
			?: return null

		schemaByRawClass.putIfAbsent(clazz, schema)
		return schema
	}

	/**
	 * Selects the schema for a runtime value under a (possibly polymorphic) declared schema.
	 *
	 * May write schema caches when falling through to [tryResolveObjectSchema].
	 *
	 * @param declared Schema declared on the property / body.
	 * @param value Runtime instance, or `null`.
	 * @return Exact subtype match, closest assignable subtype, generated schema for the runtime
	 *   class, or [declared].
	 */
	fun schemaForValue(declared: ObjectSchema, value: Any?): ObjectSchema {
		if (value == null || declared.subtypes.isEmpty()) return declared
		val runtime = value.javaClass
		declared.subtypes[runtime]?.let { return it }

		// Single pass for closest assignable subtype — avoids filter{}.toList() intermediate
		// allocation when subtypes maps are larger than a handful of entries.
		var best: ObjectSchema? = null
		var bestDistance = Int.MAX_VALUE
		for ((candidate, schema) in declared.subtypes) {
			if (!candidate.isAssignableFrom(runtime)) continue
			val d = distance(candidate, runtime)
			if (d < bestDistance) {
				bestDistance = d
				best = schema
			}
		}
		best?.let { return it }

		return tryResolveObjectSchema(runtime) ?: declared
	}

	/**
	 * Inheritance distance from [runtime] up to [declared] (lower is closer).
	 *
	 * No I/O or mutation.
	 *
	 * @param declared Candidate supertype stored in [ObjectSchema.subtypes].
	 * @param runtime Actual value class.
	 * @return Number of superclass hops, or a large distance when [declared] is not an ancestor.
	 */
	private fun distance(declared: Class<*>, runtime: Class<*>): Int {
		var d = 0
		var c: Class<*>? = runtime
		while (c != null && c != declared && c != Any::class.java) {
			d++
			c = c.superclass
		}
		return d
	}

	/**
	 * @throws IllegalStateException When [frozen] is true.
	 */
	private fun checkNotFrozen(action: String) {
		check(!frozen.get()) {
			"ValidationRegistry is frozen; cannot $action. Call register* only during host bootstrap before freeze()."
		}
	}
}

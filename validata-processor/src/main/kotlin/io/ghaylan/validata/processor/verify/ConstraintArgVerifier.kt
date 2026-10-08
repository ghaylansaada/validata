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
package io.ghaylan.validata.processor.verify

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.TypeClassification
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import java.util.*

/**
 * Compile-time checks for `@ConstraintArg` markers on constraint metadata.
 *
 * Replaces hard-coded `@Min` / `@Max` / `@MultipleOf` name tables with discovery driven by
 * [ConstraintArgHostDiscovery]. Keep parse helpers aligned with **validata-core**
 * `toConstraintNumber` and IntelliJ `ConstraintLiteralFormats`.
 *
 * @property logger KSP diagnostics sink for invalid constraint arguments
 * @property jacksonNaming wire naming for RequiredWhen gate indexes when `@JsonProperty` is absent
 *
 * @author Ghaylan Saada
 */
internal class ConstraintArgVerifier(
	private val logger: KSPLogger,
	private val jacksonNaming: JacksonPropertyNaming = JacksonPropertyNaming.IDENTITY,
) {

	/**
	 * Round-scoped host cache bound by [bindHostCache]; cleared when the round ends.
	 */
	private var hostCache: MutableMap<String, List<ConstraintArgHost>>? = null

	/**
	 * Round-scoped owner property-type index cache (RequiredWhen); cleared with the round.
	 */
	private var ownerPropertyTypeCache: MutableMap<String, Map<String, KSType>>? = null

	/**
	 * Binds (or clears) the round-scoped `@ConstraintArg` host cache.
	 *
	 * Side effects: assigns [hostCache].
	 *
	 * @param cache Round map keyed by annotation/metadata FQCN, or `null` to unbind.
	 */
	fun bindHostCache(cache: MutableMap<String, List<ConstraintArgHost>>?) {
		hostCache = cache
	}

	/**
	 * Binds (or clears) the round-scoped owner property-type index cache for `@RequiredWhen`.
	 *
	 * Side effects: assigns [ownerPropertyTypeCache].
	 *
	 * @param cache Round map keyed by owner FQCN, or `null` to unbind.
	 */
	fun bindOwnerPropertyTypeCache(cache: MutableMap<String, Map<String, KSType>>?) {
		ownerPropertyTypeCache = cache
	}

	/**
	 * Validates usage-site annotation arguments against `@ConstraintArg` markers on annotation
	 * declaration (preferred) or [metadata] (legacy).
	 *
	 * Side effects: may emit [KSPLogger.error] via host verification.
	 *
	 * @param ann Usage-site constraint (e.g. `@Min("18")`).
	 * @param metadata Resolved metadata class.
	 * @param valueType Direct annotated subject type (for [ConstraintArgKind.TYPED_LITERAL]).
	 * @param subjectNode Symbol for diagnostics.
	 * @param owner Declaring class of annotated member (DTO), when known — types
	 *   `@RequiredWhen.value` / `values` against gate sibling.
	 * @param siblingParamTypes Flat endpoint sibling name → type (query/header/path), when known.
	 * @param ownerPropertyTypes Optional precomputed owner property name → type map; when null and
	 *   `@RequiredWhen` needs it, built once per owner FQCN via [ownerPropertyTypeCache].
	 */
	fun verify(
		ann: KSAnnotation,
		metadata: KSClassDeclaration?,
		valueType: KSType,
		subjectNode: KSNode?,
		owner: KSClassDeclaration? = null,
		siblingParamTypes: Map<String, KSType>? = null,
		ownerPropertyTypes: Map<String, KSType>? = null,
	) {
		val annotationDecl = ann.annotationType.resolve().declaration as? KSClassDeclaration
		val hosts = ConstraintArgHostDiscovery.discoverHosts(annotationDecl, metadata, hostCache)
		if (hosts.isEmpty()) return

		val argsByName = extractArgs(ann)
		val requiredWhen = RequiredWhenArgRules.isRequiredWhen(ann)
		val resolvedOwnerPropertyTypes = when {
			!requiredWhen || owner == null -> null
			ownerPropertyTypes != null -> ownerPropertyTypes
			else -> {
				val key = owner.qualifiedName?.asString()
				val cache = ownerPropertyTypeCache
				if (key != null && cache != null) {
					cache.getOrPut(key) { RequiredWhenArgRules.ownerPropertyTypeIndex(owner, jacksonNaming) }
				} else {
					RequiredWhenArgRules.ownerPropertyTypeIndex(owner, jacksonNaming)
				}
			}
		}
		val gateType = if (requiredWhen) {
			RequiredWhenArgRules.gateType(
				owner = owner,
				siblingParamTypes = siblingParamTypes,
				args = argsByName,
				ownerPropertyTypes = resolvedOwnerPropertyTypes,
				naming = jacksonNaming,
			)
		} else {
			null
		}

		for (host in hosts) {
			if (requiredWhen && RequiredWhenArgRules.shouldSkipParameter(argsByName, host.parameterName)) {
				continue
			}
			val typeForHost =
				if (requiredWhen &&
					(host.parameterName == AnnotationAttrs.ConstraintPayload.VALUE ||
						host.parameterName == AnnotationAttrs.ConstraintPayload.VALUES) &&
					gateType != null
				) {
					gateType
				} else {
					valueType
				}
			val raw = argsByName[host.parameterName]
			verifyHost(ann, host, raw, typeForHost, subjectNode)
		}
	}

	/**
	 * Dispatches [host] checks for argument [raw] against [valueType].
	 *
	 * Side effects: may emit [KSPLogger.error] for ELEMENT target shape mismatches and via
	 * [verifyValueTarget] / [verifyElementTarget].
	 *
	 * @param ann Usage-site constraint annotation.
	 * @param host Discovered `@ConstraintArg` host.
	 * @param raw Argument value for [ConstraintArgHost.parameterName], or `null` if absent.
	 * @param valueType Subject type (or RequiredWhen gate type) for typed-literal checks.
	 * @param subjectNode Symbol for diagnostics; falls back to [ann] when `null`.
	 */
	private fun verifyHost(
		ann: KSAnnotation,
		host: ConstraintArgHost,
		raw: Any?,
		valueType: KSType,
		subjectNode: KSNode?,
	) {
		val site = subjectNode ?: ann
		val label = "@${ann.shortName.asString()}.${host.parameterName}"
		val prefix = host.message.ifBlank { label }
		val targetLabel = when (host.target) {
			ConstraintArgTarget.VALUE -> AnnotationAttrs.ConstraintPayload.VALUE
			ConstraintArgTarget.ELEMENT -> ConstraintArgTarget.ELEMENT.name.lowercase(Locale.ROOT)
		}

		when (host.target) {
			ConstraintArgTarget.VALUE -> verifyValueTarget(host, raw, valueType, prefix, site)
			ConstraintArgTarget.ELEMENT -> {
				if (!isCollectionArg(raw) && raw !is String) {
					// Scalar string treated as single-element for ELEMENT (rare); collections expected.
					if (raw != null) {
						logger.error(
							"$prefix has target=ELEMENT but the argument is not a collection " +
								"(got ${raw::class.simpleName}).",
							site,
						)
						return
					}
				}
				// TYPED_LITERAL elements are typed against the collection/array/map *element*
				// (or map value), not the container itself.
				val elementType = peelElementType(valueType) ?: valueType
				verifyElementTarget(host, raw, elementType, prefix, targetLabel, site)
			}
		}
	}

	/**
	 * Applies [ConstraintArgHost.kinds] to whole-argument [raw] ([ConstraintArgTarget.VALUE]).
	 *
	 * Side effects: may emit [KSPLogger.error] when a kind fails.
	 *
	 * @param host Discovered `@ConstraintArg` host.
	 * @param raw Argument value, or `null` if absent.
	 * @param valueType Subject type for [ConstraintArgKind.TYPED_LITERAL].
	 * @param prefix Diagnostic message prefix.
	 * @param site Symbol for diagnostics.
	 */
	private fun verifyValueTarget(
		host: ConstraintArgHost,
		raw: Any?,
		valueType: KSType,
		prefix: String,
		site: KSNode,
	) {
		for (kind in host.kinds) {
			val message = when (kind) {
				ConstraintArgKind.TYPED_LITERAL ->
					ConstraintArgKindValueRules.typedLiteral(raw, valueType, prefix)
				else -> ConstraintArgKindValueRules.byKind[kind]?.invoke(raw, prefix)
			} ?: continue
			logger.error(message, site)
			return
		}
	}

	/**
	 * Applies [ConstraintArgHost.kinds] to each element of [raw] ([ConstraintArgTarget.ELEMENT]).
	 *
	 * Side effects: may emit [KSPLogger.error] when a kind fails or when
	 * [ConstraintArgKind.NON_NEGATIVE] / [ConstraintArgKind.POSITIVE] is paired with ELEMENT.
	 *
	 * @param host Discovered `@ConstraintArg` host.
	 * @param raw Argument value (collection, scalar string, or `null`).
	 * @param valueType Subject type for [ConstraintArgKind.TYPED_LITERAL].
	 * @param prefix Diagnostic message prefix.
	 * @param targetLabel Human label for element (`value` / `element`) in messages.
	 * @param site Symbol for diagnostics.
	 */
	private fun verifyElementTarget(
		host: ConstraintArgHost,
		raw: Any?,
		valueType: KSType,
		prefix: String,
		targetLabel: String,
		site: KSNode,
	) {
		val elements = ConstraintArgKindElementRules.elementValues(raw)
		for (kind in host.kinds) {
			val message = when (kind) {
				ConstraintArgKind.TYPED_LITERAL ->
					ConstraintArgKindElementRules.typedLiteral(elements, valueType, prefix)
				else -> ConstraintArgKindElementRules.byKind[kind]
					?.invoke(elements, raw, prefix, targetLabel)
			} ?: continue
			logger.error(message, site)
			return
		}
	}

	/**
	 * Builds name → value map for [ann] arguments.
	 *
	 * Named arguments use KSP name. Positional (unnamed) arguments bind to next
	 * primary-constructor parameter not yet present — declaration order is contract.
	 *
	 * Side effects: none.
	 *
	 * @param ann Usage-site constraint annotation.
	 * @return Argument map used by host verification.
	 */
	private fun extractArgs(ann: KSAnnotation): Map<String, Any?> {
		val out = LinkedHashMap<String, Any?>()
		val decl = ann.annotationType.resolve().declaration as? KSClassDeclaration
		val ctorParams = decl?.primaryConstructor?.parameters
			?.mapNotNull { it.name?.asString() }
			.orEmpty()

		for (arg in ann.arguments) {
			val explicit = arg.name?.asString()
			if (explicit != null) {
				out[explicit] = arg.value
				continue
			}
			val positional = ctorParams.firstOrNull { it !in out } ?: continue
			out[positional] = arg.value
		}
		return out
	}

	/**
	 * Whether [raw] is a List or Array argument value.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value.
	 * @return `true` when [raw] is [List] or [Array].
	 */
	private fun isCollectionArg(raw: Any?): Boolean =
		ConstraintArgKindElementRules.isCollectionArg(raw)

	/**
	 * Peels the element (or map-value) type from a collection / array / map subject.
	 *
	 * Used so [ConstraintArgTarget.ELEMENT] + [ConstraintArgKind.TYPED_LITERAL] type-checks
	 * against the nested leaf, not the container (`List`, `Array`, `Map`).
	 *
	 * Side effects: none.
	 *
	 * @param subject Annotated property / parameter type.
	 * @return Nested type when [subject] is array/iterable/map; otherwise `null`.
	 */
	private fun peelElementType(subject: KSType): KSType? {
		return when {
			TypeClassification.isArray(subject) || TypeClassification.isIterable(subject) ->
				subject.arguments.firstOrNull()?.type?.resolve()
			
			TypeClassification.isMap(subject) ->
				subject.arguments.getOrNull(1)?.type?.resolve()
			
			else -> null
		}
	}
}

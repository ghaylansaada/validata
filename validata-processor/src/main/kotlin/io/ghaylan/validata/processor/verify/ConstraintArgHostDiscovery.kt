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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.KspEnumValues
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget

/**
 * Discovers `@ConstraintArg` rules from the constraint annotation declaration and/or metadata.
 *
 * Twin of [PropertyRefHostDiscovery]: authors mark parameters; tooling reads markers — never
 * hard-coded annotation simple names. Annotation params preferred; metadata is the legacy fallback.
 * Keep aligned with IntelliJ `ConstraintArgAttributeDiscovery`. Repeatable markers expand to one
 * [ConstraintArgHost] per `@ConstraintArg` (and each `@ConstraintArgs` entry).*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintArgHostDiscovery {

	/**
	 * Collects `@ConstraintArg` hosts — annotation declaration preferred, else [metadata].
	 *
	 * When [hostCache] is provided, results are keyed by annotation (or metadata) type FQCN for
	 * the round so repeated usages skip property walks.
	 *
	 * Side effects: may insert into [hostCache].
	 *
	 * @param annotationDecl The `@Constraint` annotation class (use-site type), or `null`.
	 * @param metadata Resolved metadata class, or `null`.
	 * @param hostCache Optional round-scoped cache keyed by declaration FQCN.
	 * @return Discovered hosts; empty when neither declaration carries markers.
	 */
	fun discoverHosts(
		annotationDecl: KSClassDeclaration?,
		metadata: KSClassDeclaration?,
		hostCache: MutableMap<String, List<ConstraintArgHost>>? = null,
	): List<ConstraintArgHost> {
		val cacheKey = annotationDecl?.qualifiedName?.asString()
			?: metadata?.qualifiedName?.asString()
		
		if (hostCache != null && cacheKey != null && hostCache.containsKey(cacheKey)) {
			return hostCache.getValue(cacheKey)
		}
		
		val fromAnnotation = annotationDecl?.let { discoverOnDeclaration(it) }.orEmpty()
		
		val hosts = fromAnnotation.ifEmpty {
			metadata?.let { discoverOnDeclaration(it) }.orEmpty()
		}
		
		if (hostCache != null && cacheKey != null) {
			hostCache[cacheKey] = hosts
		}
		
		return hosts
	}

	/**
	 * Collects `@ConstraintArg` hosts on [metadata]'s primary constructor and properties.
	 *
	 * Side effects: none.
	 *
	 * @param metadata Metadata class declaration.
	 * @return Hosts for [metadata].
	 */
	fun discoverHosts(metadata: KSClassDeclaration): List<ConstraintArgHost> =
		discoverOnDeclaration(metadata)

	/**
	 * Scans constructor parameters then properties for `@ConstraintArg` / `@ConstraintArgs`.
	 *
	 * Side effects: none.
	 *
	 * @param decl Annotation or metadata declaration.
	 * @return Hosts in discovery order.
	 */
	private fun discoverOnDeclaration(decl: KSClassDeclaration): List<ConstraintArgHost> {
		val out = ArrayList<ConstraintArgHost>()
		val fromCtor = HashSet<String>()
		decl.primaryConstructor?.parameters?.forEach { param ->
			val name = param.name?.asString() ?: return@forEach
			val hosts = parseHosts(param.annotations, name)
			if (hosts.isNotEmpty()) {
				fromCtor += name
				out += hosts
			}
		}
		decl.getAllProperties().forEach { prop ->
			val name = prop.simpleName.asString()
			// Kotlin annotation params appear as both ctor params and properties — skip duplicates.
			if (name in fromCtor) return@forEach
			out += parseHosts(prop.annotations, name)
		}
		return out
	}

	/**
	 * Parses all `@ConstraintArg` / `@ConstraintArgs` markers on [parameterName].
	 *
	 * Side effects: none.
	 *
	 * @param annotations Annotations on the parameter / property.
	 * @param parameterName Kotlin parameter name.
	 * @return Zero or more hosts for [parameterName].
	 */
	private fun parseHosts(annotations: Sequence<KSAnnotation>, parameterName: String): List<ConstraintArgHost> {
		val out = ArrayList<ConstraintArgHost>()
		for (ann in annotations) {
			val fq = AnnotationFqcn.of(ann) ?: continue
			when (fq) {
				ProcessorFqns.CONSTRAINT_ARG -> parseOne(ann, parameterName)?.let(out::add)
				ProcessorFqns.CONSTRAINT_ARGS -> {
					val nested = ann.arguments
						.firstOrNull {
							it.name?.asString() == AnnotationAttrs.ConstraintArgs.VALUE || it.name == null
						}
						?.value
					val items: List<*> = when (nested) {
						is List<*> -> nested
						null -> emptyList<Any>()
						else -> listOf(nested)
					}
					for (item in items) {
						val nestedAnn = item as? KSAnnotation ?: continue
						parseOne(nestedAnn, parameterName)?.let(out::add)
					}
				}
			}
		}
		return out
	}

	/**
	 * Builds one [ConstraintArgHost] from a single `@ConstraintArg` use-site.
	 *
	 * Side effects: none.
	 *
	 * @param argAnn `@ConstraintArg` annotation.
	 * @param parameterName Kotlin parameter that carries the marker.
	 * @return Host, or `null` when kinds are empty / unparsable.
	 */
	private fun parseOne(argAnn: KSAnnotation, parameterName: String): ConstraintArgHost? {
		val values = linkedMapOf<String, Any?>()
		for (arg in argAnn.arguments) {
			val name = arg.name?.asString() ?: continue
			values[name] = arg.value
		}
		// Positional vararg kinds may appear under null / value when kinds is absent.
		if (!values.containsKey(AnnotationAttrs.ConstraintArg.KINDS) &&
			values.containsKey(AnnotationAttrs.ConstraintArg.VALUE)
		) {
			// keep value for kinds parse below
		} else if (!values.containsKey(AnnotationAttrs.ConstraintArg.KINDS)) {
			for (arg in argAnn.arguments) {
				if (arg.name == null) {
					values[AnnotationAttrs.ConstraintArg.KINDS] = arg.value
					break
				}
			}
		}

		val kinds = parseKinds(
			values[AnnotationAttrs.ConstraintArg.KINDS] ?: values[AnnotationAttrs.ConstraintArg.VALUE],
		)
		if (kinds.isEmpty()) return null
		val target = parseTarget(values[AnnotationAttrs.ConstraintArg.TARGET]) ?: ConstraintArgTarget.VALUE
		val message = (values[AnnotationAttrs.ConstraintArg.MESSAGE] as? String).orEmpty()
		return ConstraintArgHost(
			parameterName = parameterName,
			kinds = kinds,
			target = target,
			message = message,
		)
	}

	/**
	 * Parses `kinds` (or positional vararg) into [ConstraintArgKind] constants.
	 *
	 * Side effects: none.
	 *
	 * @param raw KSP `kinds` / positional value; may be null.
	 * @return Parsed kinds; unknown entry names are dropped.
	 */
	private fun parseKinds(raw: Any?): Set<ConstraintArgKind> {
		val items: List<*> = when (raw) {
			null -> return emptySet()
			is List<*> -> raw
			else -> listOf(raw)
		}
		return items.mapNotNull { item -> KspEnumValues.parse<ConstraintArgKind>(item) }.toSet()
	}

	/**
	 * Parses `target` into a [ConstraintArgTarget].
	 *
	 * Side effects: none.
	 *
	 * @param raw KSP target argument; may be null.
	 * @return Target, or `null` when absent / unknown.
	 */
	private fun parseTarget(raw: Any?): ConstraintArgTarget? =
		KspEnumValues.parse(raw)
}

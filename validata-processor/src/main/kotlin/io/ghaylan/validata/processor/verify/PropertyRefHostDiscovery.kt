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
import io.ghaylan.validata.processor.model.PropertyRefs
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope

/**
 * Discovers property-path hosts by reading `@PropertyRef` on annotation / metadata declarations.
 *
 * Authors mark parameters; tooling reads markers — never hard-coded argument-name or annotation
 * simple-name tables. Annotation params are preferred over metadata (Option 2 dual-read).
 * Keep aligned with IntelliJ `PropertyRefAttributeDiscovery`.*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefHostDiscovery {
	
	/**
	 * Pulls non-blank path strings from [ann] for hosts discovered on [metadata] / the annotation type.
	 *
	 * Side effects: none.
	 *
	 * @param ann Usage-site constraint annotation (e.g. `@Compare(ref = "password")`).
	 * @param metadata Metadata class from `@Constraint`, or `null` when unresolved.
	 * @param hostCache Optional round-scoped cache passed to [discoverHosts].
	 * @return Sibling and element paths with merged compatibility; empty when no hosts.	 
	 */
	fun extractRefs(
		ann: KSAnnotation,
		metadata: KSClassDeclaration?,
		hostCache: MutableMap<String, List<PropertyRefHost>>? = null,
	): PropertyRefs {
		val annotationDecl = ann.annotationType.resolve().declaration as? KSClassDeclaration
		val hosts = discoverHosts(annotationDecl, metadata, hostCache)
		if (hosts.isEmpty()) return PropertyRefs()
		val byName = hosts.associateBy { it.parameterName }
		val siblings = mutableListOf<String>()
		val elements = mutableListOf<String>()
		val siblingKinds = mutableListOf<PropertyRefCompatibilityKind>()
		val boundNames = mutableSetOf<String>()
		val ctorParams = annotationDecl?.primaryConstructor?.parameters?.mapNotNull { it.name?.asString() }.orEmpty()
		
		for (arg in ann.arguments) {
			val name = arg.name?.asString()
				?: ctorParams.firstOrNull { it !in boundNames }
				?: continue
			boundNames += name
			val host = byName[name]
				?: continue
			val paths = stringPaths(arg.value)
			if (paths.isEmpty()) continue
			if (host.elementScope) {
				elements += paths
			}
			else {
				siblings += paths
				siblingKinds += host.compatibility
			}
		}
		
		return PropertyRefs(
			siblings = siblings,
			elements = elements,
			compatibilityKind = mergeSiblingKinds(siblingKinds),
		)
	}
	
	/**
	 * Collects `@PropertyRef` hosts — annotation declaration preferred, else [metadata].
	 *
	 * When [hostCache] is provided, results are keyed by annotation (or metadata) type FQCN for
	 * the round so repeated usages of the same constraint type skip property walks.
	 *
	 * Side effects: may insert into [hostCache].
	 *
	 * @param annotationDecl Constraint annotation class, or `null`.
	 * @param metadata Metadata class, or `null`.
	 * @param hostCache Optional round-scoped cache keyed by declaration FQCN.
	 * @return Discovered hosts; empty when neither declaration carries markers.	 
	 */
	fun discoverHosts(
		annotationDecl: KSClassDeclaration?,
		metadata: KSClassDeclaration?,
		hostCache: MutableMap<String, List<PropertyRefHost>>? = null,
	): List<PropertyRefHost> {
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
	 * Collects `@PropertyRef` hosts on [metadata]'s primary constructor and properties.
	 *
	 * Markers on the same name are merged so a bare defaulted copy cannot hide a fully-specified one.
	 *
	 * Side effects: none.
	 *
	 * @param metadata Metadata class declaration.
	 * @return Merged hosts for [metadata].	 
	 */
	fun discoverHosts(metadata: KSClassDeclaration): List<PropertyRefHost> =
		discoverOnDeclaration(metadata)
	
	/**
	 * Scans constructor parameters then properties for `@PropertyRef`.
	 *
	 * Side effects: none.
	 *
	 * @param decl Annotation or metadata declaration.
	 * @return Hosts keyed by parameter name (merged on collision).	 
	 */
	private fun discoverOnDeclaration(decl: KSClassDeclaration): List<PropertyRefHost> {
		val byName = linkedMapOf<String, PropertyRefHost>()
		fun put(host: PropertyRefHost) {
			byName[host.parameterName] = mergeHosts(byName[host.parameterName], host)
		}
		decl.primaryConstructor?.parameters?.forEach { param ->
			val name = param.name?.asString()
				?: return@forEach
			parseHost(param.annotations, name)?.let(::put)
		}
		decl.getAllProperties().forEach { prop ->
				val name = prop.simpleName.asString()
				parseHost(prop.annotations, name)?.let(::put)
			}
		return byName.values.toList()
	}
	
	/**
	 * Unions two hosts for the same parameter name.
	 *
	 * Side effects: none.
	 *
	 * @param existing Prior host, or `null`.
	 * @param incoming Newly parsed host.
	 * @return Merged host (element scope ORs; compatibility prefers stricter).	 
	 */
	private fun mergeHosts(
		existing: PropertyRefHost?,
		incoming: PropertyRefHost
	): PropertyRefHost {
		if (existing == null) return incoming
		return PropertyRefHost(
			parameterName = incoming.parameterName,
			elementScope = existing.elementScope || incoming.elementScope,
			compatibility = mergeSiblingKinds(listOf(existing.compatibility, incoming.compatibility)),
		)
	}
	
	/**
	 * Builds a host from `@PropertyRef` on [parameterName], or `null` when the marker is absent.
	 *
	 * Side effects: none.
	 *
	 * @param annotations Annotations on the parameter / property.
	 * @param parameterName Kotlin parameter name.
	 * @return Host, or `null`.	 
	 */
	private fun parseHost(
		annotations: Sequence<KSAnnotation>,
		parameterName: String
	): PropertyRefHost? {
		val propertyRef = annotations.firstOrNull {
			AnnotationFqcn.isA(it, ProcessorFqns.PROPERTY_REF)
		}
			?: return null
		val values = linkedMapOf<String, Any?>()
		for (arg in propertyRef.arguments) {
			val name = arg.name?.asString()
				?: continue
			values[name] = arg.value
		}
		val scope = KspEnumValues.parse<PropertyRefScope>(values[AnnotationAttrs.PropertyRef.SCOPE])
			?: PropertyRefScope.SIBLING
		val compatibility = KspEnumValues.parse<PropertyRefCompatibilityKind>(
			values[AnnotationAttrs.PropertyRef.COMPATIBILITY],
		)
			?: PropertyRefCompatibilityKind.NONE
		
		return PropertyRefHost(
			parameterName = parameterName,
			elementScope = scope == PropertyRefScope.ELEMENT,
			compatibility = compatibility,
		)
	}
	
	/**
	 * Normalizes a path argument to non-blank strings (scalar or `List`).
	 *
	 * Side effects: none.
	 *
	 * @param value Raw KSP argument value; may be null.
	 * @return Non-blank path strings; empty when none.	 
	 */
	private fun stringPaths(value: Any?): List<String> =
		when (value) {
			is String -> listOf(value).filter { it.isNotBlank() }
			is List<*> -> value.filterIsInstance<String>().filter { it.isNotBlank() }
			else -> emptyList()
		}
	
	/**
	 * Merges sibling [PropertyRefCompatibilityKind] values into one kind for [PropertyRefs].
	 *
	 * When multiple distinct non-[PropertyRefCompatibilityKind.NONE] kinds conflict: prefers
	 * [PropertyRefCompatibilityKind.COMPARABLE_FAMILY] if present, otherwise the
	 * lexicographically smallest kind name (stable). Conflicting markers on one constraint
	 * are an authoring error; verify/diagnostics may still report incompatibilities later.
	 *
	 * Side effects: none.
	 *
	 * @param kinds Compatibility kinds from sibling hosts.
	 * @return Merged kind for [PropertyRefs].	 
	 */
	private fun mergeSiblingKinds(kinds: List<PropertyRefCompatibilityKind>): PropertyRefCompatibilityKind {
		val nonNone = kinds.filter { it != PropertyRefCompatibilityKind.NONE }.distinct()
		return when {
			nonNone.isEmpty() -> PropertyRefCompatibilityKind.NONE
			nonNone.size == 1 -> nonNone.single()
			PropertyRefCompatibilityKind.COMPARABLE_FAMILY in nonNone -> PropertyRefCompatibilityKind.COMPARABLE_FAMILY
			else -> nonNone.minBy { it.name }
		}
	}
}

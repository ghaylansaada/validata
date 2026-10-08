/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.discovery.propertyref

import io.ghaylan.validata.intellij.model.PropertyRefHostAttribute
import io.ghaylan.validata.intellij.model.PropertyRefMetadataHost
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Bridges a usage-site annotation argument (or the annotation as a whole) to a discovered
 * `@PropertyRef` host attribute.
 *
 * ## Why it exists
 *
 * Path inspections and completions start from a string literal inside e.g.
 * `@Compare(ref = "…", operation = Compare.Operation.EQ)`. They need to know whether that argument is a property-path host,
 * and with which [PropertyRefScope] / [PropertyRefCompatibilityKind]. This matcher answers
 * that using [PropertyRefAttributeDiscovery] only — never a bootstrap allowlist of annotation
 * or parameter names.
 *
 * ## How it fits the plugin
 *
 * Sits between PSI usage sites and the discovery / analysis layers. Callers pass a
 * [KtAnnotationEntry] (+ optional [KtValueArgument]); they receive a
 * [PropertyRefHostAttribute] suitable for inspections, or `null` when discovery finds nothing.
 *
 * ## What it is NOT
 *
 * - Not a path resolver (does not walk object schemas or validate path strings).
 * - Not the KSP twin — compile-time host extraction lives in processor
 *   `PropertyRefHostDiscovery`; this only matches IDE usage sites to discovered hosts.
 * - Not a cache; it always goes through [PropertyRefAttributeDiscovery.discoverHosts]
 *   (which may itself hit [ConstraintDiscoveryCache]).*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefAnnotationMatcher {
	
	/**
	 * Matches [argument] on [annotation] against discovered `@PropertyRef` hosts.
	 *
	 * Resolves the annotation declaration, discovers hosts, maps the value argument to a
	 * parameter name (named arg, primary-constructor index, or host-order fallback), then
	 * returns the host whose [PropertyRefMetadataHost.parameterName] matches.
	 *
	 * Called from path-aware inspections / completions when the caret or problem element is a
	 * string argument on a constraint annotation.
	 *
	 * @param annotation Usage-site constraint annotation (e.g. `@Compare(ref = "x", operation = Compare.Operation.EQ)`).
	 * @param argument The value argument that owns the string literal under analysis.
	 * @return A [PropertyRefHostAttribute] describing that host, or `null` when the annotation
	 *   short name is missing, discovery returns no hosts, the argument cannot be mapped to a
	 *   parameter name, or no host uses that name.	 
	 */
	fun matchAttribute(
		annotation: KtAnnotationEntry,
		argument: KtValueArgument,
	): PropertyRefHostAttribute? {
		val declaration = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
		val annotationClass = declaration as? KtClass
		val fqName = declaration?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
		val shortName = annotation.shortName?.asString()
			?: annotationClass?.name
			?: return null
		val discovered = PropertyRefAttributeDiscovery.discoverHosts(annotation)
		if (discovered.isEmpty()) return null
		val paramName = when {
			annotationClass != null -> resolveParameterName(annotation, argument, annotationClass)
			else -> resolveParameterNameFromHosts(annotation, argument, discovered)
		}
			?: return null
		val host = discovered.find { it.parameterName == paramName }
			?: return null
		return PropertyRefHostAttribute(
			annotationFqName = fqName
				?: "discovered.$shortName",
			parameterName = host.parameterName,
			scope = host.scope,
			compatibilityKind = host.compatibilityKind,
		)
	}
	
	/**
	 * Resolves subject-type / annotation-level sibling compatibility without a specific value
	 * argument.
	 *
	 * Picks [PropertyRefAttributeDiscovery.primarySiblingCompatibility] when present, then
	 * prefers a SIBLING host carrying that kind; otherwise falls back to the first discovered
	 * host. Used when inspections need “the” compatibility rule for the annotation (e.g.
	 * comparing the annotated subject to a path) rather than attributing one string arg.
	 *
	 * @param annotation Usage-site constraint annotation.
	 * @return A synthetic [PropertyRefHostAttribute] carrying the chosen compatibility, or
	 *   `null` when the short name is missing or discovery finds no hosts. Compatibility may be
	 *   [PropertyRefCompatibilityKind.NONE] when no sibling host declares a stronger kind.	 
	 */
	fun matchAnnotationCompatibility(
		annotation: KtAnnotationEntry,
	): PropertyRefHostAttribute? {
		val shortName = annotation.shortName?.asString()
			?: return null
		val hosts = PropertyRefAttributeDiscovery.discoverHosts(annotation)
		if (hosts.isEmpty()) return null
		val fqName = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
			?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
			?: "discovered.$shortName"
		val kind = PropertyRefAttributeDiscovery.primarySiblingCompatibility(hosts)
			?: PropertyRefCompatibilityKind.NONE
		val host = hosts.firstOrNull {
			it.scope == PropertyRefScope.SIBLING && it.compatibilityKind == kind
		}
			?: hosts.first()
		return PropertyRefHostAttribute(
			annotationFqName = fqName,
			parameterName = host.parameterName,
			scope = host.scope,
			compatibilityKind = kind,
		)
	}
	
	/**
	 * Maps [argument] to the annotation value-parameter name it binds to.
	 *
	 * Named arguments win (`property = "…"` → `"property"`). Otherwise the positional index
	 * into [annotation]'s value-argument list is mapped to
	 * [annotationClass]'s primary-constructor value-parameter names — matching Kotlin call
	 * conventions for annotation usage.
	 *
	 * @param annotation Usage-site annotation whose argument list contains [argument].
	 * @param argument Argument whose parameter name is needed.
	 * @param annotationClass Resolved Kotlin declaration of the annotation type (must expose
	 *   a primary constructor with matching parameter order).
	 * @return Parameter name, or `null` when [argument] is not in the list, there is no primary
	 *   constructor, or the index is out of range.	 
	 */
	fun resolveParameterName(
		annotation: KtAnnotationEntry,
		argument: KtValueArgument,
		annotationClass: KtClass,
	): String? {
		argument.getArgumentName()?.asName?.asString()
			?.let { return it }
		val index = annotation.valueArguments.indexOf(argument)
		if (index < 0) return null
		val params = annotationClass.primaryConstructor?.valueParameters
			?: return null
		return params.getOrNull(index)?.name
	}
	
	/**
	 * Maps [argument] to a host parameter name when the annotation [KtClass] cannot be typed
	 * but discovery already listed hosts.
	 *
	 * Named args still win. For positional args: a single-host annotation with index `0` uses
	 * that host’s name; otherwise hosts are indexed in discovery order (same merge order as
	 * [PropertyRefAttributeDiscovery]). Used for light-platform tests and binary annotations
	 * where [resolveParameterName] has no constructor PSI.
	 *
	 * @param annotation Usage-site annotation.
	 * @param argument Argument under analysis.
	 * @param hosts Non-empty list from [PropertyRefAttributeDiscovery.discoverHosts].
	 * @return Parameter name, or `null` when the argument is not in the list or the index has
	 *   no corresponding host (except the single-host / index-0 special case).	 
	 */
	private fun resolveParameterNameFromHosts(
		annotation: KtAnnotationEntry,
		argument: KtValueArgument,
		hosts: List<PropertyRefMetadataHost>,
	): String? {
		argument.getArgumentName()?.asName?.asString()
			?.let { return it }
		val index = annotation.valueArguments.indexOf(argument)
		if (index < 0) return null
		if (hosts.size == 1 && index == 0) return hosts.single().parameterName
		return hosts.getOrNull(index)?.parameterName
	}
}

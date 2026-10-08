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

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import io.ghaylan.validata.intellij.contract.PropertyRefLibraryFqns
import io.ghaylan.validata.intellij.discovery.PsiClassLookup
import io.ghaylan.validata.intellij.discovery.cache.ConstraintDiscoveryCache
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery.collectKtHosts
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery.mergeHosts
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery.parseKtCompatibility
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery.parseKtScope
import io.ghaylan.validata.intellij.model.PropertyRefMetadataHost
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Discovers which annotation value-parameters are property-path hosts by reading
 * `@PropertyRef` markers on the user’s classpath (PSI).
 *
 * ## Why it exists
 *
 * Validata authors mark metadata (or annotation) parameters with `@PropertyRef(scope, …)`.
 * KSP (`PropertyRefHostDiscovery` in **validata-processor**) reads those markers at compile
 * time; the IntelliJ plugin must apply the **same** semantics in the editor so path
 * inspections and completions light up for builtins and consumer constraints without a
 * hard-coded FQCN / argument-name table.
 *
 * ## How it fits the plugin
 *
 * Entry point for the `discovery.propertyref` package. [PropertyRefAnnotationMatcher], path
 * analysis, and diagnostics call [discoverHosts] / helpers here. Results are modeled as
 * [PropertyRefMetadataHost] and may be memoized by [ConstraintDiscoveryCache] when an FQCN
 * is available.
 *
 * ## Discovery order (aligned with KSP Option 2)
 *
 * 1. `@PropertyRef` on the constraint **annotation** declaration’s parameters / properties
 * 2. Else the same markers on the conventional generated `{Name}Constraint` metadata class
 *    (same package / same file when present)
 *
 * Empty list when markers are missing or unresolved — callers must **not** invent hosts from
 * FQCN allowlists.
 *
 * ## What it is NOT
 *
 * - Not a path validator (does not resolve `"password"` against a schema).
 * - Not the KSP processor; it only mirrors that discovery contract over IntelliJ PSI.
 * - Not a registry of Validata builtin annotations.*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefAttributeDiscovery {


	/**
	 * Discovers `@PropertyRef` hosts for the annotation class resolved from [annotation].
	 *
	 * Resolves the declaration, then either returns [discoverHostsUncached] directly (no FQCN)
	 * or goes through [ConstraintDiscoveryCache.hosts] when [annotationFqName] is available so
	 * repeated inspections within one PSI stamp reuse the list.
	 *
	 * @param annotation Usage-site constraint annotation (e.g. `@Compare(ref = "…", operation = Compare.Operation.EQ)`).
	 * @return Discovered hosts for that annotation type; empty when the declaration cannot be
	 *   resolved or no `@PropertyRef` markers are found (including failed metadata fallback).
	 */
	fun discoverHosts(annotation: KtAnnotationEntry): List<PropertyRefMetadataHost> {
		val declaration = resolveAnnotationDeclaration(annotation) ?: return emptyList()
		val fqName = annotationFqName(declaration) ?: return discoverHostsUncached(declaration)
		val project = annotation.project
		return ConstraintDiscoveryCache.getInstance(project).hosts(fqName) {
			discoverHostsUncached(declaration)
		}
	}

	/**
	 * Uncached host discovery from a resolved annotation declaration.
	 *
	 * Dispatches on [KtClass] vs [PsiClass] so Kotlin sources and binary / Java annotations
	 * both work. Called from [discoverHosts] (cache miss) and from tests that need a fresh
	 * walk without the project service.
	 *
	 * @param declaration Annotation type as [KtClass], [PsiClass], or other [PsiElement].
	 * @return Hosts from annotation params or `{Name}Constraint` fallback; empty for unknown
	 *   declaration kinds or when neither surface carries `@PropertyRef`.
	 */
	fun discoverHostsUncached(declaration: PsiElement): List<PropertyRefMetadataHost> =
		when (declaration) {
			is KtClass -> discoverFromKtAnnotationClass(declaration)
			is PsiClass -> discoverFromPsiAnnotationClass(declaration)
			else -> emptyList()
		}

	/**
	 * Primary non-[PropertyRefCompatibilityKind.NONE] sibling compatibility for subject-type
	 * checks across a discovered host list.
	 *
	 * Collects kinds from hosts with [PropertyRefScope.SIBLING], ignoring `NONE`. If
	 * [PropertyRefCompatibilityKind.COMPARABLE_FAMILY] appears among them it wins (ordering
	 * constraints are stricter than equality-style same-kind); otherwise the first remaining
	 * kind is returned. Used by [PropertyRefAnnotationMatcher.matchAnnotationCompatibility]
	 * and sibling subject comparisons.
	 *
	 * @param hosts Hosts from [discoverHosts] / [discoverHostsUncached] (may mix scopes).
	 * @return The chosen sibling kind, or `null` when no sibling host declares a non-`NONE`
	 *   kind (callers typically treat that as “path existence only”).
	 */
	fun primarySiblingCompatibility(hosts: List<PropertyRefMetadataHost>): PropertyRefCompatibilityKind? {
		val siblingKinds = hosts
			.filter { it.scope == PropertyRefScope.SIBLING }
			.map { it.compatibilityKind }
			.filter { it != PropertyRefCompatibilityKind.NONE }
		return when {
			siblingKinds.isEmpty() -> null
			PropertyRefCompatibilityKind.COMPARABLE_FAMILY in siblingKinds ->
				PropertyRefCompatibilityKind.COMPARABLE_FAMILY
			else -> siblingKinds.first()
		}
	}

	/**
	 * Builds a user-facing diagnostic when a `@Constraint` annotation has no discoverable
	 * `@PropertyRef` markers and no generated `{Name}Constraint` class is on the classpath yet.
	 *
	 * Only applies to Kotlin source declarations ([KtClass]). Returns `null` when the type is
	 * not a `@Constraint`, when hosts already resolve, or when the convention metadata class
	 * is present (even if empty of markers — that is a different problem).
	 *
	 * @param annotation Usage-site annotation to diagnose.
	 * @return Hint string mentioning `{ShortName}Constraint` and KSP / classpath, or `null`
	 *   when this diagnostic does not apply.
	 */
	fun unresolvedConstraintMetadataMessage(annotation: KtAnnotationEntry): String? {
		val declaration = resolveAnnotationDeclaration(annotation) as? KtClass ?: return null
		if (findKtConstraintAnnotation(declaration) == null) return null
		if (collectKtHosts(declaration).isNotEmpty()) return null
		if (resolveConventionMetadataClass(declaration) != null) return null
		val shortName = annotation.shortName?.asString() ?: declaration.name ?: "constraint"
		return "Cannot resolve generated metadata for @$shortName (${shortName}Constraint) — " +
			"ensure validata-processor KSP ran and the library is on the module classpath."
	}

	/**
	 * Kotlin-source path: hosts on the annotation class, else on `{Name}Constraint`.
	 *
	 * @param annotationClass Resolved Kotlin annotation declaration.
	 * @return Merged hosts from the first non-empty surface; empty if both fail.
	 */
	private fun discoverFromKtAnnotationClass(annotationClass: KtClass): List<PropertyRefMetadataHost> {
		val fromAnnotationParams = collectKtHosts(annotationClass)
		if (fromAnnotationParams.isNotEmpty()) return fromAnnotationParams
		resolveConventionMetadataClass(annotationClass)?.let { return collectKtHosts(it) }
		// Annotation may be Kotlin source while `{Name}Constraint` is binary-only.
		val annFq = annotationClass.fqName?.asString()
		val metaFq = annFq?.substringBeforeLast('.', "")
			?.let { pkg -> if (pkg.isBlank()) "${annotationClass.name}Constraint" else "$pkg.${annotationClass.name}Constraint" }
			?: return emptyList()
		val psiMeta = JavaPsiFacade.getInstance(annotationClass.project)
			.findClass(metaFq, annotationClass.resolveScope)
			?: return emptyList()
		return collectPsiHosts(psiMeta)
	}

	/**
	 * Java / binary path: hosts on the annotation [PsiClass], else on `{Name}Constraint`.
	 *
	 * @param annotationClass Resolved PSI annotation class (stubs or sources).
	 * @return Merged hosts from the first non-empty surface; empty if both fail.
	 */
	private fun discoverFromPsiAnnotationClass(annotationClass: PsiClass): List<PropertyRefMetadataHost> {
		val fromAnnotationParams = collectPsiHosts(annotationClass)
		if (fromAnnotationParams.isNotEmpty()) return fromAnnotationParams
		val metadataClass = resolveConventionMetadataClass(annotationClass) ?: return emptyList()
		return collectPsiHosts(metadataClass)
	}

	/**
	 * Finds the `@Constraint` meta-annotation on a Kotlin annotation declaration.
	 *
	 * Matches by resolved FQCN ([PropertyRefLibraryFqns.CONSTRAINT]) or, when resolution fails
	 * (common in light tests), by short name `"Constraint"` with a null FQCN.
	 *
	 * @param annotationClass Constraint annotation type in source.
	 * @return The `@Constraint` entry, or `null` if absent.
	 */
	private fun findKtConstraintAnnotation(annotationClass: KtClass): KtAnnotationEntry? {
		for (entry in annotationClass.annotationEntries) {
			val resolved = resolveAnnotationDeclaration(entry)
			val fq = resolved?.let { annotationFqName(it) }
			if (fq == PropertyRefLibraryFqns.CONSTRAINT) return entry
			if (entry.shortName?.asString() == "Constraint" && fq == null) {
				return entry
			}
		}
		return null
	}

	/**
	 * Locates the KSP-conventional `{AnnotationName}Constraint` metadata class for a Kotlin
	 * annotation.
	 *
	 * Prefers a same-file declaration, then short-name search in the annotation’s resolve
	 * scope. Does not invent the class — missing metadata yields `null` (see
	 * [unresolvedConstraintMetadataMessage]).
	 *
	 * @param annotationClass Annotation whose simple name drives `{Name}Constraint`.
	 * @return Metadata [KtClass], or `null` when the name is blank or no class is found.
	 */
	private fun resolveConventionMetadataClass(annotationClass: KtClass): KtClass? {
		val annName = annotationClass.name ?: return null
		val metaName = annName + "Constraint"
		annotationClass.containingKtFile.declarations
			.filterIsInstance<KtClass>()
			.firstOrNull { it.name == metaName }
			?.let { return it }
		val preferredPkg = annotationClass.fqName?.asString()?.substringBeforeLast('.', "")
		return pickBestClass(
			findClassesByShortName(
				annotationClass.project,
				metaName,
				annotationClass.resolveScope,
			),
			preferredPackage = preferredPkg,
		) as? KtClass
	}

	/**
	 * Locates `{AnnotationName}Constraint` for a PSI annotation class.
	 *
	 * Tries same-package FQCN via [JavaPsiFacade], then short-name search. Same convention as
	 * the Kotlin overload and as KSP-generated metadata placement.
	 *
	 * @param annotationClass Annotation PSI class.
	 * @return Metadata [PsiClass], or `null` when unresolved.
	 */
	private fun resolveConventionMetadataClass(annotationClass: PsiClass): PsiClass? {
		val annName = annotationClass.name ?: return null
		val metaName = annName + "Constraint"
		val fq = annotationClass.qualifiedName?.substringBeforeLast('.', "")?.let { pkg ->
			if (pkg.isBlank()) metaName else "$pkg.$metaName"
		} ?: metaName
		val preferredPkg = annotationClass.qualifiedName?.substringBeforeLast('.', "")
		JavaPsiFacade.getInstance(annotationClass.project)
			.findClass(fq, annotationClass.resolveScope)
			?.let { return it }
		return pickBestClass(
			findClassesByShortName(
				annotationClass.project,
				metaName,
				annotationClass.resolveScope,
			),
			preferredPackage = preferredPkg,
		) as? PsiClass
	}

	/**
	 * Collects and merges `@PropertyRef` hosts from a Kotlin metadata / annotation class.
	 *
	 * Scans primary-constructor value parameters and declared [KtProperty]s. Multiple markers
	 * for the same parameter name are [mergeHosts]-ed (Kotlin may repeat markers on param vs
	 * property depending on `@Target`) so a bare defaulted copy cannot hide a fully-specified
	 * one.
	 *
	 * @param metadata Annotation or `{Name}Constraint` class to scan.
	 * @return Hosts in encounter order (LinkedHashMap values); empty when nothing is marked.
	 */
	private fun collectKtHosts(metadata: KtClass): List<PropertyRefMetadataHost> {
		val byName = linkedMapOf<String, PropertyRefMetadataHost>()
		/**
		 * Merges [host] into [byName] under [PropertyRefMetadataHost.parameterName].
		 *
		 * @param host host to insert or merge
		 */
		fun put(host: PropertyRefMetadataHost) {
			byName[host.parameterName] = mergeHosts(byName[host.parameterName], host)
		}
		metadata.primaryConstructor?.valueParameters?.forEach { param ->
			val name = param.name ?: return@forEach
			parseKtHost(param.annotationEntries, name)?.let(::put)
		}
		metadata.declarations.filterIsInstance<KtProperty>().forEach { prop ->
			val name = prop.name ?: return@forEach
			parseKtHost(prop.annotationEntries, name)?.let(::put)
		}
		return byName.values.toList()
	}

	/**
	 * Collects and merges `@PropertyRef` hosts from a PSI metadata / annotation class.
	 *
	 * Scans the first constructor’s parameters, fields, and JavaBean-style `getX` methods
	 * (binary Kotlin annotations often expose elements as methods). Same merge rules as
	 * [collectKtHosts].
	 *
	 * @param metadata Annotation or `{Name}Constraint` PSI class.
	 * @return Merged hosts; empty when nothing is marked.
	 */
	private fun collectPsiHosts(metadata: PsiClass): List<PropertyRefMetadataHost> {
		val byName = linkedMapOf<String, PropertyRefMetadataHost>()
		/**
		 * Merges [host] into [byName] under [PropertyRefMetadataHost.parameterName].
		 *
		 * @param host host to insert or merge
		 */
		fun put(host: PropertyRefMetadataHost) {
			byName[host.parameterName] = mergeHosts(byName[host.parameterName], host)
		}
		metadata.constructors.firstOrNull()?.parameterList?.parameters?.forEach { param ->
			val name = param.name
			parsePsiHost(param, name)?.let(::put)
		}
		for (field in metadata.fields) {
			val name = field.name
			parsePsiHost(field, name)?.let(::put)
		}
		// Binary Kotlin annotations / data classes expose markers on getX, bare x, and
		// x$annotations / getX$annotations (same shapes as ConstraintArgAttributeDiscovery).
		for (method in metadata.methods) {
			val propName = psiHostParameterName(method.name) ?: continue
			parsePsiHost(method, propName)?.let(::put)
		}
		val defaultImpls = metadata.findInnerClassByName("DefaultImpls", false)
			?: metadata.qualifiedName?.let { fq ->
				JavaPsiFacade.getInstance(metadata.project)
					.findClass("$fq\$DefaultImpls", metadata.resolveScope)
			}
		defaultImpls?.methods?.forEach { method ->
			val propName = psiHostParameterName(method.name) ?: return@forEach
			parsePsiHost(method, propName)?.let(::put)
		}
		return byName.values.toList()
	}

	/**
	 * Derives the annotation / metadata parameter name from a PSI method name.
	 *
	 * Handles `property`, `getProperty`, `property$annotations`, and `getProperty$annotations`.
	 *
	 * @param methodName PSI method simple name
	 * @return parameter name, or `null` when [methodName] is not a host-bearing method shape
	 */
	private fun psiHostParameterName(methodName: String): String? {
		val base = if (methodName.endsWith("\$annotations")) {
			methodName.removeSuffix("\$annotations")
		} else {
			methodName
		}
		if (base.isEmpty()) return null
		return when {
			base.startsWith("get") && base.length > 3 ->
				base.removePrefix("get").replaceFirstChar { it.lowercaseChar() }
			else -> base
		}
	}

	/**
	 * Parses a single `@PropertyRef` from Kotlin annotation entries on one parameter / property.
	 *
	 * Accepts resolved FQCN [PropertyRefLibraryFqns.PROPERTY_REF] or unresolved short name
	 * `"PropertyRef"` (light tests). Reads `scope` and `compatibility` via [parseKtScope] /
	 * [parseKtCompatibility].
	 *
	 * @param annotations Annotations present on the parameter or property.
	 * @param parameterName Usage-site argument name this host binds to.
	 * @return A [PropertyRefMetadataHost], or `null` when no `@PropertyRef` is present.
	 */
	private fun parseKtHost(annotations: List<KtAnnotationEntry>, parameterName: String): PropertyRefMetadataHost? {
		val propertyRef = annotations.firstOrNull { entry ->
			val resolved = resolveAnnotationDeclaration(entry)
			val fq = resolved?.let { annotationFqName(it) }
			fq == PropertyRefLibraryFqns.PROPERTY_REF || (entry.shortName?.asString() == "PropertyRef" && fq == null)
		} ?: return null
		return PropertyRefMetadataHost(
			parameterName = parameterName,
			scope = parseKtScope(propertyRef),
			compatibilityKind = parseKtCompatibility(propertyRef),
		)
	}

	/**
	 * Parses `@PropertyRef` from a PSI modifier-list owner (parameter, field, or method).
	 *
	 * Requires a fully qualified annotation match via [PsiModifierListOwner.getAnnotation];
	 * unresolved short names are not accepted on the PSI path.
	 *
	 * @param owner PSI element carrying annotations.
	 * @param parameterName Derived host name (constructor param, field, or bean property).
	 * @return Host with scope / compatibility from annotation attributes, or `null` if the
	 *   marker is absent.
	 */
	private fun parsePsiHost(owner: PsiModifierListOwner, parameterName: String): PropertyRefMetadataHost? {
		val propertyRef = owner.getAnnotation(PropertyRefLibraryFqns.PROPERTY_REF) ?: return null
		return PropertyRefMetadataHost(
			parameterName = parameterName,
			scope = if (parsePsiEnumSimpleName(propertyRef, "scope") == "ELEMENT") {
				PropertyRefScope.ELEMENT
			} else {
				PropertyRefScope.SIBLING
			},
			compatibilityKind = when (parsePsiEnumSimpleName(propertyRef, "compatibility")) {
				"SAME_SCALAR_KIND" -> PropertyRefCompatibilityKind.SAME_SCALAR_KIND
				"COMPARABLE_FAMILY" -> PropertyRefCompatibilityKind.COMPARABLE_FAMILY
				else -> PropertyRefCompatibilityKind.NONE
			},
		)
	}

	/**
	 * Reads `scope` from a Kotlin `@PropertyRef` entry.
	 *
	 * @param propertyRef The `@PropertyRef` annotation entry.
	 * @return [PropertyRefScope.ELEMENT] when the enum simple name is `ELEMENT`; otherwise
	 *   [PropertyRefScope.SIBLING] (library default / missing argument).
	 */
	private fun parseKtScope(propertyRef: KtAnnotationEntry): PropertyRefScope {
		val name = enumSimpleName(namedArgument(propertyRef, "scope")?.getArgumentExpression())
		return if (name == "ELEMENT") PropertyRefScope.ELEMENT
		else PropertyRefScope.SIBLING
	}

	/**
	 * Reads `compatibility` from a Kotlin `@PropertyRef` entry.
	 *
	 * @param propertyRef The `@PropertyRef` annotation entry.
	 * @return Mapped [PropertyRefCompatibilityKind], or [PropertyRefCompatibilityKind.NONE]
	 *   when missing / unrecognized.
	 */
	private fun parseKtCompatibility(propertyRef: KtAnnotationEntry): PropertyRefCompatibilityKind =
		when (enumSimpleName(namedArgument(propertyRef, "compatibility")?.getArgumentExpression())) {
			"SAME_SCALAR_KIND" -> PropertyRefCompatibilityKind.SAME_SCALAR_KIND
			"COMPARABLE_FAMILY" -> PropertyRefCompatibilityKind.COMPARABLE_FAMILY
			else -> PropertyRefCompatibilityKind.NONE
		}

	/**
	 * Extracts the simple enum / field name from a PSI annotation attribute.
	 *
	 * Handles [PsiReferenceExpression] to enum constants or fields; falls back to text after
	 * the last `.` for other PSI shapes.
	 *
	 * @param annotation PSI annotation (e.g. `@PropertyRef`).
	 * @param attributeName Attribute to read (`scope`, `compatibility`, …).
	 * @return Simple name such as `ELEMENT` / `SAME_SCALAR_KIND`, or `null` when absent.
	 */
	private fun parsePsiEnumSimpleName(annotation: PsiAnnotation, attributeName: String): String? {
		val value = annotation.findAttributeValue(attributeName) ?: return null
		return when (value) {
			is PsiReferenceExpression -> {
				when (val resolved = value.resolve()) {
					is PsiEnumConstant -> resolved.name
					is PsiField -> resolved.name
					else -> value.referenceName
				}
			}
			else -> value.text?.substringAfterLast('.')?.trim()
		}
	}

	/**
	 * Extracts an enum entry’s simple name from a Kotlin expression (`ELEMENT`,
	 * `Scope.ELEMENT`, …).
	 *
	 * @param expression Argument expression, or `null`.
	 * @return Simple name, or `null` when [expression] is null / blank after stripping.
	 */
	private fun enumSimpleName(expression: KtExpression?): String? {
		if (expression == null) return null
		return when (expression) {
			is KtNameReferenceExpression -> expression.getReferencedName()
			is KtDotQualifiedExpression ->
				(expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
					?: expression.text.substringAfterLast('.')
			else -> expression.text.substringAfterLast('.').takeIf { it.isNotBlank() }
		}
	}

	/**
	 * Finds a named value argument on a Kotlin annotation entry.
	 *
	 * @param entry Annotation whose arguments are searched.
	 * @param name Argument name (`scope`, `compatibility`, …).
	 * @return Matching [KtValueArgument], or `null` when only positional / absent.
	 */
	private fun namedArgument(entry: KtAnnotationEntry, name: String): KtValueArgument? {
		entry.valueArguments.forEach { arg ->
			val ktArg = arg as? KtValueArgument ?: return@forEach
			if (ktArg.getArgumentName()?.asName?.asString() == name) return ktArg
		}
		return null
	}

	/**
	 * Merges two `@PropertyRef` hosts that share a [PropertyRefMetadataHost.parameterName].
	 *
	 * Scope: `ELEMENT` wins if either side is `ELEMENT` (OR). Compatibility: drop `NONE`, then
	 * prefer `COMPARABLE_FAMILY` when both non-`NONE` kinds disagree, else the sole / first
	 * remaining kind. Matches KSP `PropertyRefHostDiscovery.mergeHosts` intent so param vs
	 * property duplicates do not lose author intent.
	 *
	 * @param existing Previously stored host for the name, or `null`.
	 * @param incoming Newly parsed host for the same name.
	 * @return [incoming] when [existing] is null; otherwise a merged [PropertyRefMetadataHost].
	 */
	private fun mergeHosts(existing: PropertyRefMetadataHost?, incoming: PropertyRefMetadataHost): PropertyRefMetadataHost {
		if (existing == null) return incoming
		val kinds = listOf(existing.compatibilityKind, incoming.compatibilityKind)
		val nonNone = kinds.filter { it != PropertyRefCompatibilityKind.NONE }.distinct()
		val mergedKind = when {
			nonNone.isEmpty() -> PropertyRefCompatibilityKind.NONE
			nonNone.size == 1 -> nonNone.single()
			PropertyRefCompatibilityKind.COMPARABLE_FAMILY in nonNone ->
				PropertyRefCompatibilityKind.COMPARABLE_FAMILY
			else -> nonNone.first()
		}
		return PropertyRefMetadataHost(
			parameterName = incoming.parameterName,
			scope = if (
				existing.scope == PropertyRefScope.ELEMENT ||
				incoming.scope == PropertyRefScope.ELEMENT
			) {
				PropertyRefScope.ELEMENT
			} else {
				PropertyRefScope.SIBLING
			},
			compatibilityKind = mergedKind,
		)
	}

	/**
	 * Resolves the annotation class declaration for a usage-site [KtAnnotationEntry].
	 *
	 * Prefers Kotlin `mainReference`, then same-file declarations, non-star imports (including
	 * aliases), same-package FQCN, and finally [findClassesByShortName]. Light platform tests
	 * often leave annotation references unresolved — these fallbacks keep discovery usable.
	 *
	 * Also used by ConstraintArg / validatedBy discovery for shared resolution semantics.
	 *
	 * @param annotation Usage-site annotation entry.
	 * @return [KtClass] or [PsiClass] for the annotation type, or `null` when nothing matches.
	 */
	internal fun resolveAnnotationDeclaration(annotation: KtAnnotationEntry): PsiElement? {
		annotation.calleeExpression?.mainReference?.resolve()?.let { return it }

		val shortName = annotation.shortName?.asString() ?: return null
		val file = annotation.containingKtFile

		file.declarations.filterIsInstance<KtClass>().firstOrNull { it.name == shortName }
			?.let { return it }

		for (imp in file.importDirectives) {
			// Prefer importedFqName over deprecated ImportPath (scheduled for removal).
			if (imp.isAllUnder) continue
			val path = imp.importedFqName?.asString() ?: continue
			val matches = when (val alias = imp.aliasName) {
				null -> path == shortName || path.endsWith(".$shortName")
				else -> alias == shortName
			}
			if (matches) {
				findClassByFqName(annotation.project, path, annotation.resolveScope)?.let { return it }
			}
		}

		val pkg = file.packageFqName.asString()
		val candidateFq = if (pkg.isEmpty()) shortName else "$pkg.$shortName"
		findClassByFqName(annotation.project, candidateFq, annotation.resolveScope)?.let { return it }

		return pickBestClass(
			findClassesByShortName(annotation.project, shortName, annotation.resolveScope),
			preferredPackage = pkg.takeIf { it.isNotEmpty() },
		)
	}

	/**
	 * Fully qualified name of an annotation declaration when available.
	 *
	 * @param declaration Result of [resolveAnnotationDeclaration] or similar.
	 * @return Kotlin [KtClass.fqName] / Java [PsiClass.qualifiedName], or `null` for other
	 *   element kinds or unnamed types (cache keys require a non-null FQCN).
	 */
	internal fun annotationFqName(declaration: PsiElement): String? =
		when (declaration) {
			is KtClass -> declaration.fqName?.asString()
			is PsiClass -> declaration.qualifiedName
			else -> null
		}

	private fun findClassByFqName(
		project: Project,
		fqName: String,
		scope: SearchScope,
	): PsiElement? = PsiClassLookup.findClassByFqName(project, fqName, scope)

	/**
	 * Collects classes with the given simple name (delegates to [PsiClassLookup]).
	 */
	internal fun findClassesByShortName(
		project: Project,
		shortName: String,
		scope: SearchScope,
	): List<PsiElement> = PsiClassLookup.findClassesByShortName(project, shortName, scope)

	/**
	 * Picks the best class from ambiguous short-name hits (delegates to [PsiClassLookup]).
	 */
	internal fun pickBestClass(
		candidates: List<PsiElement>,
		preferredPackage: String? = null,
	): PsiElement? = PsiClassLookup.pickBestClass(candidates, preferredPackage)
}

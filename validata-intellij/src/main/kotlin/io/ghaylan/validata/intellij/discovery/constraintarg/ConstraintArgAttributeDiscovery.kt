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

package io.ghaylan.validata.intellij.discovery.constraintarg

import com.intellij.psi.*
import io.ghaylan.validata.intellij.contract.PropertyRefLibraryFqns
import io.ghaylan.validata.intellij.discovery.cache.ConstraintDiscoveryCache
import io.ghaylan.validata.intellij.discovery.constraintarg.ConstraintArgAttributeDiscovery.parseKtHosts
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import io.ghaylan.validata.intellij.model.ConstraintArgMetadataHost
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Discovers `@ConstraintArg` / `@ConstraintArgs` rules on constraint annotation declarations.
 *
 * ## Why it exists
 *
 * Authors mark annotation (or metadata) parameters with `@ConstraintArg(kinds = …, target = …)`
 * so tooling can validate literal arguments (not-blank, typed literal, regex, …) independently
 * of property-path hosts. KSP (`ConstraintArgHostDiscovery` in **validata-processor**) reads
 * those markers at compile time; this object applies the same contract over IntelliJ PSI so
 * inspections fire in the editor for builtins and consumer constraints.
 *
 * ## How it fits the plugin
 *
 * Twin of [PropertyRefAttributeDiscovery] for argument-value rules.
 * Constraint-arg inspections call [discoverHosts]; resolution of the usage-site annotation type
 * is delegated to [PropertyRefAttributeDiscovery.resolveAnnotationDeclaration] so both discovery
 * paths share fallbacks (imports, short-name search, light tests). Results are memoized by
 * [ConstraintDiscoveryCache.constraintArgHosts] when an FQCN is available.
 * Marker attribute decoding lives in [ConstraintArgMarkerParser].
 *
 * ## Discovery order (Option 2)
 *
 * 1. Markers on the constraint **annotation** declaration
 * 2. Else markers copied onto the generated `{Name}Constraint` metadata class
 *
 * Repeatable `@ConstraintArg` (and container `@ConstraintArgs`) yield **one**
 * [ConstraintArgMetadataHost] per marker — a parameter may contribute several hosts with
 * different targets / kind sets. Unlike `@PropertyRef`, hosts are **not**
 * merged by parameter name alone.
 *
 * ## What it is NOT
 *
 * - Not a runtime argument checker (does not evaluate literals itself).
 * - Not path / `@PropertyRef` discovery.
 * - Not an FQCN allowlist of Validata builtins — new constraints light up when they declare
 *   markers on the classpath.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintArgAttributeDiscovery {

	/**
	 * Discovers `@ConstraintArg` hosts for the annotation class resolved from [annotation].
	 *
	 * When the annotation declaration has a resolvable FQCN, results are memoized via
	 * [ConstraintDiscoveryCache.constraintArgHosts] for the current PSI stamp.
	 *
	 * @param annotation Usage-site constraint annotation whose type carries (or generates)
	 *   `@ConstraintArg` markers.
	 * @return Hosts from annotation params or `{Name}Constraint` fallback; empty when the
	 *   declaration is unresolved or no markers are found.
	 */
	fun discoverHosts(annotation: KtAnnotationEntry): List<ConstraintArgMetadataHost> {
		val declaration = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
			?: return emptyList()
		val fqName = PropertyRefAttributeDiscovery.annotationFqName(declaration)
			?: return discoverHostsUncached(declaration)
		return ConstraintDiscoveryCache.getInstance(annotation.project).constraintArgHosts(fqName) {
			discoverHostsUncached(declaration)
		}
	}

	/**
	 * Uncached host discovery from a resolved annotation declaration.
	 *
	 * @param declaration Annotation type as [KtClass], [PsiClass], or other [PsiElement].
	 * @return Hosts from the Kotlin or PSI path; empty for unknown kinds or missing markers.
	 */
	fun discoverHostsUncached(declaration: PsiElement): List<ConstraintArgMetadataHost> =
		when (declaration) {
			is KtClass -> discoverFromKtAnnotationClass(declaration)
			is PsiClass -> discoverFromPsiAnnotationClass(declaration)
			else -> emptyList()
		}

	/**
	 * Kotlin-source path: hosts on the annotation class, else on `{Name}Constraint`
	 * (Kotlin source or binary).
	 *
	 * @param annotationClass Resolved Kotlin annotation declaration.
	 * @return Hosts from the first non-empty surface; empty if both fail.
	 */
	private fun discoverFromKtAnnotationClass(annotationClass: KtClass): List<ConstraintArgMetadataHost> {
		val fromAnnotationParams = collectKtHosts(annotationClass)
		if (fromAnnotationParams.isNotEmpty()) return fromAnnotationParams
		resolveConventionMetadataClass(annotationClass)?.let { return collectKtHosts(it) }
		// Annotation may be Kotlin source while `{Name}Constraint` is binary-only.
		val annFq = annotationClass.fqName?.asString()
		val metaFq = annFq?.substringBeforeLast('.', "")
			?.let { pkg ->
				if (pkg.isBlank()) "${annotationClass.name}Constraint"
				else "$pkg.${annotationClass.name}Constraint"
			}
			?: return emptyList()
		val psiMeta = JavaPsiFacade.getInstance(annotationClass.project)
			.findClass(metaFq, annotationClass.resolveScope)
			?: return emptyList()
		return collectPsiHosts(psiMeta)
	}

	/**
	 * Java / binary path: hosts on the annotation [PsiClass], else on `{Name}Constraint`.
	 *
	 * @param annotationClass Resolved PSI annotation class.
	 * @return Hosts from the first non-empty surface; empty if both fail.
	 */
	private fun discoverFromPsiAnnotationClass(annotationClass: PsiClass): List<ConstraintArgMetadataHost> {
		val fromAnnotationParams = collectPsiHosts(annotationClass)
		if (fromAnnotationParams.isNotEmpty()) return fromAnnotationParams
		val metadataClass = resolveConventionMetadataClass(annotationClass) ?: return emptyList()
		return collectPsiHosts(metadataClass)
	}

	/**
	 * Locates `{AnnotationName}Constraint` for a Kotlin annotation (same-file, then short-name
	 * search via [PropertyRefAttributeDiscovery.findClassesByShortName]).
	 *
	 * @param annotationClass Annotation whose simple name drives the convention name.
	 * @return Metadata [KtClass], or `null` when missing.
	 */
	private fun resolveConventionMetadataClass(annotationClass: KtClass): KtClass? {
		val annName = annotationClass.name ?: return null
		val metaName = annName + "Constraint"
		annotationClass.containingKtFile.declarations
			.filterIsInstance<KtClass>()
			.firstOrNull { it.name == metaName }
			?.let { return it }
		val preferredPkg = annotationClass.fqName?.asString()?.substringBeforeLast('.', "")
		return PropertyRefAttributeDiscovery.pickBestClass(
			PropertyRefAttributeDiscovery.findClassesByShortName(
				annotationClass.project,
				metaName,
				annotationClass.resolveScope,
			),
			preferredPackage = preferredPkg,
		) as? KtClass
	}

	/**
	 * Locates `{AnnotationName}Constraint` for a PSI annotation (same-package FQCN, then
	 * short-name search).
	 *
	 * @param annotationClass Annotation PSI class.
	 * @return Metadata [PsiClass], or `null` when unresolved.
	 */
	private fun resolveConventionMetadataClass(annotationClass: PsiClass): PsiClass? {
		val annName = annotationClass.name ?: return null
		val metaName = annName + "Constraint"
		val preferredPkg = annotationClass.qualifiedName?.substringBeforeLast('.', "")
		val fq = preferredPkg?.let { pkg ->
			if (pkg.isBlank()) metaName else "$pkg.$metaName"
		} ?: metaName
		JavaPsiFacade.getInstance(annotationClass.project)
			.findClass(fq, annotationClass.resolveScope)
			?.let { return it }
		return PropertyRefAttributeDiscovery.pickBestClass(
			PropertyRefAttributeDiscovery.findClassesByShortName(
				annotationClass.project,
				metaName,
				annotationClass.resolveScope,
			),
			preferredPackage = preferredPkg,
		) as? PsiClass
	}

	/**
	 * Collects `@ConstraintArg` hosts from Kotlin primary-constructor parameters and
	 * properties. Does not merge by name — each marker becomes its own host; duplicates of the
	 * same `(parameterName, target, kinds)` triple are collapsed later in [parseKtHosts].
	 *
	 * @param metadata Annotation or `{Name}Constraint` class.
	 * @return Flat list of hosts (may be empty).
	 */
	private fun collectKtHosts(metadata: KtClass): List<ConstraintArgMetadataHost> {
		val out = ArrayList<ConstraintArgMetadataHost>()
		metadata.primaryConstructor?.valueParameters?.forEach { param ->
			val name = param.name ?: return@forEach
			out += parseKtHosts(param.annotationEntries, name)
		}
		metadata.declarations.filterIsInstance<KtProperty>().forEach { prop ->
			val name = prop.name ?: return@forEach
			out += parseKtHosts(prop.annotationEntries, name)
		}
		return out
	}

	/**
	 * Collects `@ConstraintArg` hosts from PSI constructors, fields, methods, and
	 * `DefaultImpls` (same binary shapes as PropertyRef discovery).
	 *
	 * @param metadata Annotation or `{Name}Constraint` PSI class.
	 * @return Distinct hosts (may be empty).
	 */
	private fun collectPsiHosts(metadata: PsiClass): List<ConstraintArgMetadataHost> {
		val out = ArrayList<ConstraintArgMetadataHost>()
		metadata.constructors.firstOrNull()?.parameterList?.parameters?.forEach { param ->
			val name = param.name
			out += parsePsiHosts(param, name)
		}
		for (field in metadata.fields) {
			val name = field.name
			out += parsePsiHosts(field, name)
		}
		// Binary Kotlin annotations / data classes expose markers on getX, bare x, and
		// x$annotations / getX$annotations (same shapes as PropertyRefAttributeDiscovery).
		for (method in metadata.methods) {
			val propName = psiHostParameterName(method.name) ?: continue
			out += parsePsiHosts(method, propName)
		}
		val defaultImpls = metadata.findInnerClassByName("DefaultImpls", false)
			?: metadata.qualifiedName?.let { fq ->
				JavaPsiFacade.getInstance(metadata.project)
					.findClass("$fq\$DefaultImpls", metadata.resolveScope)
			}
		defaultImpls?.methods?.forEach { method ->
			val propName = psiHostParameterName(method.name) ?: return@forEach
			out += parsePsiHosts(method, propName)
		}
		return out.distinctBy { it.parameterName to it.target to it.kinds }
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
	 * Parses all `@ConstraintArg` / `@ConstraintArgs` entries on a Kotlin parameter or property.
	 *
	 * @param annotations Annotations on the parameter / property.
	 * @param parameterName Usage-site argument name hosts bind to.
	 * @return Distinct hosts for this parameter (may be empty).
	 */
	private fun parseKtHosts(
		annotations: List<KtAnnotationEntry>,
		parameterName: String,
	): List<ConstraintArgMetadataHost> {
		val out = ArrayList<ConstraintArgMetadataHost>()
		for (entry in annotations) {
			val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(entry)
			val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
			val short = entry.shortName?.asString()
			val isArg = fq == PropertyRefLibraryFqns.CONSTRAINT_ARG || (short == "ConstraintArg" && fq == null)
			val isArgs = fq == PropertyRefLibraryFqns.CONSTRAINT_ARGS || (short == "ConstraintArgs" && fq == null)
			when {
				isArg -> ConstraintArgMarkerParser.parseKtOne(entry, parameterName)?.let(out::add)
				isArgs -> {
					entry.valueArgumentList?.arguments?.forEach { arg ->
						val expr = arg?.getArgumentExpression() ?: return@forEach
						expr.children.filterIsInstance<KtAnnotationEntry>().forEach { nested ->
							ConstraintArgMarkerParser.parseKtOne(nested, parameterName)?.let(out::add)
						}
					}
					entry.children.filterIsInstance<KtAnnotationEntry>().forEach { nested ->
						ConstraintArgMarkerParser.parseKtOne(nested, parameterName)?.let(out::add)
					}
				}
			}
		}
		return out.distinctBy { it.parameterName to it.target to it.kinds }
	}

	/**
	 * Parses `@ConstraintArg` / `@ConstraintArgs` from a PSI modifier-list owner.
	 *
	 * @param owner Parameter, field, or method.
	 * @param parameterName Derived host parameter name.
	 * @return Distinct hosts for this owner (may be empty).
	 */
	private fun parsePsiHosts(
		owner: PsiModifierListOwner,
		parameterName: String,
	): List<ConstraintArgMetadataHost> {
		val out = ArrayList<ConstraintArgMetadataHost>()
		owner.annotations.forEach { ann ->
			val fq = ann.qualifiedName
			when {
				fq == PropertyRefLibraryFqns.CONSTRAINT_ARG ||
					fq?.endsWith(".ConstraintArg") == true ->
					ConstraintArgMarkerParser.parsePsiOne(ann, parameterName)?.let(out::add)
				fq == PropertyRefLibraryFqns.CONSTRAINT_ARGS ||
					fq?.endsWith(".ConstraintArgs") == true -> {
					val value = ann.findAttributeValue("value")
					val nested = value?.children?.filterIsInstance<PsiAnnotation>().orEmpty()
					if (nested.isNotEmpty()) {
						nested.forEach {
							ConstraintArgMarkerParser.parsePsiOne(it, parameterName)?.let(out::add)
						}
					} else {
						out += ConstraintArgMarkerParser.parseHostsFromText(
							value?.text.orEmpty(),
							parameterName,
						)
					}
				}
			}
		}
		return out.distinctBy { it.parameterName to it.target to it.kinds }
	}
}

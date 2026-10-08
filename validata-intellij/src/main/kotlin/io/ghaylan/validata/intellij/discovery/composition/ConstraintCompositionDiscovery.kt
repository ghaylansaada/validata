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

package io.ghaylan.validata.intellij.discovery.composition

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import io.ghaylan.validata.intellij.contract.PropertyRefLibraryFqns
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByMissingValidatedBy
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByNotAConstraint
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByOk
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByResult
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import io.ghaylan.validata.intellij.discovery.validatedby.ConstraintValidatedByDiscovery
import org.jetbrains.kotlin.psi.*

/**
 * Discovers composed (non-`@Constraint`) annotations and their nested leaf `@Constraint`s.
 *
 * Mirrors KSP `ConstraintModelBuilder` expand: absent / AND flattens; OR is one synthetic site.
 * Presence simple names (`Required` / `RequiredWhen`) match processor
 * `AnnotationAttrs.PresenceSimpleNames`.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintCompositionDiscovery {

	/** Presence annotation simple names rejected as OR members (KSP parity). */
	private val PRESENCE_SIMPLE_NAMES: Set<String> = setOf("Required", "RequiredWhen")

	/**
	 * Framework / JDK meta-annotations ignored while collecting leaf members.
	 */
	private val IGNORABLE_FQNS: Set<String> = setOf(
		PropertyRefLibraryFqns.CONSTRAINT,
		PropertyRefLibraryFqns.CONSTRAINT_COMPOSITION,
	)

	/**
	 * Analyzes a usage-site annotation for composition (resolve declaration → [analyzeDeclaration]).
	 *
	 * @param annotation use-site entry (e.g. `@EmailOrPhone` on a property)
	 * @return composition analysis, or `null` when the declaration cannot be resolved
	 */
	fun analyzeUsage(annotation: KtAnnotationEntry): CompositionAnalysis? {
		val declaration = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
			?: return null
		return analyzeDeclaration(declaration)
	}

	/**
	 * Analyzes an annotation **class** declaration for nested leaf constraints and optional
	 * `@ConstraintComposition`.
	 *
	 * @param declaration annotation type ([KtClass] or [PsiClass])
	 * @return analysis; [CompositionAnalysis.isComposed] is false when there are no leaves and
	 *   no composition marker
	 */
	fun analyzeDeclaration(declaration: PsiElement): CompositionAnalysis =
		when (declaration) {
			is KtClass -> analyzeKt(declaration)
			is PsiClass -> analyzePsi(declaration)
			else -> CompositionAnalysis.EMPTY
		}

	/**
	 * Whether [annotation] is `@ConstraintComposition` (FQCN or unresolved short name).
	 *
	 * @param annotation candidate meta-annotation on an annotation class
	 */
	fun isConstraintComposition(annotation: KtAnnotationEntry): Boolean {
		val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
		val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
		if (fq == PropertyRefLibraryFqns.CONSTRAINT_COMPOSITION) return true
		return annotation.shortName?.asString() == "ConstraintComposition" && fq == null
	}

	private fun analyzeKt(annotationClass: KtClass): CompositionAnalysis {
		if (findKtConstraint(annotationClass) != null) {
			// Outer @Constraint types are ordinary constraints, not composition roots.
			return CompositionAnalysis.EMPTY
		}
		var compositionEntry: KtAnnotationEntry? = null
		var mode = CompositionModeView.AND
		val leaves = mutableListOf<CompositionLeaf>()

		for (entry in annotationClass.annotationEntries) {
			if (isConstraintComposition(entry)) {
				compositionEntry = entry
				mode = readModeKt(entry)
				continue
			}
			if (isIgnorableKt(entry)) continue
			val shortName = entry.shortName?.asString() ?: continue
			val leafDecl = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(entry)
				?: continue
			appendLeaf(leaves, shortName, entry, leafDecl)
		}

		return CompositionAnalysis(
			mode = mode,
			leaves = leaves,
			compositionEntry = compositionEntry,
			hasCompositionMarker = compositionEntry != null,
		)
	}

	private fun analyzePsi(annotationClass: PsiClass): CompositionAnalysis {
		if (annotationClass.getAnnotation(PropertyRefLibraryFqns.CONSTRAINT) != null) {
			return CompositionAnalysis.EMPTY
		}
		val compositionPsi = annotationClass.getAnnotation(PropertyRefLibraryFqns.CONSTRAINT_COMPOSITION)
		val mode = if (compositionPsi != null) readModePsi(compositionPsi) else CompositionModeView.AND
		val leaves = mutableListOf<CompositionLeaf>()

		for (annotation in annotationClass.annotations) {
			val fq = annotation.qualifiedName ?: continue
			if (fq in IGNORABLE_FQNS) continue
			if (fq.startsWith("java.lang.annotation.") || fq.startsWith("kotlin.")) continue

			val shortName = fq.substringAfterLast('.')
			val resolved = annotation.resolveAnnotationType() ?: continue
			appendLeaf(leaves, shortName, entry = null, leafDecl = resolved)
		}

		return CompositionAnalysis(
			mode = mode,
			leaves = leaves,
			compositionEntry = null,
			hasCompositionMarker = compositionPsi != null,
		)
	}

	/**
	 * Appends a leaf or nested-composed placeholder for [leafDecl] when it qualifies.
	 */
	private fun appendLeaf(
		leaves: MutableList<CompositionLeaf>,
		shortName: String,
		entry: KtAnnotationEntry?,
		leafDecl: PsiElement,
	) {
		when (val validatedBy = ConstraintValidatedByDiscovery.analyzeDeclaration(leafDecl)) {
			is ConstraintValidatedByOk,
			is ConstraintValidatedByMissingValidatedBy -> {
				leaves += CompositionLeaf(
					shortName = shortName,
					entry = entry,
					validatedBy = validatedBy,
					isPresence = shortName in PRESENCE_SIMPLE_NAMES,
					isNestedComposed = false,
				)
			}
			is ConstraintValidatedByNotAConstraint -> {
				if (analyzeDeclaration(leafDecl).isComposed) {
					leaves += CompositionLeaf(
						shortName = shortName,
						entry = entry,
						validatedBy = ConstraintValidatedByNotAConstraint,
						isPresence = false,
						isNestedComposed = true,
					)
				}
			}
		}
	}

	private fun findKtConstraint(annotationClass: KtClass): KtAnnotationEntry? {
		for (entry in annotationClass.annotationEntries) {
			val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(entry)
			val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
			if (fq == PropertyRefLibraryFqns.CONSTRAINT) return entry
			if (entry.shortName?.asString() == "Constraint" && fq == null) return entry
		}
		return null
	}

	private fun isIgnorableKt(entry: KtAnnotationEntry): Boolean {
		val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(entry)
		val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
		if (fq != null) {
			if (fq in IGNORABLE_FQNS) return true
			if (fq.startsWith("kotlin.") || fq.startsWith("java.lang.annotation.")) return true
			if (fq.startsWith("kotlin.annotation.")) return true
		}
		val short = entry.shortName?.asString() ?: return true
		return short in setOf(
			"Target", "Retention", "MustBeDocumented", "Repeatable", "Deprecated",
			"ConstraintMessage", "ConstraintGroups",
		)
	}

	private fun readModeKt(entry: KtAnnotationEntry): CompositionModeView {
		val arg = entry.valueArguments.firstOrNull() as? KtValueArgument ?: return CompositionModeView.AND
		val expr = arg.getArgumentExpression() ?: return CompositionModeView.AND
		val text = when (expr) {
			is KtDotQualifiedExpression ->
				(expr.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
					?: expr.text
			is KtNameReferenceExpression -> expr.getReferencedName()
			else -> expr.text
		}
		return if (text.endsWith("OR") || text == "OR") CompositionModeView.OR else CompositionModeView.AND
	}

	private fun readModePsi(annotation: PsiAnnotation): CompositionModeView {
		val value = annotation.findAttributeValue("value") ?: return CompositionModeView.AND
		val ref = value.reference?.resolve() as? PsiEnumConstant
		val name = ref?.name ?: value.text
		return if (name.endsWith("OR") || name == "OR") CompositionModeView.OR else CompositionModeView.AND
	}
}

/**
 * AND / OR expand mode discovered from `@ConstraintComposition` (default AND when absent).
 * 
 * @author Ghaylan Saada

 */
internal enum class CompositionModeView {
	AND,
	OR,
}

/**
 * One nested member on a composed annotation declaration.
 *
 * @property shortName leaf annotation simple name
 * @property entry Kotlin source entry when available (null for binary PSI)
 * @property validatedBy `validatedBy` analysis for leaf `@Constraint`s
 * @property isPresence `Required` / `RequiredWhen` (illegal as OR member)
 * @property isNestedComposed another composed annotation nested inside (illegal under OR v1)
 * 
 * @author Ghaylan Saada

 */
internal data class CompositionLeaf(
	val shortName: String,
	val entry: KtAnnotationEntry?,
	val validatedBy: ConstraintValidatedByResult,
	val isPresence: Boolean,
	val isNestedComposed: Boolean,
)

/**
 * Result of composing nested leaf constraints on a non-`@Constraint` annotation type.
 *
 * @property mode expand mode (AND default)
 * @property leaves discovered leaf / nested members
 * @property compositionEntry `@ConstraintComposition` entry when in Kotlin source
 * @property hasCompositionMarker whether `@ConstraintComposition` was present
 * 
 * @author Ghaylan Saada

 */
internal data class CompositionAnalysis(
	val mode: CompositionModeView,
	val leaves: List<CompositionLeaf>,
	val compositionEntry: KtAnnotationEntry?,
	val hasCompositionMarker: Boolean,
) {
	/** True when there is at least one leaf or an explicit composition marker.
 */
	val isComposed: Boolean
		get() = leaves.isNotEmpty() || hasCompositionMarker

	/** Leaf `@Constraint` members only (excludes nested composed placeholders).
 */
	val constraintLeaves: List<CompositionLeaf>
		get() = leaves.filter { !it.isNestedComposed && it.validatedBy != ConstraintValidatedByNotAConstraint }

	companion object {
		val EMPTY: CompositionAnalysis = CompositionAnalysis(
			mode = CompositionModeView.AND,
			leaves = emptyList(),
			compositionEntry = null,
			hasCompositionMarker = false,
		)
	}
}

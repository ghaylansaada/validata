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
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.ConstraintMeta
import io.ghaylan.validata.processor.model.ConstraintModel
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Expands composed constraint annotations (AND / OR) during [ConstraintModelBuilder] resolution.
 *
 * AND composition is handled by the caller's recursive expand; this type owns OR expansion,
 * composition-mode detection, and usage-site `message` / `groups` rendering for synthetic
 * composition models.
 *
 * @property logger KSP logger for OR composition diagnostics.
 * @property findConstraintMeta Resolves `@Constraint` meta for a usage- or meta-annotation.*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintCompositionExpander(
	private val logger: KSPLogger,
	private val findConstraintMeta: (KSAnnotation) -> ConstraintMeta?,
) {
	
	/**
	 * Reads `@ConstraintComposition` on [decl], defaulting to AND when absent.
	 *
	 * No side effects.
	 *
	 * @param decl Composed annotation class declaration.
	 * @return Expand mode for nested meta-annotations.	 
	 */
	fun readCompositionMode(decl: KSClassDeclaration): CompositionExpandMode {
		val compositionAnn = decl.annotations.firstOrNull {
			AnnotationFqcn.isA(it, ProcessorFqns.CONSTRAINT_COMPOSITION)
		} ?: return CompositionExpandMode.AND
		for (arg in compositionAnn.arguments) {
			val name = arg.name?.asString()
			if (name != null && name != AnnotationAttrs.ConstraintComposition.VALUE) continue
			val text = arg.value?.toString().orEmpty()
			if (text.endsWith(".OR") || text == "OR") return CompositionExpandMode.OR
		}
		return CompositionExpandMode.AND
	}
	
	/**
	 * Expands an OR-composed annotation into one [ConstraintModel] with nested leaf children.
	 *
	 * Logs KSP errors for &lt;2 leaves, presence members, or nested composed members.
	 *
	 * @param usageAnn Usage-site annotation (e.g. `@EmailOrPhone` on a field).
	 * @param decl Annotation class declaration carrying nested leaf constraints.
	 * @param addLeaf Builds a leaf model or returns null on validator mismatch.
	 * @param emit Adds the finished composition model to the parent list.
	 * @param subjectNode Symbol for IDE/build log location.
	 *
	 * May emit KSP errors via [logger]; mutates [emit] callback targets on success.	 
	 */
	fun expandOrComposition(
		usageAnn: KSAnnotation,
		decl: KSClassDeclaration,
		addLeaf: (KSAnnotation, ConstraintMeta) -> ConstraintModel?,
		emit: (ConstraintModel) -> Unit,
		subjectNode: KSNode?,
	) {
		val composedName = usageAnn.shortName.asString()
		val children = mutableListOf<ConstraintModel>()
		
		for (metaAnn in decl.annotations) {
			if (isIgnorableCompositionMeta(metaAnn)) continue
			val leafMeta = findConstraintMeta(metaAnn)
			if (leafMeta != null) {
				val child = addLeaf(metaAnn, leafMeta)
					?: return
				children += child
				continue
			}
			
			if (looksLikeComposedAnnotation(metaAnn)) {
				logger.error(
					"@$composedName uses @ConstraintComposition(OR) but nests composed annotation " + "'${metaAnn.shortName.asString()}'. OR members must be leaf @Constraint " + "annotations (v1).",
					subjectNode,
				)
				return
			}
		}
		val orError = CompositionOrRules.validateLeaves(composedName, children)
		if (orError != null) {
			logger.error(orError, subjectNode)
			return
		}
		
		emit(
			ConstraintModel(
				metadataConstructorCall = "",
				validatorExpression = "",
				order = children.maxOf { it.order } + 1,
				annotationSimpleName = composedName,
				compositionChildren = children,
				compositionMessageExpr = renderCompositionMessage(usageAnn),
				compositionGroupsExpr = renderCompositionGroups(usageAnn),
			),
		)
	}
	
	/**
	 * Whether [ann] is a framework / JDK meta-annotation ignored during composition expand.
	 *
	 * @param ann Meta-annotation on a composed annotation class.
	 * @return `true` when [ann] is not a constraint member candidate.	 
	 */
	private fun isIgnorableCompositionMeta(ann: KSAnnotation): Boolean {
		val fqcn = AnnotationFqcn.of(ann) ?: return true
		if (fqcn == ProcessorFqns.CONSTRAINT_COMPOSITION) return true
		if (fqcn == ProcessorFqns.CONSTRAINT) return true
		if (fqcn.startsWith(TypeNames.KOTLIN_PACKAGE_PREFIX)) return true
		if (fqcn.startsWith(TypeNames.JAVA_LANG_ANNOTATION_PACKAGE_PREFIX)) return true
		if (fqcn.startsWith(TypeNames.KOTLIN_ANNOTATION_PACKAGE_PREFIX)) return true
		return false
	}
	
	/**
	 * Heuristic: [ann]'s type carries nested `@Constraint` meta-annotations (composed root).
	 *
	 * @param ann Candidate meta-annotation that is not itself a `@Constraint`.
	 * @return `true` when the declaration looks like a composed annotation.	 
	 */
	private fun looksLikeComposedAnnotation(ann: KSAnnotation): Boolean {
		val declaration = ann.annotationType.resolve().declaration as? KSClassDeclaration
			?: return false
		val hasComposition = declaration.annotations.any {
			AnnotationFqcn.isA(it, ProcessorFqns.CONSTRAINT_COMPOSITION)
		}
		val hasConstraintMeta = declaration.annotations.any {
			findConstraintMeta(it) != null
		}
		
		return hasComposition || hasConstraintMeta
	}
	
	/**
	 * Renders the outer composition `message` from the usage-site annotation.
	 *
	 * @param usageAnn Usage site (may omit `message`).
	 * @return Kotlin string literal expression.	 
	 */
	private fun renderCompositionMessage(usageAnn: KSAnnotation): String {
		val raw = usageAnn.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.ConstraintPayload.MESSAGE }?.value as? String
		return CompositionPayloadExprs.message(raw)
	}
	
	/**
	 * Renders the outer composition `groups` from the usage-site annotation.
	 *
	 * @param usageAnn Usage site (may omit `groups`).
	 * @return Kotlin `setOf(…::class)` expression.	 
	 */
	private fun renderCompositionGroups(usageAnn: KSAnnotation): String {
		val groupsArg = usageAnn.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.ConstraintPayload.GROUPS }
			?: return CompositionPayloadExprs.groups(null)
		
		@Suppress("UNCHECKED_CAST")
		val types = groupsArg.value as? List<KSType>
		if (types.isNullOrEmpty()) return CompositionPayloadExprs.groups(null)
		val fqcns = types.mapNotNull { type -> type.declaration.qualifiedName?.asString() }
		return CompositionPayloadExprs.groups(fqcns.ifEmpty { null })
	}
}

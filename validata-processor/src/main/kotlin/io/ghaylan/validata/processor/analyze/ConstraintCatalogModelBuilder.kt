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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.ConstraintValidatorTypeResolver
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.ConstraintCatalogAnnotationModel
import io.ghaylan.validata.processor.model.ConstraintCatalogEntryModel

/**
 * Builds [ConstraintCatalogAnnotationModel]s for every `@Constraint`-annotated annotation in the
 * current compilation.
 *
 * Enforces compile-time rules that used to fail only at runtime:
 * - duplicate `(metadata, valueType)` bindings within this module
 * - unresolved generic type parameters on validators
 * - validators that are neither Kotlin `object`s nor public no-arg constructible
 *
 * Output feeds the root-module `ConstraintCatalog` SPI (not `validata-schema`).
 *
 * @property logger catalog binding / constructibility diagnostics
 * @property typeResolver resolves `ConstraintValidator<V, C>` for each `validatedBy` entry*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintCatalogModelBuilder(
	private val logger: KSPLogger,
	private val typeResolver: ConstraintValidatorTypeResolver = ConstraintValidatorTypeResolver(),
) {
	
	/**
	 * Discovers all `@Constraint` annotations and builds one model per annotation class.
	 *
	 * @param resolver current KSP round resolver
	 * @return models sorted by annotation FQCN; empty when this compilation declares none	 
	 */
	fun buildAll(resolver: Resolver): List<ConstraintCatalogAnnotationModel> =
		buildAll(resolver, skipFqcns = emptySet())
	
	/**
	 * Discovers all `@Constraint` annotations and builds one model per annotation class.
	 *
	 * @param resolver current KSP round resolver
	 * @return models sorted by annotation FQCN; empty when this compilation declares none
	 *
	 * May emit KSP errors via [logger]; mutates `seenBindings` across annotations in the overload
	 * that accepts [skipFqcns].	 
	 */
	fun buildAll(
		resolver: Resolver,
		skipFqcns: Set<String>,
	): List<ConstraintCatalogAnnotationModel> {
		val annotations = resolver.getSymbolsWithAnnotation(ProcessorFqns.CONSTRAINT)
			.filterIsInstance<KSClassDeclaration>()
			.toList()
			.sortedBy {
				it.qualifiedName?.asString().orEmpty()
			}
		
		val models = ArrayList<ConstraintCatalogAnnotationModel>(annotations.size)
		val seenBindings = HashMap<Pair<String, String>, String>()
		
		for (annotationDecl in annotations) {
			val fqcn = annotationDecl.qualifiedName?.asString()
			if (fqcn != null && fqcn in skipFqcns) continue
			buildCatalogAnnotationModel(annotationDecl, seenBindings)?.let { models += it }
		}
		return models
	}
	
	/**
	 * Builds the catalog model for a single `@Constraint` annotation declaration.
	 *
	 * @param seenBindings `(metadataFqcn, valueTypeFqcn) → validatorFqcn` across this module —
	 *   used to detect duplicate catalog keys before codegen
	 * @return model with at least one entry, or `null` when metadata / validators are unusable	 
	 */
	private fun buildCatalogAnnotationModel(
		annotationDecl: KSClassDeclaration,
		seenBindings: MutableMap<Pair<String, String>, String>,
	): ConstraintCatalogAnnotationModel? {
		val annotationFqcn = annotationDecl.qualifiedName?.asString()
			?: return null
		val constraintAnn = annotationDecl.annotations.firstOrNull {
			AnnotationFqcn.isA(it, ProcessorFqns.CONSTRAINT)
		}
			?: return null
		val validatedByArg = constraintAnn.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.Constraint.VALIDATED_BY }
			?: constraintAnn.arguments.firstOrNull { it.name == null }
		val metadataFqcn = MetadataFqcnResolver.conventionFqcn(annotationDecl)
		if (!hasRoleMarkers(annotationDecl)) {
			logger.error(
				"@Constraint on '$annotationFqcn' must declare @ConstraintMessage and @ConstraintGroups on annotation parameters (metadata is generated as '$metadataFqcn').",
				annotationDecl,
			)
			return null
		}
		@Suppress("UNCHECKED_CAST")
		val validatedBy = (validatedByArg?.value as? List<KSType>).orEmpty()
		if (validatedBy.isEmpty()) {
			logger.error("@Constraint on '$annotationFqcn' has an empty 'validatedBy' list.", annotationDecl)
			return null
		}
		val entries = ArrayList<ConstraintCatalogEntryModel>(validatedBy.size)
		for (validatorType in validatedBy.sortedBy {
			it.declaration.qualifiedName?.asString().orEmpty()
		}) {
			val validatorDecl = validatorType.declaration as? KSClassDeclaration
				?: continue
			val validatorFqcn = validatorDecl.qualifiedName?.asString()
				?: continue
			val resolved = typeResolver.resolve(validatorDecl)
			if (resolved == null) {
				logger.error(
					"Validator '$validatorFqcn' does not implement ConstraintValidator<V, C>.",
					validatorDecl,
				)
				continue
			}
			if (typeResolver.isUnresolvedTypeParameter(resolved.valueType)) {
				logger.error(
					"Validator '$validatorFqcn' uses an unresolved type parameter as its value type. Register a concrete subtype or object instead (e.g. object IntRangeValidator : RangeValidator<Int>()).",
					validatorDecl,
				)
				continue
			}
			val valueTypeFqcn = resolved.valueType.declaration.qualifiedName?.asString()
			if (valueTypeFqcn == null) {
				logger.error("Validator '$validatorFqcn' has a value type without a qualified name.", validatorDecl)
				continue
			}
			val resolvedMetadataFqcn = resolved.metadataType.declaration.qualifiedName?.asString()
			if (resolvedMetadataFqcn != null && resolvedMetadataFqcn != metadataFqcn && resolvedMetadataFqcn != ProcessorFqns.CONSTRAINT_METADATA) {
				logger.error(
					"Validator '$validatorFqcn' binds metadata '$resolvedMetadataFqcn' but generated metadata for @$annotationFqcn is '$metadataFqcn'.",
					validatorDecl,
				)
				continue
			}
			val objectSingleton = validatorDecl.classKind == ClassKind.OBJECT
			if (!objectSingleton && !hasPublicNoArgConstructor(validatorDecl)) {
				logger.error(
					"Validator '$validatorFqcn' is not a Kotlin object and has no public no-arg constructor. ConstraintCatalog cannot instantiate it at runtime.",
					validatorDecl,
				)
				continue
			}
			val bindingKey = metadataFqcn to valueTypeFqcn
			val previousValidator = seenBindings.put(bindingKey, validatorFqcn)
			if (previousValidator != null) {
				logger.error(
					"Duplicate constraint catalog binding for metadata='$metadataFqcn' valueType='$valueTypeFqcn': both '$previousValidator' and '$validatorFqcn' claim it. Remove one or specialize the value types.",
					validatorDecl,
				)
				continue
			}
			
			entries += ConstraintCatalogEntryModel(
				annotationFqcn = annotationFqcn,
				metadataFqcn = metadataFqcn,
				valueTypeFqcn = valueTypeFqcn,
				validatorFqcn = validatorFqcn,
				objectSingleton = objectSingleton,
			)
		}
		
		if (entries.isEmpty()) return null
		
		return ConstraintCatalogAnnotationModel(
			annotationFqcn = annotationFqcn,
			annotationSimpleName = annotationDecl.simpleName.asString(),
			sourceFilePath = annotationDecl.containingFile?.filePath,
			entries = entries,
		)
	}
	
	/**
	 * `true` when [decl] can be constructed with `Class.newInstance()`-style no-arg construction:
	 * primary constructor with zero parameters, or any `<init>()` with zero parameters.
	 *
	 * Kotlin `object` validators skip this check (they use the singleton instance).
	 *
	 * No side effects.
	 *
	 * @param decl validator class declaration
	 * @return `true` when a public no-arg constructor exists	 
	 */
	private fun hasPublicNoArgConstructor(decl: KSClassDeclaration): Boolean {
		val primary = decl.primaryConstructor
		return primary != null && primary.parameters.isEmpty() || decl.declarations.filterIsInstance<com.google.devtools.ksp.symbol.KSFunctionDeclaration>().any { fn ->
				fn.simpleName.asString() == "<init>" && fn.parameters.isEmpty()
			}
	}
	
	/**
	 * Requires both `@ConstraintMessage` and `@ConstraintGroups` (same contract as
	 * [ConstraintMetadataModelBuilder]) so catalog emission never references missing metadata.
	 *
	 * No side effects.
	 *
	 * @param annotationDecl `@Constraint` annotation class
	 * @return `true` when both role markers are present on constructor parameters	 
	 */
	private fun hasRoleMarkers(annotationDecl: KSClassDeclaration): Boolean {
		val params = annotationDecl.primaryConstructor?.parameters
			?: return false
		var hasMessage = false
		var hasGroups = false
		for (param in params) {
			for (ann in param.annotations) {
				when (AnnotationFqcn.of(ann)) {
					ProcessorFqns.CONSTRAINT_MESSAGE -> hasMessage = true
					ProcessorFqns.CONSTRAINT_GROUPS -> hasGroups = true
				}
			}
		}
		return hasMessage && hasGroups
	}
}

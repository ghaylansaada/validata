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
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.ghaylan.validata.processor.compat.has
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.GeneratedMetadataClassModel
import io.ghaylan.validata.processor.model.GeneratedMetadataPropertyModel
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Builds [GeneratedMetadataClassModel] from a `@Constraint` annotation that opts into Option 2
 * generation via `@ConstraintMessage` / `@ConstraintGroups` role markers.
 *
 * @property logger role-marker and payload-type diagnostics*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintMetadataModelBuilder(
	private val logger: KSPLogger,
) {
	
	/**
	 * Builds Option 2 generated metadata for [annotationDecl] when it opts in via role markers.
	 *
	 * May emit KSP errors via [logger] when role markers or payload types are invalid.
	 *
	 * @param annotationDecl `@Constraint` annotation class declaration
	 * @return generated metadata model, or `null` for legacy hand-written metadata path	 
	 */
	fun buildIfOptedIn(annotationDecl: KSClassDeclaration): GeneratedMetadataClassModel? {
		if (annotationDecl.classKind != ClassKind.ANNOTATION_CLASS) return null
		val params = annotationDecl.primaryConstructor?.parameters?.toList().orEmpty()
		if (params.isEmpty()) return null
		val messageParams = params.filter { it.has(ProcessorFqns.CONSTRAINT_MESSAGE) }
		val groupsParams = params.filter { it.has(ProcessorFqns.CONSTRAINT_GROUPS) }
		if (messageParams.isEmpty() && groupsParams.isEmpty()) {
			return null // legacy hand-written metadata path
		}
		val annotationFqcn = annotationDecl.qualifiedName?.asString()
			?: return null
		
		if (messageParams.size != 1) {
			logger.error(
				"@Constraint annotation '$annotationFqcn' must have exactly one @ConstraintMessage " + "parameter (found ${messageParams.size}).",
				annotationDecl,
			)
			return null
		}
		if (groupsParams.size != 1) {
			logger.error(
				"@Constraint annotation '$annotationFqcn' must have exactly one @ConstraintGroups " + "parameter (found ${groupsParams.size}).",
				annotationDecl,
			)
			return null
		}
		val messageParam = messageParams.single()
		val groupsParam = groupsParams.single()
		
		if (!ConstraintMetadataPropertyBuilder.isStringType(messageParam.type.resolve())) {
			logger.error(
				"@ConstraintMessage on '$annotationFqcn.${messageParam.name?.asString()}' must be String.",
				messageParam,
			)
			return null
		}
		if (!ConstraintMetadataPropertyBuilder.isKClassArrayType(groupsParam.type.resolve())) {
			logger.error(
				"@ConstraintGroups on '$annotationFqcn.${groupsParam.name?.asString()}' must be Array<KClass<*>>.",
				groupsParam,
			)
			return null
		}
		
		val (packageName, simpleName) = resolveGeneratedFqcn(annotationDecl)
		val properties = mutableListOf<GeneratedMetadataPropertyModel>()
		for (param in params) {
			val name = param.name?.asString()
				?: continue
			properties += when (param) {
				messageParam -> GeneratedMetadataPropertyModel(
					name = name,
					typeSource = "String",
					isMessage = true,
				)
				
				groupsParam -> GeneratedMetadataPropertyModel(
					name = name,
					typeSource = "Set<KClass<*>>",
					isGroups = true,
					typeImports = listOf(TypeNames.KCLASS),
				)
				
				else -> ConstraintMetadataPropertyBuilder.buildPayloadProperty(param, annotationDecl)
			}
		}
		
		return GeneratedMetadataClassModel(
			packageName = packageName,
			simpleName = simpleName,
			annotationFqcn = annotationFqcn,
			properties = properties,
		)
	}
	
	/**
	 * Splits [MetadataFqcnResolver.conventionFqcn] into package and simple name for codegen.
	 *
	 * No side effects.
	 *
	 * @param annotationDecl `@Constraint` annotation class
	 * @return package name and generated metadata simple name	 
	 */
	private fun resolveGeneratedFqcn(annotationDecl: KSClassDeclaration): Pair<String, String> {
		val metadataFqcn = MetadataFqcnResolver.conventionFqcn(annotationDecl)
		val pkg = metadataFqcn.substringBeforeLast('.', "")
		val simple = metadataFqcn.substringAfterLast('.')
		return if (pkg.isNotBlank() && simple.isNotBlank()) pkg to simple
		else annotationDecl.packageName.asString() to (annotationDecl.simpleName.asString() + "Constraint")
	}
}

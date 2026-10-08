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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.KspEnumValues
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.GeneratedMetadataPropertyModel
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Renders Option-2 metadata property models (payload types + copied marker annotations).*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintMetadataPropertyBuilder {
	
	/**
	 * Builds one generated metadata property from a non-role annotation parameter.
	 *
	 * Mutates `imports` with types referenced by the rendered payload type.
	 *
	 * @param param annotation constructor parameter
	 * @param annotationDecl owning `@Constraint` annotation class
	 * @return property model with rendered type and copied markers	 
	 */
	fun buildPayloadProperty(
		param: KSValueParameter,
		annotationDecl: KSClassDeclaration,
	): GeneratedMetadataPropertyModel {
		val name = param.name?.asString().orEmpty()
		val imports = mutableListOf<String>()
		val typeSource = renderPayloadType(param.type.resolve(), annotationDecl, imports)
		return GeneratedMetadataPropertyModel(
			name = name,
			typeSource = typeSource,
			markerAnnotationsSource = renderCopiedMarkers(param),
			typeImports = imports.distinct(),
		)
	}
	
	/**
	 * Whether [type] is the Kotlin `String` type (nullable or not).
	 *
	 * No side effects.
	 *
	 * @param type resolved parameter type
	 * @return `true` for `kotlin.String` or simple name `String`	 
	 */
	fun isStringType(type: KSType): Boolean {
		val q = type.declaration.qualifiedName?.asString()
		return KnownTypes.isString(q.orEmpty()) || type.declaration.simpleName.asString() == "String"
	}
	
	/**
	 * Whether [type] is `Array<KClass<*>>` (Kotlin or Java array spelling).
	 *
	 * No side effects.
	 *
	 * @param type resolved parameter type
	 * @return `true` when the outer type is an array of `KClass`	 
	 */
	fun isKClassArrayType(type: KSType): Boolean {
		val q = type.declaration.qualifiedName?.asString()
		val simple = type.declaration.simpleName.asString()
		if (q != TypeNames.ARRAY_KOTLIN && simple != "Array") return false
		val arg = type.arguments.firstOrNull()?.type?.resolve()
			?: return false
		val argQ = arg.declaration.qualifiedName?.asString()
		return argQ == TypeNames.KCLASS || arg.declaration.simpleName.asString() == "KClass"
	}
	
	/**
	 * Renders the metadata payload type for one annotation parameter, mapping arrays to `Set`.
	 *
	 * Mutates [imports] with referenced user types.
	 *
	 * @param type resolved annotation parameter type
	 * @param annotationDecl owning annotation (for nested enum / sealed naming)
	 * @param imports collector for import lines in generated metadata
	 * @return Kotlin type source text for the metadata property	 
	 */
	private fun renderPayloadType(
		type: KSType,
		annotationDecl: KSClassDeclaration,
		imports: MutableList<String>,
	): String {
		val decl = type.declaration
		val qName = decl.qualifiedName?.asString()
		val simple = decl.simpleName.asString()
		
		if (qName == TypeNames.ARRAY_KOTLIN || simple == "Array") {
			val arg = type.arguments.firstOrNull()?.type?.resolve()
			val elem = arg?.let { renderPayloadType(it, annotationDecl, imports) }
				?: "Any"
			return "Set<$elem>"
		}
		
		KnownTypes.primitiveArrayElementFqcn(
			qName
				?: "")?.let { elemFqcn ->
				val elem = when (elemFqcn) {
					TypeNames.INT_KOTLIN -> "Int"
					TypeNames.LONG_KOTLIN -> "Long"
					TypeNames.SHORT_KOTLIN -> "Short"
					TypeNames.BYTE_KOTLIN -> "Byte"
					TypeNames.FLOAT_KOTLIN -> "Float"
					TypeNames.DOUBLE_KOTLIN -> "Double"
					TypeNames.BOOLEAN_KOTLIN -> "Boolean"
					TypeNames.CHAR_KOTLIN -> "Char"
					else -> elemFqcn
				}
				return "Set<$elem>"
			}
		
		if (type.arguments.isNotEmpty()) {
			val args = type.arguments.joinToString(", ") { arg ->
				arg.type?.resolve()?.let { renderPayloadType(it, annotationDecl, imports) }
					?: "*"
			}
			return "${renderRawName(type, annotationDecl, imports)}<$args>"
		}
		return renderRawName(type, annotationDecl, imports)
	}
	
	/**
	 * Renders a non-array type name for metadata, preferring simple names and nested types.
	 *
	 * Mutates [imports] when a fully qualified import is required.
	 *
	 * @param type resolved type reference
	 * @param annotationDecl owning annotation for nested type qualification
	 * @param imports collector for import lines in generated metadata
	 * @return Kotlin type name source text	 
	 */
	private fun renderRawName(
		type: KSType,
		annotationDecl: KSClassDeclaration,
		imports: MutableList<String>,
	): String {
		val decl = type.declaration as? KSClassDeclaration
			?: return type.declaration.simpleName.asString()
		val qName = decl.qualifiedName?.asString()
			?: return decl.simpleName.asString()
		val parent = decl.parentDeclaration as? KSClassDeclaration
		if (parent != null && parent.qualifiedName?.asString() == annotationDecl.qualifiedName?.asString()) {
			annotationDecl.qualifiedName?.asString()?.let { imports += it }
			return "${annotationDecl.simpleName.asString()}.${decl.simpleName.asString()}"
		}
		return when (qName) {
			TypeNames.STRING_KOTLIN -> "String"
			TypeNames.INT_KOTLIN -> "Int"
			TypeNames.LONG_KOTLIN -> "Long"
			TypeNames.BOOLEAN_KOTLIN -> "Boolean"
			TypeNames.DOUBLE_KOTLIN -> "Double"
			TypeNames.FLOAT_KOTLIN -> "Float"
			else -> {
				imports += qName
				decl.simpleName.asString()
			}
		}
	}
	
	/**
	 * Copies `@ConstraintArg`, `@ConstraintArgs`, and `@PropertyRef` markers onto generated metadata.
	 *
	 * No side effects.
	 *
	 * @param param annotation constructor parameter carrying marker annotations
	 * @return Kotlin source lines for metadata property markers	 
	 */
	private fun renderCopiedMarkers(param: KSValueParameter): List<String> {
		val lines = mutableListOf<String>()
		for (ann in param.annotations) {
			val fq = AnnotationFqcn.of(ann)
				?: continue
			when (fq) {
				ProcessorFqns.CONSTRAINT_ARG -> lines += renderConstraintArg(ann)
				ProcessorFqns.CONSTRAINT_ARGS -> {
					val nested = ann.arguments.firstOrNull {
						it.name?.asString() == AnnotationAttrs.ConstraintArgs.VALUE || it.name == null
					}?.value
					val items: List<*> = when (nested) {
						is List<*> -> nested
						null -> emptyList<Any>()
						else -> listOf(nested)
					}
					for (item in items) {
						val nestedAnn = item as? KSAnnotation
							?: continue
						lines += renderConstraintArg(nestedAnn)
					}
				}
				
				ProcessorFqns.PROPERTY_REF -> lines += renderPropertyRef(ann)
			}
		}
		return lines
	}
	
	/**
	 * Renders one `@ConstraintArg` marker for generated metadata source.
	 *
	 * No side effects.
	 *
	 * @param ann `@ConstraintArg` on an annotation parameter
	 * @return Kotlin annotation source text	 
	 */
	private fun renderConstraintArg(ann: KSAnnotation): String {
		val kinds = mutableListOf<String>()
		var message = ""
		var target: ConstraintArgTarget? = null
		val targetEntryNames = ConstraintArgTarget.entries.map { it.name }.toSet()
		for (arg in ann.arguments) {
			when (arg.name?.asString()) {
				AnnotationAttrs.ConstraintArg.KINDS, AnnotationAttrs.ConstraintArg.VALUE, null -> {
					val items: List<*> = when (val raw = arg.value) {
						is List<*> -> raw
						null -> emptyList<Any>()
						else -> listOf(raw)
					}
					for (item in items) {
						val kind = KspEnumValues.parse<ConstraintArgKind>(item)
						if (kind != null) {
							kinds += "ConstraintArgKind.${kind.name}"
							continue
						}
						if (KspEnumValues.entryName(item) in targetEntryNames) continue
					}
				}
				
				AnnotationAttrs.ConstraintArg.TARGET -> target = KspEnumValues.parse(arg.value)
				AnnotationAttrs.ConstraintArg.MESSAGE -> message = (arg.value as? String).orEmpty()
			}
		}
		val parts = mutableListOf<String>()
		if (kinds.isNotEmpty()) parts += kinds.joinToString(", ")
		if (target != null && target != ConstraintArgTarget.VALUE) {
			parts += "target = ConstraintArgTarget.${target.name}"
		}
		if (message.isNotBlank()) parts += "message = \"${message.escape()}\""
		return "@ConstraintArg(${parts.joinToString(", ")})"
	}
	
	/**
	 * Renders one `@PropertyRef` marker for generated metadata source.
	 *
	 * No side effects.
	 *
	 * @param ann `@PropertyRef` on an annotation parameter
	 * @return Kotlin annotation source text	 
	 */
	private fun renderPropertyRef(ann: KSAnnotation): String {
		var scope: PropertyRefScope? = null
		var compatibility: PropertyRefCompatibilityKind? = null
		for (arg in ann.arguments) {
			when (arg.name?.asString()) {
				AnnotationAttrs.PropertyRef.SCOPE -> scope = KspEnumValues.parse(arg.value)
				AnnotationAttrs.PropertyRef.COMPATIBILITY -> compatibility = KspEnumValues.parse(arg.value)
			}
		}
		val parts = mutableListOf<String>()
		if (scope != null && scope != PropertyRefScope.SIBLING) {
			parts += "scope = PropertyRefScope.${scope.name}"
		}
		if (compatibility != null && compatibility != PropertyRefCompatibilityKind.NONE) {
			parts += "compatibility = PropertyRefCompatibilityKind.${compatibility.name}"
		}
		return if (parts.isEmpty()) "@PropertyRef" else "@PropertyRef(${parts.joinToString(", ")})"
	}
	
	/**
	 * Escapes [this] for embedding in a generated Kotlin string literal.
	 *
	 * No side effects.
	 *
	 * @return escaped string body without surrounding quotes	 
	 */
	private fun String.escape(): String =
		io.ghaylan.validata.processor.compat.KotlinStringLiteral.escape(this)
}

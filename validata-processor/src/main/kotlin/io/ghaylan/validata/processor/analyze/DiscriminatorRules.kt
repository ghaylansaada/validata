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
import com.google.devtools.ksp.symbol.*
import io.ghaylan.validata.processor.compat.TypeClassification
import io.ghaylan.validata.processor.fqns.AnnotationAttrs

/**
 * Discriminator property checks for `@Validatable(discriminator = …)`.
 *
 * @property logger emits KSP errors for missing or non-scalar discriminator properties*
 * 
 * @author Ghaylan Saada
 */
internal class DiscriminatorRules(
	private val logger: KSPLogger,
) {
	
	/**
	 * When `discriminator` is non-blank, requires a **scalar** Kotlin property of that name on
	 * [clazz] (looked up in [propertyIndex], typically from [KSClassDeclaration.getAllProperties]).
	 *
	 * May emit KSP errors via [logger].
	 *
	 * @param clazz `@Validatable` type carrying the discriminator attribute
	 * @param validatableAnn `@Validatable` usage on [clazz], or `null` when absent
	 * @param propertyIndex simple-name index of properties on [clazz]	 
	 */
	fun verifyDiscriminator(
		clazz: KSClassDeclaration,
		validatableAnn: KSAnnotation?,
		propertyIndex: Map<String, KSPropertyDeclaration>,
	) {
		if (validatableAnn == null) return
		val raw = validatableAnn.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.Validatable.DISCRIMINATOR }?.value as? String
			?: return
		val discriminator = raw.trim()
		if (discriminator.isEmpty()) return
		val prop = propertyIndex[discriminator]
		if (prop == null) {
			logger.error(
				"@Validatable on '${clazz.qualifiedName?.asString()}' declares discriminator " + "'$discriminator', but that type has no property named '$discriminator'. " + "Add the property (or fix the discriminator spelling).",
				validatableAnn,
			)
			return
		}
		val discType = prop.type.resolve()
		if (!isTypedLiteralScalar(discType)) {
			val typeQ = discType.makeNotNullable().declaration.qualifiedName?.asString()
				?: discType.toString()
			logger.error(
				"@Validatable on '${clazz.qualifiedName?.asString()}' declares discriminator " + "'$discriminator' of type '$typeQ', but only scalar types are allowed " + "(String, number, temporal, or enum).",
				validatableAnn,
			)
		}
	}
	
	/**
	 * Resolves the discriminator property type when `discriminator` is non-blank, present, and
	 * a typed-literal scalar.
	 *
	 * No side effects.
	 *
	 * @param clazz `@Validatable` type carrying the discriminator attribute
	 * @param validatableAnn `@Validatable` usage on [clazz], or `null` when absent
	 * @param propertyIndex simple-name index of properties on [clazz]
	 * @return resolved scalar discriminator type, or `null` when absent or invalid	 
	 */
	fun discriminatorPropertyType(
		clazz: KSClassDeclaration,
		validatableAnn: KSAnnotation?,
		propertyIndex: Map<String, KSPropertyDeclaration>,
	): KSType? {
		if (validatableAnn == null) return null
		val raw = validatableAnn.arguments.firstOrNull { it.name?.asString() == AnnotationAttrs.Validatable.DISCRIMINATOR }?.value as? String
			?: return null
		val discriminator = raw.trim()
		if (discriminator.isEmpty()) return null
		val prop = propertyIndex[discriminator]
			?: return null
		val type = prop.type.resolve()
		return type.takeIf { isTypedLiteralScalar(it) }
	}
	
	/** Whether [type] is a typed-literal scalar host for discriminator / `Subtype.name`.
	 *
	 * No side effects.
	 *
	 * @param type property or literal host type
	 * @return `true` for enum or platform scalar types	 
	 */
	fun isTypedLiteralScalar(type: KSType): Boolean {
		val notNull = type.makeNotNullable()
		val decl = notNull.declaration as? KSClassDeclaration
		if (decl?.classKind == ClassKind.ENUM_CLASS) return true
		val qName = decl?.qualifiedName?.asString()
			?: return false
		return TypeClassification.isScalar(qName)
	}
	
	companion object {
		
		/**
		 * Builds a simple-name → property map from [KSClassDeclaration.getAllProperties].
		 *
		 * No side effects.
		 *
		 * @param clazz class whose properties are indexed
		 * @return map keyed by property simple name		 
		 */
		fun propertyIndex(clazz: KSClassDeclaration): Map<String, KSPropertyDeclaration> {
			val map = HashMap<String, KSPropertyDeclaration>()
			for (prop in clazz.getAllProperties()) {
				map[prop.simpleName.asString()] = prop
			}
			return map
		}
	}
}

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
package io.ghaylan.validata.openapi.aot

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.enrichment.ConstraintMetadataOpenApiSerializer
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.presentation.ApiError
import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.registerType

/**
 * AOT / GraalVM hints for Validata OpenAPI ServiceLoaders, presentation annotations, and
 * constraint metadata reflection used when serializing `x-validata-constraints`.
 *
 * Registers resource patterns and reflective access so native images can discover
 * [OpenApiConstraintMapper] / [ConstraintDocumentation] implementations, read
 * presentation annotations, and introspect built-in `*Constraint` metadata classes.*
 * 
 * @author Ghaylan Saada
 */
class ValidataOpenApiRuntimeHints: RuntimeHintsRegistrar {
	
	/**
	 * Registers ServiceLoader resource patterns and reflective access for OpenAPI SPI types,
	 * presentation annotations, and built-in constraint metadata.
	 *
	 * Mutates [hints].
	 *
	 * @param hints Spring AOT hints builder
	 * @param classLoader unused; hints are type-literal	 
	 */
	override fun registerHints(
		hints: RuntimeHints,
		classLoader: ClassLoader?
	) {
		hints.resources()
			.registerPattern("META-INF/services/" + OpenApiConstraintMapper::class.java.name)
			.registerPattern("META-INF/services/" + ConstraintDocumentation::class.java.name)
			
		val reflection = hints.reflection()
		
		reflection.registerType<OpenApiConstraintMapper>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<ConstraintDocumentation>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<ErrorDocPublisher>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<ApiError>(MemberCategory.INVOKE_DECLARED_METHODS)
			.registerType<ConstraintMetadata>(MemberCategory.INVOKE_PUBLIC_METHODS)
		
		for (type in BUILTIN_CONSTRAINT_METADATA) {
			reflection.registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
		}
	}
	
	/**
	 * Built-in KSP-generated metadata classes introspected by [ConstraintMetadataOpenApiSerializer].
	 */
	companion object {
		
		/**
		 * First-party constraint metadata types registered for native-image reflection.
		 */
		val BUILTIN_CONSTRAINT_METADATA: List<Class<*>> = listOf(
			AssertConstraint::class.java,
			BarcodeConstraint::class.java,
			Base64Constraint::class.java,
			RelativeToNowConstraint::class.java,
			CompareConstraint::class.java,
			CompositionConstraint::class.java,
			ContainsConstraint::class.java,
			CoordinateConstraint::class.java,
			CreditCardConstraint::class.java,
			DaysOfMonthConstraint::class.java,
			DaysOfWeekConstraint::class.java,
			DigitsConstraint::class.java,
			DistinctConstraint::class.java,
			EmailConstraint::class.java,
			FilePathConstraint::class.java,
			HexColorConstraint::class.java,
			HtmlConstraint::class.java,
			FinancialCodeConstraint::class.java,
			ChecksumConstraint::class.java,
			IpAddressConstraint::class.java,
			IsoCountryConstraint::class.java,
			IsoCurrencyConstraint::class.java,
			IsoLanguageConstraint::class.java,
			MaxConstraint::class.java,
			MinConstraint::class.java,
			MonthsConstraint::class.java,
			MultipleOfConstraint::class.java,
			NumberSignConstraint::class.java,
			NumberParityConstraint::class.java,
			PasswordConstraint::class.java,
			PhoneConstraint::class.java,
			RangeConstraint::class.java,
			RegexConstraint::class.java,
			RequiredConstraint::class.java,
			RequiredWhenConstraint::class.java,
			SizeConstraint::class.java,
			UrlConstraint::class.java,
			InConstraint::class.java,
			NotInConstraint::class.java)
	}
}

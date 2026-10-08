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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.openapi.docs.ConstraintDocumentations
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMappers
import io.ghaylan.validata.openapi.support.OddYearsProbeConstraint
import io.ghaylan.validata.openapi.support.OddYearsProbeOpenApiConstraintMapper
import io.ghaylan.validata.openapi.support.UnknownProbeConstraint
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.MapShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import kotlin.reflect.KClass

/**
 * Constraint metadata → OpenAPI via [ConstraintDocumentations] (+ optional OpenAPI mappers).
 *
 * Guards Size shape-sensitivity, unmapped-list accumulation, and HexColor's bounded pattern.
 * 
 * @author Ghaylan Saada
 */
class ConstraintSchemaApplicatorTest {
	
	private val groups: Set<KClass<*>> = setOf(OnDefault::class)
	
	/**
	 * Prefer core SPI; no OpenAPI-only mappers.
	 */
	private val mappers: List<OpenApiConstraintMapper> = emptyList()
	
	@BeforeEach
	fun reset() {
		OpenApiConstraintMappers.resetForTests()
		ConstraintDocumentations.resetForTests()
	}
	
	@Test
	@DisplayName("Size on string shape sets minLength/maxLength")
	fun sizeOnStringShape() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			SizeConstraint(min = 2, max = 40, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.minLength).isEqualTo(2)
		assertThat(schema.maxLength).isEqualTo(40)
		assertThat(integerFacet(schema, "getMinItems")).isNull()
		assertThat(integerFacet(schema, "getMaxItems")).isNull()
	}
	
	@Test
	@DisplayName("Size on iterable shape sets minItems/maxItems — not minLength")
	fun sizeOnIterableShape() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			SizeConstraint(min = 1, max = 5, message = "", groups = groups),
			schema,
			IterableShape(element = ScalarShape(ScalarKind.STRING)),
			mappers,
		)
		assertThat(schema.minItems).isEqualTo(1)
		assertThat(schema.maxItems).isEqualTo(5)
		assertThat(integerFacet(schema, "getMinLength")).isNull()
		assertThat(integerFacet(schema, "getMaxLength")).isNull()
	}
	
	@Test
	@DisplayName("Size on map shape sets minProperties/maxProperties")
	fun sizeOnMapShape() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			SizeConstraint(min = 0, max = 3, message = "", groups = groups),
			schema,
			MapShape(
				key = ScalarShape(ScalarKind.STRING),
				value = ScalarShape(ScalarKind.STRING),
			),
			mappers,
		)
		assertThat(schema.minProperties).isEqualTo(0)
		assertThat(schema.maxProperties).isEqualTo(3)
		assertThat(integerFacet(schema, "getMinLength")).isNull()
		assertThat(integerFacet(schema, "getMinItems")).isNull()
	}
	
	@Test
	@DisplayName("Regex sets pattern")
	fun regexSetsPattern() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			RegexConstraint(pattern = "^[A-Z]+$", name = "", message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.pattern).isEqualTo("^[A-Z]+$")
	}
	
	@Test
	@DisplayName("@In sets enum")
	fun inSetsEnum() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			InConstraint(
				values = setOf("B", "A"),
				message = "",
				groups = groups,
			),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.enum).containsExactly("A", "B")
	}

	@Test
	@DisplayName("@NotIn sets not.enum forbidden values")
	fun notInSetsNotEnum() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			NotInConstraint(
				values = setOf("B", "A"),
				message = "",
				groups = groups,
			),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.enum).isNull()
		assertThat(schema.not?.enum).containsExactly("A", "B")
	}
	
	@Test
	@DisplayName("DaysOfWeek allow-list sets enum of day names")
	fun daysOfWeekSetsEnum() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			DaysOfWeekConstraint(
				days = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
				negated = false,
				message = "",
				groups = groups,
			),
			schema,
			ScalarShape(ScalarKind.TEMPORAL),
			mappers,
		)
		assertThat(schema.enum).containsExactly("FRIDAY", "MONDAY")
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
	}
	
	@Test
	@DisplayName("DaysOfWeek negated sets not.enum of forbidden day names")
	fun daysOfWeekNegatedSetsNotEnum() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			DaysOfWeekConstraint(
				days = setOf(DayOfWeek.SUNDAY),
				negated = true,
				message = "",
				groups = groups,
			),
			schema,
			ScalarShape(ScalarKind.TEMPORAL),
			mappers,
		)
		assertThat(schema.not?.enum).containsExactly("SUNDAY")
	}
	
	@Test
	@DisplayName("Barcode and FinancialCode emit type-specific formats")
	fun barcodeAndFinancialFormats() {
		val ean = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			BarcodeConstraint(type = Barcode.Type.EAN, message = "", groups = groups),
			ean,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(ean.format).isEqualTo("ean")
		
		val isbn = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			BarcodeConstraint(type = Barcode.Type.ISBN, message = "", groups = groups),
			isbn,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(isbn.format).isEqualTo("isbn")
		
		val iban = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			FinancialCodeConstraint(
				type = FinancialCode.Type.IBAN,
				countries = emptySet(),
				message = "",
				groups = groups,
			),
			iban,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(iban.format).isEqualTo("iban")
		
		val isin = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			FinancialCodeConstraint(
				type = FinancialCode.Type.ISIN,
				countries = emptySet(),
				message = "",
				groups = groups,
			),
			isin,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(isin.format).isEqualTo("isin")
	}
	
	@Test
	@DisplayName("Password emits format=password and length bounds")
	fun passwordFormatAndLengths() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			PasswordConstraint(
				minLength = 10,
				maxLength = 32,
				requireUppercase = false,
				requireLowercase = false,
				requireDigit = false,
				requireSpecialChar = false,
				allowedSpecialChars = "!@",
				noSequentialChars = false,
				noRepetitivePatterns = false,
				message = "",
				groups = groups,
			),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.format).isEqualTo("password")
		assertThat(schema.minLength).isEqualTo(10)
		assertThat(schema.maxLength).isEqualTo(32)
	}
	
	@Test
	@DisplayName("Min/Max set numeric bounds and exclusive flags")
	fun minMaxBounds() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			MinConstraint(value = "18", inclusive = true, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.INTEGRAL),
			mappers,
		)
		ConstraintSchemaApplicator.apply(
			MaxConstraint(value = "120", inclusive = false, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.INTEGRAL),
			mappers,
		)
		assertThat(schema.minimum).isEqualByComparingTo(BigDecimal("18"))
		assertThat(schema.maximum).isEqualByComparingTo(BigDecimal("120"))
		assertThat(schema.exclusiveMaximum).isTrue()
	}
	
	@Test
	@DisplayName("Range sets both numeric bounds and exclusive flags")
	fun rangeBounds() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			RangeConstraint(
				from = "18",
				to = "120",
				fromInclusive = true,
				toInclusive = false,
				negated = false,
				message = "",
				groups = groups,
			),
			schema,
			ScalarShape(ScalarKind.INTEGRAL),
			mappers,
		)
		assertThat(schema.minimum).isEqualByComparingTo(BigDecimal("18"))
		assertThat(schema.maximum).isEqualByComparingTo(BigDecimal("120"))
		assertThat(schema.exclusiveMaximum).isTrue()
		assertThat(schema.exclusiveMinimum).isFalse()
	}
	
	@Test
	@DisplayName("Required marks nullable=false and signals parent required")
	fun requiredMarksExtension() {
		val schema = Schema<Any>()
		val required = ConstraintSchemaApplicator.apply(
			RequiredConstraint(mode = Required.Mode.STRICT, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(required).isTrue()
		assertThat(schema.nullable).isFalse()
		assertThat(schema.extensions?.get("x-validata-required")).isNull()
	}
	
	@Test
	@DisplayName("HexColor publishes the same pattern as HexColorValidator")
	fun hexColorBoundedPattern() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			HexColorConstraint("", groups),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.pattern).isEqualTo("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
	}
	
	@Test
	@DisplayName("CompositionConstraint owns no native facets and is not unmapped")
	fun compositionHasNoNativeFacets() {
		ConstraintDocumentations.resetForTests()
		val schema = Schema<Any>()
		val emailMeta = EmailConstraint("", groups)
		val composition = CompositionConstraint(
			message = "",
			groups = groups,
			children = listOf(
				CompiledConstraint(emailMeta, ValidatorBackedRunner(EmailValidator, emailMeta), 0),
			),
		)
		ConstraintSchemaApplicator.apply(composition, schema, ScalarShape(ScalarKind.STRING), mappers)
		assertThat(schema.format).isNull()
		assertThat(schema.pattern).isNull()
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
	}
	
	@Test
	@DisplayName("Unknown custom metadata is not flagged unmapped (goes to x-validata-constraints)")
	fun unknownCustomSkipsUnmapped() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			UnknownProbeConstraint(),
			schema,
			null,
			mappers,
		)
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
	}
	
	@Test
	@DisplayName("Custom OpenApiConstraintMapper on classpath wins over documenters")
	fun customMapperFromServiceLoader() {
		OpenApiConstraintMappers.resetForTests()
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			OddYearsProbeConstraint(),
			schema,
			ScalarShape(ScalarKind.INTEGRAL),
			OpenApiConstraintMappers.all(),
		)
		assertThat(schema.extensions[OddYearsProbeOpenApiConstraintMapper.PROBE_EXTENSION]).isEqualTo(OddYearsProbeOpenApiConstraintMapper.PROBE_VALUE)
		assertThat(schema.extensions).doesNotContainKey(ConstraintExtensionKeys.UNMAPPED)
		assertThat(schema.description).isNull()
	}
	
	@Test
	@DisplayName("owned non-numeric Min/MultipleOf are not flagged unmapped")
	fun ownedNonNumericBoundsAreNotUnmapped() {
		val schema = Schema<Any>()
		val shape = ScalarShape(ScalarKind.STRING)
		ConstraintSchemaApplicator.apply(
			MinConstraint(value = "2020-02-29", inclusive = true, message = "", groups = groups),
			schema,
			shape,
			mappers,
		)
		ConstraintSchemaApplicator.apply(
			MultipleOfConstraint("also-bad", "", groups),
			schema,
			shape,
			mappers,
		)
		ConstraintSchemaApplicator.apply(
			MinConstraint(value = "PT2H", inclusive = true, message = "", groups = groups),
			schema,
			shape,
			mappers,
		)
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
		assertThat(schema.minimum).isNull()
		assertThat(schema.multipleOf).isNull()
	}
	
	@Test
	@DisplayName("Coordinate LATITUDE sets native minimum/maximum bounds")
	fun latitudeNativeBounds() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			CoordinateConstraint(axis = Coordinate.Axis.LATITUDE, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.DECIMAL),
			mappers,
		)
		assertThat(schema.minimum).isEqualByComparingTo(BigDecimal("-90"))
		assertThat(schema.maximum).isEqualByComparingTo(BigDecimal("90"))
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
	}
	
	@Test
	@DisplayName("NumberSign POSITIVE sets exclusive minimum 0 unless allowZero")
	fun positiveNativeBounds() {
		val strict = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			NumberSignConstraint(
				sign = NumberSign.Sign.POSITIVE,
				allowZero = false,
				message = "",
				groups = groups,
			),
			strict,
			ScalarShape(ScalarKind.DECIMAL),
			mappers,
		)
		assertThat(strict.minimum).isEqualByComparingTo(BigDecimal.ZERO)
		assertThat(strict.exclusiveMinimum).isTrue()
		val withZero = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			NumberSignConstraint(
				sign = NumberSign.Sign.POSITIVE,
				allowZero = true,
				message = "",
				groups = groups,
			),
			withZero,
			ScalarShape(ScalarKind.DECIMAL),
			mappers,
		)
		assertThat(withZero.minimum).isEqualByComparingTo(BigDecimal.ZERO)
		assertThat(withZero.exclusiveMinimum).isFalse()
	}
	
	@Test
	@DisplayName("NumberSign NEGATIVE sets exclusive maximum 0 unless allowZero")
	fun negativeNativeBounds() {
		val strict = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			NumberSignConstraint(
				sign = NumberSign.Sign.NEGATIVE,
				allowZero = false,
				message = "",
				groups = groups,
			),
			strict,
			ScalarShape(ScalarKind.DECIMAL),
			mappers,
		)
		assertThat(strict.maximum).isEqualByComparingTo(BigDecimal.ZERO)
		assertThat(strict.exclusiveMaximum).isTrue()
		val withZero = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			NumberSignConstraint(
				sign = NumberSign.Sign.NEGATIVE,
				allowZero = true,
				message = "",
				groups = groups,
			),
			withZero,
			ScalarShape(ScalarKind.DECIMAL),
			mappers,
		)
		assertThat(withZero.maximum).isEqualByComparingTo(BigDecimal.ZERO)
		assertThat(withZero.exclusiveMaximum).isFalse()
	}
	
	@Test
	@DisplayName("NumberParity EVEN sets multipleOf 2")
	fun evenMultipleOf() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			NumberParityConstraint(value = NumberParity.Value.EVEN, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.INTEGRAL),
			mappers,
		)
		assertThat(schema.multipleOf).isEqualByComparingTo(BigDecimal("2"))
		assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).isNull()
	}
	
	@Test
	@DisplayName("Odd / Digits / Barcode are owned with no native facets")
	fun oddDigitsBarcodeOwnedWithoutNativeFacets() {
		val shape = ScalarShape(ScalarKind.STRING)
		for (meta in listOf(
			NumberParityConstraint(value = NumberParity.Value.ODD, message = "", groups = groups),
			io.ghaylan.validata.constraint.annotation.DigitsConstraint(3, 2, "", groups),
			io.ghaylan.validata.constraint.annotation.BarcodeConstraint(
				io.ghaylan.validata.constraint.annotation.Barcode.Type.ISBN,
				"",
				groups,
			),
		)) {
			val schema = Schema<Any>()
			ConstraintSchemaApplicator.apply(meta, schema, shape, mappers)
			assertThat(schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)).describedAs(meta::class.java.simpleName)
				.isNull()
			assertThat(schema.pattern).isNull()
			assertThat(schema.multipleOf).isNull()
			assertThat(schema.minimum).isNull()
			assertThat(schema.maximum).isNull()
		}
	}
	
	@Test
	@DisplayName("Size max Int.MAX_VALUE omits maxLength")
	fun sizeMaxIntMaxValueOmitsMaxLength() {
		val schema = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			SizeConstraint(min = 1, max = Int.MAX_VALUE, message = "", groups = groups),
			schema,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(schema.minLength).isEqualTo(1)
		assertThat(integerFacet(schema, "getMaxLength")).isNull()
	}
	
	@Test
	@DisplayName("IpAddress ANY emits no format; Coordinate LONGITUDE sets geographic bounds")
	fun ipAnyAndLongitudeFacets() {
		val ip = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			IpAddressConstraint(type = IpAddress.Type.ANY, message = "", groups = groups),
			ip,
			ScalarShape(ScalarKind.STRING),
			mappers,
		)
		assertThat(ip.format).isNull()
		val lon = Schema<Any>()
		ConstraintSchemaApplicator.apply(
			CoordinateConstraint(axis = Coordinate.Axis.LONGITUDE, message = "", groups = groups),
			lon,
			ScalarShape(ScalarKind.DECIMAL),
			mappers,
		)
		assertThat(lon.minimum).isEqualByComparingTo(BigDecimal("-180"))
		assertThat(lon.maximum).isEqualByComparingTo(BigDecimal("180"))
	}
	
	@Test
	@DisplayName("ServiceLoader discovers built-in ConstraintDocumentation for Size")
	fun serviceLoaderDiscoversBuiltInDocs() {
		ConstraintDocumentations.resetForTests()
		val hints = ConstraintDocumentations.resolve(
			SizeConstraint(1, 2, "", groups),
			ScalarShape(ScalarKind.STRING),
		)
		assertThat(hints.isEmpty).isFalse()
		assertThat(hints.facets).isNotEmpty()
	}
	
	private fun integerFacet(
		schema: Schema<*>,
		getter: String
	): Int? = Schema::class.java.getMethod(getter)
		.invoke(schema) as Int?
}

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
package io.ghaylan.validata.constraint.spi

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.constraint.validator.bound.duration.DurationMaxValidator
import io.ghaylan.validata.constraint.validator.bound.duration.DurationMinValidator
import io.ghaylan.validata.constraint.validator.bound.duration.DurationRangeValidator
import io.ghaylan.validata.constraint.validator.bound.month.MonthMaxValidator
import io.ghaylan.validata.constraint.validator.bound.month.MonthMinValidator
import io.ghaylan.validata.constraint.validator.bound.month.MonthRangeValidator
import io.ghaylan.validata.constraint.validator.bound.monthday.MonthDayMaxValidator
import io.ghaylan.validata.constraint.validator.bound.monthday.MonthDayMinValidator
import io.ghaylan.validata.constraint.validator.bound.monthday.MonthDayRangeValidator
import io.ghaylan.validata.constraint.validator.bound.period.PeriodMaxValidator
import io.ghaylan.validata.constraint.validator.bound.period.PeriodMinValidator
import io.ghaylan.validata.constraint.validator.bound.period.PeriodRangeValidator
import io.ghaylan.validata.constraint.validator.bound.yearmonth.YearMonthMaxValidator
import io.ghaylan.validata.constraint.validator.bound.yearmonth.YearMonthMinValidator
import io.ghaylan.validata.constraint.validator.bound.yearmonth.YearMonthRangeValidator
import io.ghaylan.validata.constraint.validator.number.max.NumberMaxValidator
import io.ghaylan.validata.constraint.validator.number.min.NumberMinValidator
import io.ghaylan.validata.constraint.validator.number.range.NumberRangeValidator
import io.ghaylan.validata.constraint.validator.size.ArraySizeValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.constraint.validator.size.CollectionSizeValidator
import io.ghaylan.validata.constraint.validator.size.MapSizeValidator
import io.ghaylan.validata.constraint.validator.temporal.max.TemporalMaxValidator
import io.ghaylan.validata.constraint.validator.temporal.min.TemporalMinValidator
import io.ghaylan.validata.constraint.validator.temporal.range.TemporalRangeValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Catalog completeness for every built-in `@Constraint` annotation.
 *
 * Locks annotation → `@Constraint.validatedBy` → generated [ConstraintCatalog] entries so a
 * missing binding cannot ship silently.
 * 
 * @author Ghaylan Saada
 */
class BuiltInCatalogCompletenessTest {
	
	private val builtInAnnotations: List<KClass<out Annotation>> = listOf(
		Assert::class,
		Barcode::class,
		Base64::class,
		Contains::class,
		RelativeToNow::class,
		Compare::class,
		Coordinate::class,
		CreditCard::class,
		DaysOfMonth::class,
		DaysOfWeek::class,
		Digits::class,
		Distinct::class,
		Email::class,
		FilePath::class,
		HexColor::class,
		Html::class,
		FinancialCode::class,
		Checksum::class,
		IpAddress::class,
		IsoCountry::class,
		IsoCurrency::class,
		IsoLanguage::class,
		Max::class,
		Min::class,
		Months::class,
		MultipleOf::class,
		NumberSign::class,
		NumberParity::class,
		Password::class,
		Phone::class,
		Range::class,
		Regex::class,
		Required::class,
		RequiredWhen::class,
		Size::class,
		Url::class,
		In::class,
		NotIn::class,
	)
	
	@Nested
	@DisplayName("annotation inventory")
	inner class AnnotationInventory {
		
		@Test
		@DisplayName("exactly 38 built-in @Constraint annotations are tracked")
		fun exactlyThirtyEightBuiltIns() {
			assertThat(builtInAnnotations).hasSize(38)
		}
		
		@Test
		@DisplayName("every built-in annotation carries @Constraint")
		fun everyAnnotationHasConstraintMeta() {
			for (annotation in builtInAnnotations) {
				assertThat(annotation.java.getAnnotation(Constraint::class.java)).withFailMessage { "${annotation.simpleName} missing @Constraint" }.isNotNull
			}
		}
	}
	
	@Nested
	@DisplayName("validatedBy ↔ catalog")
	inner class ValidatedByMatchesCatalog {
		
		@Test
		@DisplayName("every annotation has catalog entries whose validatorTypes exactly match validatedBy")
		fun catalogMatchesValidatedBy() {
			GeneratedConstraintCatalogs.resetForTests()
			val entries = GeneratedConstraintCatalogs.all()
			
			for (annotation in builtInAnnotations) {
				val constraintMeta = annotation.java.getAnnotation(Constraint::class.java)!!
				val expectedValidators = constraintMeta.validatedBy.map { it.java }
					.toSet()
				val catalogValidators = entries.filter { it.annotationType == annotation.java }
					.map { it.validatorType }
					.toSet()
				
				assertThat(catalogValidators).withFailMessage {
					"${annotation.simpleName}: catalog validators=$catalogValidators " + "validatedBy=$expectedValidators"
				}
					.isEqualTo(expectedValidators)
			}
		}
		
		@Test
		@DisplayName("every catalog factory returns an instance of its validatorType")
		fun factoriesMatchValidatorType() {
			GeneratedConstraintCatalogs.resetForTests()
			for (entry in GeneratedConstraintCatalogs.all()) {
				val instance = entry.defaultInstanceFactory()
				assertThat(instance).withFailMessage { "${entry.validatorType.name} factory returned null-incompatible type" }
					.isInstanceOf(entry.validatorType)
			}
		}
	}
	
	@Nested
	@DisplayName("multi-validator bindings")
	inner class MultiValidatorBindings {
		
		@Test
		@DisplayName("@Min binds exactly 7 validators (number + temporal families)")
		fun minHasSevenValidators() {
			assertValidators(
				Min::class,
				NumberMinValidator::class.java,
				TemporalMinValidator::class.java,
				DurationMinValidator::class.java,
				PeriodMinValidator::class.java,
				YearMonthMinValidator::class.java,
				MonthDayMinValidator::class.java,
				MonthMinValidator::class.java,
			)
		}
		
		@Test
		@DisplayName("@Max binds exactly 7 validators (number + temporal families)")
		fun maxHasSevenValidators() {
			assertValidators(
				Max::class,
				NumberMaxValidator::class.java,
				TemporalMaxValidator::class.java,
				DurationMaxValidator::class.java,
				PeriodMaxValidator::class.java,
				YearMonthMaxValidator::class.java,
				MonthDayMaxValidator::class.java,
				MonthMaxValidator::class.java,
			)
		}
		
		@Test
		@DisplayName("@Range binds exactly 7 validators (number + temporal families)")
		fun rangeHasSevenValidators() {
			assertValidators(
				Range::class,
				NumberRangeValidator::class.java,
				TemporalRangeValidator::class.java,
				DurationRangeValidator::class.java,
				PeriodRangeValidator::class.java,
				YearMonthRangeValidator::class.java,
				MonthDayRangeValidator::class.java,
				MonthRangeValidator::class.java,
			)
		}
		
		@Test
		@DisplayName("@Size binds exactly 4 carrier validators")
		fun sizeHasFourValidators() {
			assertValidators(
				Size::class,
				CharSequenceSizeValidator::class.java,
				CollectionSizeValidator::class.java,
				MapSizeValidator::class.java,
				ArraySizeValidator::class.java,
			)
		}
	}
	
	@Nested
	@DisplayName("catalog scale")
	inner class CatalogScale {
		
		@Test
		@DisplayName("built-in catalog exposes at least one entry per validator binding (≥ 50)")
		fun atLeastFiftyEntries() {
			GeneratedConstraintCatalogs.resetForTests()
			val entries = GeneratedConstraintCatalogs.all()
			val distinctValidators = entries.map { it.validatorType }
				.toSet()
			assertThat(distinctValidators).withFailMessage {
				"validators=${
					distinctValidators.map { it.simpleName }
						.sorted()
				}"
			}
				.hasSizeGreaterThanOrEqualTo(50)
			assertThat(entries.size).isGreaterThanOrEqualTo(50)
		}
	}
	
	private fun assertValidators(
		annotation: KClass<out Annotation>,
		vararg expected: Class<out io.ghaylan.validata.constraint.ConstraintValidator<*, *>>,
	) {
		GeneratedConstraintCatalogs.resetForTests()
		val catalogValidators = GeneratedConstraintCatalogs.all()
			.filter { it.annotationType == annotation.java }
			.map { it.validatorType }
			.toSet()
		assertThat(catalogValidators).containsExactlyInAnyOrder(*expected)
	}
}

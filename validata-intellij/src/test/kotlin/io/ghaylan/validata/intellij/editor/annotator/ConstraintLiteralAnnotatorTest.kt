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

package io.ghaylan.validata.intellij.editor.annotator

import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.support.PropertyRefLightFixtures
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Platform coverage for [ConstraintLiteralAnnotator] via `@ConstraintArg` discovery.
 * 
 * @author Ghaylan Saada
 */
class ConstraintLiteralAnnotatorTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("invalid @Min number literal is highlighted")
	fun testInvalidNumberMinIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"BadNumber.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Min
			data class User(
			  @field:Min("aaa")
			  val age: Int?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected invalid number highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("decimal number") == true || it.description?.contains("Min") == true || it.description?.contains("value") == true
			},
		)
	}
	
	@DisplayName("valid @Min number literal is not an error")
	fun testValidNumberMinIsNotError() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"OkNumber.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Min
			data class User(
			  @field:Min("18")
			  val age: Int?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			highlights.any {
				it.description?.contains("decimal number") == true || it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && it.description?.contains(
					"value") == true
			},
		)
		assertTrue(
			"valid number Min should use NUMBER attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.NUMBER },
		)
	}
	
	@DisplayName("valid temporal @Min literal uses temporal color")
	fun testValidTemporalMinUsesTemporalColor() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"OkDate.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Min
			import java.time.LocalDate
			data class Window(
			  @field:Min("2020-01-01")
			  val start: LocalDate?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && it.description?.contains("literal") == true
			},
		)
		assertTrue(
			"valid temporal Min should use TEMPORAL attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.TEMPORAL },
		)
	}
	
	@DisplayName("valid Month @Min literal uses ENUM color (not temporal amber)")
	fun testValidMonthMinUsesEnumColor() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"OkMonth.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Min
			import java.time.Month
			data class Season(
			  @field:Min("MARCH")
			  val earliest: Month?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && it.description?.contains("literal") == true
			},
		)
		assertTrue(
			"Month enum name should use ENUM attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.ENUM },
		)
		assertFalse(
			"Month enum name must not use TEMPORAL amber",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.TEMPORAL },
		)
	}
	
	@DisplayName("blank @Min number literal is highlighted")
	fun testBlankNumberMinIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"BlankNumber.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Min
			data class User(
			  @field:Min("")
			  val age: Int?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected blank highlight, got: ${highlights.map { it.description }}",
			highlights.any { it.description?.contains("blank") == true },
		)
	}
	
	@DisplayName("invalid @In enum literal is highlighted")
	fun testInvalidEnumInIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"BadEnum.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN, USER }
			data class Box(
			  @field:In(values = ["NOPE"])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected unknown enum constant highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("NOPE") == true || it.description?.contains("enum") == true
			},
		)
	}
	
	@DisplayName("valid @In enum literal is not an error")
	fun testValidEnumInIsNotError() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"OkEnum.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN, USER }
			data class Box(
			  @field:In(values = ["ADMIN", "USER"])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			"valid enum In should not error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && (it.description?.contains("values") == true || it.description?.contains(
					"enum") == true || it.description?.contains("blank") == true || it.description?.contains("empty") == true)
			},
		)
		assertTrue(
			"valid enum In elements should use ENUM attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.ENUM },
		)
	}
	
	@DisplayName("empty @In array is highlighted")
	fun testEmptyInIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"EmptyIn.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN }
			data class Box(
			  @field:In(values = [])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NON_EMPTY highlight on values=[], got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("empty") == true || it.description?.contains("values") == true
			},
		)
	}
	
	@DisplayName("blank @In element is highlighted")
	fun testBlankInElementIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"BlankIn.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN }
			data class Box(
			  @field:In(values = [""])
			  val role: Role?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NOT_BLANK highlight on values=[\"\"], got: ${highlights.map { it.description }}",
			highlights.any { it.description?.contains("blank") == true },
		)
	}
	
	@DisplayName("negative @Size min is highlighted")
	fun testNegativeSizeMinIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addSizeConstraint(myFixture)
		myFixture.configureByText(
			"BadSize.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Size
			data class User(
			  @field:Size(min = -1, max = 40)
			  val name: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NON_NEGATIVE highlight on min=-1, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains(">= 0") == true || it.description?.contains("min") == true
			},
		)
	}
	
	@DisplayName("negative @Uuid version is highlighted")
	fun testNegativeUuidVersionIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addUuidConstraint(myFixture)
		myFixture.configureByText(
			"BadUuid.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Uuid
			data class User(
			  @field:Uuid(version = -1)
			  val id: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NON_NEGATIVE highlight on version=-1, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains(">= 0") == true || it.description?.contains("version") == true
			},
		)
	}
	
	@DisplayName("negative @Password minLength is highlighted")
	fun testNegativePasswordMinLengthIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addPasswordConstraint(myFixture)
		myFixture.configureByText(
			"BadPassword.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Password
			data class User(
			  @field:Password(minLength = -1)
			  val secret: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NON_NEGATIVE highlight on minLength=-1, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains(">= 0") == true || it.description?.contains("minLength") == true
			},
		)
	}
	
	@DisplayName("negative @Url maxLength is highlighted")
	fun testNegativeUrlMaxLengthIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addUrlConstraint(myFixture)
		myFixture.configureByText(
			"BadUrl.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Url
			data class User(
			  @field:Url(maxLength = -1)
			  val homepage: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NON_NEGATIVE highlight on maxLength=-1, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains(">= 0") == true || it.description?.contains("maxLength") == true
			},
		)
	}
	
	@DisplayName("valid @Size bounds are not errors")
	fun testValidSizeBoundsAreNotError() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addSizeConstraint(myFixture)
		myFixture.configureByText(
			"OkSize.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Size
			data class User(
			  @field:Size(min = 0, max = 40)
			  val name: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && (it.description?.contains(">= 0") == true || it.description?.contains(
					"must be > 0") == true)
			},
		)
	}
	
	@DisplayName("@RequiredWhen EQ with bad enum value is highlighted")
	fun testRequiredWhenEqualsBadEnumIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRequiredWhenConstraint(myFixture)
		myFixture.configureByText(
			"BadEquals.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			enum class ContactType { PHONE, EMAIL }
			data class Box(
			  val contactType: ContactType?,
			  @field:RequiredWhen(ref = "contactType", condition = RequiredWhen.Condition.EQ, value = "PHON")
			  val phone: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected bad enum literal highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("PHON") == true || it.description?.contains("enum") == true || it.description?.contains("value") == true
			},
		)
	}
	
	@DisplayName("@RequiredWhen MISSING does not flag default value literal")
	fun testRequiredWhenMissingDoesNotFlagDefaultValue() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRequiredWhenConstraint(myFixture)
		myFixture.configureByText(
			"MissingOk.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			data class Box(
			  val email: String?,
			  @field:RequiredWhen(ref = "email", condition = RequiredWhen.Condition.MISSING)
			  val phone: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			"MISSING must not require value/values, got: ${highlights.map { it.description }}",
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && (it.description?.contains("value") == true || it.description?.contains(
					"values") == true || it.description?.contains("blank") == true || it.description?.contains("empty") == true)
			},
		)
	}
	
	@DisplayName("invalid @Max number literal is highlighted")
	fun testInvalidMaxIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMaxConstraint(myFixture)
		myFixture.configureByText(
			"BadMax.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Max
			data class User(
			  @field:Max("nope")
			  val age: Int?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected invalid Max highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("decimal number") == true || it.description?.contains("Max") == true || it.description?.contains("value") == true
			},
		)
	}
	
	@DisplayName("invalid @MultipleOf factor is highlighted")
	fun testInvalidMultipleOfFactorIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMultipleOfConstraint(myFixture)
		myFixture.configureByText(
			"BadMultiple.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.MultipleOf
			data class User(
			  @field:MultipleOf("abc")
			  val amount: Int?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected invalid MultipleOf factor, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("decimal number") == true || it.description?.contains("factor") == true || it.description?.contains("MultipleOf") == true
			},
		)
	}
	
	@DisplayName("invalid @Regex pattern is highlighted")
	fun testInvalidRegexPatternIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRegexConstraint(myFixture)
		myFixture.configureByText(
			"BadRegex.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Regex
			data class User(
			  @field:Regex(pattern = "(", name = "BAD")
			  val code: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected invalid regex highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("regex") == true || it.description?.contains("pattern") == true
			},
		)
	}
	
	@DisplayName("negative @RelativeToNow within is highlighted")
	fun testFutureNegativeWithinIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRelativeToNowConstraint(myFixture)
		myFixture.configureByText(
			"BadFuture.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RelativeToNow
			import java.time.LocalDate
			data class User(
			  @field:RelativeToNow(within = -1)
			  val due: LocalDate?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected NON_NEGATIVE on within, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains(">= 0") == true || it.description?.contains("within") == true
			},
		)
	}
	
	@DisplayName("empty @DaysOfWeek days is highlighted")
	fun testDayOfWeekInEmptyIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addDaysOfWeekConstraint(myFixture)
		myFixture.configureByText(
			"BadDayOfWeekIn.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.DaysOfWeek
			import java.time.LocalDate
			data class User(
			  @field:DaysOfWeek(days = [])
			  val scheduled: LocalDate?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected empty days highlight, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("empty") == true || it.description?.contains("days") == true
			},
		)
	}
	
	@DisplayName("invalid @Contains enum literal on list element is highlighted")
	fun testInvalidEnumContainsIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addContainsConstraint(myFixture)
		myFixture.configureByText(
			"BadContainsEnum.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.Contains
			enum class Tier { A, B }
			data class Row(
			  @field:Contains(values = ["Z"])
			  val tiers: List<Tier>?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected invalid Contains literal, got: ${highlights.map { it.description }}",
			highlights.any { it.description?.contains("Z") == true || it.description?.contains("literal") == true },
		)
	}

	@DisplayName("empty @NotIn values is highlighted")
	fun testNotInEmptyIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addNotInConstraint(myFixture)
		myFixture.configureByText(
			"BadNotIn.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.NotIn
			data class User(
			  @field:NotIn(values = [])
			  val role: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected empty NotIn values, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("empty") == true || it.description?.contains("values") == true
			},
		)
	}
	
	@DisplayName("@RequiredWhen IN with empty values is highlighted")
	fun testRequiredWhenInEmptyValuesIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRequiredWhenConstraint(myFixture)
		myFixture.configureByText(
			"BadIn.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			data class Box(
			  val status: String?,
			  @field:RequiredWhen(ref = "status", condition = RequiredWhen.Condition.IN, values = [])
			  val assignee: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected empty values for IN, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("empty") == true || it.description?.contains("values") == true
			},
		)
	}
	
	@DisplayName("@RequiredWhen EQ ignores inactive empty values (no false positive)")
	fun testRequiredWhenEqualsDoesNotFlagEmptyValues() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRequiredWhenConstraint(myFixture)
		myFixture.configureByText(
			"EqualsSkipValues.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			data class Box(
			  val status: String?,
			  @field:RequiredWhen(
			    ref = "status",
			    condition = RequiredWhen.Condition.EQ,
			    value = "OPEN",
			    values = [],
			  )
			  val assignee: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			"EQ must skip inactive values=[], got: ${highlights.map { it.description }}",
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && (it.description?.contains("empty") == true || it.description?.contains(
					"values") == true)
			},
		)
	}
	
	@DisplayName("handler @RequiredWhen EQ bad number against Int gate is highlighted")
	fun testEndpointRequiredWhenBadNumberIsHighlighted() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRequiredWhenConstraint(myFixture)
		myFixture.configureByText(
			"BadEndpointRequiredWhen.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			class UserController {
			  fun lookup(
			    @RequiredWhen(ref = "tenant", condition = RequiredWhen.Condition.EQ, value = "aaa")
			    q: Int?,
			    tenant: Int,
			  ) = Unit
			}
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected invalid gate number for handler RequiredWhen.value, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("decimal number") == true || it.description?.contains("aaa") == true
			},
		)
	}
	
	@DisplayName("handler @RequiredWhen EQ valid number uses NUMBER color")
	fun testEndpointRequiredWhenValidNumberUsesNumberColor() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addRequiredWhenConstraint(myFixture)
		myFixture.configureByText(
			"OkEndpointRequiredWhen.kt",
			"""
			package test.literal
			import io.ghaylan.validata.constraint.annotation.RequiredWhen
			class UserController {
			  fun lookup(
			    @RequiredWhen(ref = "tenant", condition = RequiredWhen.Condition.EQ, value = "55")
			    q: Int?,
			    tenant: Int,
			  ) = Unit
			}
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertFalse(
			highlights.any {
				it.severity === com.intellij.lang.annotation.HighlightSeverity.ERROR && it.description?.contains("decimal number") == true
			},
		)
		assertTrue(
			"valid handler RequiredWhen.value should use NUMBER attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.NUMBER },
		)
	}
}

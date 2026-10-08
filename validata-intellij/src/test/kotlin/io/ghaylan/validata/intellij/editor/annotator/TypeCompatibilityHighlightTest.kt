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

import com.intellij.lang.annotation.HighlightSeverity
import io.ghaylan.validata.intellij.editor.color.ConstraintHighlightingColors
import io.ghaylan.validata.intellij.support.PropertyRefLightFixtures
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Platform tests: scalar type compatibility highlighting (KSP parity).
 *
 * Incompatible **resolved** refs keep field purple [ConstraintHighlightingColors.PROPERTY_REF] and use
 * underline-only errors — full red text is only for unresolved names.
 * 
 * @author Ghaylan Saada
 */
class TypeCompatibilityHighlightTest: ValidataLightPlatformTestCase() {
	
	
	@DisplayName("COMPARABLE_FAMILY string vs Int on @Compare EQUAL is highlighted as incompatible")
	fun testStringComparedToIntIsError() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"BadEqual.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class BadEqual(
			  val age: Int,
			  @Compare(ref = "age", operation = Compare.Operation.EQ)
			  val name: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected type mismatch, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("cannot equal") == true || it.description?.contains("cannot compare") == true
			},
		)
		assertTrue(
			"incompatible resolved ref must keep PROPERTY_REF blue, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.PROPERTY_REF },
		)
		assertFalse(
			"type mismatch must not paint full red UNRESOLVED text",
			highlights.any {
				it.severity === HighlightSeverity.ERROR &&
					(it.description?.contains("cannot equal") == true || it.description?.contains("cannot compare") == true) &&
					it.forcedTextAttributesKey == ConstraintHighlightingColors.UNRESOLVED
			},
		)
	}
	
	@DisplayName("numeric Int vs BigInteger is compatible (no type error)")
	fun testIntComparedToBigIntegerIsOk() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"OkNumeric.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Compare
			import java.math.BigInteger
			data class OkNumeric(
			  val big: BigInteger,
			  @Compare(ref = "big", operation = Compare.Operation.GT)
			  val small: Int,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"Int↔BigInteger must be compatible, got: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("cannot equal") == true || it.description?.contains("cannot compare") == true || it.description?.contains("Cannot resolve property") == true
			},
		)
	}
	
	@DisplayName("COMPARABLE_FAMILY string vs Int on @Compare GREATER is highlighted as incompatible")
	fun testGreaterThanStringToIntIsError() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"BadGt.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class BadGt(
			  val label: String,
			  @Compare(ref = "label", operation = Compare.Operation.GT)
			  val amount: Int,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected type mismatch, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("cannot equal") == true || it.description?.contains("cannot compare") == true
			},
		)
		assertTrue(
			"incompatible resolved ref must keep PROPERTY_REF blue, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.PROPERTY_REF },
		)
		assertFalse(
			"type mismatch must not paint full red UNRESOLVED text",
			highlights.any {
				it.severity === HighlightSeverity.ERROR &&
					(it.description?.contains("cannot equal") == true || it.description?.contains("cannot compare") == true) &&
					it.forcedTextAttributesKey == ConstraintHighlightingColors.UNRESOLVED
			},
		)
	}
	
	@DisplayName("@Compare GT on String subject is accepted (COMPARABLE_FAMILY orderable)")
	fun testGreaterThanOnStringSubjectIsOk() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"OkStringSubject.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class OkStringSubject(
			  val other: String,
			  @Compare(ref = "other", operation = Compare.Operation.GT)
			  val name: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"@Compare on String must be allowed under COMPARABLE_FAMILY, got: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("cannot apply to") == true ||
					it.description?.contains("cannot be applied to") == true ||
					it.description?.contains("cannot compare") == true
			},
		)
	}
	
	@DisplayName("@Compare GREATER on Int subject is accepted")
	fun testGreaterThanOnIntSubjectIsOk() {
		addLibraryMarkers()
		myFixture.configureByFiles("CompareStub.kt")
		myFixture.configureByText(
			"OkSubject.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Compare
			data class OkSubject(
			  val min: Int,
			  @Compare(ref = "min", operation = Compare.Operation.GT)
			  val max: Int,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"@Compare on Int must be allowed, got: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("cannot apply to") == true || it.description?.contains("cannot be applied to") == true
			},
		)
	}
	
	@DisplayName("typed validator V=String rejects Int subject")
	fun testEmailOnIntSubjectIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"EmailStub.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email
			class EmailConstraint
			object EmailValidator : ConstraintValidator<CharSequence, EmailConstraint>()
			""".trimIndent(),
		)
		myFixture.configureByText(
			"BadEmail.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Email
			data class BadEmail(
			  @Email
			  val age: Int,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected @Email on Int error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("@Email cannot be applied to") == true && it.description?.contains("Int") == true
			},
		)
	}
	
	@DisplayName("custom constraint with wrong subject V is highlighted")
	fun testCustomConstraintWrongSubjectIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"OddYearsStub.kt",
			"""
			package sample.custom
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			@Constraint(validatedBy = [OddYearsValidator::class])
			annotation class OddYears
			class OddYearsConstraint
			object OddYearsValidator : ConstraintValidator<Int, OddYearsConstraint>()
			""".trimIndent(),
		)
		myFixture.configureByText(
			"BadOdd.kt",
			"""
			package test.compat
			import sample.custom.OddYears
			data class BadOdd(
			  @OddYears
			  val label: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected custom @OddYears on String error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("@OddYears cannot be applied to") == true && it.description?.contains("String") == true
			},
		)
	}
	
	@DisplayName("typed validator V=String accepts String subject")
	fun testEmailOnStringSubjectIsOk() {
		addLibraryMarkers()
		myFixture.configureByText(
			"EmailStubOk.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email
			class EmailConstraint
			object EmailValidator : ConstraintValidator<CharSequence, EmailConstraint>()
			""".trimIndent(),
		)
		myFixture.configureByText(
			"OkEmail.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Email
			data class OkEmail(
			  @Email
			  val address: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"@Email on String must be allowed, got: ${highlights.map { it.description }}",
			highlights.none { it.description?.contains("cannot be applied to") == true },
		)
	}
	
	@DisplayName("type-use constraint on List element uses element subject type")
	fun testEmailTypeUseOnListElementIsOk() {
		addLibraryMarkers()
		myFixture.configureByText(
			"EmailStubTypeUse.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email
			class EmailConstraint
			object EmailValidator : ConstraintValidator<CharSequence, EmailConstraint>()
			""".trimIndent(),
		)
		myFixture.configureByText(
			"OkTypeUse.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Email
			data class OkTypeUse(
			  val emails: List<@Email String>,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"List<@Email String> must use String subject, got: ${highlights.map { it.description }}",
			highlights.none { it.description?.contains("cannot be applied to") == true },
		)
	}
	
	@DisplayName("constraint on List property subject is rejected when V is String")
	fun testEmailOnListPropertyIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"EmailStubList.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email
			class EmailConstraint
			object EmailValidator : ConstraintValidator<CharSequence, EmailConstraint>()
			""".trimIndent(),
		)
		myFixture.configureByText(
			"BadListEmail.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Email
			data class BadListEmail(
			  @Email
			  val emails: List<String>,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected @Email on List error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("@Email cannot be applied to") == true && it.description?.contains("List") == true
			},
		)
	}

	@DisplayName("@Contains on String subject is rejected")
	fun testContainsOnStringIsError() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addContainsConstraint(myFixture)
		myFixture.configureByText(
			"BadContainsString.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Contains
			data class BadContainsString(
			  @Contains(values = ["a"])
			  val label: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected @Contains on String error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("@Contains") == true &&
					it.description?.contains("String") == true &&
					(it.description?.contains("cannot be applied to") == true ||
						it.description?.contains("no validator") == true)
			},
		)
	}

	@DisplayName("@Contains on List subject is accepted")
	fun testContainsOnListIsOk() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addContainsConstraint(myFixture)
		myFixture.configureByText(
			"OkContainsList.kt",
			"""
			package test.compat
			import io.ghaylan.validata.constraint.annotation.Contains
			data class OkContainsList(
			  @Contains(values = ["draft"])
			  val tags: Collection<String>,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"@Contains on collection must be allowed, got: ${highlights.map { it.description }}",
			highlights.none { it.description?.contains("@Contains cannot be applied to") == true },
		)
	}
}

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

import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Subject-type fit for composed (non-`@Constraint`) annotations with nested leaf constraints.
 * 
 * @author Ghaylan Saada
 */
class ConstraintCompositionSubjectTypeTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("EmailOrPhone on String has no subject-type error")
	fun testEmailOrPhoneOnStringOk() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Ok.kt",
			"""
			package test.composition
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()

			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email

			data class PhoneConstraint(val message: String = "")
			object PhoneValidator : ConstraintValidator<String, PhoneConstraint>()

			@Constraint(validatedBy = [PhoneValidator::class])
			annotation class Phone

			@Email
			@Phone
			@ConstraintComposition(Mode.OR)
			annotation class EmailOrPhone

			data class Holder(
			  @EmailOrPhone
			  val contact: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"composed String must not error: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("cannot be applied to") == true || it.description?.contains("must declare @Constraint(validatedBy") == true
			},
		)
	}
	
	@DisplayName("EmailOrPhone on Int is highlighted as an error")
	fun testEmailOrPhoneOnIntError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Mismatch.kt",
			"""
			package test.composition
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()

			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email

			data class PhoneConstraint(val message: String = "")
			object PhoneValidator : ConstraintValidator<String, PhoneConstraint>()

			@Constraint(validatedBy = [PhoneValidator::class])
			annotation class Phone

			@Email
			@Phone
			@ConstraintComposition(Mode.OR)
			annotation class EmailOrPhone

			data class Holder(
			  @EmailOrPhone
			  val age: Int,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected composed subject mismatch, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("cannot be applied to") == true && it.description?.contains("Int") == true
			},
		)
	}
}

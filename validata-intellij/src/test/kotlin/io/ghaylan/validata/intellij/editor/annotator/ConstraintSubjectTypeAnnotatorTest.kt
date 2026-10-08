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
 * Subject-type fit vs `validatedBy` `ConstraintValidator<V, C>` (and optional PropertyRef gate).
 * 
 * @author Ghaylan Saada
 */
class ConstraintSubjectTypeAnnotatorTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("empty validatedBy is highlighted as an error")
	fun testEmptyValidatedByIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"EmptyVb.kt",
			"""
			package test.subject
			import io.ghaylan.validata.constraint.Constraint

			@Constraint(validatedBy = [])
			annotation class Broken

			data class Holder(
			  @Broken
			  val name: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected empty validatedBy error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("must declare @Constraint(validatedBy") == true
			},
		)
	}
	
	@DisplayName("validator V mismatch vs subject is highlighted as an error")
	fun testValidatorVMismatchIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Mismatch.kt",
			"""
			package test.subject
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()

			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email

			data class Holder(
			  @Email
			  val age: Int,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected subject mismatch, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("cannot be applied to") == true && it.description?.contains("Int") == true
			},
		)
	}
	
	@DisplayName("matching validator V has no subject-type error")
	fun testMatchingValidatorHasNoSubjectError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Ok.kt",
			"""
			package test.subject
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()

			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email

			data class Holder(
			  @Email
			  val address: String,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"matching V must not error: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("cannot be applied to") == true || it.description?.contains("must declare @Constraint(validatedBy") == true
			},
		)
	}
}

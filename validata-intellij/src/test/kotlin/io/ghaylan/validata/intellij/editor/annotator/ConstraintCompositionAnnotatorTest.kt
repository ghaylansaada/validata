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
 * Declaration-site OR composition authoring rules (KSP T6 parity).
 * 
 * @author Ghaylan Saada
 */
class ConstraintCompositionAnnotatorTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("OR with a single leaf is highlighted as an error")
	fun testOrWithSingleLeafIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"SingleLeaf.kt",
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

			@Email
			@ConstraintComposition(Mode.OR)
			annotation class OnlyEmail
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected OR <2 leaves error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("@ConstraintComposition(OR)") == true && it.description?.contains("at least 2") == true
			},
		)
	}
	
	@DisplayName("OR with Required member is highlighted as an error")
	fun testOrWithPresenceIsError() {
		addLibraryMarkers()
		myFixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Required.kt",
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
			data class RequiredConstraint(val message: String = "")
			object RequiredValidator : ConstraintValidator<Any, RequiredConstraint>()
			@Constraint(validatedBy = [RequiredValidator::class])
			annotation class Required
			""".trimIndent(),
		)
		myFixture.configureByText(
			"PresenceOr.kt",
			"""
			package test.composition
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.constraint.annotation.Required

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()

			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email

			@Required
			@Email
			@ConstraintComposition(Mode.OR)
			annotation class RequiredOrEmail
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected presence-in-OR error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("presence constraint") == true || it.description?.contains("Presence must stay outside OR") == true
			},
		)
	}
	
	@DisplayName("valid EmailOrPhone OR composition has no composition authoring error")
	fun testValidOrHasNoCompositionError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"ValidOr.kt",
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
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"valid OR must not error: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("@ConstraintComposition(OR)") == true && (it.description?.contains("at least 2") == true || it.description?.contains(
					"presence") == true)
			},
		)
	}
	
	@DisplayName("OR nesting a composed annotation is highlighted as an error")
	fun testOrWithNestedComposedIsError() {
		addLibraryMarkers()
		myFixture.configureByText(
			"NestedOr.kt",
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
			annotation class EmailAndPhone

			@EmailAndPhone
			@Email
			@ConstraintComposition(Mode.OR)
			annotation class NestedOr
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected nested composed OR error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("@ConstraintComposition(OR)") == true && (it.description?.contains("nests composed") == true || it.description?.contains(
					"EmailAndPhone") == true)
			},
		)
	}
}

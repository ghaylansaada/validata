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

package io.ghaylan.validata.intellij.discovery.composition

import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.DisplayName

/**
 * Declaration-site composition discovery (AND flatten / OR leaves / nested composed).
 * 
 * @author Ghaylan Saada
 */
class ConstraintCompositionDiscoveryTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("AND composed annotation discovers two leaf constraints")
	fun testAndDiscoversTwoLeaves() {
		addLibraryMarkers()
		myFixture.configureByText(
			"AndCompose.kt",
			"""
			package test.composition
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintComposition
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
			@ConstraintComposition
			annotation class EmailAndPhone
			""".trimIndent(),
		)
		val decl = myFixture.file.children.filterIsInstance<KtClass>()
			.first { it.name == "EmailAndPhone" }
		val analysis = ConstraintCompositionDiscovery.analyzeDeclaration(decl)
		assertTrue("expected composed, got $analysis", analysis.isComposed)
		assertEquals(CompositionModeView.AND, analysis.mode)
		assertEquals(2, analysis.constraintLeaves.size)
		assertTrue(analysis.constraintLeaves.any { it.shortName == "Email" })
		assertTrue(analysis.constraintLeaves.any { it.shortName == "Phone" })
	}
	
	@DisplayName("nested composed member is flagged isNestedComposed under OR root")
	fun testNestedComposedLeafIsDetected() {
		addLibraryMarkers()
		myFixture.configureByText(
			"NestedCompose.kt",
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
		val decl = myFixture.file.children.filterIsInstance<KtClass>()
			.first { it.name == "NestedOr" }
		val analysis = ConstraintCompositionDiscovery.analyzeDeclaration(decl)
		assertEquals(CompositionModeView.OR, analysis.mode)
		assertTrue(
			"expected nested composed leaf, got ${analysis.leaves}",
			analysis.leaves.any { it.shortName == "EmailAndPhone" && it.isNestedComposed },
		)
	}
	
	@DisplayName("@Constraint outer type is not treated as composition root")
	fun testConstraintOuterIsNotComposed() {
		addLibraryMarkers()
		myFixture.configureByText(
			"OuterConstraint.kt",
			"""
			package test.composition
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()
			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email
			""".trimIndent(),
		)
		val decl = myFixture.file.children.filterIsInstance<KtClass>()
			.first { it.name == "Email" }
		val analysis = ConstraintCompositionDiscovery.analyzeDeclaration(decl)
		assertFalse(analysis.isComposed)
		assertTrue(analysis.leaves.isEmpty())
	}
}

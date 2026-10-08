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

package io.ghaylan.validata.intellij.discovery.validatedby

import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByMissingValidatedBy
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByNotAConstraint
import io.ghaylan.validata.intellij.discovery.cache.ConstraintValidatedByOk
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.DisplayName

/**
 * Discovers accepted subject types from `@Constraint(validatedBy = […])`.
 * 
 * @author Ghaylan Saada
 */
class ConstraintValidatedByDiscoveryTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("typed validatedBy yields Ok with String V")
	fun testTypedValidatorYieldsOkWithValueTypes() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Typed.kt",
			"""
			package test.vb
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class EmailConstraint(val message: String = "")
			object EmailValidator : ConstraintValidator<String, EmailConstraint>()

			@Constraint(validatedBy = [EmailValidator::class])
			annotation class Email

			data class User(
			  @Email
			  val address: String,
			)
			""".trimIndent(),
		)
		val result = ConstraintValidatedByDiscovery.analyze(findAnnotation("Email"))
		assertTrue("expected Ok, got $result", result is ConstraintValidatedByOk)
		val types = (result as ConstraintValidatedByOk).valueTypes
		assertTrue(
			"expected String subject V, got ${types.map { it.qualifiedName }}",
			types.any { it.qualifiedName == "kotlin.String" || it.qualifiedName.endsWith(".String") },
		)
	}
	
	@DisplayName("empty validatedBy yields MissingValidatedBy")
	fun testEmptyValidatedByIsMissing() {
		addLibraryMarkers()
		myFixture.configureByText(
			"EmptyVb.kt",
			"""
			package test.vb
			import io.ghaylan.validata.constraint.Constraint

			@Constraint(validatedBy = [])
			annotation class Broken

			data class Holder(
			  @Broken
			  val name: String,
			)
			""".trimIndent(),
		)
		val result = ConstraintValidatedByDiscovery.analyze(findAnnotation("Broken"))
		assertEquals(ConstraintValidatedByMissingValidatedBy, result)
	}
	
	@DisplayName("ordinary annotation yields NotAConstraint")
	fun testOrdinaryAnnotationIsNotAConstraint() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Plain.kt",
			"""
			package test.vb
			annotation class Hint
			data class Holder(
			  @Hint
			  val name: String,
			)
			""".trimIndent(),
		)
		val result = ConstraintValidatedByDiscovery.analyze(findAnnotation("Hint"))
		assertEquals(ConstraintValidatedByNotAConstraint, result)
	}
	
	@DisplayName("CharSequence validatedBy V is discovered as subject type")
	fun testCharSequenceValidatorYieldsOk() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Cs.kt",
			"""
			package test.vb
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator

			data class SizeConstraint(val message: String = "")
			object SizeValidator : ConstraintValidator<CharSequence, SizeConstraint>()

			@Constraint(validatedBy = [SizeValidator::class])
			annotation class Size

			data class User(
			  @Size
			  val name: String,
			)
			""".trimIndent(),
		)
		val result = ConstraintValidatedByDiscovery.analyze(findAnnotation("Size"))
		assertTrue("expected Ok, got $result", result is ConstraintValidatedByOk)
		val types = (result as ConstraintValidatedByOk).valueTypes
		assertTrue(
			"expected CharSequence V, got ${types.map { it.qualifiedName }}",
			types.any {
				it.qualifiedName == "kotlin.CharSequence" || it.qualifiedName.endsWith(".CharSequence")
			},
		)
	}
	
	private fun findAnnotation(shortName: String): KtAnnotationEntry = myFixture.file.children.filterIsInstance<KtClass>()
		.mapNotNull { it.primaryConstructor }
		.flatMap { it.valueParameters }
		.flatMap { it.annotationEntries }
		.first { it.shortName?.asString() == shortName }
}

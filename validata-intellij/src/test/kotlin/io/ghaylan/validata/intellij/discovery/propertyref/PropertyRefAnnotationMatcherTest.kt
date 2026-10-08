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

package io.ghaylan.validata.intellij.discovery.propertyref

import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.junit.jupiter.api.DisplayName

/**
 * Direct coverage for [PropertyRefAnnotationMatcher] (usage-site arg → discovered host).
 * 
 * @author Ghaylan Saada
 */
class PropertyRefAnnotationMatcherTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("matchAttribute returns SIBLING SAME_SCALAR_KIND for @MatchesSibling property")
	fun testMatchAttributeOnDiscoveredHost() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Match.kt",
			"""
			package test.matcher
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.constraint.PropertyRef
			import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind

			@Constraint(validatedBy = [MatchesSiblingValidator::class])
			annotation class MatchesSibling(
			  @PropertyRef(compatibility = PropertyRefCompatibilityKind.SAME_SCALAR_KIND)
			  val property: String,
			)
			data class MatchesSiblingConstraint(
			  @PropertyRef(compatibility = PropertyRefCompatibilityKind.SAME_SCALAR_KIND)
			  val property: String,
			)
			object MatchesSiblingValidator : ConstraintValidator<String, MatchesSiblingConstraint>()

			data class Req(
			  val password: String,
			  @MatchesSibling(property = "password")
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val literal = findExactStringLiteral("password")
		val argument = literal.getStrictParentOfType<KtValueArgument>()!!
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>()!!
		val attr = PropertyRefAnnotationMatcher.matchAttribute(annotation, argument)
		assertNotNull("expected PropertyRef host match", attr)
		assertEquals("property", attr!!.parameterName)
		assertEquals(PropertyRefScope.SIBLING, attr.scope)
		assertEquals(PropertyRefCompatibilityKind.SAME_SCALAR_KIND, attr.compatibilityKind)
	}
	
	@DisplayName("matchAttribute returns null for unmarked annotation parameter")
	fun testMatchAttributeNullWhenUnmarked() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Unmarked.kt",
			"""
			package test.matcher
			import io.ghaylan.validata.constraint.Constraint

			@Constraint(validatedBy = [])
			annotation class Hint(val property: String)

			data class Req(
			  val password: String,
			  @Hint(property = "password")
			  val note: String,
			)
			""".trimIndent(),
		)
		val literal = findExactStringLiteral("password")
		val argument = literal.getStrictParentOfType<KtValueArgument>()!!
		val annotation = argument.getStrictParentOfType<KtAnnotationEntry>()!!
		assertNull(PropertyRefAnnotationMatcher.matchAttribute(annotation, argument))
	}
	
	@DisplayName("matchAnnotationCompatibility prefers primary sibling kind")
	fun testMatchAnnotationCompatibility() {
		addLibraryMarkers()
		myFixture.configureByText(
			"Compat.kt",
			"""
			package test.matcher
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.constraint.PropertyRef
			import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind

			@Constraint(validatedBy = [MatchesSiblingValidator::class])
			annotation class MatchesSibling(
			  @PropertyRef(compatibility = PropertyRefCompatibilityKind.SAME_SCALAR_KIND)
			  val property: String,
			)
			data class MatchesSiblingConstraint(
			  @PropertyRef(compatibility = PropertyRefCompatibilityKind.SAME_SCALAR_KIND)
			  val property: String,
			)
			object MatchesSiblingValidator : ConstraintValidator<String, MatchesSiblingConstraint>()

			data class Req(
			  val password: String,
			  @MatchesSibling("password")
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val annotation = myFixture.file.children.filterIsInstance<KtClass>()
			.flatMap { it.primaryConstructor?.valueParameters.orEmpty() }
			.flatMap { it.annotationEntries }
			.first { it.shortName?.asString() == "MatchesSibling" }
		val attr = PropertyRefAnnotationMatcher.matchAnnotationCompatibility(annotation)
		assertNotNull(attr)
		assertEquals(PropertyRefCompatibilityKind.SAME_SCALAR_KIND, attr!!.compatibilityKind)
	}
}

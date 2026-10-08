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

import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.junit.jupiter.api.DisplayName

/**
 * PSI discovery of `@PropertyRef` hosts from `@Constraint` annotations — no FQCN allowlist.
 * 
 * @author Ghaylan Saada
 */
class PropertyRefDiscoveryTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("custom @PropertyRef constraint resolves via discovery (no allowlist)")
	fun testCustomConstraintResolvesViaDiscovery() {
		addLibraryMarkers()
		myFixture.configureByText(
			"CustomRequest.kt",
			"""
			package test.discovery
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

			data class CustomRequest(
			  val password: String,
			  @MatchesSibling("password")
			  val confirmation: String,
			)
			""".trimIndent(),
		)
		val literal = findExactStringLiteral("password")
		val refs = literal.references.filterIsInstance<PropertyRefPsiReference>()
		assertEquals("discovery must attach a PropertyRefPsiReference", 1, refs.size)
		assertEquals("password",
			(refs.single()
				.resolve() as? KtNamedDeclaration)?.name)
	}
	
	@DisplayName("unmarked annotation does not attach property-ref references")
	fun testUnmarkedAnnotationDoesNotAttachReference() {
		addLibraryMarkers()
		myFixture.configureByText(
			"HintRequest.kt",
			"""
			package test.discovery
			import io.ghaylan.validata.constraint.Constraint

			@Constraint(validatedBy = [])
			annotation class PatternHint(val property: String)

			data class PatternHintConstraint(
			  val property: String,
			)

			data class HintRequest(
			  val password: String,
			  @PatternHint("password")
			  val note: String,
			)
			""".trimIndent(),
		)
		val literal = findExactStringLiteral("password")
		val refs = literal.references.filterIsInstance<PropertyRefPsiReference>()
		assertTrue(
			"unmarked annotation must not attach refs (got ${refs.size})",
			refs.isEmpty(),
		)
	}
}

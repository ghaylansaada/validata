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

package io.ghaylan.validata.intellij.discovery.constraintarg

import io.ghaylan.validata.intellij.support.PropertyRefLightFixtures
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.DisplayName

/**
 * Discovers `@ConstraintArg` hosts from annotation / metadata declarations (IDE twin of KSP).
 * 
 * @author Ghaylan Saada
 */
class ConstraintArgAttributeDiscoveryTest: ValidataLightPlatformTestCase() {
	
	@DisplayName("@Min value discovers TYPED_LITERAL and NOT_BLANK ConstraintArg hosts")
	fun testMinValueDiscoversTypedLiteralAndNotBlank() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addMinConstraint(myFixture)
		myFixture.configureByText(
			"MinUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.Min
			data class Age(
			  @Min("18")
			  val years: Int,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("Min"))
		assertTrue("expected @ConstraintArg hosts on Min.value, got $hosts", hosts.isNotEmpty())
		val valueHosts = hosts.filter { it.parameterName == "value" }
		assertTrue(valueHosts.any { ConstraintArgKind.TYPED_LITERAL in it.kinds })
		assertTrue(valueHosts.any { ConstraintArgKind.NOT_BLANK in it.kinds })
	}
	
	@DisplayName("@In values discovers ELEMENT TYPED_LITERAL hosts")
	fun testInDiscoversElementTypedLiteral() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addInConstraint(myFixture)
		myFixture.configureByText(
			"InUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.In
			enum class Role { ADMIN, USER }
			data class Account(
			  @In(["ADMIN"])
			  val role: Role,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("In"))
		val elementHosts = hosts.filter {
			it.parameterName == "values" && it.target == ConstraintArgTarget.ELEMENT
		}
		assertTrue(
			"expected ELEMENT TYPED_LITERAL on values, got $hosts",
			elementHosts.any { ConstraintArgKind.TYPED_LITERAL in it.kinds },
		)
	}
	
	@DisplayName("@Contains values discovers ELEMENT TYPED_LITERAL hosts")
	fun testContainsDiscoversElementTypedLiteral() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addContainsConstraint(myFixture)
		myFixture.configureByText(
			"ContainsUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.Contains
			enum class Tier { A, B }
			data class Row(
			  @Contains(values = ["A"])
			  val tiers: List<Tier>,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("Contains"))
		val elementHosts = hosts.filter {
			it.parameterName == "values" && it.target == ConstraintArgTarget.ELEMENT
		}
		assertTrue(
			"expected ELEMENT TYPED_LITERAL on values, got $hosts",
			elementHosts.any { ConstraintArgKind.TYPED_LITERAL in it.kinds },
		)
	}

	@DisplayName("@Uuid version discovers NON_NEGATIVE ConstraintArg host")
	fun testUuidVersionDiscoversNonNegative() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addUuidConstraint(myFixture)
		myFixture.configureByText(
			"UuidUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.Uuid
			data class Id(
			  @Uuid(version = 4)
			  val value: String,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("Uuid"))
		assertTrue(
			"expected NON_NEGATIVE on version, got $hosts",
			hosts.any {
				it.parameterName == "version" && ConstraintArgKind.NON_NEGATIVE in it.kinds
			},
		)
	}
	
	@DisplayName("@Password length bounds discover NON_NEGATIVE ConstraintArg hosts")
	fun testPasswordLengthsDiscoverNonNegative() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addPasswordConstraint(myFixture)
		myFixture.configureByText(
			"PasswordUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.Password
			data class Creds(
			  @Password(minLength = 8, maxLength = 32)
			  val secret: String,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("Password"))
		assertTrue(hosts.any { it.parameterName == "minLength" && ConstraintArgKind.NON_NEGATIVE in it.kinds })
		assertTrue(hosts.any { it.parameterName == "maxLength" && ConstraintArgKind.NON_NEGATIVE in it.kinds })
	}
	
	@DisplayName("@Url maxLength discovers NON_NEGATIVE ConstraintArg host")
	fun testUrlMaxLengthDiscoversNonNegative() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addUrlConstraint(myFixture)
		myFixture.configureByText(
			"UrlUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.Url
			data class Link(
			  @Url(maxLength = 128)
			  val href: String,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("Url"))
		assertTrue(
			"expected NON_NEGATIVE on maxLength, got $hosts",
			hosts.any {
				it.parameterName == "maxLength" && ConstraintArgKind.NON_NEGATIVE in it.kinds
			},
		)
	}
	
	@DisplayName("@ConstraintArgs container discovers ELEMENT + VALUE hosts")
	fun testConstraintArgsContainerDiscoversElementAndValue() {
		addLibraryMarkers()
		PropertyRefLightFixtures.addConstraintArgsElementConstraint(myFixture)
		myFixture.configureByText(
			"TagsInUse.kt",
			"""
			package test.args
			import io.ghaylan.validata.constraint.annotation.TagsIn
			data class Box(
			  @TagsIn(values = ["a"])
			  val tags: List<String>,
			)
			""".trimIndent(),
		)
		val hosts = ConstraintArgAttributeDiscovery.discoverHosts(findAnnotation("TagsIn"))
		assertTrue(
			"expected VALUE NON_EMPTY on values, got $hosts",
			hosts.any {
				it.parameterName == "values" && it.target == ConstraintArgTarget.VALUE && ConstraintArgKind.NON_EMPTY in it.kinds
			},
		)
		assertTrue(
			"expected ELEMENT TYPED_LITERAL on values, got $hosts",
			hosts.any {
				it.parameterName == "values" && it.target == ConstraintArgTarget.ELEMENT && ConstraintArgKind.TYPED_LITERAL in it.kinds
			},
		)
	}
	
	private fun findAnnotation(shortName: String): KtAnnotationEntry = myFixture.file.children.filterIsInstance<KtClass>()
		.mapNotNull { it.primaryConstructor }
		.flatMap { it.valueParameters }
		.flatMap { it.annotationEntries }
		.first { it.shortName?.asString() == shortName }
}

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
import io.ghaylan.validata.intellij.support.ApiErrorLightFixtures
import io.ghaylan.validata.intellij.support.ValidataLightPlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * IDE diagnostics for `@ApiError` catalog contracts (KSP `ApiErrorCatalogVerifier` parity).
 *
 * Mirrors constraint typed-literal DX: unresolved red text, enum constant color, enum catalog.
 * 
 * @author Ghaylan Saada

 */
class ApiErrorCatalogAnnotatorTest : ValidataLightPlatformTestCase() {

	@DisplayName("invalid enum code is highlighted as unresolved")
	fun testInvalidEnumCodeIsError() {
		ApiErrorLightFixtures.addPresentationMarkers(myFixture)
		myFixture.configureByText(
			"BadCode.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
			  EMAIL_TAKEN("taken");
			  override val code: String get() = name
			}

			data class Req(
			  @ApiError(code = "NOPE", catalog = UserErrors::class)
			  val email: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected enum membership error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("NOPE") == true &&
					it.description?.contains("UserErrors") == true
			},
		)
		assertTrue(
			"invalid code should use UNRESOLVED attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.UNRESOLVED },
		)
	}

	@DisplayName("valid enum code uses ENUM color and is not an error")
	fun testValidEnumCodeHasEnumColor() {
		ApiErrorLightFixtures.addPresentationMarkers(myFixture)
		myFixture.configureByText(
			"GoodCode.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
			  EMAIL_TAKEN("taken");
			  override val code: String get() = name
			}

			data class Req(
			  @ApiError(code = "EMAIL_TAKEN", catalog = UserErrors::class)
			  val email: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected no ApiError catalog errors, got: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("@ApiError") == true
			},
		)
		assertTrue(
			"valid enum ApiError code should use ENUM attributes, got: ${highlights.map { it.forcedTextAttributesKey }}",
			highlights.any { it.forcedTextAttributesKey == ConstraintHighlightingColors.ENUM },
		)
	}

	@DisplayName("enum in another file still validates code membership")
	fun testCrossFileEnumCatalog() {
		ApiErrorLightFixtures.addPresentationMarkers(myFixture)
		myFixture.addFileToProject(
			"test/apierror/SampleApiErrors.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.model.ConstraintErrorDefinition
			enum class SampleApiErrors(override val message: String) : ConstraintErrorDefinition {
			  NAME_INVALID("bad name"),
			  USER_NOT_FOUND("missing");
			  override val code: String get() = name
			}
			""".trimIndent(),
		)
		myFixture.configureByText(
			"CreateUser.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.openapi.presentation.ApiError

			data class CreateUserRequest(
			  @field:ApiError(code = "NOPE", catalog = SampleApiErrors::class)
			  val name: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected cross-file enum membership error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("NOPE") == true &&
					it.description?.contains("SampleApiErrors") == true
			},
		)
	}

	@DisplayName("blank code is highlighted as an error")
	fun testBlankCodeIsError() {
		ApiErrorLightFixtures.addPresentationMarkers(myFixture)
		myFixture.configureByText(
			"Blank.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError

			enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
			  EMAIL_TAKEN("taken");
			  override val code: String get() = name
			}

			data class Req(
			  @ApiError(code = "  ", catalog = UserErrors::class)
			  val email: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected blank-code error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("must not be blank") == true
			},
		)
	}

	@DisplayName("non-enum catalog class is highlighted as an error")
	fun testNonEnumCatalogIsError() {
		ApiErrorLightFixtures.addPresentationMarkers(myFixture)
		myFixture.configureByText(
			"BadCatalog.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import io.ghaylan.validata.openapi.presentation.ApiError

			class NotAnEnum : ConstraintErrorDefinition {
			  override val code: String get() = "X"
			  override val message: String get() = "x"
			}

			data class Req(
			  @ApiError(code = "X", catalog = NotAnEnum::class)
			  val email: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected non-enum catalog error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("enum class") == true
			},
		)
	}

	@DisplayName("enum without ConstraintErrorDefinition is highlighted as an error")
	fun testEnumMissingInterfaceIsError() {
		ApiErrorLightFixtures.addPresentationMarkers(myFixture)
		myFixture.configureByText(
			"PlainEnum.kt",
			"""
			package test.apierror
			import io.ghaylan.validata.openapi.presentation.ApiError

			enum class PlainErrors { EMAIL_TAKEN }

			data class Req(
			  @ApiError(code = "EMAIL_TAKEN", catalog = PlainErrors::class)
			  val email: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected ConstraintErrorDefinition error, got: ${highlights.map { it.description }}",
			highlights.any {
				it.description?.contains("ConstraintErrorDefinition") == true
			},
		)
	}

	@DisplayName("without openapi stubs the annotator is a no-op")
	fun testAbsentOpenApiAnnotationsAreNoOp() {
		// Intentionally do not install ApiErrorLightFixtures.
		myFixture.configureByText(
			"NoOpenApi.kt",
			"""
			package test.apierror

			data class Req(
			  val email: String?,
			)
			""".trimIndent(),
		)
		val highlights = myFixture.doHighlighting()
		assertTrue(
			"expected no ApiError diagnostics without openapi, got: ${highlights.map { it.description }}",
			highlights.none {
				it.description?.contains("@ApiError") == true
			},
		)
	}
}

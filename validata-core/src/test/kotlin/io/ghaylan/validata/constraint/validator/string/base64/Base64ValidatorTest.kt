/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ghaylan.validata.constraint.validator.string.base64

import io.ghaylan.validata.constraint.annotation.Base64Constraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [Base64Validator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("Base64Validator")
class Base64ValidatorTest {
	
	private fun c(
		urlSafe: Boolean = false,
		requirePadding: Boolean = true
	) = Base64Constraint(
		urlSafe = urlSafe,
		requirePadding = requirePadding,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(Base64Validator, c())
		}
	}
	
	@Nested
	@DisplayName("standard Base64")
	inner class Standard {
		
		@Test
		@DisplayName("padded payload passes")
		fun padded() {
			assertValid(Base64Validator, "dGVzdA==", c())
		}
		
		@Test
		@DisplayName("unpadded payload fails when padding is required")
		fun unpaddedFailsWhenRequired() {
			assertInvalid(Base64Validator, "dGVzdA", c(requirePadding = true), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("unpadded payload passes when padding is not required")
		fun unpaddedPassesWhenOptional() {
			assertValid(Base64Validator, "dGVzdA", c(requirePadding = false))
		}
		
		@Test
		@DisplayName("invalid alphabet fails with VALUE_FORMAT_INVALID")
		fun invalidAlphabet() {
			assertInvalid(Base64Validator, "dGVzd*==", c(), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("whitespace-only string fails with VALUE_FORMAT_INVALID")
		fun whitespaceOnly() {
			assertInvalid(Base64Validator, "   ", c(), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
	
	@Nested
	@DisplayName("URL-safe Base64")
	inner class UrlSafe {
		
		@Test
		@DisplayName("url-safe payload passes")
		fun urlSafePayload() {
			assertValid(Base64Validator, "dGVzdA", c(urlSafe = true, requirePadding = false))
		}
		
		@Test
		@DisplayName("standard alphabet fails url-safe mode")
		fun standardAlphabetFails() {
			assertInvalid(
				Base64Validator,
				"dGVzdA+",
				c(urlSafe = true, requirePadding = false),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
}

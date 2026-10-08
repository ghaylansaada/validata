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
package io.ghaylan.validata.constraint.validator.string.url

import io.ghaylan.validata.constraint.annotation.Url
import io.ghaylan.validata.constraint.annotation.UrlConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

/** Unit tests for [UriValidator].
 *
 * @author Ghaylan Saada
 */
@DisplayName("UriValidator")
class UriValidatorTest {

	private fun c(
		type: Url.Type = Url.Type.ANY,
		maxLength: Int = 2048,
		allowedPorts: Set<String> = setOf("*"),
		allowedParams: Set<String> = setOf("*"),
		allowedProtocols: Set<String> = setOf("*"),
		allowedExtensions: Set<String> = setOf("*"),
	) = UrlConstraint(
		type = type,
		maxLength = maxLength,
		allowedPorts = allowedPorts,
		allowedParams = allowedParams,
		allowedProtocols = allowedProtocols,
		allowedExtensions = allowedExtensions,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)

	@Nested
	@DisplayName("null handling")
	inner class NullHandling {

		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(UriValidator, c())
		}
	}

	@Nested
	@DisplayName("valid URIs")
	inner class Valid {

		@Test
		@DisplayName("https URI with permissive policy passes")
		fun httpsExample() {
			assertValid(UriValidator, URI("https://example.com/path"), c())
		}

		@Test
		@DisplayName("image URI with matching extension passes")
		fun imageExtension() {
			assertValid(
				UriValidator,
				URI("https://cdn.example.com/photo.png"),
				c(type = Url.Type.IMAGE),
			)
		}
	}

	@Nested
	@DisplayName("policy filters")
	inner class PolicyFilters {

		@Test
		@DisplayName("disallowed scheme fails with VALUE_NOT_ALLOWED")
		fun protocolNotAllowed() {
			assertInvalid(
				UriValidator,
				URI("http://example.com"),
				c(allowedProtocols = setOf("https")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}

		@Test
		@DisplayName("WEBSITE type without host fails with VALUE_INVALID")
		fun missingHost() {
			assertInvalid(
				UriValidator,
				URI("path/to/resource"),
				c(type = Url.Type.WEBSITE),
				ConstraintErrorCode.VALUE_INVALID,
			)
		}
	}
}

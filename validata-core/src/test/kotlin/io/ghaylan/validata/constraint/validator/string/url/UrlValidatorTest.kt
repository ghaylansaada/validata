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

/** Unit tests for [UrlValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("UrlValidator")
class UrlValidatorTest {
	
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
	@DisplayName("null and blank handling")
	inner class NullAndBlank {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(UrlValidator, c())
		}
		
		@Test
		@DisplayName("blank value is skipped")
		fun skipsBlank() {
			assertValid(UrlValidator, "   ", c())
		}
	}
	
	@Nested
	@DisplayName("valid URLs")
	inner class Valid {
		
		@Test
		@DisplayName("https URL with permissive policy passes")
		fun httpsExample() {
			assertValid(UrlValidator, "https://example.com/path", c())
		}
		
		@Test
		@DisplayName("URL with allowed query parameter passes")
		fun allowedQueryParam() {
			val withFoo = c(allowedParams = setOf("foo"))
			assertValid(UrlValidator, "https://example.com?foo=1", withFoo)
		}
		
		@Test
		@DisplayName("image URL with matching extension passes")
		fun imageExtension() {
			assertValid(UrlValidator, "https://cdn.example.com/photo.png", c(type = Url.Type.IMAGE))
		}
	}
	
	@Nested
	@DisplayName("length and syntax")
	inner class LengthAndSyntax {
		
		@Test
		@DisplayName("URL exceeding maxLength fails with TEXT_TOO_LONG")
		fun tooLong() {
			assertInvalid(
				UrlValidator,
				"https://example.com",
				c(maxLength = 10),
				ConstraintErrorCode.TEXT_TOO_LONG,
			)
		}
		
		@Test
		@DisplayName("malformed URI fails with VALUE_FORMAT_INVALID")
		fun malformedUri() {
			assertInvalid(UrlValidator, "not a url", c(), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
	
	@Nested
	@DisplayName("host policy")
	inner class HostPolicy {
		
		@Test
		@DisplayName("WEBSITE type without host fails with VALUE_INVALID")
		fun missingHost() {
			assertInvalid(
				UrlValidator,
				"path/to/resource",
				c(type = Url.Type.WEBSITE),
				ConstraintErrorCode.VALUE_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("protocol, port, query, and extension policy")
	inner class PolicyFilters {
		
		@Test
		@DisplayName("disallowed scheme fails with VALUE_NOT_ALLOWED")
		fun protocolNotAllowed() {
			assertInvalid(
				UrlValidator,
				"http://example.com",
				c(allowedProtocols = setOf("https")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("disallowed port fails with VALUE_NOT_ALLOWED")
		fun portNotAllowed() {
			assertInvalid(
				UrlValidator,
				"https://example.com:8080/path",
				c(allowedPorts = setOf("443")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("any query string fails when params list is empty")
		fun queryRejectedOutright() {
			assertInvalid(
				UrlValidator,
				"https://example.com?a=1",
				c(allowedParams = emptySet()),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("unlisted query key fails with VALUE_NOT_ALLOWED")
		fun queryParamNotAllowed() {
			assertInvalid(
				UrlValidator,
				"https://example.com?bar=1",
				c(allowedParams = setOf("foo")),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("non-image extension fails for IMAGE type with VALUE_NOT_ALLOWED")
		fun extensionNotAllowed() {
			assertInvalid(
				UrlValidator,
				"https://example.com/file.txt",
				c(type = Url.Type.IMAGE),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
	}
}

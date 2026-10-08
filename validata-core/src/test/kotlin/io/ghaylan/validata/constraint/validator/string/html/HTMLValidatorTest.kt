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
package io.ghaylan.validata.constraint.validator.string.html

import io.ghaylan.validata.constraint.annotation.HtmlConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [HtmlValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("HtmlValidator")
class HtmlValidatorTest {
	
	private fun c(
		allowedTags: Set<String> = setOf("b", "i"),
		allowedAttrs: Set<String> = emptySet(),
		allowedProtocols: Set<String> = emptySet(),
	) = HtmlConstraint(
		allowedTags = allowedTags,
		allowedAttrs = allowedAttrs,
		allowedProtocols = allowedProtocols,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
	
	@Nested
	@DisplayName("null and blank handling")
	inner class NullAndBlank {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(HtmlValidator, c())
		}
		
		@Test
		@DisplayName("blank value is skipped")
		fun skipsBlank() {
			assertValid(HtmlValidator, "   ", c())
		}
	}
	
	@Nested
	@DisplayName("valid markup")
	inner class Valid {
		
		@Test
		@DisplayName("allowed tag passes")
		fun allowedTag() {
			assertValid(HtmlValidator, "<b>hi</b>", c())
		}
	}
	
	@Nested
	@DisplayName("tag policy")
	inner class TagPolicy {
		
		@Test
		@DisplayName("disallowed tag fails with VALUE_NOT_ALLOWED")
		fun disallowedTag() {
			assertInvalid(
				HtmlValidator,
				"<script>x</script>",
				c(),
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
	}
	
	@Nested
	@DisplayName("attribute and protocol policy")
	inner class AttributeAndProtocol {
		
		private val linkPolicy = c(
			allowedTags = setOf("a"),
			allowedAttrs = setOf("a:href"),
			allowedProtocols = setOf("a:href:https"),
		)
		
		@Test
		@DisplayName("disallowed attribute fails with VALUE_NOT_ALLOWED")
		fun disallowedAttribute() {
			assertInvalid(
				HtmlValidator,
				"""<a href="https://example.com" onclick="alert(1)">link</a>""",
				linkPolicy,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("disallowed URI scheme fails with VALUE_NOT_ALLOWED")
		fun disallowedProtocol() {
			assertInvalid(
				HtmlValidator,
				"""<a href="javascript:alert(1)">link</a>""",
				linkPolicy,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
	}
	
	@Nested
	@DisplayName("sanitization failures")
	inner class Sanitization {
		
		@Test
		@DisplayName("markup altered by sanitization fails with VALUE_SANITIZATION_MISMATCH")
		fun sanitizationMismatch() {
			assertInvalid(
				HtmlValidator,
				"<b>hi",
				c(allowedTags = setOf("b")),
				ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH,
			)
		}
	}
}

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
package io.ghaylan.validata.constraint.validator.string.isolanguage

import io.ghaylan.validata.constraint.annotation.IsoLanguageConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [IsoLanguageValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("IsoLanguageValidator")
class IsoLanguageValidatorTest {
	
	private val c = IsoLanguageConstraint("", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(IsoLanguageValidator, c)
		}
	}
	
	@Nested
	@DisplayName("valid tags")
	inner class Valid {
		
		@Test
		@DisplayName("two-letter language code passes")
		fun languageOnly() {
			assertValid(IsoLanguageValidator, "en", c)
		}
		
		@Test
		@DisplayName("language-region tag passes")
		fun languageRegion() {
			assertValid(IsoLanguageValidator, "en-US", c)
		}
	}
	
	@Nested
	@DisplayName("invalid tags")
	inner class Invalid {
		
		@Test
		@DisplayName("unknown language code fails with VALUE_FORMAT_INVALID")
		fun unknownLanguage() {
			assertInvalid(IsoLanguageValidator, "abc", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("underscore separator fails with VALUE_FORMAT_INVALID")
		fun underscoreSeparator() {
			assertInvalid(IsoLanguageValidator, "en_US", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("three-letter code fails with VALUE_FORMAT_INVALID")
		fun threeLetterCode() {
			assertInvalid(IsoLanguageValidator, "eng", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
}

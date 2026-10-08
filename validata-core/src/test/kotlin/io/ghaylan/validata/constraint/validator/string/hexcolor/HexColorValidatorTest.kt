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
package io.ghaylan.validata.constraint.validator.string.hexcolor

import io.ghaylan.validata.constraint.annotation.HexColorConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [HexColorValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("HexColorValidator")
class HexColorValidatorTest {
	
	private val c = HexColorConstraint("", ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(HexColorValidator, c)
		}
	}
	
	@Nested
	@DisplayName("valid colors")
	inner class Valid {
		
		@Test
		@DisplayName("six-digit hex passes")
		fun sixDigit() {
			assertValid(HexColorValidator, "#FF00AA", c)
		}
		
		@Test
		@DisplayName("three-digit shorthand hex passes")
		fun threeDigit() {
			assertValid(HexColorValidator, "#f0a", c)
		}
	}
	
	@Nested
	@DisplayName("invalid colors")
	inner class Invalid {
		
		@Test
		@DisplayName("named color fails with VALUE_FORMAT_INVALID")
		fun namedColor() {
			assertInvalid(HexColorValidator, "red", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("missing hash prefix fails with VALUE_FORMAT_INVALID")
		fun missingHash() {
			assertInvalid(HexColorValidator, "FF00AA", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("wrong length fails with VALUE_FORMAT_INVALID")
		fun wrongLength() {
			assertInvalid(HexColorValidator, "#FF00", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("non-hex characters fail with VALUE_FORMAT_INVALID")
		fun nonHexCharacters() {
			assertInvalid(HexColorValidator, "#GGGGGG", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("whitespace-only string fails with VALUE_FORMAT_INVALID")
		fun whitespaceOnly() {
			assertInvalid(HexColorValidator, "   ", c, ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
}

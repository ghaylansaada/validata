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
package io.ghaylan.validata.constraint.validator.required

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RequiredValidator].
 * 
 * @author Ghaylan Saada
 */
class RequiredValidatorTest {
	
	private fun constraint(mode: Required.Mode = Required.Mode.STRICT): RequiredConstraint =
		RequiredConstraint(mode = mode, message = "", groups = ValidatorTestSupport.defaultGroups)
	
	@Nested
	@DisplayName("default (STRICT) mode")
	inner class DefaultStrictMode {
		
		@Test
		@DisplayName("constraint() helper defaults to STRICT")
		fun helperDefaultsToStrict() {
			assertInvalid(RequiredValidator, mapOf("a" to null, "b" to ""), constraint(), ConstraintErrorCode.VALUE_EMPTY)
		}
		
		@Test
		@DisplayName("null fails with VALUE_MISSING")
		fun nullFailsWithValueMissing() {
			assertInvalid(RequiredValidator, null, constraint(), ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("non-blank scalar passes")
		fun nonBlankScalarPasses() {
			assertValid(RequiredValidator, "ok", constraint())
		}
	}
	
	@Nested
	@DisplayName("NULL mode")
	inner class NullMode {
		
		@Test
		@DisplayName("null fails with VALUE_MISSING")
		fun nullFailsWithValueMissing() {
			assertInvalid(RequiredValidator, null, constraint(Required.Mode.NULL), ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("blank CharSequence passes")
		fun blankCharSequencePasses() {
			assertValid(RequiredValidator, "  ", constraint(Required.Mode.NULL))
		}
		
		@Test
		@DisplayName("empty CharSequence passes")
		fun emptyCharSequencePasses() {
			assertValid(RequiredValidator, "", constraint(Required.Mode.NULL))
		}
		
		@Test
		@DisplayName("empty collection passes")
		fun emptyCollectionPasses() {
			assertValid(RequiredValidator, emptyList<Any>(), constraint(Required.Mode.NULL))
		}
		
		@Test
		@DisplayName("empty map passes")
		fun emptyMapPasses() {
			assertValid(RequiredValidator, emptyMap<String, Any>(), constraint(Required.Mode.NULL))
		}
		
		@Test
		@DisplayName("non-null scalar passes")
		fun nonNullScalarPasses() {
			assertValid(RequiredValidator, 0, constraint(Required.Mode.NULL))
			assertValid(RequiredValidator, false, constraint(Required.Mode.NULL))
		}
	}
	
	@Nested
	@DisplayName("EMPTY mode")
	inner class EmptyMode {
		
		@Test
		@DisplayName("null fails with VALUE_MISSING")
		fun nullFailsWithValueMissing() {
			assertInvalid(RequiredValidator, null, constraint(Required.Mode.EMPTY), ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("blank CharSequence passes (whitespace is not empty)")
		fun blankCharSequencePasses() {
			assertValid(RequiredValidator, "  ", constraint(Required.Mode.EMPTY))
		}
		
		@Test
		@DisplayName("empty CharSequence fails with TEXT_BLANK")
		fun emptyCharSequenceFailsWithValueEmpty() {
			assertInvalid(RequiredValidator, "", constraint(Required.Mode.EMPTY), ConstraintErrorCode.TEXT_BLANK)
		}
		
		@Test
		@DisplayName("empty collection fails with VALUE_EMPTY")
		fun emptyCollectionFailsWithValueEmpty() {
			assertInvalid(RequiredValidator, emptyList<Any>(), constraint(Required.Mode.EMPTY), ConstraintErrorCode.VALUE_EMPTY)
		}
		
		@Test
		@DisplayName("non-empty collection passes")
		fun nonEmptyCollectionPasses() {
			assertValid(RequiredValidator, listOf(1), constraint(Required.Mode.EMPTY))
		}
	}
	
	@Nested
	@DisplayName("STRICT mode")
	inner class StrictMode {
		
		@Test
		@DisplayName("null fails with VALUE_MISSING")
		fun nullFailsWithValueMissing() {
			assertInvalid(RequiredValidator, null, constraint(Required.Mode.STRICT), ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("blank CharSequence fails with TEXT_BLANK")
		fun blankCharSequenceFailsWithValueEmpty() {
			assertInvalid(RequiredValidator, "  ", constraint(Required.Mode.STRICT), ConstraintErrorCode.TEXT_BLANK)
		}
		
		@Test
		@DisplayName("empty CharSequence fails with TEXT_BLANK")
		fun emptyCharSequenceFailsWithValueEmpty() {
			assertInvalid(RequiredValidator, "", constraint(Required.Mode.STRICT), ConstraintErrorCode.TEXT_BLANK)
		}
		
		@Test
		@DisplayName("non-blank CharSequence passes")
		fun nonBlankCharSequencePasses() {
			assertValid(RequiredValidator, "x", constraint(Required.Mode.STRICT))
		}
		
		@Test
		@DisplayName("empty collection fails with VALUE_EMPTY")
		fun emptyCollectionFailsWithValueEmpty() {
			assertInvalid(RequiredValidator, emptyList<Any>(), constraint(Required.Mode.STRICT), ConstraintErrorCode.VALUE_EMPTY)
		}
		
		@Test
		@DisplayName("nested all-empty structure fails with VALUE_EMPTY")
		fun nestedAllEmptyFailsWithValueEmpty() {
			val nested = mapOf("a" to null, "b" to "")
			assertInvalid(RequiredValidator, nested, constraint(Required.Mode.STRICT), ConstraintErrorCode.VALUE_EMPTY)
		}
		
		@Test
		@DisplayName("nested structure with present leaf passes")
		fun nestedWithPresentLeafPasses() {
			val nested = mapOf("a" to "x", "b" to "")
			assertValid(RequiredValidator, nested, constraint(Required.Mode.STRICT))
		}
	}
}

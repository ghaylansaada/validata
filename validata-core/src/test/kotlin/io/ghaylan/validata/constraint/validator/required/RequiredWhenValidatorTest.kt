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
import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.constraint.annotation.RequiredWhenConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.SiblingContexts
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RequiredWhenValidator].
 * 
 * @author Ghaylan Saada
 */
class RequiredWhenValidatorTest {
	
	private fun constraint(
		condition: RequiredWhen.Condition,
		value: String = "YES",
		values: Set<String> = emptySet(),
		mode: Required.Mode = Required.Mode.STRICT,
	): RequiredWhenConstraint = RequiredWhenConstraint(
		ref = "gate",
		condition = condition,
		value = value,
		values = values,
		mode = mode,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
	
	@Nested
	@DisplayName("EQ gate")
	inner class EqualsGate {
		
		@Test
		@DisplayName("requires payload when gate matches")
		fun requiresPayloadWhenGateMatches() {
			val c = constraint(RequiredWhen.Condition.EQ)
			val ctx = SiblingContexts.gate("YES")
			assertInvalid(
				RequiredWhenValidator,
				null,
				c,
				ConstraintErrorCode.VALUE_MISSING,
				c,
				ctx,
			)
		}
		
		@Test
		@DisplayName("skips when gate does not match")
		fun skipsWhenGateDoesNotMatch() {
			val c = constraint(RequiredWhen.Condition.EQ)
			val ctx = SiblingContexts.gate("NO")
			assertValid(RequiredWhenValidator, null, c, ctx)
			assertValid(RequiredWhenValidator, "  ", c, ctx)
		}
		
		@Test
		@DisplayName("passes when gate matches and payload is present")
		fun passesWhenGateMatchesAndPayloadPresent() {
			val c = constraint(RequiredWhen.Condition.EQ)
			val ctx = SiblingContexts.gate("YES")
			assertValid(RequiredWhenValidator, "ok", c, ctx)
		}
		
		@Test
		@DisplayName("null gate never matches")
		fun nullGateNeverMatches() {
			val c = constraint(RequiredWhen.Condition.EQ)
			val ctx = SiblingContexts.gate(null)
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
	}
	
	@Nested
	@DisplayName("NE gate")
	inner class NotEqualsGate {
		
		@Test
		@DisplayName("requires payload when gate differs from literal")
		fun requiresPayloadWhenGateDiffers() {
			val c = constraint(RequiredWhen.Condition.NE)
			val ctx = SiblingContexts.gate("NO")
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
		
		@Test
		@DisplayName("skips when gate equals literal")
		fun skipsWhenGateEqualsLiteral() {
			val c = constraint(RequiredWhen.Condition.NE)
			val ctx = SiblingContexts.gate("YES")
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
		
		@Test
		@DisplayName("null gate never activates")
		fun nullGateNeverActivates() {
			val c = constraint(RequiredWhen.Condition.NE)
			val ctx = SiblingContexts.gate(null)
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
	}
	
	@Nested
	@DisplayName("IN gate")
	inner class InGate {
		
		@Test
		@DisplayName("requires payload when gate is in allowed values")
		fun requiresPayloadWhenGateInValues() {
			val c = constraint(RequiredWhen.Condition.IN, value = "", values = setOf("A", "B"))
			val ctx = SiblingContexts.gate("A")
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
		
		@Test
		@DisplayName("skips when gate is not in allowed values")
		fun skipsWhenGateNotInValues() {
			val c = constraint(RequiredWhen.Condition.IN, value = "", values = setOf("A", "B"))
			val ctx = SiblingContexts.gate("Z")
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
		
		@Test
		@DisplayName("null gate never matches")
		fun nullGateNeverMatches() {
			val c = constraint(RequiredWhen.Condition.IN, value = "", values = setOf("A"))
			val ctx = SiblingContexts.gate(null)
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
	}
	
	@Nested
	@DisplayName("NIN gate")
	inner class NotInGate {
		
		@Test
		@DisplayName("requires payload when gate is not in excluded values")
		fun requiresPayloadWhenGateNotInValues() {
			val c = constraint(RequiredWhen.Condition.NIN, value = "", values = setOf("A", "B"))
			val ctx = SiblingContexts.gate("Z")
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
		
		@Test
		@DisplayName("skips when gate is in excluded values")
		fun skipsWhenGateInValues() {
			val c = constraint(RequiredWhen.Condition.NIN, value = "", values = setOf("A", "B"))
			val ctx = SiblingContexts.gate("A")
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
		
		@Test
		@DisplayName("null gate never activates")
		fun nullGateNeverActivates() {
			val c = constraint(RequiredWhen.Condition.NIN, value = "", values = setOf("A"))
			val ctx = SiblingContexts.gate(null)
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
	}
	
	@Nested
	@DisplayName("PRESENT gate")
	inner class PresentGate {
		
		@Test
		@DisplayName("requires payload when gate property is present")
		fun requiresPayloadWhenGatePresent() {
			val c = constraint(RequiredWhen.Condition.PRESENT, value = "")
			val ctx = SiblingContexts.gate("x")
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
		
		@Test
		@DisplayName("skips when gate property is missing")
		fun skipsWhenGateMissing() {
			val c = constraint(RequiredWhen.Condition.PRESENT, value = "")
			val ctx = SiblingContexts.gate(null)
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
		
		@Test
		@DisplayName("blank gate is not present under STRICT")
		fun blankGateIsNotPresentUnderDeep() {
			val c = constraint(RequiredWhen.Condition.PRESENT, value = "")
			val ctx = SiblingContexts.gate("  ")
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
	}
	
	@Nested
	@DisplayName("MISSING (absent) gate")
	inner class MissingGate {
		
		@Test
		@DisplayName("requires payload when gate property is missing")
		fun requiresPayloadWhenGateMissing() {
			val c = constraint(RequiredWhen.Condition.MISSING, value = "")
			val ctx = SiblingContexts.gate(null)
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
		
		@Test
		@DisplayName("skips when gate property is present")
		fun skipsWhenGatePresent() {
			val c = constraint(RequiredWhen.Condition.MISSING, value = "")
			val ctx = SiblingContexts.gate("x")
			assertValid(RequiredWhenValidator, null, c, ctx)
		}
		
		@Test
		@DisplayName("blank gate counts as missing under STRICT")
		fun blankGateCountsAsMissingUnderDeep() {
			val c = constraint(RequiredWhen.Condition.MISSING, value = "")
			val ctx = SiblingContexts.gate("  ")
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
	}
	
	@Nested
	@DisplayName("Ordering gates (GT / LT / GTE / LTE)")
	inner class OrderingGates {
		
		@Test
		@DisplayName("GT activates only above the literal")
		fun greaterThan() {
			val c = constraint(RequiredWhen.Condition.GT, value = "10")
			assertInvalid(
				RequiredWhenValidator,
				null,
				c,
				ConstraintErrorCode.VALUE_MISSING,
				SiblingContexts.intGate(11),
			)
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(10))
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(9))
		}
		
		@Test
		@DisplayName("GTE activates at and above the literal")
		fun greaterThanOrEqual() {
			val c = constraint(RequiredWhen.Condition.GTE, value = "10")
			assertInvalid(
				RequiredWhenValidator,
				null,
				c,
				ConstraintErrorCode.VALUE_MISSING,
				SiblingContexts.intGate(10),
			)
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(9))
		}
		
		@Test
		@DisplayName("LT activates only below the literal")
		fun lowerThan() {
			val c = constraint(RequiredWhen.Condition.LT, value = "10")
			assertInvalid(
				RequiredWhenValidator,
				null,
				c,
				ConstraintErrorCode.VALUE_MISSING,
				SiblingContexts.intGate(9),
			)
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(10))
		}
		
		@Test
		@DisplayName("LTE activates at and below the literal")
		fun lowerThanOrEqual() {
			val c = constraint(RequiredWhen.Condition.LTE, value = "10")
			assertInvalid(
				RequiredWhenValidator,
				null,
				c,
				ConstraintErrorCode.VALUE_MISSING,
				SiblingContexts.intGate(10),
			)
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(11))
		}
		
		/**
		 * Numeric comparison must win over lexicographic comparison: `"9" > "10"` as text,
		 * but `9 < 10` as numbers.
		 */
		@Test
		@DisplayName("compares numerically, not lexicographically")
		fun comparesNumerically() {
			val c = constraint(RequiredWhen.Condition.GT, value = "10")
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(9))
		}
		
		@Test
		@DisplayName("falls back to string ordering for non-numeric gates")
		fun fallsBackToStringOrdering() {
			val c = constraint(RequiredWhen.Condition.GT, value = "m")
			assertInvalid(
				RequiredWhenValidator,
				null,
				c,
				ConstraintErrorCode.VALUE_MISSING,
				SiblingContexts.gate("z"),
			)
			assertValid(RequiredWhenValidator, null, c, SiblingContexts.gate("a"))
		}
		
		@Test
		@DisplayName("null gate never activates an ordering gate")
		fun nullGateNeverActivates() {
			for (condition in listOf(
				RequiredWhen.Condition.GT,
				RequiredWhen.Condition.GTE,
				RequiredWhen.Condition.LT,
				RequiredWhen.Condition.LTE,
			)) {
				val c = constraint(condition, value = "10")
				assertValid(RequiredWhenValidator, null, c, SiblingContexts.intGate(null))
			}
		}
	}
	
	@Nested
	@DisplayName("Active gate applies Required semantics")
	inner class ActiveRequiredSemantics {
		
		@Test
		@DisplayName("blank payload fails with TEXT_BLANK when gate is active")
		fun blankPayloadFailsWithValueEmptyWhenGateActive() {
			val c = constraint(RequiredWhen.Condition.EQ)
			val ctx = SiblingContexts.gate("YES")
			assertInvalid(RequiredWhenValidator, "  ", c, ConstraintErrorCode.TEXT_BLANK, ctx)
		}
		
		@Test
		@DisplayName("inactive gate skips blank payload")
		fun inactiveGateSkipsBlankPayload() {
			val c = constraint(RequiredWhen.Condition.EQ)
			val ctx = SiblingContexts.gate("NO")
			assertValid(RequiredWhenValidator, "  ", c, ctx)
		}
		
		@Test
		@DisplayName("numeric gate compares via toString")
		fun numericGateComparesViaToString() {
			val c = constraint(RequiredWhen.Condition.EQ, value = "42")
			val ctx = SiblingContexts.intGate(42)
			assertInvalid(RequiredWhenValidator, null, c, ConstraintErrorCode.VALUE_MISSING, ctx)
		}
	}
}

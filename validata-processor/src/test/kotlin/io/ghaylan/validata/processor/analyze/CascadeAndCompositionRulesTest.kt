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
package io.ghaylan.validata.processor.analyze

import io.ghaylan.validata.processor.model.ConstraintModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure-slice unit coverage for [CascadeDecision] and [CompositionOrRules] (T4.1 / T4.4).
 * 
 * @author Ghaylan Saada
 */
class CascadeAndCompositionRulesTest {
	
	@Nested
	@DisplayName("CascadeDecision")
	inner class Cascade {
		
		@Test
		@DisplayName("facts.noCascade yields SCALAR_OTHER (pure table; production uses CascadePolicy first)")
		fun noCascade() {
			assertThat(
				CascadeDecision.decide(
					CascadeFacts(
						markedValidatable = true,
						noCascade = true,
						sameCompilation = true,
						strictCrossModuleCascade = true,
					),
				),
			).isEqualTo(CascadeOutcome.SCALAR_OTHER)
		}
		
		@Test
		@DisplayName("marked @Validatable yields OBJECT_REF")
		fun marked() {
			assertThat(
				CascadeDecision.decide(
					CascadeFacts(
						markedValidatable = true,
						noCascade = false,
						sameCompilation = false,
						strictCrossModuleCascade = false,
					),
				),
			).isEqualTo(CascadeOutcome.OBJECT_REF)
		}
		
		@Test
		@DisplayName("same-compilation unmarked yields ERROR_DYNAMIC")
		fun sameCompilationUnmarked() {
			assertThat(
				CascadeDecision.decide(
					CascadeFacts(
						markedValidatable = false,
						noCascade = false,
						sameCompilation = true,
						strictCrossModuleCascade = false,
					),
				),
			).isEqualTo(CascadeOutcome.ERROR_DYNAMIC)
		}
		
		@Test
		@DisplayName("strict cross-module unmarked yields ERROR_DYNAMIC")
		fun strictCrossModule() {
			assertThat(
				CascadeDecision.decide(
					CascadeFacts(
						markedValidatable = false,
						noCascade = false,
						sameCompilation = false,
						strictCrossModuleCascade = true,
					),
				),
			).isEqualTo(CascadeOutcome.ERROR_DYNAMIC)
		}
		
		@Test
		@DisplayName("cross-module unmarked without strict yields WARN_OBJECT_REF")
		fun crossModuleWarn() {
			assertThat(
				CascadeDecision.decide(
					CascadeFacts(
						markedValidatable = false,
						noCascade = false,
						sameCompilation = false,
						strictCrossModuleCascade = false,
					),
				),
			).isEqualTo(CascadeOutcome.WARN_OBJECT_REF)
		}
	}
	
	@Nested
	@DisplayName("CompositionOrRules")
	inner class CompositionOr {
		
		@Test
		@DisplayName("fewer than two leaves is rejected")
		fun tooFewLeaves() {
			val err = CompositionOrRules.validateLeaves(
				"LonelyOr",
				listOf(leaf("FormatOk")),
			)
			assertThat(err).contains("OR requires at least 2")
		}
		
		@Test
		@DisplayName("presence leaf is rejected")
		fun presenceRejected() {
			val err = CompositionOrRules.validateLeaves(
				"RequiredOrFormat",
				listOf(leaf("Required"), leaf("FormatOk")),
			)
			assertThat(err).contains("Presence must stay outside OR")
		}
		
		@Test
		@DisplayName("two format leaves are accepted")
		fun twoLeavesOk() {
			assertThat(
				CompositionOrRules.validateLeaves(
					"FormatOrToken",
					listOf(leaf("FormatOk"), leaf("TokenCheck")),
				),
			).isNull()
		}
	}
	
	private fun leaf(simpleName: String): ConstraintModel = ConstraintModel(
		metadataConstructorCall = "${simpleName}Constraint()",
		validatorExpression = "t.${simpleName}Validator",
		order = 0,
		annotationSimpleName = simpleName,
	)
}

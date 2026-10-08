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
package io.ghaylan.validata.engine

import io.ghaylan.validata.constraint.annotation.EmailConstraint
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.TestValidationContext
import io.ghaylan.validata.support.ValidatorTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [compileConstraints] and [ValidatorBackedRunner].
 * 
 * @author Ghaylan Saada
 */
class CompileConstraintsAndRunnerTest {
	
	private val groups = ValidatorTestSupport.defaultGroups
	
	@Nested
	@DisplayName("compileConstraints")
	inner class Compile {
		
		@Test
		@DisplayName("empty map yields empty compiled list")
		fun emptyMap() {
			assertThat(compileConstraints(emptyMap<io.ghaylan.validata.constraint.ConstraintMetadata, io.ghaylan.validata.constraint.ConstraintValidator<*, *>>())).isEmpty()
		}
		
		@Test
		@DisplayName("preserves LinkedHashMap iteration order with sequential order indices")
		fun preservesOrder() {
			val required = RequiredConstraint(Required.Mode.STRICT, "", groups)
			val email = EmailConstraint("", groups)
			val map = linkedMapOf(
				required to RequiredValidator,
				email to EmailValidator,
			)
			val compiled = compileConstraints(map)
			assertThat(compiled).hasSize(2)
			assertThat(compiled.map { it.order }).containsExactly(0, 1)
			assertThat(compiled[0].metadata).isSameAs(required)
			assertThat(compiled[1].metadata).isSameAs(email)
			assertThat(compiled[0].runner).isInstanceOf(ValidatorBackedRunner::class.java)
		}
	}
	
	@Nested
	@DisplayName("ValidatorBackedRunner")
	inner class Runner {
		
		@Test
		@DisplayName("execute delegates to validator and returns ConstraintError on failure")
		fun executeFailure() {
			val metadata = RequiredConstraint(Required.Mode.STRICT, "", groups)
			val runner = ValidatorBackedRunner(RequiredValidator, metadata)
			val error = runner.execute(null, TestValidationContext(groups = groups))
			assertThat(error).isNotNull
			assertThat(error!!.code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}
		
		@Test
		@DisplayName("execute returns null when validator accepts the value")
		fun executeSuccess() {
			val metadata = RequiredConstraint(Required.Mode.STRICT, "", groups)
			val runner = ValidatorBackedRunner(RequiredValidator, metadata)
			assertThat(runner.execute("ok", TestValidationContext(groups = groups))).isNull()
		}
		
		@Test
		@DisplayName("run without ValidationContext fails loudly")
		fun runWithoutContextFails() {
			val metadata = RequiredConstraint(Required.Mode.STRICT, "", groups)
			val runner = ValidatorBackedRunner(RequiredValidator, metadata)
			assertThatThrownBy { runner.run("x", metadata) }.isInstanceOf(IllegalStateException::class.java)
				.hasMessageContaining("ValidationContext")
		}
	}
}

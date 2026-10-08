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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.runtime.ValidationContext
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class EndpointErrorCodeCollectorWarnTest {
	
	@BeforeEach
	fun reset() {
		EndpointErrorCodeCollector.resetForTests()
	}
	
	@Test
	@DisplayName("empty possibleErrorCodes logs a one-time warning")
	fun warnsOnceOnEmptyPossibleErrorCodes(output: CapturedOutput) {
		val size = SizeConstraint(1, 2, "", setOf(OnDefault::class))
		val endpoint = EndpointSchema(
			id = "warn-test",
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "x",
						externalName = "x",
						shape = ScalarShape(ScalarKind.STRING),
						read = { null },
						constraints = listOf(
							CompiledConstraint(
								size,
								ValidatorBackedRunner(EmptyCodesValidator, size),
								0,
							),
						),
					),
				),
			),
			groups = setOf(OnDefault::class),
		)
		
		EndpointErrorCodeCollector.collect(endpoint)
		EndpointErrorCodeCollector.collect(endpoint)
		val matches = Regex("empty possibleErrorCodes").findAll(output.out + output.err)
			.count()
		assertThat(matches).isEqualTo(1)
		assertThat(output.out + output.err).contains(EmptyCodesValidator::class.java.name)
			.contains("Override possibleErrorCodes")
			.doesNotContain("OPENAPI_INTEGRATION_PLAN")
		assertThat(EndpointErrorCodeCollector.collect(endpoint)).contains("VALUE_TYPE_MISMATCH", "STRUCTURE_DEPTH_EXCEEDED")
			.doesNotContain("TEXT_TOO_SHORT")
	}
	
	/**
	 * Stub validator that intentionally leaves possibleErrorCodes empty.
	 */
	private object EmptyCodesValidator: ConstraintValidator<Any, ConstraintMetadata>() {
		
		override fun validate(
			value: Any,
			constraint: ConstraintMetadata,
			context: ValidationContext,
		): ConstraintError<*>? = null
	}
}

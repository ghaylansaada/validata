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
package io.ghaylan.validata.golden

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.MinConstraint
import io.ghaylan.validata.constraint.validator.bound.duration.DurationMinValidator
import io.ghaylan.validata.constraint.validator.temporal.min.TemporalMinValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

/**
 * L4 golden: Duration Min and LocalDate Min on one DTO in a single engine walk.
 * 
 * @author Ghaylan Saada
 */
class TemporalBoundsValidationTest {
	
	data class SessionConfig(
		val sessionTimeout: Duration?,
		val startDate: LocalDate?,
	)
	
	private val groups = setOf(OnDefault::class)
	
	private fun <M: ConstraintMetadata> compiled(
		meta: M,
		validator: ConstraintValidator<*, M>,
	) = CompiledConstraint(
		metadata = meta,
		runner = ValidatorBackedRunner(validator, meta),
		order = 0,
	)
	
	private fun schema(): ObjectSchema {
		val durationMin = compiled(
			MinConstraint("PT30M", true, "", groups),
			DurationMinValidator,
		)
		val dateMin = compiled(
			MinConstraint("2024-01-01", true, "", groups),
			TemporalMinValidator,
		)
		return ObjectSchema(
			type = SessionConfig::class.java,
			properties = listOf(
				PropertySpec(
					"sessionTimeout",
					"sessionTimeout",
					ScalarShape(ScalarKind.TEMPORAL),
					{ (it as SessionConfig).sessionTimeout },
					constraints = listOf(durationMin),
				),
				PropertySpec(
					"startDate",
					"startDate",
					ScalarShape(ScalarKind.TEMPORAL),
					{ (it as SessionConfig).startDate },
					constraints = listOf(dateMin),
				),
			),
		)
	}
	
	@Nested
	@DisplayName("Duration Min + LocalDate Min")
	inner class Bounds {
		
		@Test
		@DisplayName("valid session config yields no errors")
		fun validConfig() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-temporal-valid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = SessionConfig(
					sessionTimeout = Duration.ofHours(1),
					startDate = LocalDate.of(2024, 6, 1),
				),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).isEmpty()
		}
		
		@Test
		@DisplayName("below-min duration and date report TEMPORAL_DURATION_TOO_SHORT and TEMPORAL_TOO_EARLY")
		fun invalidConfig() {
			val engine = EngineTestSupport.engine()
			val endpoint = EngineTestSupport.bodyRequestSchema(
				body = schema(),
				id = "golden-temporal-invalid",
			)
			val errors = engine.validateRequest(
				schema = endpoint,
				body = SessionConfig(
					sessionTimeout = Duration.ofMinutes(15),
					startDate = LocalDate.of(2023, 12, 31),
				),
				params = null,
				headers = null,
				pathVariables = null,
			)
			assertThat(errors).hasSize(2)
			assertThat(errors.map { it.path }).containsExactlyInAnyOrder("sessionTimeout", "startDate")
			assertThat(errors.map { it.code }).containsExactlyInAnyOrder(ConstraintErrorCode.TEMPORAL_DURATION_TOO_SHORT,
				ConstraintErrorCode.TEMPORAL_TOO_EARLY)
		}
	}
}

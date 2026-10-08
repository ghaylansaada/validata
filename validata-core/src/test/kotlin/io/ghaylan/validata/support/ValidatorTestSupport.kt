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
package io.ghaylan.validata.support

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext
import org.assertj.core.api.Assertions.assertThat
import kotlin.reflect.KClass

/**
 * Shared assertions for L2 validator unit tests.
 *
 * Prefer these helpers over open-coding AssertJ so every `*ValidatorTest` reads the same way.*
 * 
 * @author Ghaylan Saada
 */
object ValidatorTestSupport {
	
	val defaultGroups: Set<KClass<*>> = setOf(OnDefault::class)
	
	fun ctx(): ValidationContext = TestValidationContext(groups = defaultGroups)
	
	fun <V: Any, C: ConstraintMetadata> assertValid(
		validator: ConstraintValidator<V, C>,
		value: Any?,
		constraint: C,
		context: ValidationContext = ctx(),
	) {
		assertThat(validator.runValidation(value, constraint, context)).withFailMessage {
			"expected valid for value=$value constraint=$constraint"
		}
			.isNull()
	}
	
	fun <V: Any, C: ConstraintMetadata> assertInvalid(
		validator: ConstraintValidator<V, C>,
		value: Any?,
		constraint: C,
		expectedCode: ConstraintErrorDefinition,
		context: ValidationContext = ctx(),
	) {
		val error = validator.runValidation(value, constraint, context)
		assertThat(error).withFailMessage { "expected failure code=$expectedCode for value=$value" }
			.isNotNull()
		assertThat(error!!.code).isEqualTo(expectedCode)
	}
	
	/**
	 * Like [assertInvalid], also asserting the failure [ConstraintError.metadata].
	 */
	fun <V: Any, C: ConstraintMetadata> assertInvalid(
		validator: ConstraintValidator<V, C>,
		value: Any?,
		constraint: C,
		expectedCode: ConstraintErrorDefinition,
		expectedMetadata: Any?,
		context: ValidationContext = ctx(),
	) {
		val error = validator.runValidation(value, constraint, context)
		assertThat(error).withFailMessage { "expected failure code=$expectedCode for value=$value" }
			.isNotNull()
		assertThat(error!!.code).isEqualTo(expectedCode)
		assertThat(error.metadata).isEqualTo(expectedMetadata)
	}
	
	fun <V: Any, C: ConstraintMetadata> assertSkipsNull(
		validator: ConstraintValidator<V, C>,
		constraint: C,
		context: ValidationContext = ctx(),
	) {
		assertValid(validator, null, constraint, context)
	}
}

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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.constraint.validator.size.CollectionSizeValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class EndpointErrorCodeCollectorTest {

	@BeforeEach
	fun resetCaches() {
		EndpointErrorCodeCollector.resetForTests()
	}

	@Test
	@DisplayName("string Size contributes TEXT_* codes, not COLLECTION_*")
	fun stringSizeExcludesCollectionCodes() {
		val size = SizeConstraint(2, 40, "", setOf(OnDefault::class))
		val endpoint = EndpointSchema(
			id = "test",
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "name",
						externalName = "first_name",
						shape = ScalarShape(ScalarKind.STRING),
						read = ValueReader { null },
						constraints = listOf(
							CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 0),
						),
					),
				),
			),
			groups = setOf(OnDefault::class),
		)
		val codes = EndpointErrorCodeCollector.collect(endpoint)
		assertThat(codes).contains(
			ConstraintErrorCode.VALUE_TYPE_MISMATCH.name,
			ConstraintErrorCode.STRUCTURE_DEPTH_EXCEEDED.name,
			"TEXT_TOO_SHORT",
			"TEXT_TOO_LONG",
		)
		assertThat(codes).doesNotContain(
			ConstraintErrorCode.COLLECTION_TOO_SMALL.name,
			ConstraintErrorCode.COLLECTION_TOO_LARGE.name,
		)
	}

	@Test
	@DisplayName("Required adds VALUE_MISSING; inactive group constraints are omitted")
	fun groupsFilterAndRequired() {
		val requiredCreate = RequiredConstraint(Required.Mode.STRICT, "", setOf(OnCreate::class))
		val collSize = SizeConstraint(1, 3, "", setOf(OnDefault::class))
		val endpoint = EndpointSchema(
			id = "test",
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "note",
						externalName = "note",
						shape = ScalarShape(ScalarKind.STRING),
						read = ValueReader { null },
						constraints = listOf(
							CompiledConstraint(
								requiredCreate,
								ValidatorBackedRunner(RequiredValidator, requiredCreate),
								0,
							),
						),
					),
					PropertySpec(
						declaredName = "tags",
						externalName = "tags",
						shape = IterableShape(ScalarShape(ScalarKind.STRING)),
						read = ValueReader { null },
						constraints = listOf(
							CompiledConstraint(
								collSize,
								ValidatorBackedRunner(CollectionSizeValidator, collSize),
								0,
							),
						),
					),
				),
			),
			groups = setOf(OnDefault::class),
		)
		val codes = EndpointErrorCodeCollector.collect(endpoint)
		assertThat(codes).contains(
			ConstraintErrorCode.COLLECTION_TOO_SMALL.name,
			ConstraintErrorCode.COLLECTION_TOO_LARGE.name,
		)
		assertThat(codes).doesNotContain(ConstraintErrorCode.VALUE_MISSING.name)
		// RequiredValidator codes only when Required is active
		assertThat(codes).doesNotContain("VALUE_EMPTY")
	}

	@Test
	@DisplayName("collect caches by endpoint id and returns the same set instance")
	fun collectCachesByEndpointId() {
		val size = SizeConstraint(2, 40, "", setOf(OnDefault::class))
		val endpoint = EndpointSchema(
			id = "cache-key",
			requestBody = ObjectSchema(
				type = Any::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "name",
						externalName = "name",
						shape = ScalarShape(ScalarKind.STRING),
						read = ValueReader { null },
						constraints = listOf(
							CompiledConstraint(size, ValidatorBackedRunner(CharSequenceSizeValidator, size), 0),
						),
					),
				),
			),
			groups = setOf(OnDefault::class),
		)
		val first = EndpointErrorCodeCollector.collect(endpoint)
		val second = EndpointErrorCodeCollector.collect(endpoint)
		assertThat(second).isSameAs(first)
	}
}

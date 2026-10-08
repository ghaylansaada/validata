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

import io.ghaylan.validata.constraint.annotation.EmailConstraint
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.constraint.validator.size.CharSequenceSizeValidator
import io.ghaylan.validata.constraint.validator.size.CollectionSizeValidator
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.openapi.docs.ConstraintDocumentations
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ObjectSchemaOpenApiEnricherTest {
	
	private val groups: Set<KClass<*>> = setOf(OnDefault::class)
	private val mappers: List<OpenApiConstraintMapper> = emptyList()
	
	@BeforeEach
	fun reset() {
		ConstraintDocumentations.resetForTests()
		OpenApiEnrichmentCache.resetForTests()
	}
	
	@Test
	@DisplayName("enriches by externalName and renames declared-name keys")
	fun usesExternalNameAndSize() {
		val openApi = Schema<Any>().apply {
			properties = linkedMapOf(
				"name" to Schema<Any>(),
				"tags" to Schema<Any>(),
			)
		}
		val requiredMeta = RequiredConstraint(Required.Mode.STRICT, "", groups)
		val sizeMeta = SizeConstraint(2, 40, "", groups)
		val tagsSize = SizeConstraint(1, 5, "", groups)
		val ir = ObjectSchema(
			type = Any::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "name",
					externalName = "first_name",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { null },
					constraints = listOf(
						CompiledConstraint(requiredMeta, ValidatorBackedRunner(RequiredValidator, requiredMeta), 0),
						CompiledConstraint(sizeMeta, ValidatorBackedRunner(CharSequenceSizeValidator, sizeMeta), 1),
					),
				),
				PropertySpec(
					declaredName = "tags",
					externalName = "tags",
					shape = IterableShape(element = ScalarShape(ScalarKind.STRING)),
					read = ValueReader { null },
					constraints = listOf(
						CompiledConstraint(tagsSize, ValidatorBackedRunner(CollectionSizeValidator, tagsSize), 0),
					),
				),
			),
		)
		
		ObjectSchemaOpenApiEnricher.enrich(openApi, ir, mappers)
		
		assertThat(openApi.properties).containsKeys("first_name", "tags")
		assertThat(openApi.properties).doesNotContainKey("name")
		assertThat(openApi.required).contains("first_name")
		val nameSchema = openApi.properties["first_name"]!!
		assertThat(nameSchema.minLength).isEqualTo(2)
		assertThat(nameSchema.maxLength).isEqualTo(40)
		@Suppress("UNCHECKED_CAST")
		val constraints = nameSchema.extensions[ConstraintExtensionKeys.CONSTRAINTS] as List<Map<String, Any?>>
		assertThat(constraints.map { it[ConstraintExtensionKeys.CONSTRAINT_KIND] }).containsExactly("Required", "Size")
		assertThat(nameSchema.extensions[ConstraintExtensionKeys.ERRORS]).isInstanceOf(List::class.java)
		val tagsSchema = openApi.properties["tags"]!!
		assertThat(tagsSchema.minItems).isEqualTo(1)
		assertThat(tagsSchema.maxItems).isEqualTo(5)
	}
	
	@Test
	@DisplayName("type-use Email on list elements lands on items schema")
	fun nestedElementConstraints() {
		val openApi = Schema<Any>().apply {
			properties = linkedMapOf("emails" to Schema<Any>())
		}
		val emailMeta = EmailConstraint(message = "", groups = groups)
		val ir = ObjectSchema(
			type = Any::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "emails",
					externalName = "emails",
					shape = IterableShape(
						element = ScalarShape(
							kind = ScalarKind.STRING,
							constraints = listOf(
								CompiledConstraint(emailMeta, ValidatorBackedRunner(EmailValidator, emailMeta), 0),
							),
						),
					),
					read = { null },
				),
			),
		)
		
		ObjectSchemaOpenApiEnricher.enrich(openApi, ir, mappers)
		val emails = openApi.properties["emails"]!!
		val items = emails.items
		assertThat(items).isNotNull
		assertThat(items!!.format).isEqualTo("email")
		@Suppress("UNCHECKED_CAST")
		val itemConstraints = items.extensions[ConstraintExtensionKeys.CONSTRAINTS] as List<Map<String, Any?>>
		assertThat(itemConstraints.map { it[ConstraintExtensionKeys.CONSTRAINT_KIND] }).containsExactly("Email")
		assertThat(emails.extensions?.get(ConstraintExtensionKeys.CONSTRAINTS)).isNull()
	}
}

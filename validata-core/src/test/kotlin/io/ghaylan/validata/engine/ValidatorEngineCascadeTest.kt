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
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import io.ghaylan.validata.support.EngineTestSupport.Batch
import io.ghaylan.validata.support.EngineTestSupport.Settings
import io.ghaylan.validata.support.EngineTestSupport.TinyBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Cascade edge cases: empty collections, map paths, headers/path sections,
 * null element indices, and null request bodies.
 * 
 * @author Ghaylan Saada

 */
class ValidatorEngineCascadeTest {

	private val engine = EngineTestSupport.engine()

	data class EmailBody(val email: String?)

	private fun emailWithRequiredAndFormatSchema(): ObjectSchema {
		val groups = setOf(OnDefault::class)
		val requiredMeta = RequiredConstraint(Required.Mode.STRICT, "", groups)
		val required = CompiledConstraint(
			metadata = requiredMeta,
			runner = ValidatorBackedRunner(RequiredValidator, requiredMeta),
			order = 0,
		)
		val emailMeta = EmailConstraint("", groups)
		val email = CompiledConstraint(
			metadata = emailMeta,
			runner = ValidatorBackedRunner(EmailValidator, emailMeta),
			order = 1,
		)
		// Property @Required + type-use @Email on the same path.
		return ObjectSchema(
			type = EmailBody::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "email",
					externalName = "email",
					shape = ScalarShape(
						kind = ScalarKind.STRING,
						constraints = listOf(email),
					),
					read = ValueReader { (it as EmailBody).email },
					constraints = listOf(required),
				),
			),
		)
	}
	@Test
	@DisplayName("empty list does not invent element violations — only container constraints run")
	fun emptyListSkipsElementLoop() {
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = emptyList()),
			params = null,
			headers = null,
			pathVariables = null,
		)
		// @Required on the list itself may still fire depending on Required.Mode.STRICT + empty list semantics;
		// element paths like items[0].name must never appear for an empty collection.
		assertThat(errors.map { it.path.orEmpty() }).noneMatch { it.startsWith("items[") }
	}

	@Test
	@DisplayName("null list value does not cascade into element paths")
	fun nullListSkipsElementLoop() {
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.batchSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Batch(items = null),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path.orEmpty() }).noneMatch { it.startsWith("items[") }
	}

	@Test
	@DisplayName("map value violations use path[key] shape")
	fun mapValuePathShape() {
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.settingsSchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = Settings(entries = mapOf("theme" to (null as String?))),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).contains("entries[theme]")
	}

	@Test
	@DisplayName("headers and path sections are validated when present on the endpoint schema")
	fun headersAndPathSections() {
		val schema = EngineTestSupport.bodyRequestSchema(
		body = EngineTestSupport.tinyBodySchema(),
		id = "cascade-sections",
	).copy(
		headers = EngineTestSupport.flatMapSchema("X-Token"),
		pathVariables = EngineTestSupport.flatMapSchema("id"))
		
		val errors = engine.validateRequest(
			schema = schema,
			body = TinyBody(name = "ok"),
			params = null,
			headers = mapOf("X-Token" to null),
			pathVariables = mapOf("id" to null))
		
		assertThat(errors.map { it.path }).containsExactlyInAnyOrder("X-Token", "id")
	}

	@Test
	@DisplayName("null elements keep original indices for type-use @Required")
	fun nullListElementsValidatedAtIndex() {
		val schema = EngineTestSupport.bodyRequestSchema(
			EngineTestSupport.stringListWithRequiredElementsSchema(),
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = EngineTestSupport.StringListBody(items = listOf("ok", null, "also")),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactly("items[1]")
	}

	@Test
	@DisplayName("null request body still validates body schema properties")
	fun nullBodyRunsPropertyConstraints() {
		val schema = EngineTestSupport.bodyRequestSchema(EngineTestSupport.tinyBodySchema())
		val errors = engine.validateRequest(
			schema = schema,
			body = null,
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactly("name")
	}

	@Test
	@DisplayName("oneErrorPerParam keeps a single error when property and type-use share a path")
	fun oneErrorPerParamAcrossPropertyAndTypeUse() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = emailWithRequiredAndFormatSchema(),
			oneErrorPerParam = true,
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = EmailBody(email = ""),
			params = null,
			headers = null,
			pathVariables = null,
		)
		// Blank fails @Required (DEEP) on the property list; type-use @Email must not add a second path error.
		assertThat(errors).hasSize(1)
		assertThat(errors.single().path).isEqualTo("email")
	}

	@Test
	@DisplayName("oneErrorPerParam=false collects property and type-use errors on the same path")
	fun collectMultipleErrorsOnSamePath() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = emailWithRequiredAndFormatSchema(),
			oneErrorPerParam = false,
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = EmailBody(email = ""),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors).hasSize(2)
		assertThat(errors.map { it.path }.toSet()).containsExactly("email")
		assertThat(errors.map { it.code }).contains(
			ConstraintErrorCode.TEXT_BLANK,
			ConstraintErrorCode.VALUE_FORMAT_INVALID,
		)
	}

	@Test
	@DisplayName("map key constraints emit errors under entries.keys[…]")
	fun mapKeyConstraints() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.settingsSchema(constrainKeys = true),
			id = "map-keys",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = Settings(entries = mapOf("" to "ok")),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path.orEmpty() }).anyMatch { it.contains("keys") }
	}

	@Test
	@DisplayName("valid request with all sections populated returns no errors")
	fun allSectionsValid() {
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			query = EngineTestSupport.flatMapSchema("q"),
			id = "all-valid",
		).copy(
			headers = EngineTestSupport.flatMapSchema("X-Token"),
			pathVariables = EngineTestSupport.flatMapSchema("id"),
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = TinyBody(name = "ok"),
			params = mapOf("q" to "1"),
			headers = mapOf("X-Token" to "t"),
			pathVariables = mapOf("id" to "42"),
		)
		assertThat(errors).isEmpty()
	}
}

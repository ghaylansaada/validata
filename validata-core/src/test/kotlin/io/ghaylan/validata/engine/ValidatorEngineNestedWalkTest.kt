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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.MapShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Nested iterable / map walker path fixtures and empty body schema skip.
 * 
 * @author Ghaylan Saada
 */
class ValidatorEngineNestedWalkTest {
	
	private val engine = EngineTestSupport.engine()
	private val groups: Set<KClass<*>> = setOf(OnDefault::class)
	
	data class NestedLists(val rows: List<List<String?>>?)
	
	data class ListOfMaps(val rows: List<Map<String, String?>>?)
	
	private fun required(): CompiledConstraint {
		val meta = RequiredConstraint(Required.Mode.STRICT, "", groups)
		return CompiledConstraint(meta, ValidatorBackedRunner(RequiredValidator, meta), 0)
	}
	
	@Test
	@DisplayName("list-of-list element violations use nested index paths")
	fun listOfListPaths() {
		val element = ScalarShape(ScalarKind.STRING, constraints = listOf(required()))
		val inner = IterableShape(element = element)
		val schema = ObjectSchema(
			type = NestedLists::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "rows",
					externalName = "rows",
					shape = IterableShape(element = inner),
					read = ValueReader { (it as NestedLists).rows },
				),
			),
		)
		val errors = engine.validateRequest(
			schema = EngineTestSupport.bodyRequestSchema(schema, id = "nested-lists"),
			body = NestedLists(rows = listOf(listOf("ok", null))),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).contains("rows[0][1]")
	}
	
	@Test
	@DisplayName("list-of-map value violations use indexed map paths")
	fun listOfMapPaths() {
		val valueShape = ScalarShape(ScalarKind.STRING, constraints = listOf(required()))
		val mapShape = MapShape(key = ScalarShape(ScalarKind.STRING), value = valueShape)
		val schema = ObjectSchema(
			type = ListOfMaps::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "rows",
					externalName = "rows",
					shape = IterableShape(element = mapShape),
					read = ValueReader { (it as ListOfMaps).rows },
				),
			),
		)
		val errors = engine.validateRequest(
			schema = EngineTestSupport.bodyRequestSchema(schema, id = "list-of-maps"),
			body = ListOfMaps(rows = listOf(mapOf("name" to null))),
			params = null,
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).contains("rows[0][name]")
	}
	
	@Test
	@DisplayName("empty body schema skips body walk while query is still validated")
	fun emptyBodySchemaStillValidatesQuery() {
		val emptyBody = ObjectSchema(type = Any::class.java, properties = emptyList())
		val schema = EngineTestSupport.bodyRequestSchema(
			body = emptyBody,
			query = EngineTestSupport.flatMapSchema("q"),
			id = "empty-body",
		)
		val errors = engine.validateRequest(
			schema = schema,
			body = Any(),
			params = mapOf("q" to null),
			headers = null,
			pathVariables = null,
		)
		assertThat(errors.map { it.path }).containsExactly("q")
	}
}

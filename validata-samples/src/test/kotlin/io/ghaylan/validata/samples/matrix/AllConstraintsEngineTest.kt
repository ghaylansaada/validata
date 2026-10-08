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
package io.ghaylan.validata.samples.matrix

import io.ghaylan.validata.bootstrap.ValidationRegistryInitializer
import io.ghaylan.validata.engine.ValidationOptions
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * Smoke: sample kitchen-sink schemas resolve through [ValidationRegistryInitializer] into
 * [ValidatorEngine.validate] without Spring MVC.
 *
 * Full `(path, code)` coverage for every built-in constraint lives in [AllConstraintsIT]
 * (HTTP acceptance) and in `validata-core` validator / golden suites (engine semantics).
 * This class only proves the non-web wiring path still works for the sample DTOs.*
 * 
 * @author Ghaylan Saada
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AllConstraintsEngineTest {
	
	private lateinit var applicationContext: AnnotationConfigApplicationContext
	private lateinit var engine: ValidatorEngine
	
	/** Starts a non-web context and a [ValidatorEngine] over the generated registry.	 */
	@BeforeAll
	fun setUp() {
		applicationContext = AnnotationConfigApplicationContext().apply { refresh() }
		val registry = ValidationRegistry().also { reg ->
			ValidationRegistryInitializer(reg).apply {
				setApplicationContext(applicationContext)
				afterPropertiesSet()
			}
		}
		engine = ValidatorEngine(registry)
	}
	
	/** Closes the non-web context started in [setUp].	 */
	@AfterAll
	fun tearDown() {
		applicationContext.close()
	}
	
	@Test
	@DisplayName("accepts a fully valid kitchen-sink payload under OnDefault")
	fun acceptsFullyValidPayload() {
		assertThat(validate(AllConstraintsFixtures.valid())).isEmpty()
	}
	
	@Test
	@DisplayName("rejects a representative violation via validate(params)")
	fun rejectsRepresentativeViolation() {
		val errors = validate(AllConstraintsFixtures.valid(email = "not-an-email"))
		assertThat(errors.any { it.path == "email" && codeName(it) == "VALUE_FORMAT_INVALID" }).withFailMessage("Expected (email, VALUE_FORMAT_INVALID) in ${
			errors.map {
				"${it.path}:${
					codeName(it)
				}"
			}
		}")
			.isTrue()
	}
	
	@Test
	@DisplayName("OnCreate-only createOnlyNote is skipped under OnDefault and fires under OnCreate")
	fun createOnlyNoteGroupFiltering() {
		val missing = AllConstraintsFixtures.valid(createOnlyNote = null)
		
		assertThat(validate(missing, groups = arrayOf(OnDefault::class))).isEmpty()
		val underCreate = validate(missing, groups = arrayOf(OnCreate::class))
		assertThat(underCreate.any { it.path == "createOnlyNote" && codeName(it) == "VALUE_MISSING" }).isTrue()
	}
	
	/**
	 * @param target request instance to validate
	 * @param groups active constraint groups
	 * @return constraint errors from [ValidatorEngine.validate]	 
	 */
	private fun validate(
		target: AllConstraintsRequest,
		groups: Array<kotlin.reflect.KClass<*>> = arrayOf(OnDefault::class),
	): List<ConstraintError<*>> = engine.validate(
		params = target,
		options = ValidationOptions(
			oneErrorPerParam = false,
			groups = groups,
		),
	)
	
	/** Enum constant name of [ConstraintError.code], or `null` when the code is not an enum.	 */
	private fun codeName(error: ConstraintError<*>): String? = (error.code as? Enum<*>)?.name
}

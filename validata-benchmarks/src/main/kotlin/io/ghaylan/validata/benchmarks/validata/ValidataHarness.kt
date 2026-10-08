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
package io.ghaylan.validata.benchmarks.validata

import io.ghaylan.validata.bootstrap.ValidationRegistryInitializer
import io.ghaylan.validata.constraint.spi.ConstraintCatalog
import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.engine.ValidationOptions
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.reflect.KClass

/**
 * Production-shaped [ValidatorEngine] wiring for JMH and fixture parity tests.
 *
 * Mirrors how a Spring Boot app obtains the engine: an application context, a
 * [ValidationRegistry] populated by [ValidationRegistryInitializer] from the KSP
 * [ConstraintCatalog], and schemas discovered through [GeneratedSchemas] when
 * [ValidatorEngine.validate] resolves `@Validatable` types on this module's classpath.
 *
 * ## Lifecycle
 *
 * Construct once per JMH `@State` (or once per test class). Closing [close] releases the Spring
 * context. Do not rebuild the registry inside a benchmark loop.
 *
 * ## Limits
 *
 * [ValidationLimits.maxErrors] is raised so All-Invalid cells (up to 1000 leaves) collect the full
 * failure set — matching Hibernate Validator's default `validate` behavior. Production apps keep
 * the engine default (200); this is benchmark/parity only.
 *
 * ## Thread safety
 *
 * [ValidatorEngine] is stateless per call; a single harness instance may be shared across JMH
 * threads for read-only validation of immutable payloads.
 *
 * ```kotlin
 * ValidataHarness().use { harness ->
 *     val errors = harness.validate(SizedPayloads.customSmallValid())
 *     check(errors.isEmpty())
 * }
 * ```*
 * 
 * @author Ghaylan Saada
 */
class ValidataHarness: AutoCloseable {
	
	private val applicationContext = AnnotationConfigApplicationContext().apply { refresh() }
	
	/**
	 * Registry backed by generated catalogs/schemas on the benchmarks classpath.	 
	 */
	val registry: ValidationRegistry = ValidationRegistry().also { registry ->
		ValidationRegistryInitializer(registry).apply {
			setApplicationContext(applicationContext)
			afterPropertiesSet()
		}
	}
	
	/**
	 * Engine under measurement — the Validata API for object validation.
	 *
	 * `maxErrors` covers the largest All-Invalid cell (1000 leaves) without early abort.
	 */
	val engine: ValidatorEngine = ValidatorEngine(
		validationRegistry = registry,
		limits = ValidationLimits(maxErrors = 10_000),
	)
	
	/**
	 * Validates [target] with default groups and **without** per-field fail-fast so the invalid
	 * JMH cell collects the full failure set (comparable to HV's default `validate` behavior).
	 *
	 * @param target object whose runtime class has a generated [ObjectSchema]
	 * @param groups active validation groups (defaults to [OnDefault])
	 * @return constraint errors; empty when [target] is valid	 
	 */
	fun validate(
		target: Any,
		groups: Array<KClass<*>> = arrayOf(OnDefault::class),
	): List<ConstraintError<*>> = engine.validate(
		params = target,
		options = ValidationOptions(
			oneErrorPerParam = false,
			groups = groups,
		),
	)
	
	override fun close() {
		applicationContext.close()
	}
}

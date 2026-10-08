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
package io.ghaylan.validata.concurrency

import io.ghaylan.validata.constraint.annotation.DistinctConstraint
import io.ghaylan.validata.constraint.spi.GeneratedConstraintCatalogs
import io.ghaylan.validata.constraint.validator.distinct.DistinctValidator
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.RegistryFixtureDto
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Concurrent smoke coverage for shared [ValidatorEngine] + registry and
 * [GeneratedConstraintCatalogs] double-checked load (audit Task 1.5).
 * 
 * @author Ghaylan Saada

 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentValidationStressTest {

	data class TagList(val tags: List<String>?)

	companion object {
		private const val THREADS = 32
		private const val ITERATIONS = 200
	}

	@BeforeAll
	fun warmSchemas() {
		GeneratedSchemas.resetForTests()
		GeneratedConstraintCatalogs.resetForTests()
	}

	@AfterAll
	fun clearSchemas() {
		GeneratedSchemas.resetForTests()
		GeneratedConstraintCatalogs.resetForTests()
	}

	@Test
	@DisplayName("shared engine + Required schema: N threads × iterations with stable results")
	fun concurrentStandaloneValidation() {
		val registry = ValidationRegistry().also { it.freeze() }
		val engine = ValidatorEngine(registry)

		assertThat(engine.validate(RegistryFixtureDto(value = "ok"))).isEmpty()
		assertThat(engine.validate(RegistryFixtureDto(value = null))).isNotEmpty()

		val failures = AtomicInteger(0)
		val firstError = AtomicReference<Throwable?>(null)
		val start = CountDownLatch(1)
		val done = CountDownLatch(THREADS)
		val pool = Executors.newFixedThreadPool(THREADS)

		repeat(THREADS) { threadIdx ->
			pool.execute {
				try {
					start.await()
					repeat(ITERATIONS) { i ->
						val valid = engine.validate(RegistryFixtureDto(value = "t$threadIdx-$i"))
						val invalid = engine.validate(RegistryFixtureDto(value = null))
						if (valid.isNotEmpty()) {
							failures.incrementAndGet()
						}
						if (invalid.isEmpty() || invalid.none { it.path == "value" }) {
							failures.incrementAndGet()
						}
					}
				} catch (t: Throwable) {
					firstError.compareAndSet(null, t)
					failures.incrementAndGet()
				} finally {
					done.countDown()
				}
			}
		}

		start.countDown()
		assertThat(done.await(60, TimeUnit.SECONDS)).isTrue()
		pool.shutdown()
		assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()

		firstError.get()?.let { throw AssertionError("worker failed", it) }
		assertThat(failures.get()).isZero()
	}

	@Test
	@DisplayName("shared engine + Distinct list: concurrent validateRequest is exception-free")
	fun concurrentDistinctValidation() {
		val engine = EngineTestSupport.engine()
		val endpoint = EngineTestSupport.bodyRequestSchema(
			body = distinctListSchema(),
			id = "stress-distinct",
		)

		val unique = TagList(tags = listOf("a", "b", "c"))
		val dupes = TagList(tags = listOf("a", "a"))

		assertThat(engine.validateRequest(endpoint, unique, null, null, null)).isEmpty()
		assertThat(engine.validateRequest(endpoint, dupes, null, null, null)).isNotEmpty()

		val failures = AtomicInteger(0)
		val firstError = AtomicReference<Throwable?>(null)
		val start = CountDownLatch(1)
		val done = CountDownLatch(THREADS)
		val pool = Executors.newFixedThreadPool(THREADS)

		repeat(THREADS) {
			pool.execute {
				try {
					start.await()
					repeat(ITERATIONS) {
						val ok = engine.validateRequest(endpoint, unique, null, null, null)
						val bad = engine.validateRequest(endpoint, dupes, null, null, null)
						if (ok.isNotEmpty() || bad.isEmpty()) {
							failures.incrementAndGet()
						}
					}
				} catch (t: Throwable) {
					firstError.compareAndSet(null, t)
					failures.incrementAndGet()
				} finally {
					done.countDown()
				}
			}
		}

		start.countDown()
		assertThat(done.await(60, TimeUnit.SECONDS)).isTrue()
		pool.shutdown()
		assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()

		firstError.get()?.let { throw AssertionError("worker failed", it) }
		assertThat(failures.get()).isZero()
	}

	@Test
	@DisplayName("concurrent GeneratedConstraintCatalogs.all() returns a stable non-empty snapshot")
	fun concurrentCatalogAll() {
		GeneratedConstraintCatalogs.resetForTests()

		val failures = AtomicInteger(0)
		val firstError = AtomicReference<Throwable?>(null)
		val sizes = ConcurrentHashMap.newKeySet<Int>()
		val start = CountDownLatch(1)
		val done = CountDownLatch(THREADS)
		val pool = Executors.newFixedThreadPool(THREADS)

		repeat(THREADS) {
			pool.execute {
				try {
					start.await()
					repeat(50) {
						val snapshot = GeneratedConstraintCatalogs.all()
						if (snapshot.isEmpty()) {
							failures.incrementAndGet()
						}
						sizes.add(snapshot.size)
					}
				} catch (t: Throwable) {
					firstError.compareAndSet(null, t)
					failures.incrementAndGet()
				} finally {
					done.countDown()
				}
			}
		}

		start.countDown()
		assertThat(done.await(60, TimeUnit.SECONDS)).isTrue()
		pool.shutdown()
		assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()

		firstError.get()?.let { throw AssertionError("worker failed", it) }
		assertThat(failures.get()).isZero()
		assertThat(sizes).hasSize(1)
		assertThat(sizes.single()).isGreaterThan(0)
	}

	@Test
	@DisplayName("frozen registry rejects register* while concurrent validate continues")
	fun freezeUnderConcurrentValidate() {
		val registry = ValidationRegistry().also { it.freeze() }
		val engine = ValidatorEngine(registry)
		val schema = EngineTestSupport.bodyRequestSchema(
			body = EngineTestSupport.tinyBodySchema(),
			id = "freeze-concurrent",
		)

		val failures = AtomicInteger(0)
		val firstError = AtomicReference<Throwable?>(null)
		val start = CountDownLatch(1)
		val done = CountDownLatch(THREADS)
		val pool = Executors.newFixedThreadPool(THREADS)

		repeat(THREADS) { idx ->
			pool.execute {
				try {
					start.await()
					repeat(ITERATIONS) {
						if (idx % 4 == 0) {
							try {
								registry.registerStaticSchemas(mapOf("x" to schema))
								failures.incrementAndGet()
							} catch (_: IllegalStateException) {
								// expected
							}
						} else {
							val errors = engine.validateRequest(
								schema,
								EngineTestSupport.TinyBody(name = null),
								null,
								null,
								null,
							)
							if (errors.none { it.path == "name" }) {
								failures.incrementAndGet()
							}
						}
					}
				} catch (t: Throwable) {
					firstError.compareAndSet(null, t)
					failures.incrementAndGet()
				} finally {
					done.countDown()
				}
			}
		}

		start.countDown()
		assertThat(done.await(60, TimeUnit.SECONDS)).isTrue()
		pool.shutdown()
		assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()
		firstError.get()?.let { throw AssertionError("worker failed", it) }
		assertThat(failures.get()).isZero()
	}

	private fun distinctListSchema(): ObjectSchema {
		val groups = setOf(OnDefault::class)
		val distinct = DistinctConstraint(by = emptySet(), message = "", groups = groups)
		val compiled = CompiledConstraint(
			metadata = distinct,
			runner = ValidatorBackedRunner(DistinctValidator, distinct),
			order = 0,
		)
		return ObjectSchema(
			type = TagList::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "tags",
					externalName = "tags",
					shape = IterableShape(
						element = ScalarShape(
							kind = ScalarKind.STRING,
							constraints = listOf(compiled),
						),
					),
					read = ValueReader { (it as TagList).tags },
				),
			),
		)
	}
}

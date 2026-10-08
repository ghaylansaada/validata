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
package io.ghaylan.validata.web

import io.ghaylan.validata.bootstrap.fixture.MethodLevelValidateController
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.getUniqueIdentifier
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.request.EndpointArgumentSlot
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.web.fixture.plancache.MissingSchemaBodyController
import io.ghaylan.validata.web.fixture.plancache.PlainEchoController
import io.ghaylan.validata.web.fixture.plancache.ZeroParamValidateController
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Ownership: [ValidatedEndpointPlanCache] — Skip vs Active, fail-loud missing schemas, concurrent first hit.
 *
 * Threat: a racey or non-atomic cache that drops an Active plan or returns Skip for an annotated
 * handler would skip validation under load.
 * 
 * @author Ghaylan Saada

 */
class ValidatedEndpointPlanCacheTest {

	@Nested
	@DisplayName("Skip vs Active")
	inner class SkipAndActive {

		@Test
		@DisplayName("zero-parameter @Validate handler resolves to Skip")
		fun zeroParameterAnnotatedHandlerIsSkip() {
			val controller = ZeroParamValidateController()
			val method = ZeroParamValidateController::class.java.getDeclaredMethod("ping")
			val handlerMethod = HandlerMethod(controller, method)
			val cache = ValidatedEndpointPlanCache(ValidationRegistry())

			val plan = cache.planFor(handlerMethod, method)

			assertThat(plan).isEqualTo(HandlerValidationPlan.Skip)
			assertThat(cache.cachedMethodCount()).isEqualTo(1)
		}

		@Test
		@DisplayName("unannotated handler resolves to Skip without requiring a registry schema")
		fun unannotatedHandlerIsSkip() {
			val controller = PlainEchoController()
			val method = PlainEchoController::class.java.getDeclaredMethod("echo", String::class.java)
			val handlerMethod = HandlerMethod(controller, method)
			val cache = ValidatedEndpointPlanCache(ValidationRegistry())

			assertThat(cache.planFor(handlerMethod, method)).isEqualTo(HandlerValidationPlan.Skip)
		}

		@Test
		@DisplayName("repeated planFor for the same method hits the cache")
		fun repeatedLookupUsesCache() {
			val controller = PlainEchoController()
			val method = PlainEchoController::class.java.getDeclaredMethod("echo", String::class.java)
			val handlerMethod = HandlerMethod(controller, method)
			val cache = ValidatedEndpointPlanCache(ValidationRegistry())

			repeat(3) { cache.planFor(handlerMethod, method) }

			assertThat(cache.cachedMethodCount()).isEqualTo(1)
		}

		@Test
		@DisplayName("annotated handler with parameters but no EndpointSchema fails with a clear error")
		fun annotatedHandlerMissingSchemaFailsLoudly() {
			val controller = MissingSchemaBodyController()
			val method = MissingSchemaBodyController::class.java.getDeclaredMethod("create", String::class.java)
			val handlerMethod = HandlerMethod(controller, method)
			val cache = ValidatedEndpointPlanCache(ValidationRegistry())

			assertThatThrownBy { cache.planFor(handlerMethod, method) }
				.isInstanceOf(IllegalStateException::class.java)
				.hasMessageContaining("No generated EndpointSchema")
				.hasMessageContaining(method.declaringClass.name)
				.hasMessageContaining("validata-processor")
		}
	}

	@Nested
	@DisplayName("Concurrent first-hit")
	inner class ConcurrentFirstHit {

		@Test
		@DisplayName("parallel planFor for an annotated Active handler yields one Active entry")
		fun concurrentActiveFirstHitIsStable() {
			val controller = MethodLevelValidateController()
			val method = MethodLevelValidateController::class.java.getDeclaredMethod("create", String::class.java)
			val handlerMethod = HandlerMethod(controller, method)
			val endpointId = method.getUniqueIdentifier()
			val registry = ValidationRegistry()
			registry.registerStaticSchemas(
				mapOf(
					endpointId to EndpointSchema(
						id = endpointId,
						oneErrorPerParam = true,
						groups = setOf(OnDefault::class),
						argumentLayout = listOf(EndpointArgumentSlot(EndpointArgumentKind.BODY)),
					),
				),
			)
			val cache = ValidatedEndpointPlanCache(registry)

			val workers = 32
			val barrier = CyclicBarrier(workers)
			val plans = ConcurrentLinkedQueue<HandlerValidationPlan>()
			val failure = AtomicReference<Throwable>()
			val pool = Executors.newFixedThreadPool(workers)
			try {
				repeat(workers) {
					pool.execute {
						try {
							barrier.await(5, TimeUnit.SECONDS)
							plans.add(cache.planFor(handlerMethod, method))
						} catch (t: Throwable) {
							failure.compareAndSet(null, t)
						}
					}
				}
			} finally {
				pool.shutdown()
				assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()
			}

			assertThat(failure.get()).isNull()
			assertThat(plans).hasSize(workers)
			assertThat(plans).allSatisfy { plan ->
				assertThat(plan).isInstanceOf(HandlerValidationPlan.Active::class.java)
				assertThat((plan as HandlerValidationPlan.Active).endpointId).isEqualTo(endpointId)
			}
			assertThat(cache.cachedMethodCount()).isEqualTo(1)
		}
	}

	@Nested
	@DisplayName("schemaWhenBodyAbsent")
	inner class SchemaWhenBodyAbsent {

		@Test
		@DisplayName("Active plans with a body schema precompute a null-body variant once")
		fun precomputesNullBodySchema() {
			val controller = MethodLevelValidateController()
			val method = MethodLevelValidateController::class.java.getDeclaredMethod("create", String::class.java)
			val handlerMethod = HandlerMethod(controller, method)
			val endpointId = method.getUniqueIdentifier()
			val bodySchema = ObjectSchema(type = Any::class.java, properties = emptyList())
			val schema = EndpointSchema(
				id = endpointId,
				oneErrorPerParam = true,
				groups = setOf(OnDefault::class),
				requestBody = bodySchema,
				argumentLayout = listOf(EndpointArgumentSlot(EndpointArgumentKind.BODY)),
			)
			val registry = ValidationRegistry()
			registry.registerStaticSchemas(mapOf(endpointId to schema))
			val cache = ValidatedEndpointPlanCache(registry)

			val plan = cache.planFor(handlerMethod, method) as HandlerValidationPlan.Active

			assertThat(plan.schema.requestBody).isSameAs(bodySchema)
			assertThat(plan.schemaWhenBodyAbsent.requestBody).isNull()
			assertThat(plan.schemaWhenBodyAbsent).isNotSameAs(plan.schema)
			// Second resolve must reuse the same precomputed instances (no per-request copy).
			val again = cache.planFor(handlerMethod, method) as HandlerValidationPlan.Active
			assertThat(again.schemaWhenBodyAbsent).isSameAs(plan.schemaWhenBodyAbsent)
		}
	}
}

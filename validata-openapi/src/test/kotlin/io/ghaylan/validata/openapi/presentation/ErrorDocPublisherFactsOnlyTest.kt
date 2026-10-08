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
package io.ghaylan.validata.openapi.presentation

import io.swagger.v3.oas.models.Operation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards host-agnostic [ErrorDocPublishContext] usage when springdoc handles are absent.
 * 
 * @author Ghaylan Saada
 */
class ErrorDocPublisherFactsOnlyTest {
	
	@Test
	@DisplayName("facts-only publisher no-ops safely when operation is null")
	fun factsOnlyNoOpsOnNullOperation() {
		val publisher = ErrorDocPublisher { ctx ->
			val operation = ctx.operation
				?: return@ErrorDocPublisher
			operation.addExtension("x-validata-test", true)
		}
		val context = ErrorDocPublishContext(
			surface = DeclaredErrorSurface(
				detailCodes = listOf("EMAIL_INVALID"),
				detailErrorDocs = emptyList(),
			),
			endpointId = "facts-only",
			operation = null,
			handlerMethod = null,
		)
		
		assertThatCode { publisher.publish(context) }.doesNotThrowAnyException()
	}
	
	@Test
	@DisplayName("publisher mutates operation when host handle is present")
	fun mutatesWhenOperationPresent() {
		val operation = Operation()
		val publisher = ErrorDocPublisher { ctx ->
			val op = ctx.operation
				?: return@ErrorDocPublisher
			op.addExtension("x-validata-test", ctx.surface.detailCodes)
		}
		publisher.publish(
			ErrorDocPublishContext(
				surface = DeclaredErrorSurface(
					detailCodes = listOf("EMAIL_INVALID"),
					detailErrorDocs = emptyList(),
				),
				endpointId = "with-op",
				operation = operation,
			),
		)
		assertThat(operation.extensions["x-validata-test"]).isEqualTo(listOf("EMAIL_INVALID"))
	}
}

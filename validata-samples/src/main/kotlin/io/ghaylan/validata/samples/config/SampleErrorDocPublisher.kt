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
package io.ghaylan.validata.samples.config

import io.ghaylan.validata.openapi.presentation.DeclaredErrorSurface
import io.ghaylan.validata.openapi.presentation.ErrorDocPublishContext
import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import io.ghaylan.validata.samples.error.SampleErrorBody
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses

/**
 * Sample-owned [ErrorDocPublisher] that documents [SampleErrorBody] on operations with declared
 * validation detail codes. Not part of validata-openapi — copy or replace in real apps.
 * 
 * @author Ghaylan Saada
 */
class SampleErrorDocPublisher: ErrorDocPublisher {
	
	override fun publish(context: ErrorDocPublishContext) {
		val surface = context.surface
		if (surface.detailCodes.isEmpty() && surface.detailErrorDocs.isEmpty()) {
			return
		}
		val operation = context.operation
			?: return
		val responses = operation.responses
			?: ApiResponses().also { operation.responses = it }
		responses.addApiResponse(
			"400",
			ApiResponse().description("Request validation failed")
				.content(
					Content().addMediaType(
						"application/json",
						MediaType().schema(validationEnvelope(surface)),
					),
				),
		)
	}
	
	private fun validationEnvelope(surface: DeclaredErrorSurface): Schema<Any> {
		val errorItem = Schema<Any>().apply {
			type = "object"
			properties = linkedMapOf(
				"path" to Schema<Any>().apply { type = "string" },
				"location" to Schema<Any>().apply {
					type = "string"
					enum = listOf("BODY", "QUERY", "HEADER", "PATH", "OTHER")
				},
				"code" to Schema<Any>().apply {
					type = "string"
					if (surface.detailCodes.isNotEmpty()) enum = surface.detailCodes
				},
				"message" to Schema<Any>().apply { type = "string" },
				"context" to Schema<Any>(),
			)
		}
		return Schema<Any>().apply {
			type = "object"
			properties = linkedMapOf(
				"status" to Schema<Any>().apply { type = "integer"; example = 400 },
				"code" to Schema<Any>().apply { type = "string"; nullable = true },
				"message" to Schema<Any>().apply { type = "string" },
				"errors" to Schema<Any>().apply {
					type = "array"
					items = errorItem
				},
			)
		}
	}
}

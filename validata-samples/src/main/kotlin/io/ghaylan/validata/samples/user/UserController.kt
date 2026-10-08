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
package io.ghaylan.validata.samples.user

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.samples.error.SampleApiErrors
import io.ghaylan.validata.samples.error.SampleBusinessException
import io.ghaylan.validata.schema.Validate
import org.springframework.web.bind.annotation.*

/**
 * Quick-start `@Validate` API: JSON body on create, and path / query / header on lookup.
 *
 * Business 404 is app-owned ([SampleBusinessException]); OpenAPI documents field-level codes
 * with `@ApiError` on request DTOs.
 * 
 * @author Ghaylan Saada
 */
@RestController
@RequestMapping("/api/users")
class UserController {
	
	@Validate
	@PostMapping
	fun create(
		@RequestBody body: CreateUserRequest
	): String = "created ${body.name}"
	
	@Validate
	@GetMapping("/{userId}")
	fun lookup(
		
		@Required
		@Size(min = 3, max = 32)
		@PathVariable("userId") userId: String?,
		
		@Required
		@Size(min = 2, max = 64)
		@RequestParam("q") q: String?,
		
		@Required
		@Size(min = 2, max = 64)
		@RequestHeader("X-Tenant") tenant: String?,
	): String {
		if (userId == "missing") {
			val declared = SampleApiErrors.USER_NOT_FOUND
			throw SampleBusinessException(declared.httpStatus, declared.code, declared.message)
		}
		return "user=$userId q=$q tenant=$tenant"
	}
}

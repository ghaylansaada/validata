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
package io.ghaylan.validata.web.fixture.controller

import io.ghaylan.validata.constraint.annotation.Min
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.web.fixture.dto.UserDto
import org.springframework.web.bind.annotation.*

/** Method-level `@Validate` — body, query, path, header, and multi-value params.
 * 
 * @author Ghaylan Saada
 */
@RestController
@RequestMapping("/api/method-level")
class MethodLevelController {
	
	@PostMapping("/users")
	@Validate
	fun createUser(
		@RequestBody
		user: UserDto
	): String = "created ${user.name}"
	
	@GetMapping("/search")
	@Validate
	fun search(
		@RequestParam("q")
		@Required
		@Size(min = 3, max = 20)
		q: String,
		@RequestParam("page", required = false)
		@Min("1")
		page: Int?,
	): String = "searched $q"
	
	@GetMapping("/orders/{orderId}")
	@Validate
	fun getOrder(
		@PathVariable("orderId")
		@Required
		@Size(min = 6, max = 12)
		orderId: String,
		@RequestHeader("X-Tenant")
		@Required
		@Size(min = 2, max = 10)
		tenant: String,
	): String = "order $orderId"
	
	@GetMapping("/tags")
	@Validate
	fun tags(
		@RequestParam("tags")
		@Required
		@Size(min = 2, max = 4)
		tags: List<@Size(min = 3, max = 8) String>?,
	): String = "tags ${tags?.size}"
	
	@GetMapping("/limit")
	@Validate
	fun limit(
		@RequestParam("limit")
		@Required
		limit: Int,
	): String = "limit=$limit"
	
	@PostMapping("/unvalidated")
	fun unvalidated(
		@RequestBody
		user: UserDto
	): String = "accepted"
}

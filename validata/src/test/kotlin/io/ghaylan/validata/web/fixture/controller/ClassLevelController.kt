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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.web.fixture.dto.UserDto
import org.springframework.web.bind.annotation.*

/** Class-level `@Validate` — discovery must match method-level schema registration.
 * 
 * @author Ghaylan Saada
 */
@RestController
@RequestMapping("/api/class-level")
@Validate
class ClassLevelController {
	
	@PostMapping("/users")
	fun createUser(
		@RequestBody
		user: UserDto
	): String = "created ${user.name}"
	
	@GetMapping("/search")
	fun search(
		@RequestParam("q")
		@Required
		q: String?
	): String = "searched $q"
	
	@PostMapping("/bulk-check")
	@Validate(failFast = true, oneErrorPerParam = false)
	fun bulkCheck(
		@RequestBody
		user: UserDto
	): String = "checked ${user.name}"
}

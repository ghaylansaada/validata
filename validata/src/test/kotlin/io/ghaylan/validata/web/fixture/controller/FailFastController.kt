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

import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.web.fixture.dto.UserDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fail-fast")
class FailFastController {
	
	@PostMapping("/users")
	@Validate(failFast = true, oneErrorPerParam = false)
	fun createUser(
		@RequestBody
		user: UserDto
	): String = "created ${user.name}"
	
	@PostMapping("/users-collect")
	@Validate(failFast = false, oneErrorPerParam = false)
	fun createUserCollect(
		@RequestBody
		user: UserDto
	): String = "created ${user.name}"
}

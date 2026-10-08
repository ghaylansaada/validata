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

import io.ghaylan.validata.samples.constraint.OddYears
import io.ghaylan.validata.schema.Validate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Kitchen-sink endpoint: [AllConstraintsRequest] plus [OddYears] through the generated request
 * schema SPI. Not a product API — constraint-coverage only.
 * 
 * @author Ghaylan Saada
 */
@RestController
@RequestMapping("/api/all-constraints")
class AllConstraintsController {
	
	/**
	 * @param body payload that must satisfy every default-group constraint on
	 *   [AllConstraintsRequest]
	 * @return confirmation including [AllConstraintsRequest.label]
	 */
	@Validate
	@PostMapping
	fun validate(
		@RequestBody body: AllConstraintsRequest
	): String = "ok ${body.label}"
}

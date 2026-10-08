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
package io.ghaylan.validata.samples.error

import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.openapi.presentation.ApiError

/**
 * Sample business error catalog used by docs ([ApiError]) and runtime throws.
 *
 * Implements [ConstraintErrorDefinition] so `@ApiError(catalog = …)` can resolve
 * [message] / [code] when the annotation leaves `message` blank.
 *
 * OpenAPI bakes field-level codes into schema IR via KSP; runtime still throws
 * [SampleBusinessException] for app-owned failures.
 *
 * @property httpStatus HTTP status associated with the code
 * @property message Default documentation / runtime message
 * 
 * @author Ghaylan Saada
 */
enum class SampleApiErrors(
	val httpStatus: Int,
	override val message: String,
): ConstraintErrorDefinition {
	
	/**
	 * Lookup path did not match a stored user.
	 */
	USER_NOT_FOUND(404, "No user exists with the given id"),
	
	/**
	 *  Display name failed a documented business rule (field-level [ApiError]).
	 */
	NAME_INVALID(400, "Display name failed a business rule"),
	
	/**
	 * `@RequiredWhen` phone when `contactType` is PHONE.
	 */
	PHONE_BACKUP_REQUIRED(400, "Phone is required when contact type is PHONE");
	
	/**
	 * Machine code — the enum constant name, matching [ApiError.code].
	 */
	override val code: String get() = name
}

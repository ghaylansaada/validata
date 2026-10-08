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
package io.ghaylan.validata.model

/**
 * Contract for machine-readable constraint error codes and their default messages.
 *
 * Built-in codes: [ConstraintErrorCode]. Apps may ship their own enums that implement this.
 * 
 * @author Ghaylan Saada
 */
interface ConstraintErrorDefinition {
	
	/**
	 * Stable identifier for clients.
	 *
	 * Must stay fixed when [message] changes. Built-ins use the enum constant name.
	 */
	val code: String
	
	/**
	 * Default user-facing text when the constraint leaves `message` blank.
	 */
	val message: String
}

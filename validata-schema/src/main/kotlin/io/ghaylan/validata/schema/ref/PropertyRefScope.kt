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
package io.ghaylan.validata.schema.ref

/**
 * Where a `@PropertyRef`-marked path resolves on the validated subject.
 *
 * Lives in validata-schema so `validata-processor` and IDE tooling can parse KSP / PSI enum
 * arguments without depending on validata-core. Entry names are the discovery contract — keep stable.
 *
 * @see PropertyRefCompatibilityKind
 * 
 * @author Ghaylan Saada
 */
enum class PropertyRefScope {
	
	/**
	 * A property of the same object (e.g. `@Compare(ref = "password")`).
	 */
	SIBLING,
	
	/**
	 * A property of each collection element (e.g. `List<@Distinct(by = ["email"]) UserDto>`).
	 */
	ELEMENT,
}

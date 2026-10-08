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
 * Scalar type-compatibility rule applied by a cross-field `@PropertyRef`.
 *
 * Declared on metadata via `PropertyRef.compatibility` (in **validata-core**) so KSP and the
 * IntelliJ plugin discover the rule from the library. Interpreted by [PropertyRefScalarCompatibility].
 *
 * Keeping the enum here (not in validata-core) lets the processor depend on schema alone for path
 * and scalar checks without pulling Spring host types.
 * 
 * @author Ghaylan Saada
 */
enum class PropertyRefCompatibilityKind {
	
	/**
	 * No scalar-kind check — existence / readability of the path only.
	 *
	 * Use for `@RequiredWhen(ref = …)` and `@Distinct(by = …)`.
	 */
	NONE,
	
	/**
	 * Both sides must share a compatible concrete scalar kind (string↔string, numeric family, …).
	 *
	 * Use for equality-style comparisons (`@Compare`).
	 */
	SAME_SCALAR_KIND,
	
	/**
	 * Both sides must be in the same comparable family (numeric ↔ numeric, or temporal ↔ temporal).
	 *
	 * Use for ordering comparisons (`@Compare`). Rejects e.g. string subjects even
	 * when the validator’s type parameter is the open `Comparable<*>`.
	 */
	COMPARABLE_FAMILY,
}

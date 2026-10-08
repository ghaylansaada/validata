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
package io.ghaylan.validata.schema.shape

/**
 * Coarse classification of a leaf value for schema-level dispatch.
 *
 * Shared vocabulary for property-ref scalar compatibility and KSP (`"INTEGRAL"`, `"TEMPORAL"`, …)
 * without depending on **validata-core** or Spring. Carried on [ScalarShape.kind]; traversal uses
 * shape, not concrete leaf types such as `LocalDate` vs `Instant`.
 * 
 * @author Ghaylan Saada
 */
enum class ScalarKind {
	
	/**
	 * `Boolean` / `boolean`.
	 */
	BOOLEAN,
	
	/**
	 * `Char` / `char`.
	 */
	CHAR,
	
	/**
	 * `String` and CharSequence-like leaves treated as text.
	 */
	STRING,
	
	/**
	 * Whole numbers: `Int`, `Long`, `Short`, `Byte`, `BigInteger` and boxed forms.
	 */
	INTEGRAL,
	
	/**
	 * Fractional numbers: `Float`, `Double`, `BigDecimal`, and similar decimal input.
	 */
	DECIMAL,
	
	/**
	 * Any `java.time` temporal, `Date`, or `Calendar`.
	 */
	TEMPORAL,
	
	/**
	 * Enum constants.
	 */
	ENUM,
	
	/**
	 * `java.util.UUID`.
	 */
	UUID,
	
	/**
	 * Leaf-like value that does not fit the above; treated as opaque by the engine.
	 */
	OTHER,
}

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
 * Where a `@ConstraintArg` rule applies on a constraint-annotation parameter.
 *
 * Authors declare targets via `ConstraintArg` (in **validata-core**). Entry names are part of the
 * KSP / IntelliJ discovery contract (alongside [ConstraintArgKind]).
 *
 * Do not confuse with [PropertyRefScope] (where a path resolves on the validated subject).
 * 
 * @author Ghaylan Saada
 */
enum class ConstraintArgTarget {
	
	/**
	 * The parameter value itself: a scalar, or the collection/array container as a whole
	 * (e.g. [ConstraintArgKind.NON_EMPTY] on `@In.values` / `@DaysOfWeek.days`).
	 */
	VALUE,
	
	/**
	 * Each element of an `Array` / `List` / `Set` argument
	 * (e.g. [ConstraintArgKind.NOT_BLANK] + [ConstraintArgKind.TYPED_LITERAL] on `@In.values`).
	 */
	ELEMENT,
}

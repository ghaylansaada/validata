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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope

/**
 * Marks a [ConstraintMetadata] field whose value names another property rather than carrying data.
 *
 * Paths are single-segment only (`"password"`, `"email"`). Dotted nested paths are not supported.
 * Blank values mean “no reference” and are skipped unless the same parameter also carries
 * `@ConstraintArg(ConstraintArgKind.NOT_BLANK)`. Prefer generated `Type_` constants or the
 * IntelliJ plugin for rename-safe authoring; KSP verifies both forms at compile time.
 *
 * @property scope Resolve root for the path ([PropertyRefScope]).
 * @property compatibility Scalar check between the annotated subject and the referenced leaf;
 *   default [PropertyRefCompatibilityKind.NONE] (existence only).
 * 
 * @author Ghaylan Saada
 */
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyRef(
	val scope: PropertyRefScope = PropertyRefScope.SIBLING,
	val compatibility: PropertyRefCompatibilityKind = PropertyRefCompatibilityKind.NONE)

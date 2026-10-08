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

/**
 * Marks the annotation parameter that becomes [ConstraintMetadata.message] when KSP generates
 * metadata (Option 2).
 *
 * Role marker for the annotation declaration — not a use-site argument check.
 * Do not confuse with [ConstraintArg].
 *
 * Every `@Constraint` annotation that opts into generated metadata must declare **exactly one**
 * parameter with this marker; the parameter type must be [String].
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER)
annotation class ConstraintMessage

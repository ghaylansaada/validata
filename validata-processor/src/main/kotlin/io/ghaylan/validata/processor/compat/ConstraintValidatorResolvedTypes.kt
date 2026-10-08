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
package io.ghaylan.validata.processor.compat

import com.google.devtools.ksp.symbol.KSType

/**
 * Pair of resolved `ConstraintValidator` type arguments from [ConstraintValidatorTypeResolver].
 *
 * @property valueType `V` in `ConstraintValidator<V, C>` — drives validator selection ranking
 * @property metadataType `C` in `ConstraintValidator<V, C>` — must match the generated
 *   `{AnnotationSimpleName}Constraint` metadata type
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintValidatorResolvedTypes(
	val valueType: KSType,
	val metadataType: KSType
)

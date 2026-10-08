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
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.symbol.KSNode

/**
 * Shared context for one [ShapeModelBuilder.buildShape] walk (property, parameter, or type arg).
 *
 * @property ownerQualifiedName declaring type / endpoint owner FQCN (error context)
 * @property subjectName property or parameter name (error context)
 * @property subjectNode symbol for IDE/build log location
 * @property visiting cycle set from the schema builder (passed through for nested type args)
 * @property cascadePolicy whether nested object types may cascade
 * 
 * @author Ghaylan Saada
 */
internal data class ShapeBuildContext(
	val ownerQualifiedName: String,
	val subjectName: String,
	val subjectNode: KSNode?,
	val visiting: MutableSet<String>,
	val cascadePolicy: CascadePolicy,
)

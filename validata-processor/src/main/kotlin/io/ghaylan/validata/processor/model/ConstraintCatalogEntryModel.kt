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
package io.ghaylan.validata.processor.model

/**
 * One validator binding discovered for a `@Constraint` annotation, ready for code generation.
 *
 * Property values are FQCN strings because the runtime types they name
 * (`ConstraintMetadata`, `ConstraintValidator`, …) live in **`validata-core`** — not on this
 * processor’s compile classpath (`validata-core` → `ksp(processor)` would cycle).
 *
 * @property annotationFqcn fully qualified annotation class (e.g. `…Required`)
 * @property metadataFqcn fully qualified `ConstraintMetadata` class
 * @property valueTypeFqcn fully qualified value type accepted by the validator (`Any`, `CharSequence`, …)
 * @property validatorFqcn fully qualified validator class or object
 * @property objectSingleton `true` when the validator is a Kotlin `object` (emit the singleton)
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintCatalogEntryModel(
	val annotationFqcn: String,
	val metadataFqcn: String,
	val valueTypeFqcn: String,
	val validatorFqcn: String,
	val objectSingleton: Boolean
)
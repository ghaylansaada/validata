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
 * All catalog entries belonging to one `@Constraint`-annotated annotation class.
 *
 * Emitted as one generated file (Section 4: one file per constraint annotation) plus a reference
 * from the per-module aggregator.
 *
 * @property annotationFqcn fully qualified annotation class
 * @property annotationSimpleName short name used in generated factory function names
 * @property sourceFilePath optional path of the declaring source file (diagnostics only; not used
 *   for KSP [com.google.devtools.ksp.processing.Dependencies] — processors re-resolve [com.google.devtools.ksp.symbol.KSFile]
 *   from the current round's [com.google.devtools.ksp.processing.Resolver])
 * @property entries validator bindings for this annotation, sorted by validator FQCN
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintCatalogAnnotationModel(
	val annotationFqcn: String,
	val annotationSimpleName: String,
	val sourceFilePath: String?,
	val entries: List<ConstraintCatalogEntryModel>
)

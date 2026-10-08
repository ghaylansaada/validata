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
 * Parsed `@Constraint(validatedBy = …)` payload for one annotation **type**, plus the
 * convention-derived `{AnnotationSimpleName}Constraint` metadata FQCN.
 *
 * Produced by ConstraintModelBuilder when it inspects
 * the annotation class (e.g. `@Required`), not a single usage site.
 *
 * @property metadataQualifiedName FQCN of the generated `ConstraintMetadata` implementation
 * @property validatorQualifiedNames FQCNs listed in `validatedBy` (one or more validators)
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintMeta(
	val metadataQualifiedName: String,
	val validatorQualifiedNames: List<String>
)
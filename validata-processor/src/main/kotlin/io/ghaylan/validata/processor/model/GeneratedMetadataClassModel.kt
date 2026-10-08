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
 * Model for one generated metadata class from a `@Constraint` annotation declaration.
 *
 * @property packageName package of the generated file
 * @property simpleName e.g. `SizeConstraint`
 * @property annotationFqcn source annotation FQCN (`// Source:` banner; KDoc uses simple name + import)
 * @property properties constructor properties in emission order
 * 
 * @author Ghaylan Saada
 */
internal data class GeneratedMetadataClassModel(
	val packageName: String,
	val simpleName: String,
	val annotationFqcn: String,
	val properties: List<GeneratedMetadataPropertyModel>,
)

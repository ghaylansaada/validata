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
package io.ghaylan.validata.schema.support.dto

import io.ghaylan.validata.schema.shape.ObjectRefShape

/**
 * Minimal self-referential DTO used by object-schema smoke tests.
 *
 * @property name leaf string (nullable so presence constraints can be exercised)
 * @property child nested node forming a schema cycle via [ObjectRefShape]
 * 
 * @author Ghaylan Saada
 */
data class Node(
	val name: String?,
	val child: Node?,
)

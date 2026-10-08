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

import io.ghaylan.validata.schema.PropertyPathTest

/**
 * Root object with a nested [Address] used by [PropertyPathTest].
 *
 * @property address nested object for dotted-path walks
 * @property name sibling scalar for single-segment reads
 * 
 * @author Ghaylan Saada
 */
data class User(
	val address: Address?,
	val name: String?,
)

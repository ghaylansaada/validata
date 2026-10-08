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
package io.ghaylan.validata.schema.request

/**
 * One positional slot in a handler method's argument array.
 *
 * Indices in [EndpointSchema.argumentLayout] match Spring's resolved `args` array order
 * (declaration order, including non-transport parameters as [EndpointArgumentKind.OTHER]).
 *
 * @property kind Transport role for this index
 * @property name Effective query/header/path name when [kind] is QUERY, HEADER, or PATH; empty otherwise
 * 
 * @author Ghaylan Saada
 */
data class EndpointArgumentSlot(
	val kind: EndpointArgumentKind,
	val name: String = "")

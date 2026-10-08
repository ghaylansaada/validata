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
package io.ghaylan.validata.engine.fastpath

import io.ghaylan.validata.schema.ObjectSchema

/**
 * Visited set keyed by [ObjectSchema.type] so cyclic `lazy { build() }` refs terminate
 * during [SchemaGroupFastPath] scans.*
 * 
 * @author Ghaylan Saada
 */
internal class SchemaTypeVisited {
	
	/**
	 * Runtime classes already seen in the current scan.
	 */
	private val seen = HashSet<Class<*>>()
	
	/**
	 * Records [type] as visited.
	 *
	 * Mutates [seen].
	 *
	 * @param type Runtime class of an [ObjectSchema] node.
	 * @return `true` if this is the first visit; `false` if already seen (cycle).	 
	 */
	fun add(type: Class<*>): Boolean = seen.add(type)
}

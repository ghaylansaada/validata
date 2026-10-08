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
package io.ghaylan.validata.schema

import io.ghaylan.validata.schema.shape.ObjectRefShape

/**
 * Dotted-path split and read helpers for cross-field property references on an [ObjectSchema] graph.
 *
 * Shared by KSP, runtime sibling reads, and the IntelliJ plugin. Not a JSONPath engine and does not validate values.*
 * 
 * @author Ghaylan Saada
 */
object PropertyPath {

	/**
	 * Maximum segment count for a cross-field reference path (processor and generated `Fields` nesting).
	 */
	const val MAX_REFERENCE_PATH_DEPTH: Int = 6

	/**
	 * Max property names listed in unknown-segment diagnostics (keeps failure messages bounded).
	 */
	private const val MAX_KNOWN_NAMES_IN_ERROR: Int = 20

	/**
	 * Splits a dotted path into non-blank trimmed segments.
	 *
	 * No side effects.
	 *
	 * @param path e.g. `"address.city"` or a single sibling name `"password"`
	 * @return ordered segments; empty when [path] is blank
	 */
	fun split(path: String): List<String> {
		// Single pass: avoid the three intermediate lists from split().map().filter().
		// Cross-field refs call this on the hot path; paths are short (≤ MAX_REFERENCE_PATH_DEPTH).
		if (path.isEmpty()) return emptyList()
		val segments = ArrayList<String>(4)
		var start = 0
		var i = 0
		while (i <= path.length) {
			val atEnd = i == path.length
			if (atEnd || path[i] == '.') {
				val raw = path.substring(start, i)
				val trimmed = raw.trim()
				if (trimmed.isNotEmpty()) segments.add(trimmed)
				start = i + 1
			}
			i++
		}
		return segments
	}

	/**
	 * Looks up a property by declared or external name on [schema].
	 *
	 * No side effects.
	 *
	 * @param schema owner schema
	 * @param name segment spelling (Kotlin name or `@JsonProperty` wire name)
	 * @return matching [PropertySpec], or `null` when neither index contains [name]
	 */
	fun findProperty(schema: ObjectSchema, name: String): PropertySpec? =
		schema.byDeclaredName[name] ?: schema.byExternalName[name]

	/**
	 * Reads a nested property value from [rootInstance] via [rootSchema], stepping only through object-ref segments.
	 *
	 * Calls [ValueReader.read] per step; does not mutate the instance graph.
	 *
	 * @param rootSchema schema of [rootInstance]
	 * @param rootInstance container object; `null` yields `null`
	 * @param path dotted path relative to the root
	 * @return leaf value, or `null` when a container along the path is null
	 * @throws IllegalArgumentException when [path] is blank or exceeds [MAX_REFERENCE_PATH_DEPTH]
	 * @throws IllegalStateException when a segment is missing or is not an object step-in
	 */
	fun read(rootSchema: ObjectSchema, rootInstance: Any?, path: String): Any? {
		val segments = split(path)
		require(segments.isNotEmpty()) {
			"Property path must not be blank — use a dotted path such as 'address.city' or a single property name (e.g. 'password')."
		}
		require(segments.size <= MAX_REFERENCE_PATH_DEPTH) {
			"Property path '$path' has ${segments.size} segments but max depth is " +
				"$MAX_REFERENCE_PATH_DEPTH — shorten the path or nest through fewer intermediate objects."
		}

		var schema = rootSchema
		var instance: Any? = rootInstance
		// Index loop instead of withIndex(): avoids IndexedValue wrappers on the cross-field hot path
		// (paths are ≤ MAX_REFERENCE_PATH_DEPTH, but this runs per sibling read).
		var index = 0
		val last = segments.lastIndex
		while (index <= last) {
			val segment = segments[index]
			val isLast = index == last
			val spec = findProperty(schema, segment)
				?: error("Unknown property '$segment' on ${schema.type.name} " +
						"(path='$path', known=${formatKnownNames(schema)}). " +
						"Use a declaredName or externalName that exists on that type.")
			if (instance == null) return null
			val value = spec.read.read(instance)
			if (isLast) return value
			val nested = (spec.shape as? ObjectRefShape)?.ref?.value
				?: error("Cannot step into '${spec.declaredName}' on ${schema.type.name} for path '$path' — " +
						"shape is ${spec.shape::class.simpleName}, not an object ref. " +
						"Only nested object properties may appear between path segments.")
			schema = nested
			instance = value
			index++
		}
		return null
	}

	/**
	 * Bounded list of declared names for diagnostics (avoids huge messages on fat DTOs).
	 */
	private fun formatKnownNames(schema: ObjectSchema): String {
		val keys = schema.byDeclaredName.keys
		if (keys.size <= MAX_KNOWN_NAMES_IN_ERROR) return keys.toString()
		val shown = keys.asSequence().take(MAX_KNOWN_NAMES_IN_ERROR).joinToString(", ")
		return "[$shown, … +${keys.size - MAX_KNOWN_NAMES_IN_ERROR} more]"
	}
}

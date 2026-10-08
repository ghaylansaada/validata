/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.path

import com.intellij.openapi.util.TextRange
import io.ghaylan.validata.schema.PropertyPath

/**
 * Path segment splitting for property-ref string literals in the IDE.
 *
 * Uses [PropertyPath.split] / [PropertyPath.MAX_REFERENCE_PATH_DEPTH] from **validata-schema**
 * for spelling rules, then adds IDE-only soft segments and [TextRange]s for PsiReferences.
 *
 * Used by `PropertyRefReferenceProvider` to attach one PsiReference per segment. Schema
 * `PropertyPath` also owns runtime `read` / `findProperty`; this object only splits and ranges —
 * it does **not** resolve members or walk object graphs.
 *
 * Not a JSONPath engine. Not responsible for rejecting nested `@PropertyRef` paths (that policy
 * lives in the reference provider / annotator: only the first hard segment is in scope).*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyPathSegments {
	
	/**
	 * Maximum number of segments retained for a cross-field reference path.
	 *
	 * Same depth cap as [PropertyPath.MAX_REFERENCE_PATH_DEPTH].	 
	 */
	const val MAX_REFERENCE_PATH_DEPTH: Int = PropertyPath.MAX_REFERENCE_PATH_DEPTH
	
	/**
	 * Splits a dotted path into non-blank trimmed segments — [PropertyPath.split].
	 *
	 * Call when only segment spellings are needed (tests, depth checks). Prefer
	 * [segmentsWithRanges] when attaching PsiReferences so ranges stay aligned with the source
	 * value text.
	 *
	 * @param path e.g. `"address.city"` or a single sibling name `"password"` (quotes not included)
	 * @return ordered non-blank trimmed segments; empty when [path] yields no content segments	 
	 */
	fun split(path: String): List<String> = PropertyPath.split(path)
	
	/**
	 * Builds [PropertyPathSegment] descriptors with value-relative ranges for PsiReference ranges.
	 *
	 * Walks `path.split('.')` while tracking character offsets so each hard segment’s
	 * [PropertyPathSegment.rangeInValue] covers only the trimmed spelling (leading/trailing spaces
	 * inside a piece are excluded from the range).
	 *
	 * When the trimmed path ends with `.` and at least one hard segment already exists, appends a
	 * soft empty segment at the caret so completion can list members of the previous segment’s
	 * type (`"address.<caret>"`). A lone `"."` / empty string does **not** synthesize a soft host
	 * here (empty `""` soft refs are created separately in the reference provider).
	 *
	 * Segments beyond [MAX_REFERENCE_PATH_DEPTH] are omitted (same cap as KSP). Does not throw on
	 * over-depth or malformed paths.
	 *
	 * @param path unquoted string-literal value text (may include spaces / trailing dot)
	 * @return at most [MAX_REFERENCE_PATH_DEPTH] segments; empty for blank / all-dot paths with no
	 *   hard content	 
	 */
	fun segmentsWithRanges(path: String): List<PropertyPathSegment> {
		val result = ArrayList<PropertyPathSegment>()
		var offset = 0
		val parts = path.split('.')
		for ((index, part) in parts.withIndex()) {
			val trimmed = part.trim()
			if (trimmed.isNotEmpty()) {
				val startInPart = part.indexOf(trimmed)
				val start = offset + startInPart
				result += PropertyPathSegment(
					name = trimmed,
					rangeInValue = TextRange(start, start + trimmed.length),
					soft = false,
				)
			}
			offset += part.length
			if (index < parts.lastIndex) {
				offset += 1
			}
		}
		val trimmedEnd = path.trimEnd()
		if (trimmedEnd.endsWith('.') && result.isNotEmpty()) {
			val caret = trimmedEnd.length
			result += PropertyPathSegment(
				name = "",
				rangeInValue = TextRange(caret, caret),
				soft = true,
			)
		}
		
		return result.take(MAX_REFERENCE_PATH_DEPTH)
	}
}

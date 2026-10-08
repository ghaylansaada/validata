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
package io.ghaylan.validata.processor.verify

import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget

/**
 * Pure / near-pure `@ConstraintArg` kind checks for [ConstraintArgTarget.ELEMENT].*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintArgKindElementRules {
	
	/**
	 * Kind → validator for [ConstraintArgTarget.ELEMENT] checks that do not need a subject [KSType].
	 *
	 * Callers iterate [ConstraintArgHost.kinds] and look up each kind.	 
	 */
	val byKind: Map<ConstraintArgKind, (
		elements: List<Any?>,
		raw: Any?,
		prefix: String,
		targetLabel: String,
	) -> String?> = mapOf(
		ConstraintArgKind.NOT_BLANK to { elements, _, prefix, targetLabel ->
			if (elements.any { it is String && it.isBlank() }) {
				"$prefix must not contain blank ${targetLabel}s — remove or fill each entry."
			}
			else {
				null
			}
		},
		ConstraintArgKind.NON_EMPTY to { elements, raw, prefix, targetLabel ->
			if (elements.isEmpty() && isCollectionArg(raw)) {
				null
			}
			else if (elements.any { it is String && it.isEmpty() }) {
				"$prefix must not contain empty ${targetLabel}s — remove or fill each entry."
			}
			else {
				null
			}
		},
		ConstraintArgKind.REGEX to { elements, _, prefix, targetLabel ->
			for (el in elements) {
				val pattern = el as? String
					?: continue
				try {
					java.util.regex.Pattern.compile(pattern)
				}
				catch (ex: java.util.regex.PatternSyntaxException) {
					return@to "$prefix $targetLabel is not a valid Java regex pattern: ${ex.description}."
				}
			}
			null
		},
		ConstraintArgKind.NON_NEGATIVE to { _, _, prefix, _ ->
			"$prefix: ${ConstraintArgKind.NON_NEGATIVE} is not valid with target=ELEMENT."
		},
		ConstraintArgKind.POSITIVE to { _, _, prefix, _ ->
			"$prefix: ${ConstraintArgKind.POSITIVE} is not valid with target=ELEMENT."
		},
	)
	
	/**
	 * [ConstraintArgKind.TYPED_LITERAL] on [ConstraintArgTarget.ELEMENT] — needs the peeled element
	 * [KSType].
	 *
	 * Side effects: none.
	 *
	 * @param elements Normalized element strings from [elementValues].
	 * @param valueType Element type for literal parsing.
	 * @param prefix Diagnostic message prefix.
	 * @return Full diagnostic sentence, or `null` when all elements pass.	 
	 */
	fun typedLiteral(
		elements: List<Any?>,
		valueType: KSType,
		prefix: String,
	): String? {
		for (el in elements) {
			val s = el as? String
				?: continue
			val err = ConstraintLiteralVerifier.typedLiteralError(s, valueType)
			if (err != null) return "$prefix $err"
		}
		return null
	}
	
	/**
	 * Whether [raw] is a collection or primitive array argument value.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value.
	 * @return `true` when [raw] is [List] or an array type accepted by [elementValues].	 
	 */
	internal fun isCollectionArg(raw: Any?): Boolean =
		raw is List<*> || raw is Array<*> || raw is IntArray || raw is LongArray || raw is ShortArray || raw is ByteArray || raw is FloatArray || raw is DoubleArray || raw is BooleanArray || raw is CharArray
	
	/**
	 * Normalizes an ELEMENT-target argument to a flat element list.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value; scalar string becomes a one-element list.
	 * @return Element values to validate; empty when [raw] is null or unsupported.	 
	 */
	internal fun elementValues(raw: Any?): List<Any?> =
		when (raw) {
			null -> emptyList()
			is String -> listOf(raw)
			is List<*> -> raw
			is Array<*> -> raw.toList()
			is IntArray -> raw.toList()
			is LongArray -> raw.toList()
			is ShortArray -> raw.toList()
			is ByteArray -> raw.toList()
			is FloatArray -> raw.toList()
			is DoubleArray -> raw.toList()
			is BooleanArray -> raw.toList()
			is CharArray -> raw.toList()
			else -> emptyList()
		}
}

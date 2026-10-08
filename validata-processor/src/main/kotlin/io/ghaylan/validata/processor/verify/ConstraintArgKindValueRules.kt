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
 * Pure / near-pure `@ConstraintArg` kind checks for [ConstraintArgTarget.VALUE].
 *
 * Returns a full diagnostic sentence (including `prefix`), or `null` when the kind passes.
 * [ConstraintArgKind.TYPED_LITERAL] is handled separately via [typedLiteral] because it needs
 * a real [KSType].*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintArgKindValueRules {
	
	/**
	 * Kind → validator for kinds that do not need a subject [KSType].
	 * Callers iterate [ConstraintArgHost.kinds] and look up each kind.	 
	 */
	val byKind: Map<ConstraintArgKind, (raw: Any?, prefix: String) -> String?> = mapOf(
		ConstraintArgKind.NOT_BLANK to { raw, prefix ->
			if (raw is String && raw.isBlank()) {
				"$prefix must not be blank — provide a non-whitespace value."
			}
			else {
				null
			}
		},
		ConstraintArgKind.NON_EMPTY to { raw, prefix ->
			if (isEmptyArg(raw)) {
				"$prefix must not be empty — provide at least one value."
			}
			else {
				null
			}
		},
		ConstraintArgKind.REGEX to { raw, prefix ->
			val pattern = raw as? String
				?: return@to "$prefix must be a regex pattern string (e.g. \"^[A-Z]+$\")."
			try {
				java.util.regex.Pattern.compile(pattern)
				null
			}
			catch (ex: java.util.regex.PatternSyntaxException) {
				"$prefix is not a valid Java regex pattern: ${ex.description}."
			}
		},
		ConstraintArgKind.NON_NEGATIVE to { raw, prefix ->
			val n = longValue(raw)
				?: return@to "$prefix must be a non-negative integer literal (e.g. 0 or 18)."
			if (n < 0) {
				"$prefix must be >= 0 (was $n) — use a non-negative integer."
			}
			else {
				null
			}
		},
		ConstraintArgKind.POSITIVE to { raw, prefix ->
			positiveError(raw, prefix)
		},
	)
	
	/**
	 * [ConstraintArgKind.TYPED_LITERAL] on [ConstraintArgTarget.VALUE] — needs a resolved subject
	 * type.
	 *
	 * Side effects: none.
	 *
	 * @param raw Whole-argument value; non-string values are ignored.
	 * @param valueType Subject type for literal parsing.
	 * @param prefix Diagnostic message prefix.
	 * @return Full diagnostic sentence, or `null` when the literal passes or is not a string.	 
	 */
	fun typedLiteral(
		raw: Any?,
		valueType: KSType,
		prefix: String
	): String? {
		val s = raw as? String
			?: return null
		val err = ConstraintLiteralVerifier.typedLiteralError(s, valueType)
			?: return null
		return "$prefix $err"
	}
	
	/**
	 * Whether [raw] is empty for [ConstraintArgKind.NON_EMPTY] checks.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value.
	 * @return `true` when null, blank string, or empty collection/array.	 
	 */
	internal fun isEmptyArg(raw: Any?): Boolean =
		when (raw) {
			null -> true
			is String -> raw.isEmpty()
			is List<*> -> raw.isEmpty()
			is Array<*> -> raw.isEmpty()
			is IntArray -> raw.isEmpty()
			is LongArray -> raw.isEmpty()
			is ShortArray -> raw.isEmpty()
			is ByteArray -> raw.isEmpty()
			is FloatArray -> raw.isEmpty()
			is DoubleArray -> raw.isEmpty()
			is BooleanArray -> raw.isEmpty()
			is CharArray -> raw.isEmpty()
			else -> false
		}
	
	/**
	 * Coerces [raw] to a signed integer for [ConstraintArgKind.NON_NEGATIVE] checks.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value.
	 * @return Parsed long, or `null` when [raw] is not a numeric literal.	 
	 */
	internal fun longValue(raw: Any?): Long? =
		when (raw) {
			is Int -> raw.toLong()
			is Long -> raw
			is Short -> raw.toLong()
			is Byte -> raw.toLong()
			is String -> raw.trim().replace("_", "").toLongOrNull()
			else -> null
		}
	
	/**
	 * Coerces [raw] to a floating-point value for [ConstraintArgKind.POSITIVE] checks.
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value.
	 * @return Parsed double, or `null` when [raw] is not a float/double literal.	 
	 */
	internal fun doubleValue(raw: Any?): Double? =
		when (raw) {
			is Float -> raw.toDouble()
			is Double -> raw
			else -> null
		}
	
	/**
	 * [ConstraintArgKind.POSITIVE] diagnostic for whole-argument [raw].
	 *
	 * Side effects: none.
	 *
	 * @param raw Annotation argument value.
	 * @param prefix Diagnostic message prefix.
	 * @return Full diagnostic sentence, or `null` when the value is positive.	 
	 */
	private fun positiveError(
		raw: Any?,
		prefix: String
	): String? {
		val asNumber = longValue(raw)
		if (asNumber != null) {
			return if (asNumber <= 0) "$prefix must be > 0 (was $asNumber)." else null
		}
		val asDouble = doubleValue(raw)
		if (asDouble != null) {
			return if (asDouble <= 0.0) "$prefix must be > 0 (was $asDouble)." else null
		}
		val s = raw as? String
		if (s == null || !ConstraintLiteralVerifier.isValidNumberLiteral(s)) {
			return "$prefix must be a positive decimal number string (e.g. \"5\" or \"0.25\")."
		}
		val bd = try {
			java.math.BigDecimal(
				s.trim().replace("_", ""))
		}
		catch (_: NumberFormatException) {
			null
		}
		return if (bd == null || bd.signum() <= 0) {
			"$prefix must be > 0 (was '$s')."
		}
		else {
			null
		}
	}
}

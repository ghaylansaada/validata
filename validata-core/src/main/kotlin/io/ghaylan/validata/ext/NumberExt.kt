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
package io.ghaylan.validata.ext

import io.ghaylan.validata.schema.types.ConstraintLiteralRules
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Converts a runtime [Number] to [BigDecimal] for constraint comparisons.
 *
 * No side effects.
 *
 * @receiver Number to convert.
 * @return Decimal form, or `null` when the value is non-finite (`NaN` / infinity) or unparseable.
 * */
fun Number.toBigDecimalOrNull(): BigDecimal? {
	if (this is Double && !isFinite()) return null
	if (this is Float && !isFinite()) return null
	return try {
		when (this) {
			is BigDecimal -> this
			else -> BigDecimal(toString())
		}
	}
	catch (_: NumberFormatException) {
		null
	}
}

/**
 * Parses a constraint bound / factor string into a [BigDecimal].
 *
 * Delegates to schema [ConstraintLiteralRules.parseNumber] so KSP / IntelliJ / runtime share
 * the same underscore-aware decimal grammar.
 *
 * @receiver Raw bound / factor literal.
 * @return Finite decimal, or `null` when blank or not a number.
 * */
fun String.toConstraintNumber(): BigDecimal? =
	ConstraintLiteralRules.parseNumber(this)

/**
 * Reduces an integral subject to a [Long] so numeric constraints can compare without allocating
 * a [BigDecimal].
 *
 * Integral subjects ([Byte]/[Short]/[Int]/[Long]/[BigInteger]) and integral [BigDecimal] values
 * take this path; [Float], [Double], and fractional [BigDecimal] must use the decimal compare.
 * No side effects.
 *
 * @receiver Number under validation.
 * @return Integral long form, or `null` when a decimal conversion is required (floating point,
 *   non-integral [BigDecimal], or long overflow).
 * */
internal fun Number.toIntegralLongOrNull(): Long? = when (this) {
	is Long -> this
	is Int -> toLong()
	is Short -> toLong()
	is Byte -> toLong()
	is BigInteger -> try {
		longValueExact()
	}
	catch (_: ArithmeticException) {
		null
	}
	is BigDecimal -> integralLongOrNull()
	else -> null
}

/**
 * Reduces an integral constraint bound to a [Long] so the comparison can stay on the long path.
 *
 * No side effects.
 *
 * @receiver Parsed bound / factor.
 * @return Long form when the bound has no fractional digits and fits in a long; otherwise `null`
 *   (the caller must use the [BigDecimal] compare).
 * */
internal fun BigDecimal.integralLongOrNull(): Long? {
	if (scale() > 0) return null
	return try {
		longValueExact()
	}
	catch (_: ArithmeticException) {
		null
	}
}

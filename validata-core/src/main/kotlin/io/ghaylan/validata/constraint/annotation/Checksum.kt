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
package io.ghaylan.validata.constraint.annotation

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.string.checksum.ChecksumValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import kotlin.reflect.KClass

/**
 * Asserts that a [CharSequence] slice satisfies a check-digit / checksum [algorithm].
 *
 * The validated subject is the substring `[startIndex, endIndex)` of the value
 * (`endIndex = -1` means through the end). The check digit sits at [checkDigitIndex] relative to
 * that slice (`-1` = last character). When [ignoreNonDigits] is `true`, non-digit characters are
 * stripped before most algorithms; for [Algorithm.MOD97_10], letters are kept and mapped
 * `A`–`Z` → 10–35 (ISO 7064), and only other non-alphanumeric characters are skipped.
 *
 * `null` values are skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Checksum(algorithm = Checksum.Algorithm.LUHN, ignoreNonDigits = true)
 * val accountNumber: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 * - [ConstraintErrorCode.VALUE_CHECKSUM_INVALID]
 *
 * @property algorithm Check-digit algorithm to apply.
 * @property checkDigitIndex Index of the check digit within the validated slice; `-1` = last.
 * @property startIndex Inclusive start of the validated slice within the full value.
 * @property endIndex Exclusive end of the validated slice; `-1` = end of the value.
 * @property ignoreNonDigits Whether to strip non-digits (or non-alphanumeric for MOD-97-10) first.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [ChecksumValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Checksum(
	val algorithm: Algorithm,

	val checkDigitIndex: Int = -1,

	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val startIndex: Int = 0,

	val endIndex: Int = -1,

	val ignoreNonDigits: Boolean = false,

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {

	/**
	 * Supported check-digit / checksum algorithms.
	 */
	enum class Algorithm {

		/**
		 * Classic Luhn (Mod-10) checksum; check digit included, sum ≡ 0 (mod 10).
		 */
		LUHN,

		/**
		 * Alias of [LUHN] (Luhn Mod-10), used for payment-card style identifiers.
		 */
		MOD10,

		/**
		 * Weighted Mod-11 (ISBN-10 style); check digit may be `X` for 10.
		 */
		MOD11,

		/**
		 * Verhoeff dihedral-group check-digit algorithm.
		 */
		VERHOEFF,

		/**
		 * Damm quasigroup check-digit algorithm.
		 */
		DAMM,

		/**
		 * ISO 7064 MOD 97-10 over alphanumeric characters (`A`–`Z` → 10–35).
		 */
		MOD97_10,
	}
}

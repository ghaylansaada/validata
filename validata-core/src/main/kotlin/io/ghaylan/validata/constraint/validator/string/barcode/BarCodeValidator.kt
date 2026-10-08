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
package io.ghaylan.validata.constraint.validator.string.barcode

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.Barcode
import io.ghaylan.validata.constraint.annotation.BarcodeConstraint
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.runtime.ValidationContext

/**
 * Validates retail and publishing barcodes (EAN, UPC, GTIN, ISBN, ISSN).
 *
 * Null subjects are skipped by the engine (presence is `@Required`). Spaces and hyphens are stripped before length and
 * checksum checks. Each [Barcode.Type] applies its own accepted lengths and algorithm.
 *
 * Errors: [ConstraintErrorCode.VALUE_FORMAT_INVALID], [ConstraintErrorCode.VALUE_CHECKSUM_INVALID].
 * Failures attach the [BarcodeConstraint]; messages include the barcode type name.
 *
 * Lives as a Kotlin `object` so the constraint catalog can reference a single shared
 * instance without Spring wiring; override via a `@Component` of the same type when an
 * application needs different behavior.
 *
 * @author Ghaylan Saada
 */
object BarcodeValidator : ConstraintValidator<CharSequence, BarcodeConstraint>() {

	/**
	 * Error codes this validator may emit on failure.
	 */
	override fun possibleErrorCodes(
		constraint: BarcodeConstraint,
		type: Class<*>,
	) = setOf(
		ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
		ConstraintErrorCode.VALUE_FORMAT_INVALID,
	)

	/**
	 * Validates [value] against [constraint].
	 *
	 * Null values are accepted (presence is enforced by `@Required`).
	 * Side effects: none.
	 *
	 * @param value Subject under validation; never `null` (null is skipped by the engine).
	 * @param constraint Metadata for this annotation instance.
	 * @param context Active validation cursor (path, groups, sibling access).
	 * @return Path-free violation with [constraint] attached, or `null` when valid.
	 */
	override fun validate(
		value: CharSequence,
		constraint: BarcodeConstraint,
		context: ValidationContext,
	): ConstraintError<*>? {
		val normalized = BarcodeSupport.normalize(value) ?: return formatError(constraint)

		val outcome = when (constraint.type) {
			Barcode.Type.EAN -> BarcodeSupport.validateEan(normalized)
			Barcode.Type.UPC -> BarcodeSupport.validateUpc(normalized)
			Barcode.Type.GTIN -> BarcodeSupport.validateGtin(normalized)
			Barcode.Type.ISBN -> BarcodeSupport.validateIsbn(normalized)
			Barcode.Type.ISSN -> BarcodeSupport.validateIssn(normalized)
		}

		return when (outcome) {
			BarcodeValidationOutcome.VALID -> null
			BarcodeValidationOutcome.FORMAT_INVALID -> formatError(constraint)
			BarcodeValidationOutcome.CHECKSUM_INVALID -> checksumError(constraint)
		}
	}

	/**
	 * Builds the layout violation, listing every accepted layout for [constraint]'s type.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Declared barcode constraint.
	 * @return Violation with [ConstraintErrorCode.VALUE_FORMAT_INVALID].
	 */
	private fun formatError(
		constraint: BarcodeConstraint,
	): ConstraintError<*> {
		val type = constraint.type
		val formats = BarcodeSupport.expectedFormats(type)
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_FORMAT_INVALID,
			message = "Must match one of these ${type.name} formats: ${formats.joinToString(", ")}.",
			metadata = constraint,
		)
	}

	/**
	 * Builds the check-character violation for a payload whose layout is correct.
	 *
	 * Side effects: none.
	 *
	 * @param constraint Declared barcode constraint.
	 * @return Violation with [ConstraintErrorCode.VALUE_CHECKSUM_INVALID].
	 */
	private fun checksumError(
		constraint: BarcodeConstraint,
	): ConstraintError<*> = ConstraintError(
		code = ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
		message = "${constraint.type.name} barcode is not valid; check it for a typing error.",
		metadata = constraint,
	)
}

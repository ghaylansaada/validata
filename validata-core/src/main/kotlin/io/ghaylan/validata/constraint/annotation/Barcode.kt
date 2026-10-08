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
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.string.barcode.BarcodeValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import kotlin.reflect.KClass

/**
 * Asserts that a [CharSequence] is a valid barcode or publishing identifier of the given [type].
 *
 * Spaces and hyphens are ignored as separators (e.g. `"978-0-306-40615-7"`). Each [Type] enforces
 * its own length rules and checksum:
 *
 * - [Type.EAN] — EAN-8 or EAN-13 with a GS1 mod-10 check digit.
 * - [Type.UPC] — UPC-A (12 digits) with a GS1 mod-10 check digit.
 * - [Type.GTIN] — GTIN-8, GTIN-12, GTIN-13, or GTIN-14 with a GS1 mod-10 check digit.
 * - [Type.ISBN] — ISBN-10 (nine digits plus check `0`–`9` or `X`) or ISBN-13 (`978`/`979` prefix
 *   with GS1 check).
 * - [Type.ISSN] — seven digits plus an ISSN mod-11 check digit (`0`–`9` or `X`).
 *
 * `null` values are skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Barcode(type = Barcode.Type.ISBN)
 * val isbn: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 * - [ConstraintErrorCode.VALUE_CHECKSUM_INVALID]
 *
 * @property type Barcode family to validate against.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [BarcodeValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Barcode(
	val type: Type,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	
	/**
	 * Supported barcode and publishing identifier families.
	 */
	enum class Type {
		
		/**
		 *  European Article Number — retail product codes (EAN-8 or EAN-13).
		 */
		EAN,
		
		/**
		 * Universal Product Code — North American retail UPC-A (12 digits).
		 */
		UPC,
		
		/**
		 * Global Trade Item Number — supply-chain identifiers (8, 12, 13, or 14 digits).
		 */
		GTIN,
		
		/**
		 * International Standard Book Number — ISBN-10 or ISBN-13.
		 */
		ISBN,
		
		/**
		 * International Standard Serial Number — periodical identifier (7 digits + check).
		 */
		ISSN,
	}
}

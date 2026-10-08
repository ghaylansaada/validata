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
package io.ghaylan.validata.benchmarks.validata

import io.ghaylan.validata.benchmarks.hibernate.HvFieldBlock
import io.ghaylan.validata.benchmarks.matrix.PayloadSize
import io.ghaylan.validata.benchmarks.matrix.SizedPayloads
import io.ghaylan.validata.constraint.annotation.Assert
import io.ghaylan.validata.constraint.annotation.Barcode
import io.ghaylan.validata.constraint.annotation.CreditCard
import io.ghaylan.validata.constraint.annotation.Digits
import io.ghaylan.validata.constraint.annotation.Email
import io.ghaylan.validata.constraint.annotation.FilePath
import io.ghaylan.validata.constraint.annotation.RelativeToNow
import io.ghaylan.validata.constraint.annotation.IpAddress
import io.ghaylan.validata.constraint.annotation.Max
import io.ghaylan.validata.constraint.annotation.Min
import io.ghaylan.validata.constraint.annotation.NumberSign
import io.ghaylan.validata.constraint.annotation.Regex
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.constraint.annotation.Url
import io.ghaylan.validata.schema.Validatable
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

/**
 * Sealed Validata leaf block — [PayloadSize.FIELDS_PER_BLOCK] fields per concrete subtype.
 *
 * Six concrete slices cover ~30 fair Validata ↔ HV twins. [SizedPayloads] cycles them for
 * sizes 10–1000. [ValidataSmallRoot] is a separate heterogeneous 5-field DTO (not a slice).
 *
 * HV twin: [HvFieldBlock].
 *
 * @author Ghaylan Saada
 */
@Validatable
sealed interface ValidataFieldBlock

/**
 * Presence + integer bounds (5 leaves).
 *
 * Mapping: `@Required` ≈ HV `@NotBlank` (fixture domain: non-blank / `""`);
 * `@Required(NULL)` on non-text = `@NotNull`; `@Required(EMPTY)` = `@NotEmpty`;
 * `@Min` / `@Max` = HV `@Min` / `@Max` (exact).
 */
@Validatable
data class ValidataPresenceBlock(
	@field:Required
	val notBlankName: String? = null,
	@field:Required(mode = Required.Mode.`NULL`)
	val notNullCount: Int? = null,
	@field:Required(mode = Required.Mode.EMPTY)
	val notEmptyTags: List<String>? = null,
	@field:Min("0")
	val minAge: Int? = null,
	@field:Max("150")
	val maxAge: Int? = null,
) : ValidataFieldBlock

/**
 * Decimal bounds, digits, positive / positive-or-zero.
 *
 * Mapping: `@Min`/`@Max` on [BigDecimal] ≈ HV `@DecimalMin`/`@DecimalMax` (fixture domain);
 * `@Digits` / `@Positive` exact; `@Positive(allowZero)` = HV `@PositiveOrZero` (exact).
 */
@Validatable
data class ValidataNumberBlock(
	@field:Min("0.01")
	val decimalMinPrice: BigDecimal? = null,
	@field:Max("9999.99")
	val decimalMaxPrice: BigDecimal? = null,
	@field:Digits(integer = 3, fraction = 2)
	val digitsAmount: BigDecimal? = null,
	@field:NumberSign(sign = NumberSign.Sign.POSITIVE)
	val positiveQty: Int? = null,
	@field:NumberSign(sign = NumberSign.Sign.POSITIVE, allowZero = true)
	val positiveOrZeroScore: Int? = null,
) : ValidataFieldBlock

/**
 * Sign constraints, size, regex, email.
 *
 * Mapping: `@Negative` / `@Negative(allowZero)` = HV `@Negative` / `@NegativeOrZero` (exact);
 * `@Size` / `@Regex` ≈ `@Size` / `@Pattern` (exact for ASCII fixtures); `@Email` exact.
 */
@Validatable
data class ValidataSignSizeBlock(
	@field:NumberSign(sign = NumberSign.Sign.NEGATIVE)
	val negativeDelta: Int? = null,
	@field:NumberSign(sign = NumberSign.Sign.NEGATIVE, allowZero = true)
	val negativeOrZeroAdj: Int? = null,
	@field:Size(min = 2, max = 32)
	val sizedTitle: String? = null,
	@field:Regex(pattern = "^[A-Z0-9_]+$", name = "CODE")
	val patternCode: String? = null,
	@field:Email
	val email: String? = null,
) : ValidataFieldBlock

/**
 * Temporal bounds + duration min.
 *
 * Mapping: `@RelativeToNow(LT)` / `@RelativeToNow(LTE)` /
 * `@RelativeToNow(GT)` / `@RelativeToNow(GTE)` =
 * HV `@Past` / `@PastOrPresent` / `@Future` / `@FutureOrPresent` (exact);
 * `@Min` on [Duration] ≈ HV `@DurationMin` (exact for fixture bounds).
 */
@Validatable
data class ValidataTemporalBlock(
	@field:RelativeToNow(relation = RelativeToNow.Relation.LT)
	val pastDate: LocalDate? = null,
	@field:RelativeToNow(relation = RelativeToNow.Relation.LTE)
	val pastOrPresentDate: LocalDate? = null,
	@field:RelativeToNow(relation = RelativeToNow.Relation.GT)
	val futureDate: LocalDate? = null,
	@field:RelativeToNow(relation = RelativeToNow.Relation.GTE)
	val futureOrPresentDate: LocalDate? = null,
	@field:Min("PT1S")
	val minDuration: Duration? = null,
) : ValidataFieldBlock

/**
 * Booleans, URL, card, collection size.
 *
 * Mapping: `@Assert(value = …)` ≈ HV `@AssertTrue` / `@AssertFalse`; `@Url` ≈ HV `@URL` (closest available);
 * `@CreditCard` ≈ HV `@CreditCardNumber` (fixture domain / Luhn); `@Size` on [List] = HV `@Size` (exact).
 */
@Validatable
data class ValidataFormatBlock(
	@field:Assert(value = true)
	val assertTrueFlag: Boolean? = null,
	@field:Assert(value = false)
	val assertFalseFlag: Boolean? = null,
	@field:Url
	val website: String? = null,
	@field:CreditCard
	val cardNumber: String? = null,
	@field:Size(min = 2, max = 3)
	val sizedTags: List<String>? = null,
) : ValidataFieldBlock

/**
 * File path, barcodes, IP, duration max.
 *
 * Mapping: `@FilePath` ≈ HV `@Pattern` (fixture domain); `@Barcode(EAN/ISBN)` ≈ HV `@EAN` / `@ISBN`;
 * `@IpAddress` exact; `@Max` on [Duration] ≈ HV `@DurationMax` (exact).
 */
@Validatable
data class ValidataIdentityBlock(
	@field:FilePath
	val filePath: String? = null,
	@field:Barcode(type = Barcode.Type.EAN)
	val eanCode: String? = null,
	@field:Barcode(type = Barcode.Type.ISBN)
	val isbnCode: String? = null,
	@field:IpAddress
	val ipAddress: String? = null,
	@field:Max("PT1H")
	val maxDuration: Duration? = null,
) : ValidataFieldBlock

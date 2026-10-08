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
package io.ghaylan.validata.benchmarks.hibernate

import io.ghaylan.validata.benchmarks.validata.ValidataFieldBlock
import jakarta.validation.constraints.AssertFalse
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Negative
import jakarta.validation.constraints.NegativeOrZero
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.CreditCardNumber
import org.hibernate.validator.constraints.EAN
import org.hibernate.validator.constraints.ISBN
import org.hibernate.validator.constraints.IpAddress
import org.hibernate.validator.constraints.URL
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

/**
 * Hibernate Validator twin of [ValidataFieldBlock] — six concrete 5-field slices, same bounds.
 *
 * Nested under sized roots with `@Valid` on the list so cascade cost matches Validata's nested
 * `@Validatable` walk into list elements (runtime type picks the slice).
 *
 * @author Ghaylan Saada
 */
sealed interface HvFieldBlock

/** Twin of Validata presence slice. */
data class HvPresenceBlock(
	@field:NotBlank
	val notBlankName: String? = null,
	@field:NotNull
	val notNullCount: Int? = null,
	@field:NotEmpty
	val notEmptyTags: List<String>? = null,
	@field:Min(0)
	val minAge: Int? = null,
	@field:Max(150)
	val maxAge: Int? = null,
) : HvFieldBlock

/** Twin of Validata number slice. */
data class HvNumberBlock(
	@field:DecimalMin("0.01")
	val decimalMinPrice: BigDecimal? = null,
	@field:DecimalMax("9999.99")
	val decimalMaxPrice: BigDecimal? = null,
	@field:Digits(integer = 3, fraction = 2)
	val digitsAmount: BigDecimal? = null,
	@field:Positive
	val positiveQty: Int? = null,
	@field:PositiveOrZero
	val positiveOrZeroScore: Int? = null,
) : HvFieldBlock

/** Twin of Validata sign/size slice. */
data class HvSignSizeBlock(
	@field:Negative
	val negativeDelta: Int? = null,
	@field:NegativeOrZero
	val negativeOrZeroAdj: Int? = null,
	@field:Size(min = 2, max = 32)
	val sizedTitle: String? = null,
	@field:Pattern(regexp = "^[A-Z0-9_]+$")
	val patternCode: String? = null,
	@field:Email
	val email: String? = null,
) : HvFieldBlock

/** Twin of Validata temporal slice. */
data class HvTemporalBlock(
	@field:Past
	val pastDate: LocalDate? = null,
	@field:PastOrPresent
	val pastOrPresentDate: LocalDate? = null,
	@field:Future
	val futureDate: LocalDate? = null,
	@field:FutureOrPresent
	val futureOrPresentDate: LocalDate? = null,
	@field:DurationMin(seconds = 1)
	val minDuration: Duration? = null,
) : HvFieldBlock

/** Twin of Validata format slice. */
data class HvFormatBlock(
	@field:AssertTrue
	val assertTrueFlag: Boolean? = null,
	@field:AssertFalse
	val assertFalseFlag: Boolean? = null,
	@field:URL
	val website: String? = null,
	@field:CreditCardNumber
	val cardNumber: String? = null,
	@field:Size(min = 2, max = 3)
	val sizedTags: List<String>? = null,
) : HvFieldBlock

/** Twin of Validata identity slice. */
data class HvIdentityBlock(
	@field:Pattern(regexp = "^[A-Za-z0-9._/-]+$")
	val filePath: String? = null,
	@field:EAN(type = EAN.Type.EAN8)
	val eanCode: String? = null,
	@field:ISBN(type = ISBN.Type.ISBN_13)
	val isbnCode: String? = null,
	@field:IpAddress
	val ipAddress: String? = null,
	@field:DurationMax(hours = 1)
	val maxDuration: Duration? = null,
) : HvFieldBlock

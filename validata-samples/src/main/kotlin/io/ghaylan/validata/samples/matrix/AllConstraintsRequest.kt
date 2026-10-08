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
package io.ghaylan.validata.samples.matrix

import com.fasterxml.jackson.annotation.JsonProperty
import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.openapi.presentation.ApiError
import io.ghaylan.validata.samples.constraint.EmailOrPhone
import io.ghaylan.validata.samples.constraint.OddYears
import io.ghaylan.validata.samples.error.SampleApiErrors
import io.ghaylan.validata.schema.Validatable
import java.math.BigDecimal
import java.time.*

/**
 * Kitchen-sink body DTO: every built-in constraint annotation plus [OddYears].
 *
 * Proven through [AllConstraintsController] (HTTP) and [ValidatorEngine.validate] (standalone).
 * [createOnlyNote] is active only under [OnCreate] (skipped on the default HTTP [OnDefault] path).
 *
 * @property label Required, 2–64 characters
 * @property tags Optional; [Distinct] values when present
 * @property aliases Required list size 2–3 (no uniqueness)
 * @property meta Required map size 2–3
 * @property codes Required list size 2–3
 * @property items Optional; unique [AllConstraintsLineItem.code]
 * @property secret Required; must equal [confirmSecret]
 * @property confirmSecret Required equal-to peer of [secret]
 * @property username Required; must not equal [secret]
 * @property contactType Required `EMAIL` or `PHONE`
 * @property phoneBackup Required when [contactType] is PHONE; `@Phone`
 * @property minBound Required lower peer for [maxBound]
 * @property maxBound Required; strictly greater than [minBound]
 * @property midBound Required; strictly less than [maxBound]
 * @property tier Required `A`/`B`/`C`
 * @property nickname Required; must not be `banned`
 * @property email Required email
 * @property phone Required phone
 * @property contact Required `@EmailOrPhone` (email ∨ phone)
 * @property website Required URL
 * @property password Required password policy (never echoed in messages)
 * @property zip Required five-digit ZIP ([ZIP_PATTERN])
 * @property notes Required; must contain `approved` ([NOTES_CONTAINS_APPROVED_PATTERN])
 * @property cleanNotes Required; must not contain `forbidden` ([CLEAN_NOTES_NO_FORBIDDEN_PATTERN])
 * @property html Required Html
 * @property color Required hex color
 * @property card Required PAN (test numbers only)
 * @property iban Required IBAN via `@FinancialCode`
 * @property country Required ISO country
 * @property currency Required ISO currency
 * @property language Required language tag
 * @property latitude Required geo latitude
 * @property longitude Required geo longitude
 * @property quantity Required `@Min("5")`
 * @property score Required `@Max("100.0")`
 * @property ageBand Required `@Range(from = "18", to = "120")`
 * @property multiple Required `@MultipleOf("5.0")`
 * @property positiveQty Required `@NumberSign(POSITIVE)`
 * @property negativeDelta Required `@NumberSign(NEGATIVE)`
 * @property evenCount Required `@NumberParity(EVEN)`
 * @property oddCount Required `@NumberParity(ODD)`
 * @property money Required `@Digits(integer = 5, fraction = 2)`
 * @property ean Required `@Barcode(EAN)`
 * @property clientIp Required `@IpAddress(V4)`
 * @property payloadB64 Required `@Base64`
 * @property contentType Required `@In(image/png|image/jpeg)`
 * @property uploadPath Required `@FilePath` relative path
 * @property termsAccepted Required `@Assert(value = true)`
 * @property isDraft Required `@Assert(value = false)`
 * @property bornOn Required `@RelativeToNow(LT)`
 * @property appointmentOn Required `@RelativeToNow(GT)`
 * @property windowStart Required `@Min("2020-02-29")`
 * @property windowEnd Required `@Max("2035-12-31")`
 * @property meetingOn Required Monday or Wednesday ([DaysOfWeek])
 * @property billingOn Required day 1 or 15 ([DaysOfMonth])
 * @property seasonStart Required June–August ([Months])
 * @property earliestMonth Required `@Min("MARCH")`
 * @property midYearCutoff Required `@Max("--06-30")`
 * @property earliestYearMonth Required `@Min("2020-01")`
 * @property minTenure Required `@Min("P1Y")`
 * @property maxWait Required `@Max("PT2H")`
 * @property createOnlyNote Required only under [OnCreate]
 * @property oddYear Required [OddYears]
 *
 * @author Ghaylan Saada
 */
@Validatable
data class AllConstraintsRequest(
	@Required
	@Size(min = 2, max = 64)
	val label: String?,
	
	val tags: List<@Distinct String>?,
	
	@Required
	@Size(min = 2, max = 3)
	val aliases: List<String>?,
	
	@Required
	@Size(min = 2, max = 3)
	val meta: Map<String, String>?,
	
	@Required
	@Size(min = 2, max = 3)
	val codes: List<String>?,
	
	val items: List<@Distinct(by = ["code"]) AllConstraintsLineItem>?,
	
	@Required
	@Compare(ref = "confirmSecret", operation = Compare.Operation.EQ)
	val secret: String?,
	
	@Required
	@JsonProperty("confirm_secret")
	val confirmSecret: String?,
	
	@Required
	@Compare(ref = "secret", operation = Compare.Operation.NE)
	val username: String?,
	
	@Required
	@In(values = ["EMAIL", "PHONE"])
	val contactType: ContactType?,
	
	@ApiError(code = "PHONE_BACKUP_REQUIRED", catalog = SampleApiErrors::class)
	@Phone
	@RequiredWhen(
		ref = "contactType",
		condition = RequiredWhen.Condition.EQ,
		value = "PHONE")
	val phoneBackup: String?,
	
	@Required
	val minBound: Int?,
	
	@Required
	@Compare(ref = "minBound", operation = Compare.Operation.GT)
	val maxBound: Int?,
	
	@Required
	@Compare(ref = "maxBound", operation = Compare.Operation.LT)
	val midBound: Int?,
	
	@Required
	@In(values = ["A", "B", "C"])
	val tier: String?,
	
	@Required
	@NotIn(values = ["banned"])
	val nickname: String?,
	
	@Required
	@Email
	val email: String?,
	
	@Required
	@Phone
	val phone: String?,
	
	@Required
	@EmailOrPhone
	val contact: String?,
	
	@Required
	@Url
	val website: String?,
	
	@Required
	@Password(
		minLength = 8,
		requireUppercase = true,
		requireLowercase = true,
		requireDigit = true,
		requireSpecialChar = true)
	val password: String?,
	
	@Required
	@Regex(pattern = ZIP_PATTERN, name = "ZIP")
	val zip: String?,
	
	@Required
	@Regex(pattern = NOTES_CONTAINS_APPROVED_PATTERN, name = "APPROVED")
	val notes: String?,
	
	@Required
	@Regex(pattern = CLEAN_NOTES_NO_FORBIDDEN_PATTERN, name = "NO_FORBIDDEN")
	val cleanNotes: String?,
	
	@Required
	@Html(
		allowedTags = ["b", "i", "u", "p", "ul", "li", "a", "span", "strong"],
		allowedAttrs = ["a:href"],
		allowedProtocols = ["a:href:http,https"])
	val html: String?,
	
	@Required
	@HexColor
	val color: String?,
	
	@Required
	@CreditCard
	val card: String?,
	
	@Required
	@FinancialCode(type = FinancialCode.Type.IBAN)
	val iban: String?,
	
	@Required
	@IsoCountry
	val country: String?,
	
	@Required
	@IsoCurrency
	val currency: String?,
	
	@Required
	@IsoLanguage
	val language: String?,
	
	@Required
	@Coordinate(axis = Coordinate.Axis.LATITUDE)
	val latitude: Double?,
	
	@Required
	@Coordinate(axis = Coordinate.Axis.LONGITUDE)
	val longitude: Double?,
	
	@Required
	@Min("5")
	val quantity: Int?,
	
	@Required
	@Max("100.0")
	val score: Int?,
	
	@Required
	@Range(from = "18", to = "120")
	val ageBand: Int?,
	
	@Required
	@MultipleOf("5.0")
	val multiple: Int?,
	
	@Required
	@NumberSign(sign = NumberSign.Sign.POSITIVE)
	val positiveQty: Int?,
	
	@Required
	@NumberSign(sign = NumberSign.Sign.NEGATIVE)
	val negativeDelta: Int?,
	
	@Required
	@NumberParity(value = NumberParity.Value.EVEN)
	val evenCount: Int?,
	
	@Required
	@NumberParity(value = NumberParity.Value.ODD)
	val oddCount: Int?,
	
	@Required
	@Digits(integer = 5, fraction = 2)
	val money: BigDecimal?,
	
	@Required
	@Barcode(type = Barcode.Type.EAN)
	val ean: String?,
	
	@Required
	@IpAddress(type = IpAddress.Type.V4)
	val clientIp: String?,
	
	@Required
	@Base64
	val payloadB64: String?,
	
	@Required
	@In(values = ["image/png", "image/jpeg"])
	val contentType: String?,
	
	@Required
	@FilePath
	val uploadPath: String?,
	
	@Required
	@Assert(value = true)
	val termsAccepted: Boolean?,
	
	@Required
	@Assert(value = false)
	val isDraft: Boolean?,
	
	@Required
	@RelativeToNow(relation = RelativeToNow.Relation.LT)
	val bornOn: LocalDate?,
	
	@Required
	@RelativeToNow(relation = RelativeToNow.Relation.GT)
	val appointmentOn: LocalDate?,
	
	@Required
	@Min("2020-02-29")
	val windowStart: LocalDate?,
	
	@Required
	@Max(value = "2035-12-31", inclusive = true)
	val windowEnd: LocalDate?,
	
	@Required
	@DaysOfWeek(days = [DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY])
	val meetingOn: LocalDate?,
	
	@Required
	@DaysOfMonth(days = [1, 15])
	val billingOn: LocalDate?,
	
	@Required
	@Months(months = [Month.JUNE, Month.JULY, Month.AUGUST])
	val seasonStart: LocalDate?,
	
	@Required
	@Min("MARCH")
	val earliestMonth: Month?,
	
	@Required
	@Max("--06-30")
	val midYearCutoff: MonthDay?,
	
	@Required
	@Min("2020-01")
	val earliestYearMonth: YearMonth?,
	
	@Required
	@Min("P1Y")
	val minTenure: Period?,
	
	@Required
	@Max("PT2H")
	val maxWait: Duration?,
	
	@Required(groups = [OnCreate::class])
	val createOnlyNote: String?,
	
	@Required
	@OddYears
	val oddYear: Int?
) {
	
	/**
	 * Gate for [phoneBackup] `@RequiredWhen`.
	 */
	enum class ContactType {
		
		/**
		 * Phone contact — [phoneBackup] becomes required.
		 */
		PHONE,
		
		/**
		 * Email contact — [phoneBackup] may be null.
		 */
		EMAIL
	}
	
	companion object {
		
		/**
		 * Max characters on each side of a substring regex. Bounds the sample patterns so they
		 * cannot catastrophically backtrack on unbounded input.
		 */
		const val REGEX_SEGMENT_MAX_CHARS: Int = 500
		
		/**
		 * US ZIP: exactly five digits.
		 */
		const val ZIP_PATTERN: String = "^[0-9]{5}$"
		
		/**
		 * Notes must contain `approved`. Each side of the literal is capped at
		 * [REGEX_SEGMENT_MAX_CHARS] — no unbounded `.*`.
		 */
		const val NOTES_CONTAINS_APPROVED_PATTERN: String =
			"^[\\s\\S]{0,$REGEX_SEGMENT_MAX_CHARS}approved[\\s\\S]{0,$REGEX_SEGMENT_MAX_CHARS}$"
		
		/**
		 * Notes must not contain `forbidden`. Possessive-style per-character negative lookahead,
		 * length-capped at [REGEX_SEGMENT_MAX_CHARS].
		 */
		const val CLEAN_NOTES_NO_FORBIDDEN_PATTERN: String =
			"^(?:(?!forbidden)[\\s\\S]){0,$REGEX_SEGMENT_MAX_CHARS}$"
	}
}
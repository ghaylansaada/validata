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

import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import java.math.BigDecimal
import java.time.*

/**
 * Kitchen-sink payload helpers for [AllConstraintsIT] (and a few smoke cases in
 * [AllConstraintsEngineTest]).
 *
 * Keeps valid defaults and single-field mutations in one place so HTTP acceptance cases stay
 * consistent.*
 * 
 * @author Ghaylan Saada
 */
object AllConstraintsFixtures {
	
	/**
	 * A payload that satisfies every default-group constraint on [AllConstraintsRequest].
	 *
	 * [AllConstraintsRequest.createOnlyNote] may be `null` here — it is only required under
	 * [OnCreate].
	 *
	 * @return a request whose defaults pass every [OnDefault] constraint; each parameter overrides
	 *   the corresponding [AllConstraintsRequest] property	 
	 */
	fun valid(
		label: String? = "kitchen-sink",
		tags: List<String>? = listOf("alpha", "beta"),
		aliases: List<String>? = listOf("aka", "also"),
		meta: Map<String, String>? = mapOf("k" to "v", "k2" to "v2"),
		codes: List<String>? = listOf("A", "B"),
		items: List<AllConstraintsLineItem>? = listOf(
			AllConstraintsLineItem("SKU-1", "One"),
			AllConstraintsLineItem("SKU-2", "Two"),
		),
		secret: String? = "match-me",
		confirmSecret: String? = "match-me",
		username: String? = "ada",
		contactType: AllConstraintsRequest.ContactType? = AllConstraintsRequest.ContactType.EMAIL,
		phoneBackup: String? = null,
		minBound: Int? = 10,
		maxBound: Int? = 20,
		midBound: Int? = 15,
		tier: String? = "B",
		nickname: String? = "ada",
		email: String? = "ada@example.com",
		phone: String? = "14155552671",
		contact: String? = "ada@example.com",
		website: String? = "https://example.com/path",
		password: String? = "SecurePass1!",
		zip: String? = "90210",
		notes: String? = "ticket approved for release",
		cleanNotes: String? = "all clear",
		html: String? = "<p>Hello <b>world</b></p>",
		color: String? = "#FF00AA",
		card: String? = "4111111111111111",
		iban: String? = "DE89370400440532013000",
		country: String? = "US",
		currency: String? = "USD",
		language: String? = "en",
		latitude: Double? = 40.7128,
		longitude: Double? = -74.006,
		quantity: Int? = 10,
		score: Int? = 88,
		ageBand: Int? = 35,
		multiple: Int? = 25,
		positiveQty: Int? = 7,
		negativeDelta: Int? = -3,
		evenCount: Int? = 4,
		oddCount: Int? = 5,
		money: BigDecimal? = BigDecimal("123.45"),
		ean: String? = "96385074",
		clientIp: String? = "192.0.2.1",
		payloadB64: String? = "SGVsbG8=",
		contentType: String? = "image/png",
		uploadPath: String? = "uploads/photo.png",
		termsAccepted: Boolean? = true,
		isDraft: Boolean? = false,
		bornOn: LocalDate? = LocalDate.of(2000, 1, 15),
		appointmentOn: LocalDate? = LocalDate.now()
			.plusYears(1),
		windowStart: LocalDate? = LocalDate.of(2021, 6, 1),
		windowEnd: LocalDate? = LocalDate.of(2030, 6, 1),
		meetingOn: LocalDate? = LocalDate.of(2026, 8, 3),
		billingOn: LocalDate? = LocalDate.of(2026, 8, 15),
		seasonStart: LocalDate? = LocalDate.of(2026, 6, 1),
		earliestMonth: Month? = Month.MARCH,
		midYearCutoff: MonthDay? = MonthDay.of(6, 15),
		earliestYearMonth: YearMonth? = YearMonth.of(2021, 6),
		minTenure: Period? = Period.ofYears(1),
		maxWait: Duration? = Duration.ofHours(1),
		createOnlyNote: String? = null,
		oddYear: Int? = 2025,
	): AllConstraintsRequest = AllConstraintsRequest(
		label = label,
		tags = tags,
		aliases = aliases,
		meta = meta,
		codes = codes,
		items = items,
		secret = secret,
		confirmSecret = confirmSecret,
		username = username,
		contactType = contactType,
		phoneBackup = phoneBackup,
		minBound = minBound,
		maxBound = maxBound,
		midBound = midBound,
		tier = tier,
		nickname = nickname,
		email = email,
		phone = phone,
		contact = contact,
		website = website,
		password = password,
		zip = zip,
		notes = notes,
		cleanNotes = cleanNotes,
		html = html,
		color = color,
		card = card,
		iban = iban,
		country = country,
		currency = currency,
		language = language,
		latitude = latitude,
		longitude = longitude,
		quantity = quantity,
		score = score,
		ageBand = ageBand,
		multiple = multiple,
		positiveQty = positiveQty,
		negativeDelta = negativeDelta,
		evenCount = evenCount,
		oddCount = oddCount,
		money = money,
		ean = ean,
		clientIp = clientIp,
		payloadB64 = payloadB64,
		contentType = contentType,
		uploadPath = uploadPath,
		termsAccepted = termsAccepted,
		isDraft = isDraft,
		bornOn = bornOn,
		appointmentOn = appointmentOn,
		windowStart = windowStart,
		windowEnd = windowEnd,
		meetingOn = meetingOn,
		billingOn = billingOn,
		seasonStart = seasonStart,
		earliestMonth = earliestMonth,
		midYearCutoff = midYearCutoff,
		earliestYearMonth = earliestYearMonth,
		minTenure = minTenure,
		maxWait = maxWait,
		createOnlyNote = createOnlyNote,
		oddYear = oddYear,
	)
	
	/**
	 * Failure cases for [AllConstraintsIT] — one `(path, code)` mutation per built-in (and OddYears).
	 *
	 * HTTP serializes [AllConstraintsViolation.payload] with the application JSON mapper.	 
	 */
	fun violations(): List<AllConstraintsViolation> = listOf(
		AllConstraintsViolation("Required · label missing", valid(label = null), "label", "VALUE_MISSING"),
		AllConstraintsViolation("Size · label too short", valid(label = "x"), "label", "TEXT_TOO_SHORT"),
		AllConstraintsViolation("Size · label too long", valid(label = "x".repeat(65)), "label", "TEXT_TOO_LONG"),
		AllConstraintsViolation("Distinct · duplicate tags", valid(tags = listOf("dup", "dup")), "tags[0]", "COLLECTION_DUPLICATE"),
		AllConstraintsViolation(
			"Size · aliases too many",
			valid(aliases = listOf("a", "b", "c", "d")),
			"aliases",
			"COLLECTION_TOO_LARGE",
		),
		AllConstraintsViolation(
			"Size · aliases too few",
			valid(aliases = listOf("only")),
			"aliases",
			"COLLECTION_TOO_SMALL",
		),
		AllConstraintsViolation(
			"Size · meta too few entries",
			valid(meta = mapOf("k" to "v")),
			"meta",
			"OBJECT_TOO_SMALL",
		),
		AllConstraintsViolation(
			"Size · codes too few",
			valid(codes = listOf("A")),
			"codes",
			"COLLECTION_TOO_SMALL",
		),
		AllConstraintsViolation(
			"Distinct(by) · duplicate item codes",
			valid(
				items = listOf(
					AllConstraintsLineItem("SKU-1", "One"),
					AllConstraintsLineItem("SKU-1", "Other"),
				),
			),
			"items[0]",
			"COLLECTION_DUPLICATE",
		),
		AllConstraintsViolation(
			"EqualTo · confirmSecret mismatch",
			valid(confirmSecret = "nope"),
			"secret",
			"COMPARISON_UNSATISFIED_NOT_EQUAL",
		),
		AllConstraintsViolation(
			"NotEqualTo · username equals secret",
			valid(username = "match-me"),
			"username",
			"COMPARISON_UNSATISFIED_EQUAL",
		),
		AllConstraintsViolation(
			"RequiredWhen · phoneBackup missing when contactType=PHONE",
			valid(contactType = AllConstraintsRequest.ContactType.PHONE, phoneBackup = null),
			"phoneBackup",
			"VALUE_MISSING",
		),
		AllConstraintsViolation("GreaterThan · maxBound not greater", valid(maxBound = 10), "maxBound", "COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL"),
		AllConstraintsViolation("LessThan · midBound not less", valid(midBound = 20), "midBound", "COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL"),
		AllConstraintsViolation("@In · tier not allowed", valid(tier = "Z"), "tier", "VALUE_NOT_ALLOWED"),
		AllConstraintsViolation("@NotIn · nickname banned", valid(nickname = "banned"), "nickname", "VALUE_NOT_ALLOWED"),
		AllConstraintsViolation("Email · invalid format", valid(email = "not-an-email"), "email", "VALUE_FORMAT_INVALID"),
		AllConstraintsViolation("Phone · invalid format", valid(phone = "not-a-phone"), "phone", "VALUE_FORMAT_INVALID"),
		AllConstraintsViolation(
			"EmailOrPhone · neither email nor phone",
			valid(contact = "not-contact"),
			"contact",
			"CONSTRAINT_UNSATISFIABLE",
		),
		AllConstraintsViolation("Url · invalid format", valid(website = "not a url"), "website", "VALUE_FORMAT_INVALID"),
		AllConstraintsViolation(
			"Password · missing uppercase",
			valid(password = "securepass1!"),
			"password",
			"TEXT_PATTERN_MISMATCH",
		),
		AllConstraintsViolation("Password · too short", valid(password = "Ab1!"), "password", "TEXT_TOO_SHORT"),
		AllConstraintsViolation("Regex · zip mismatch", valid(zip = "ABCDE"), "zip", "TEXT_PATTERN_MISMATCH"),
		AllConstraintsViolation(
			"Regex · notes missing approved",
			valid(notes = "pending review"),
			"notes",
			"TEXT_PATTERN_MISMATCH",
		),
		AllConstraintsViolation(
			"Regex · cleanNotes has forbidden",
			valid(cleanNotes = "forbidden word"),
			"cleanNotes",
			"TEXT_PATTERN_MISMATCH",
		),
		AllConstraintsViolation(
			"Html · script tag not allowed",
			valid(html = "<script>alert(1)</script>"),
			"html",
			"VALUE_NOT_ALLOWED",
		),
		AllConstraintsViolation("HexColor · invalid", valid(color = "red"), "color", "VALUE_FORMAT_INVALID"),
		AllConstraintsViolation(
			"CreditCard · bad checksum",
			valid(card = "4111111111111112"),
			"card",
			"VALUE_CHECKSUM_INVALID",
		),
		AllConstraintsViolation(
			"FinancialCode · IBAN bad checksum",
			valid(iban = "DE00370400440532013000"),
			"iban",
			"VALUE_CHECKSUM_INVALID",
		),
		AllConstraintsViolation("CountryCode · invalid", valid(country = "XX"), "country", "VALUE_NOT_ALLOWED"),
		AllConstraintsViolation("CurrencyCode · invalid", valid(currency = "ZZZ"), "currency", "VALUE_NOT_ALLOWED"),
		AllConstraintsViolation("LanguageCode · invalid", valid(language = "zz"), "language", "VALUE_FORMAT_INVALID"),
		AllConstraintsViolation("Latitude · out of range", valid(latitude = 91.0), "latitude", "NUMBER_OUT_OF_RANGE"),
		AllConstraintsViolation("Longitude · out of range", valid(longitude = -181.0), "longitude", "NUMBER_OUT_OF_RANGE"),
		AllConstraintsViolation("Min · quantity too small", valid(quantity = -1), "quantity", "NUMBER_TOO_SMALL"),
		AllConstraintsViolation("Max · score too large", valid(score = 101), "score", "NUMBER_TOO_LARGE"),
		AllConstraintsViolation("Range · ageBand too small", valid(ageBand = 17), "ageBand", "NUMBER_TOO_SMALL"),
		AllConstraintsViolation("Range · ageBand too large", valid(ageBand = 121), "ageBand", "NUMBER_TOO_LARGE"),
		AllConstraintsViolation("MultipleOf · not a multiple", valid(multiple = 7), "multiple", "NUMBER_NOT_MULTIPLE"),
		AllConstraintsViolation(
			"Past · bornOn in the future",
			valid(bornOn = LocalDate.now()
				.plusDays(1)),
			"bornOn",
			"TEMPORAL_NOT_IN_PAST",
		),
		AllConstraintsViolation(
			"Future · appointmentOn in the past",
			valid(appointmentOn = LocalDate.now()
				.minusDays(1)),
			"appointmentOn",
			"TEMPORAL_NOT_IN_FUTURE",
		),
		AllConstraintsViolation(
			"Min · windowStart too early",
			valid(windowStart = LocalDate.of(2019, 12, 31)),
			"windowStart",
			"TEMPORAL_TOO_EARLY",
		),
		AllConstraintsViolation(
			"Max · windowEnd too late",
			valid(windowEnd = LocalDate.of(2036, 1, 1)),
			"windowEnd",
			"TEMPORAL_TOO_LATE",
		),
		AllConstraintsViolation(
			"DayOfWeekIn · meetingOn on Sunday",
			valid(meetingOn = LocalDate.of(2026, 8, 2)),
			"meetingOn",
			"TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"DayOfMonthIn · billingOn on the 2nd",
			valid(billingOn = LocalDate.of(2026, 8, 2)),
			"billingOn",
			"TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"MonthIn · seasonStart in January",
			valid(seasonStart = LocalDate.of(2026, 1, 15)),
			"seasonStart",
			"TEMPORAL_MONTH_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"Positive · positiveQty zero",
			valid(positiveQty = 0),
			"positiveQty",
			"NUMBER_ZERO_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"Negative · negativeDelta positive",
			valid(negativeDelta = 1),
			"negativeDelta",
			"NUMBER_NOT_NEGATIVE",
		),
		AllConstraintsViolation(
			"Even · evenCount odd",
			valid(evenCount = 3),
			"evenCount",
			"NUMBER_NOT_EVEN",
		),
		AllConstraintsViolation(
			"Odd · oddCount even",
			valid(oddCount = 4),
			"oddCount",
			"NUMBER_NOT_ODD",
		),
		AllConstraintsViolation(
			"Digits · money too many fraction digits",
			valid(money = BigDecimal("1.234")),
			"money",
			"NUMBER_SCALE_EXCEEDED",
		),
		AllConstraintsViolation(
			"Barcode · ean bad checksum",
			valid(ean = "96385075"),
			"ean",
			"VALUE_CHECKSUM_INVALID",
		),
		AllConstraintsViolation(
			"IpAddress · clientIp not IPv4",
			valid(clientIp = "2001:db8::1"),
			"clientIp",
			"VALUE_FORMAT_INVALID",
		),
		AllConstraintsViolation(
			"Base64 · payloadB64 invalid",
			valid(payloadB64 = "!!!"),
			"payloadB64",
			"VALUE_FORMAT_INVALID",
		),
		AllConstraintsViolation(
			"@In · contentType not allowed",
			valid(contentType = "application/pdf"),
			"contentType",
			"VALUE_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"FilePath · uploadPath absolute",
			valid(uploadPath = "/etc/passwd"),
			"uploadPath",
			"VALUE_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"Assert · termsAccepted false",
			valid(termsAccepted = false),
			"termsAccepted",
			"VALUE_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"Assert · isDraft true",
			valid(isDraft = true),
			"isDraft",
			"VALUE_NOT_ALLOWED",
		),
		AllConstraintsViolation(
			"Min · earliestMonth before MARCH",
			valid(earliestMonth = Month.JANUARY),
			"earliestMonth",
			"TEMPORAL_TOO_EARLY",
		),
		AllConstraintsViolation(
			"Max · midYearCutoff after June 30",
			valid(midYearCutoff = MonthDay.of(7, 1)),
			"midYearCutoff",
			"TEMPORAL_TOO_LATE",
		),
		AllConstraintsViolation(
			"Min · earliestYearMonth too early",
			valid(earliestYearMonth = YearMonth.of(2019, 12)),
			"earliestYearMonth",
			"TEMPORAL_TOO_EARLY",
		),
		AllConstraintsViolation(
			"Min · minTenure too short",
			valid(minTenure = Period.ofMonths(6)),
			"minTenure",
			"TEMPORAL_DURATION_TOO_SHORT",
		),
		AllConstraintsViolation(
			"Max · maxWait too long",
			valid(maxWait = Duration.ofHours(3)),
			"maxWait",
			"TEMPORAL_DURATION_TOO_LONG",
		),
		AllConstraintsViolation("OddYears · even year", valid(oddYear = 2024), "oddYear", "YEAR_NOT_ODD"),
	)
}

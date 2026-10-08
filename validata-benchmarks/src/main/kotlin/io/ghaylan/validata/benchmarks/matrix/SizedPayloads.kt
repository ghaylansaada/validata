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
package io.ghaylan.validata.benchmarks.matrix

import io.ghaylan.validata.benchmarks.hibernate.HvExtremeRoot
import io.ghaylan.validata.benchmarks.hibernate.HvFieldBlock
import io.ghaylan.validata.benchmarks.hibernate.HvFormatBlock
import io.ghaylan.validata.benchmarks.hibernate.HvIdentityBlock
import io.ghaylan.validata.benchmarks.hibernate.HvLargeRoot
import io.ghaylan.validata.benchmarks.hibernate.HvMaximumRoot
import io.ghaylan.validata.benchmarks.hibernate.HvMediumRoot
import io.ghaylan.validata.benchmarks.hibernate.HvNumberBlock
import io.ghaylan.validata.benchmarks.hibernate.HvPresenceBlock
import io.ghaylan.validata.benchmarks.hibernate.HvSignSizeBlock
import io.ghaylan.validata.benchmarks.hibernate.HvSmallRoot
import io.ghaylan.validata.benchmarks.hibernate.HvStressRoot
import io.ghaylan.validata.benchmarks.hibernate.HvTemporalBlock
import io.ghaylan.validata.benchmarks.hibernate.HvVeryLargeRoot
import io.ghaylan.validata.benchmarks.hibernate.HvXLargeRoot
import io.ghaylan.validata.benchmarks.validata.ValidataExtremeRoot
import io.ghaylan.validata.benchmarks.validata.ValidataFieldBlock
import io.ghaylan.validata.benchmarks.validata.ValidataFormatBlock
import io.ghaylan.validata.benchmarks.validata.ValidataIdentityBlock
import io.ghaylan.validata.benchmarks.validata.ValidataLargeRoot
import io.ghaylan.validata.benchmarks.validata.ValidataMaximumRoot
import io.ghaylan.validata.benchmarks.validata.ValidataMediumRoot
import io.ghaylan.validata.benchmarks.validata.ValidataNumberBlock
import io.ghaylan.validata.benchmarks.validata.ValidataPresenceBlock
import io.ghaylan.validata.benchmarks.validata.ValidataSignSizeBlock
import io.ghaylan.validata.benchmarks.validata.ValidataSmallRoot
import io.ghaylan.validata.benchmarks.validata.ValidataStressRoot
import io.ghaylan.validata.benchmarks.validata.ValidataTemporalBlock
import io.ghaylan.validata.benchmarks.validata.ValidataVeryLargeRoot
import io.ghaylan.validata.benchmarks.validata.ValidataXLargeRoot
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

/**
 * Builds payloads for every [PayloadSize] × [InvalidityLevel] on both engines.
 *
 * Small is a heterogeneous 5-field DTO. Sizes 10–1000 cycle six 5-field constraint slices.
 * Invalid leaves are selected by [InvalidityLevel.failLeafIndices] — the same logical values
 * are supplied to Validata and Hibernate Validator.
 *
 * Dates are fixed calendar values — never `LocalDate.now()`.
 *
 * @author Ghaylan Saada
 */
object SizedPayloads {

	/** Visa test PAN that passes Luhn. Fixture only — never a real card. */
	private const val VALID_CARD = "4111111111111111"

	/** Same length as [VALID_CARD]; fails Luhn. Fixture only. */
	private const val INVALID_CARD = "4111111111111112"

	private const val VALID_FILE_PATH = "reports/2020/summary.pdf"

	private const val INVALID_FILE_PATH = ""

	/** Valid EAN-8 (GS1 check digit). */
	private const val VALID_EAN = "96385074"

	/** Same length as [VALID_EAN]; fails checksum. */
	private const val INVALID_EAN = "96385075"

	/** Valid ISBN-13 (GS1 check digit). */
	private const val VALID_ISBN = "9780306406157"

	/** Same length as [VALID_ISBN]; fails checksum / layout. */
	private const val INVALID_ISBN = "9780306406158"

	/** Ordered leaf property names for [ValidataSmallRoot] / [HvSmallRoot]. */
	val SMALL_LEAF_NAMES: List<String> = listOf("name", "age", "email", "birthDate", "tags")

	/** Ordered leaf property names per rotating slice kind (cycle index 0..5). */
	val SLICE_LEAF_NAMES: List<List<String>> = listOf(
		listOf("notBlankName", "notNullCount", "notEmptyTags", "minAge", "maxAge"),
		listOf("decimalMinPrice", "decimalMaxPrice", "digitsAmount", "positiveQty", "positiveOrZeroScore"),
		listOf("negativeDelta", "negativeOrZeroAdj", "sizedTitle", "patternCode", "email"),
		listOf("pastDate", "pastOrPresentDate", "futureDate", "futureOrPresentDate", "minDuration"),
		listOf("assertTrueFlag", "assertFalseFlag", "website", "cardNumber", "sizedTags"),
		listOf("filePath", "eanCode", "isbnCode", "ipAddress", "maxDuration"),
	)

	/**
	 * Expected leaf property names that fail for [size] at [level] (same for both engines).
	 */
	fun expectedFailLeafNames(size: PayloadSize, level: InvalidityLevel): Set<String> {
		val fails = level.failLeafIndices(size.fieldCount)
		if (size == PayloadSize.SMALL) {
			return fails.map { SMALL_LEAF_NAMES[it] }.toSet()
		}
		return fails.map { leafIndex ->
			val blockIndex = leafIndex / PayloadSize.FIELDS_PER_BLOCK
			val offset = leafIndex % PayloadSize.FIELDS_PER_BLOCK
			val sliceKind = blockIndex % PayloadSize.SLICE_KINDS
			SLICE_LEAF_NAMES[sliceKind][offset]
		}.toSet()
	}

	/**
	 * Expected full property paths that fail for [size] at [level].
	 *
	 * Small: `name`, `age`, … — Medium+: `items[i].field`.
	 */
	fun expectedFailPaths(size: PayloadSize, level: InvalidityLevel): Set<String> {
		val fails = level.failLeafIndices(size.fieldCount)
		if (size == PayloadSize.SMALL) {
			return fails.map { SMALL_LEAF_NAMES[it] }.toSet()
		}
		return fails.map { leafIndex ->
			val blockIndex = leafIndex / PayloadSize.FIELDS_PER_BLOCK
			val offset = leafIndex % PayloadSize.FIELDS_PER_BLOCK
			val sliceKind = blockIndex % PayloadSize.SLICE_KINDS
			"items[$blockIndex].${SLICE_LEAF_NAMES[sliceKind][offset]}"
		}.toSet()
	}

	private fun fail(fails: Set<Int>, leaf: Int): Boolean = leaf in fails

	private fun presence(base: Int, fails: Set<Int>): ValidataPresenceBlock = ValidataPresenceBlock(
		notBlankName = if (fail(fails, base)) "" else "Alex",
		notNullCount = if (fail(fails, base + 1)) null else 3,
		notEmptyTags = if (fail(fails, base + 2)) emptyList() else listOf("alpha", "beta"),
		minAge = if (fail(fails, base + 3)) -1 else 34,
		maxAge = if (fail(fails, base + 4)) 999 else 80,
	)

	private fun number(base: Int, fails: Set<Int>): ValidataNumberBlock = ValidataNumberBlock(
		decimalMinPrice = if (fail(fails, base)) BigDecimal.ZERO else BigDecimal("19.99"),
		decimalMaxPrice = if (fail(fails, base + 1)) BigDecimal("99999.00") else BigDecimal("100.00"),
		digitsAmount = if (fail(fails, base + 2)) BigDecimal("1234.567") else BigDecimal("12.34"),
		positiveQty = if (fail(fails, base + 3)) 0 else 4,
		positiveOrZeroScore = if (fail(fails, base + 4)) -1 else 0,
	)

	private fun signSize(base: Int, fails: Set<Int>): ValidataSignSizeBlock = ValidataSignSizeBlock(
		negativeDelta = if (fail(fails, base)) 0 else -2,
		negativeOrZeroAdj = if (fail(fails, base + 1)) 1 else 0,
		sizedTitle = if (fail(fails, base + 2)) "x" else "Lisbon",
		patternCode = if (fail(fails, base + 3)) "!!" else "TOKEN_42",
		email = if (fail(fails, base + 4)) "bad" else "alex@example.com",
	)

	private fun temporal(base: Int, fails: Set<Int>): ValidataTemporalBlock = ValidataTemporalBlock(
		pastDate = if (fail(fails, base)) LocalDate.of(2099, 1, 1) else LocalDate.of(2000, 1, 15),
		pastOrPresentDate = if (fail(fails, base + 1)) LocalDate.of(2099, 1, 1) else LocalDate.of(2020, 6, 1),
		futureDate = if (fail(fails, base + 2)) LocalDate.of(2000, 1, 1) else LocalDate.of(2099, 6, 1),
		futureOrPresentDate = if (fail(fails, base + 3)) LocalDate.of(2000, 1, 1) else LocalDate.of(2099, 12, 31),
		minDuration = if (fail(fails, base + 4)) Duration.ZERO else Duration.ofSeconds(2),
	)

	private fun format(base: Int, fails: Set<Int>): ValidataFormatBlock = ValidataFormatBlock(
		assertTrueFlag = if (fail(fails, base)) false else true,
		assertFalseFlag = if (fail(fails, base + 1)) true else false,
		website = if (fail(fails, base + 2)) "not a url" else "https://example.com/path",
		cardNumber = if (fail(fails, base + 3)) INVALID_CARD else VALID_CARD,
		sizedTags = if (fail(fails, base + 4)) listOf("only") else listOf("red", "blue"),
	)

	private fun identity(base: Int, fails: Set<Int>): ValidataIdentityBlock = ValidataIdentityBlock(
		filePath = if (fail(fails, base)) INVALID_FILE_PATH else VALID_FILE_PATH,
		eanCode = if (fail(fails, base + 1)) INVALID_EAN else VALID_EAN,
		isbnCode = if (fail(fails, base + 2)) INVALID_ISBN else VALID_ISBN,
		ipAddress = if (fail(fails, base + 3)) "not-an-ip" else "192.0.2.1",
		maxDuration = if (fail(fails, base + 4)) Duration.ofHours(2) else Duration.ofMinutes(30),
	)

	private fun toHv(src: ValidataFieldBlock): HvFieldBlock = when (src) {
		is ValidataPresenceBlock -> HvPresenceBlock(
			notBlankName = src.notBlankName,
			notNullCount = src.notNullCount,
			notEmptyTags = src.notEmptyTags,
			minAge = src.minAge,
			maxAge = src.maxAge,
		)
		is ValidataNumberBlock -> HvNumberBlock(
			decimalMinPrice = src.decimalMinPrice,
			decimalMaxPrice = src.decimalMaxPrice,
			digitsAmount = src.digitsAmount,
			positiveQty = src.positiveQty,
			positiveOrZeroScore = src.positiveOrZeroScore,
		)
		is ValidataSignSizeBlock -> HvSignSizeBlock(
			negativeDelta = src.negativeDelta,
			negativeOrZeroAdj = src.negativeOrZeroAdj,
			sizedTitle = src.sizedTitle,
			patternCode = src.patternCode,
			email = src.email,
		)
		is ValidataTemporalBlock -> HvTemporalBlock(
			pastDate = src.pastDate,
			pastOrPresentDate = src.pastOrPresentDate,
			futureDate = src.futureDate,
			futureOrPresentDate = src.futureOrPresentDate,
			minDuration = src.minDuration,
		)
		is ValidataFormatBlock -> HvFormatBlock(
			assertTrueFlag = src.assertTrueFlag,
			assertFalseFlag = src.assertFalseFlag,
			website = src.website,
			cardNumber = src.cardNumber,
			sizedTags = src.sizedTags,
		)
		is ValidataIdentityBlock -> HvIdentityBlock(
			filePath = src.filePath,
			eanCode = src.eanCode,
			isbnCode = src.isbnCode,
			ipAddress = src.ipAddress,
			maxDuration = src.maxDuration,
		)
	}

	private fun validataBlocks(count: Int, level: InvalidityLevel): List<ValidataFieldBlock> {
		val fails = level.failLeafIndices(count * PayloadSize.FIELDS_PER_BLOCK)
		return List(count) { blockIndex ->
			val base = blockIndex * PayloadSize.FIELDS_PER_BLOCK
			when (blockIndex % PayloadSize.SLICE_KINDS) {
				0 -> presence(base, fails)
				1 -> number(base, fails)
				2 -> signSize(base, fails)
				3 -> temporal(base, fails)
				4 -> format(base, fails)
				else -> identity(base, fails)
			}
		}
	}

	private fun hvBlocks(count: Int, level: InvalidityLevel): List<HvFieldBlock> =
		validataBlocks(count, level).map(::toHv)

	private fun smallRoot(level: InvalidityLevel): ValidataSmallRoot {
		val fails = level.failLeafIndices(PayloadSize.SMALL.fieldCount)
		return ValidataSmallRoot(
			name = if (fail(fails, 0)) "" else "Alex",
			age = if (fail(fails, 1)) -1 else 34,
			email = if (fail(fails, 2)) "bad" else "alex@example.com",
			birthDate = if (fail(fails, 3)) LocalDate.of(2099, 1, 1) else LocalDate.of(2000, 1, 15),
			tags = if (fail(fails, 4)) emptyList() else listOf("alpha", "beta"),
		)
	}

	private fun hvSmallRoot(level: InvalidityLevel): HvSmallRoot {
		val src = smallRoot(level)
		return HvSmallRoot(
			name = src.name,
			age = src.age,
			email = src.email,
			birthDate = src.birthDate,
			tags = src.tags,
		)
	}

	// --- Small ---

	fun customSmall(level: InvalidityLevel): ValidataSmallRoot = smallRoot(level)
	fun hvSmall(level: InvalidityLevel): HvSmallRoot = hvSmallRoot(level)

	fun customSmallValid(): ValidataSmallRoot = customSmall(InvalidityLevel.VALID)
	fun customSmallQuarterInvalid(): ValidataSmallRoot = customSmall(InvalidityLevel.QUARTER)
	fun customSmallHalfInvalid(): ValidataSmallRoot = customSmall(InvalidityLevel.HALF)
	fun customSmallAllInvalid(): ValidataSmallRoot = customSmall(InvalidityLevel.ALL)

	fun hvSmallValid(): HvSmallRoot = hvSmall(InvalidityLevel.VALID)
	fun hvSmallQuarterInvalid(): HvSmallRoot = hvSmall(InvalidityLevel.QUARTER)
	fun hvSmallHalfInvalid(): HvSmallRoot = hvSmall(InvalidityLevel.HALF)
	fun hvSmallAllInvalid(): HvSmallRoot = hvSmall(InvalidityLevel.ALL)

	// --- Medium ---

	fun customMedium(level: InvalidityLevel): ValidataMediumRoot =
		ValidataMediumRoot(validataBlocks(PayloadSize.MEDIUM.blockCount, level))

	fun hvMedium(level: InvalidityLevel): HvMediumRoot =
		HvMediumRoot(hvBlocks(PayloadSize.MEDIUM.blockCount, level))

	fun customMediumValid(): ValidataMediumRoot = customMedium(InvalidityLevel.VALID)
	fun customMediumQuarterInvalid(): ValidataMediumRoot = customMedium(InvalidityLevel.QUARTER)
	fun customMediumHalfInvalid(): ValidataMediumRoot = customMedium(InvalidityLevel.HALF)
	fun customMediumAllInvalid(): ValidataMediumRoot = customMedium(InvalidityLevel.ALL)

	fun hvMediumValid(): HvMediumRoot = hvMedium(InvalidityLevel.VALID)
	fun hvMediumQuarterInvalid(): HvMediumRoot = hvMedium(InvalidityLevel.QUARTER)
	fun hvMediumHalfInvalid(): HvMediumRoot = hvMedium(InvalidityLevel.HALF)
	fun hvMediumAllInvalid(): HvMediumRoot = hvMedium(InvalidityLevel.ALL)

	// --- Large ---

	fun customLarge(level: InvalidityLevel): ValidataLargeRoot =
		ValidataLargeRoot(validataBlocks(PayloadSize.LARGE.blockCount, level))

	fun hvLarge(level: InvalidityLevel): HvLargeRoot =
		HvLargeRoot(hvBlocks(PayloadSize.LARGE.blockCount, level))

	fun customLargeValid(): ValidataLargeRoot = customLarge(InvalidityLevel.VALID)
	fun customLargeQuarterInvalid(): ValidataLargeRoot = customLarge(InvalidityLevel.QUARTER)
	fun customLargeHalfInvalid(): ValidataLargeRoot = customLarge(InvalidityLevel.HALF)
	fun customLargeAllInvalid(): ValidataLargeRoot = customLarge(InvalidityLevel.ALL)

	fun hvLargeValid(): HvLargeRoot = hvLarge(InvalidityLevel.VALID)
	fun hvLargeQuarterInvalid(): HvLargeRoot = hvLarge(InvalidityLevel.QUARTER)
	fun hvLargeHalfInvalid(): HvLargeRoot = hvLarge(InvalidityLevel.HALF)
	fun hvLargeAllInvalid(): HvLargeRoot = hvLarge(InvalidityLevel.ALL)

	// --- XLarge ---

	fun customXLarge(level: InvalidityLevel): ValidataXLargeRoot =
		ValidataXLargeRoot(validataBlocks(PayloadSize.XLARGE.blockCount, level))

	fun hvXLarge(level: InvalidityLevel): HvXLargeRoot =
		HvXLargeRoot(hvBlocks(PayloadSize.XLARGE.blockCount, level))

	fun customXLargeValid(): ValidataXLargeRoot = customXLarge(InvalidityLevel.VALID)
	fun customXLargeQuarterInvalid(): ValidataXLargeRoot = customXLarge(InvalidityLevel.QUARTER)
	fun customXLargeHalfInvalid(): ValidataXLargeRoot = customXLarge(InvalidityLevel.HALF)
	fun customXLargeAllInvalid(): ValidataXLargeRoot = customXLarge(InvalidityLevel.ALL)

	fun hvXLargeValid(): HvXLargeRoot = hvXLarge(InvalidityLevel.VALID)
	fun hvXLargeQuarterInvalid(): HvXLargeRoot = hvXLarge(InvalidityLevel.QUARTER)
	fun hvXLargeHalfInvalid(): HvXLargeRoot = hvXLarge(InvalidityLevel.HALF)
	fun hvXLargeAllInvalid(): HvXLargeRoot = hvXLarge(InvalidityLevel.ALL)

	// --- Very Large ---

	fun customVeryLarge(level: InvalidityLevel): ValidataVeryLargeRoot =
		ValidataVeryLargeRoot(validataBlocks(PayloadSize.VERY_LARGE.blockCount, level))

	fun hvVeryLarge(level: InvalidityLevel): HvVeryLargeRoot =
		HvVeryLargeRoot(hvBlocks(PayloadSize.VERY_LARGE.blockCount, level))

	fun customVeryLargeValid(): ValidataVeryLargeRoot = customVeryLarge(InvalidityLevel.VALID)
	fun customVeryLargeQuarterInvalid(): ValidataVeryLargeRoot = customVeryLarge(InvalidityLevel.QUARTER)
	fun customVeryLargeHalfInvalid(): ValidataVeryLargeRoot = customVeryLarge(InvalidityLevel.HALF)
	fun customVeryLargeAllInvalid(): ValidataVeryLargeRoot = customVeryLarge(InvalidityLevel.ALL)

	fun hvVeryLargeValid(): HvVeryLargeRoot = hvVeryLarge(InvalidityLevel.VALID)
	fun hvVeryLargeQuarterInvalid(): HvVeryLargeRoot = hvVeryLarge(InvalidityLevel.QUARTER)
	fun hvVeryLargeHalfInvalid(): HvVeryLargeRoot = hvVeryLarge(InvalidityLevel.HALF)
	fun hvVeryLargeAllInvalid(): HvVeryLargeRoot = hvVeryLarge(InvalidityLevel.ALL)

	// --- Extreme ---

	fun customExtreme(level: InvalidityLevel): ValidataExtremeRoot =
		ValidataExtremeRoot(validataBlocks(PayloadSize.EXTREME.blockCount, level))

	fun hvExtreme(level: InvalidityLevel): HvExtremeRoot =
		HvExtremeRoot(hvBlocks(PayloadSize.EXTREME.blockCount, level))

	fun customExtremeValid(): ValidataExtremeRoot = customExtreme(InvalidityLevel.VALID)
	fun customExtremeQuarterInvalid(): ValidataExtremeRoot = customExtreme(InvalidityLevel.QUARTER)
	fun customExtremeHalfInvalid(): ValidataExtremeRoot = customExtreme(InvalidityLevel.HALF)
	fun customExtremeAllInvalid(): ValidataExtremeRoot = customExtreme(InvalidityLevel.ALL)

	fun hvExtremeValid(): HvExtremeRoot = hvExtreme(InvalidityLevel.VALID)
	fun hvExtremeQuarterInvalid(): HvExtremeRoot = hvExtreme(InvalidityLevel.QUARTER)
	fun hvExtremeHalfInvalid(): HvExtremeRoot = hvExtreme(InvalidityLevel.HALF)
	fun hvExtremeAllInvalid(): HvExtremeRoot = hvExtreme(InvalidityLevel.ALL)

	// --- Stress ---

	fun customStress(level: InvalidityLevel): ValidataStressRoot =
		ValidataStressRoot(validataBlocks(PayloadSize.STRESS.blockCount, level))

	fun hvStress(level: InvalidityLevel): HvStressRoot =
		HvStressRoot(hvBlocks(PayloadSize.STRESS.blockCount, level))

	fun customStressValid(): ValidataStressRoot = customStress(InvalidityLevel.VALID)
	fun customStressQuarterInvalid(): ValidataStressRoot = customStress(InvalidityLevel.QUARTER)
	fun customStressHalfInvalid(): ValidataStressRoot = customStress(InvalidityLevel.HALF)
	fun customStressAllInvalid(): ValidataStressRoot = customStress(InvalidityLevel.ALL)

	fun hvStressValid(): HvStressRoot = hvStress(InvalidityLevel.VALID)
	fun hvStressQuarterInvalid(): HvStressRoot = hvStress(InvalidityLevel.QUARTER)
	fun hvStressHalfInvalid(): HvStressRoot = hvStress(InvalidityLevel.HALF)
	fun hvStressAllInvalid(): HvStressRoot = hvStress(InvalidityLevel.ALL)

	// --- Maximum ---

	fun customMaximum(level: InvalidityLevel): ValidataMaximumRoot =
		ValidataMaximumRoot(validataBlocks(PayloadSize.MAXIMUM.blockCount, level))

	fun hvMaximum(level: InvalidityLevel): HvMaximumRoot =
		HvMaximumRoot(hvBlocks(PayloadSize.MAXIMUM.blockCount, level))

	fun customMaximumValid(): ValidataMaximumRoot = customMaximum(InvalidityLevel.VALID)
	fun customMaximumQuarterInvalid(): ValidataMaximumRoot = customMaximum(InvalidityLevel.QUARTER)
	fun customMaximumHalfInvalid(): ValidataMaximumRoot = customMaximum(InvalidityLevel.HALF)
	fun customMaximumAllInvalid(): ValidataMaximumRoot = customMaximum(InvalidityLevel.ALL)

	fun hvMaximumValid(): HvMaximumRoot = hvMaximum(InvalidityLevel.VALID)
	fun hvMaximumQuarterInvalid(): HvMaximumRoot = hvMaximum(InvalidityLevel.QUARTER)
	fun hvMaximumHalfInvalid(): HvMaximumRoot = hvMaximum(InvalidityLevel.HALF)
	fun hvMaximumAllInvalid(): HvMaximumRoot = hvMaximum(InvalidityLevel.ALL)
}

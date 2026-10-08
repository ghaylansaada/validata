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
import io.ghaylan.validata.benchmarks.hibernate.HvHarness
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
import io.ghaylan.validata.benchmarks.validata.ValidataHarness
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.reflect.full.memberProperties

/**
 * Correctness gate: both engines validate the same logical fixtures for every
 * [PayloadSize] × [InvalidityLevel], with matching outcomes, violation counts, and property paths.
 *
 * @author Ghaylan Saada
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchFixtureParityTest {

	private lateinit var custom: ValidataHarness
	private lateinit var hv: HvHarness

	@BeforeAll
	fun setUp() {
		custom = ValidataHarness()
		hv = HvHarness()
	}

	@AfterAll
	fun tearDown() {
		custom.close()
		hv.close()
	}

	@ParameterizedTest(name = "{0} × {1}")
	@MethodSource("matrixCells")
	@DisplayName("every size × invalidity: outcome, count, and paths match")
	fun matrixParity(size: PayloadSize, level: InvalidityLevel) {
		val customTarget = customPayload(size, level)
		val hvTarget = hvPayload(size, level)

		val customErrors = custom.validate(customTarget)
		val hvViolations = hv.validate(hvTarget)

		val expectedPaths = SizedPayloads.expectedFailPaths(size, level)
		val expectedFailCount = level.failLeafIndices(size.fieldCount).size

		if (level == InvalidityLevel.VALID) {
			assertThat(customErrors).isEmpty()
			assertThat(hvViolations).isEmpty()
			return
		}

		assertThat(customErrors).isNotEmpty
		assertThat(hvViolations).isNotEmpty

		val customPaths = customErrors.mapNotNull { it.path }.toSet()
		val hvPaths = hvViolations.map { it.propertyPath.toString() }.toSet()

		assertThat(customPaths)
			.describedAs("Validata paths for $size × $level")
			.isEqualTo(expectedPaths)
		assertThat(hvPaths)
			.describedAs("HV paths for $size × $level")
			.isEqualTo(expectedPaths)

		assertThat(customErrors)
			.describedAs("Validata violation count for $size × $level (one per intended fail leaf)")
			.hasSize(expectedFailCount)
		assertThat(hvViolations)
			.describedAs("HV violation count for $size × $level (one per intended fail leaf)")
			.hasSize(expectedFailCount)

		assertThat(customPaths).isEqualTo(hvPaths)
	}

	@Test
	@DisplayName("invalidity plan: floor percents and even spacing")
	fun invalidityPlan() {
		assertThat(InvalidityLevel.failLeafIndices(5, 0)).isEmpty()
		assertThat(InvalidityLevel.failLeafIndices(5, 25)).containsExactlyInAnyOrder(0)
		assertThat(InvalidityLevel.failLeafIndices(5, 50)).containsExactlyInAnyOrder(0, 2)
		assertThat(InvalidityLevel.failLeafIndices(5, 100)).containsExactlyInAnyOrder(0, 1, 2, 3, 4)

		assertThat(InvalidityLevel.failLeafIndices(10, 25)).containsExactlyInAnyOrder(0, 5)
		assertThat(InvalidityLevel.failLeafIndices(10, 50)).containsExactlyInAnyOrder(0, 2, 4, 6, 8)

		assertThat(InvalidityLevel.failLeafIndices(25, 25)).hasSize(6)
		assertThat(InvalidityLevel.failLeafIndices(25, 50)).hasSize(12)
	}

	@Test
	@DisplayName("field-block twins expose the same properties and documented field counts")
	fun fieldCounts() {
		val customSlices = listOf(
			ValidataPresenceBlock::class,
			ValidataNumberBlock::class,
			ValidataSignSizeBlock::class,
			ValidataTemporalBlock::class,
			ValidataFormatBlock::class,
			ValidataIdentityBlock::class,
		)
		val hvSlices = listOf(
			HvPresenceBlock::class,
			HvNumberBlock::class,
			HvSignSizeBlock::class,
			HvTemporalBlock::class,
			HvFormatBlock::class,
			HvIdentityBlock::class,
		)
		assertThat(customSlices).hasSize(PayloadSize.SLICE_KINDS)
		assertThat(hvSlices).hasSize(PayloadSize.SLICE_KINDS)
		customSlices.zip(hvSlices).forEach { (customType, hvType) ->
			val customNames = customType.memberProperties.map { it.name }.toSet()
			val hvNames = hvType.memberProperties.map { it.name }.toSet()
			assertThat(customNames).hasSize(PayloadSize.FIELDS_PER_BLOCK)
			assertThat(hvNames).isEqualTo(customNames)
		}

		val smallCustom = ValidataSmallRoot::class.memberProperties.map { it.name }.toSet()
		val smallHv = HvSmallRoot::class.memberProperties.map { it.name }.toSet()
		assertThat(smallCustom).containsExactlyInAnyOrderElementsOf(SizedPayloads.SMALL_LEAF_NAMES)
		assertThat(smallHv).isEqualTo(smallCustom)

		assertThat(SizedPayloads.customMediumValid().items).hasSize(PayloadSize.MEDIUM.blockCount)
		assertThat(SizedPayloads.customLargeValid().items).hasSize(PayloadSize.LARGE.blockCount)
		assertThat(SizedPayloads.customXLargeValid().items).hasSize(PayloadSize.XLARGE.blockCount)
		assertThat(SizedPayloads.customVeryLargeValid().items).hasSize(PayloadSize.VERY_LARGE.blockCount)
		assertThat(SizedPayloads.customExtremeValid().items).hasSize(PayloadSize.EXTREME.blockCount)
		assertThat(SizedPayloads.customStressValid().items).hasSize(PayloadSize.STRESS.blockCount)
		assertThat(SizedPayloads.customMaximumValid().items).hasSize(PayloadSize.MAXIMUM.blockCount)

		assertThat(SizedPayloads.hvMediumValid().items).hasSize(PayloadSize.MEDIUM.blockCount)
		assertThat(SizedPayloads.hvLargeValid().items).hasSize(PayloadSize.LARGE.blockCount)
		assertThat(SizedPayloads.hvXLargeValid().items).hasSize(PayloadSize.XLARGE.blockCount)
		assertThat(SizedPayloads.hvVeryLargeValid().items).hasSize(PayloadSize.VERY_LARGE.blockCount)
		assertThat(SizedPayloads.hvExtremeValid().items).hasSize(PayloadSize.EXTREME.blockCount)
		assertThat(SizedPayloads.hvStressValid().items).hasSize(PayloadSize.STRESS.blockCount)
		assertThat(SizedPayloads.hvMaximumValid().items).hasSize(PayloadSize.MAXIMUM.blockCount)

		assertThat(PayloadSize.SMALL.fieldCount).isEqualTo(5)
		assertThat(PayloadSize.MEDIUM.fieldCount).isEqualTo(10)
		assertThat(PayloadSize.LARGE.fieldCount).isEqualTo(25)
		assertThat(PayloadSize.XLARGE.fieldCount).isEqualTo(50)
		assertThat(PayloadSize.VERY_LARGE.fieldCount).isEqualTo(100)
		assertThat(PayloadSize.EXTREME.fieldCount).isEqualTo(250)
		assertThat(PayloadSize.STRESS.fieldCount).isEqualTo(500)
		assertThat(PayloadSize.MAXIMUM.fieldCount).isEqualTo(1000)

		assertThat(ValidataSmallRoot::class.simpleName).isNotNull
		assertThat(ValidataMediumRoot::class.simpleName).isNotNull
		assertThat(ValidataLargeRoot::class.simpleName).isNotNull
		assertThat(ValidataXLargeRoot::class.simpleName).isNotNull
		assertThat(ValidataVeryLargeRoot::class.simpleName).isNotNull
		assertThat(ValidataExtremeRoot::class.simpleName).isNotNull
		assertThat(ValidataStressRoot::class.simpleName).isNotNull
		assertThat(ValidataMaximumRoot::class.simpleName).isNotNull
		assertThat(HvSmallRoot::class.simpleName).isNotNull
		assertThat(HvMediumRoot::class.simpleName).isNotNull
		assertThat(HvLargeRoot::class.simpleName).isNotNull
		assertThat(HvXLargeRoot::class.simpleName).isNotNull
		assertThat(HvVeryLargeRoot::class.simpleName).isNotNull
		assertThat(HvExtremeRoot::class.simpleName).isNotNull
		assertThat(HvStressRoot::class.simpleName).isNotNull
		assertThat(HvMaximumRoot::class.simpleName).isNotNull
		assertThat(ValidataFieldBlock::class.sealedSubclasses).hasSize(PayloadSize.SLICE_KINDS)
		assertThat(HvFieldBlock::class.sealedSubclasses).hasSize(PayloadSize.SLICE_KINDS)
	}

	companion object {

		@JvmStatic
		fun matrixCells(): Stream<Arguments> = PayloadSize.entries.stream().flatMap { size ->
			InvalidityLevel.entries.stream().map { level -> Arguments.of(size, level) }
		}

		fun customPayload(size: PayloadSize, level: InvalidityLevel): Any = when (size) {
			PayloadSize.SMALL -> SizedPayloads.customSmall(level)
			PayloadSize.MEDIUM -> SizedPayloads.customMedium(level)
			PayloadSize.LARGE -> SizedPayloads.customLarge(level)
			PayloadSize.XLARGE -> SizedPayloads.customXLarge(level)
			PayloadSize.VERY_LARGE -> SizedPayloads.customVeryLarge(level)
			PayloadSize.EXTREME -> SizedPayloads.customExtreme(level)
			PayloadSize.STRESS -> SizedPayloads.customStress(level)
			PayloadSize.MAXIMUM -> SizedPayloads.customMaximum(level)
		}

		fun hvPayload(size: PayloadSize, level: InvalidityLevel): Any = when (size) {
			PayloadSize.SMALL -> SizedPayloads.hvSmall(level)
			PayloadSize.MEDIUM -> SizedPayloads.hvMedium(level)
			PayloadSize.LARGE -> SizedPayloads.hvLarge(level)
			PayloadSize.XLARGE -> SizedPayloads.hvXLarge(level)
			PayloadSize.VERY_LARGE -> SizedPayloads.hvVeryLarge(level)
			PayloadSize.EXTREME -> SizedPayloads.hvExtreme(level)
			PayloadSize.STRESS -> SizedPayloads.hvStress(level)
			PayloadSize.MAXIMUM -> SizedPayloads.hvMaximum(level)
		}
	}
}

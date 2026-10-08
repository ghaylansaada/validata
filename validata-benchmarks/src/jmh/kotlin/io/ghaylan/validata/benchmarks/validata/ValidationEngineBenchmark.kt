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

import io.ghaylan.validata.benchmarks.matrix.SizedPayloads
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * In-memory Validata throughput across all payload sizes × [InvalidityLevel] (64 cells with HV).
 *
 * Warmup, measurement, and fork counts come from the Gradle `jmh {}` block (`-PjmhPrecise`).
 * The harness is built once per trial — this class does not measure registry bootstrap.
 *
 * @author Ghaylan Saada
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
open class ValidationEngineBenchmark {

	private lateinit var harness: ValidataHarness

	private lateinit var smallValid: ValidataSmallRoot
	private lateinit var smallQuarter: ValidataSmallRoot
	private lateinit var smallHalf: ValidataSmallRoot
	private lateinit var smallAll: ValidataSmallRoot

	private lateinit var mediumValid: ValidataMediumRoot
	private lateinit var mediumQuarter: ValidataMediumRoot
	private lateinit var mediumHalf: ValidataMediumRoot
	private lateinit var mediumAll: ValidataMediumRoot

	private lateinit var largeValid: ValidataLargeRoot
	private lateinit var largeQuarter: ValidataLargeRoot
	private lateinit var largeHalf: ValidataLargeRoot
	private lateinit var largeAll: ValidataLargeRoot

	private lateinit var xlargeValid: ValidataXLargeRoot
	private lateinit var xlargeQuarter: ValidataXLargeRoot
	private lateinit var xlargeHalf: ValidataXLargeRoot
	private lateinit var xlargeAll: ValidataXLargeRoot

	private lateinit var veryLargeValid: ValidataVeryLargeRoot
	private lateinit var veryLargeQuarter: ValidataVeryLargeRoot
	private lateinit var veryLargeHalf: ValidataVeryLargeRoot
	private lateinit var veryLargeAll: ValidataVeryLargeRoot

	private lateinit var extremeValid: ValidataExtremeRoot
	private lateinit var extremeQuarter: ValidataExtremeRoot
	private lateinit var extremeHalf: ValidataExtremeRoot
	private lateinit var extremeAll: ValidataExtremeRoot

	private lateinit var stressValid: ValidataStressRoot
	private lateinit var stressQuarter: ValidataStressRoot
	private lateinit var stressHalf: ValidataStressRoot
	private lateinit var stressAll: ValidataStressRoot

	private lateinit var maximumValid: ValidataMaximumRoot
	private lateinit var maximumQuarter: ValidataMaximumRoot
	private lateinit var maximumHalf: ValidataMaximumRoot
	private lateinit var maximumAll: ValidataMaximumRoot

	/** Builds the engine and warms each size on the valid path. */
	@Setup(Level.Trial)
	fun setUp() {
		harness = ValidataHarness()

		smallValid = SizedPayloads.customSmallValid()
		smallQuarter = SizedPayloads.customSmallQuarterInvalid()
		smallHalf = SizedPayloads.customSmallHalfInvalid()
		smallAll = SizedPayloads.customSmallAllInvalid()

		mediumValid = SizedPayloads.customMediumValid()
		mediumQuarter = SizedPayloads.customMediumQuarterInvalid()
		mediumHalf = SizedPayloads.customMediumHalfInvalid()
		mediumAll = SizedPayloads.customMediumAllInvalid()

		largeValid = SizedPayloads.customLargeValid()
		largeQuarter = SizedPayloads.customLargeQuarterInvalid()
		largeHalf = SizedPayloads.customLargeHalfInvalid()
		largeAll = SizedPayloads.customLargeAllInvalid()

		xlargeValid = SizedPayloads.customXLargeValid()
		xlargeQuarter = SizedPayloads.customXLargeQuarterInvalid()
		xlargeHalf = SizedPayloads.customXLargeHalfInvalid()
		xlargeAll = SizedPayloads.customXLargeAllInvalid()

		veryLargeValid = SizedPayloads.customVeryLargeValid()
		veryLargeQuarter = SizedPayloads.customVeryLargeQuarterInvalid()
		veryLargeHalf = SizedPayloads.customVeryLargeHalfInvalid()
		veryLargeAll = SizedPayloads.customVeryLargeAllInvalid()

		extremeValid = SizedPayloads.customExtremeValid()
		extremeQuarter = SizedPayloads.customExtremeQuarterInvalid()
		extremeHalf = SizedPayloads.customExtremeHalfInvalid()
		extremeAll = SizedPayloads.customExtremeAllInvalid()

		stressValid = SizedPayloads.customStressValid()
		stressQuarter = SizedPayloads.customStressQuarterInvalid()
		stressHalf = SizedPayloads.customStressHalfInvalid()
		stressAll = SizedPayloads.customStressAllInvalid()

		maximumValid = SizedPayloads.customMaximumValid()
		maximumQuarter = SizedPayloads.customMaximumQuarterInvalid()
		maximumHalf = SizedPayloads.customMaximumHalfInvalid()
		maximumAll = SizedPayloads.customMaximumAllInvalid()

		harness.validate(smallValid)
		harness.validate(mediumValid)
		harness.validate(largeValid)
		harness.validate(xlargeValid)
		harness.validate(veryLargeValid)
		harness.validate(extremeValid)
		harness.validate(stressValid)
		harness.validate(maximumValid)
	}

	/** Releases the Spring context. */
	@TearDown(Level.Trial)
	fun tearDown() {
		harness.close()
	}

	@Benchmark fun customSmallValid(bh: Blackhole) = bh.consume(harness.validate(smallValid))
	@Benchmark fun customSmallQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(smallQuarter))
	@Benchmark fun customSmallHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(smallHalf))
	@Benchmark fun customSmallAllInvalid(bh: Blackhole) = bh.consume(harness.validate(smallAll))

	@Benchmark fun customMediumValid(bh: Blackhole) = bh.consume(harness.validate(mediumValid))
	@Benchmark fun customMediumQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(mediumQuarter))
	@Benchmark fun customMediumHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(mediumHalf))
	@Benchmark fun customMediumAllInvalid(bh: Blackhole) = bh.consume(harness.validate(mediumAll))

	@Benchmark fun customLargeValid(bh: Blackhole) = bh.consume(harness.validate(largeValid))
	@Benchmark fun customLargeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(largeQuarter))
	@Benchmark fun customLargeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(largeHalf))
	@Benchmark fun customLargeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(largeAll))

	@Benchmark fun customXLargeValid(bh: Blackhole) = bh.consume(harness.validate(xlargeValid))
	@Benchmark fun customXLargeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(xlargeQuarter))
	@Benchmark fun customXLargeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(xlargeHalf))
	@Benchmark fun customXLargeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(xlargeAll))

	@Benchmark fun customVeryLargeValid(bh: Blackhole) = bh.consume(harness.validate(veryLargeValid))
	@Benchmark fun customVeryLargeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(veryLargeQuarter))
	@Benchmark fun customVeryLargeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(veryLargeHalf))
	@Benchmark fun customVeryLargeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(veryLargeAll))

	@Benchmark fun customExtremeValid(bh: Blackhole) = bh.consume(harness.validate(extremeValid))
	@Benchmark fun customExtremeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(extremeQuarter))
	@Benchmark fun customExtremeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(extremeHalf))
	@Benchmark fun customExtremeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(extremeAll))

	@Benchmark fun customStressValid(bh: Blackhole) = bh.consume(harness.validate(stressValid))
	@Benchmark fun customStressQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(stressQuarter))
	@Benchmark fun customStressHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(stressHalf))
	@Benchmark fun customStressAllInvalid(bh: Blackhole) = bh.consume(harness.validate(stressAll))

	@Benchmark fun customMaximumValid(bh: Blackhole) = bh.consume(harness.validate(maximumValid))
	@Benchmark fun customMaximumQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(maximumQuarter))
	@Benchmark fun customMaximumHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(maximumHalf))
	@Benchmark fun customMaximumAllInvalid(bh: Blackhole) = bh.consume(harness.validate(maximumAll))
}

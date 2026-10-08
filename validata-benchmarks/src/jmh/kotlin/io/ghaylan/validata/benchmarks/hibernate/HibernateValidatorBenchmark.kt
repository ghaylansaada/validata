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

import io.ghaylan.validata.benchmarks.matrix.SizedPayloads
import io.ghaylan.validata.benchmarks.validata.ValidationEngineBenchmark
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
 * Hibernate Validator twin of [ValidationEngineBenchmark] (8 sizes × 4 invalidity levels).
 *
 * Warmup, measurement, and fork counts come from the Gradle `jmh {}` block (`-PjmhPrecise`).
 *
 * @author Ghaylan Saada
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class HibernateValidatorBenchmark {

	private lateinit var harness: HvHarness

	private lateinit var smallValid: HvSmallRoot
	private lateinit var smallQuarter: HvSmallRoot
	private lateinit var smallHalf: HvSmallRoot
	private lateinit var smallAll: HvSmallRoot

	private lateinit var mediumValid: HvMediumRoot
	private lateinit var mediumQuarter: HvMediumRoot
	private lateinit var mediumHalf: HvMediumRoot
	private lateinit var mediumAll: HvMediumRoot

	private lateinit var largeValid: HvLargeRoot
	private lateinit var largeQuarter: HvLargeRoot
	private lateinit var largeHalf: HvLargeRoot
	private lateinit var largeAll: HvLargeRoot

	private lateinit var xlargeValid: HvXLargeRoot
	private lateinit var xlargeQuarter: HvXLargeRoot
	private lateinit var xlargeHalf: HvXLargeRoot
	private lateinit var xlargeAll: HvXLargeRoot

	private lateinit var veryLargeValid: HvVeryLargeRoot
	private lateinit var veryLargeQuarter: HvVeryLargeRoot
	private lateinit var veryLargeHalf: HvVeryLargeRoot
	private lateinit var veryLargeAll: HvVeryLargeRoot

	private lateinit var extremeValid: HvExtremeRoot
	private lateinit var extremeQuarter: HvExtremeRoot
	private lateinit var extremeHalf: HvExtremeRoot
	private lateinit var extremeAll: HvExtremeRoot

	private lateinit var stressValid: HvStressRoot
	private lateinit var stressQuarter: HvStressRoot
	private lateinit var stressHalf: HvStressRoot
	private lateinit var stressAll: HvStressRoot

	private lateinit var maximumValid: HvMaximumRoot
	private lateinit var maximumQuarter: HvMaximumRoot
	private lateinit var maximumHalf: HvMaximumRoot
	private lateinit var maximumAll: HvMaximumRoot

	/** Builds the factory and warms each size on the valid path. */
	@Setup(Level.Trial)
	fun setUp() {
		harness = HvHarness()

		smallValid = SizedPayloads.hvSmallValid()
		smallQuarter = SizedPayloads.hvSmallQuarterInvalid()
		smallHalf = SizedPayloads.hvSmallHalfInvalid()
		smallAll = SizedPayloads.hvSmallAllInvalid()

		mediumValid = SizedPayloads.hvMediumValid()
		mediumQuarter = SizedPayloads.hvMediumQuarterInvalid()
		mediumHalf = SizedPayloads.hvMediumHalfInvalid()
		mediumAll = SizedPayloads.hvMediumAllInvalid()

		largeValid = SizedPayloads.hvLargeValid()
		largeQuarter = SizedPayloads.hvLargeQuarterInvalid()
		largeHalf = SizedPayloads.hvLargeHalfInvalid()
		largeAll = SizedPayloads.hvLargeAllInvalid()

		xlargeValid = SizedPayloads.hvXLargeValid()
		xlargeQuarter = SizedPayloads.hvXLargeQuarterInvalid()
		xlargeHalf = SizedPayloads.hvXLargeHalfInvalid()
		xlargeAll = SizedPayloads.hvXLargeAllInvalid()

		veryLargeValid = SizedPayloads.hvVeryLargeValid()
		veryLargeQuarter = SizedPayloads.hvVeryLargeQuarterInvalid()
		veryLargeHalf = SizedPayloads.hvVeryLargeHalfInvalid()
		veryLargeAll = SizedPayloads.hvVeryLargeAllInvalid()

		extremeValid = SizedPayloads.hvExtremeValid()
		extremeQuarter = SizedPayloads.hvExtremeQuarterInvalid()
		extremeHalf = SizedPayloads.hvExtremeHalfInvalid()
		extremeAll = SizedPayloads.hvExtremeAllInvalid()

		stressValid = SizedPayloads.hvStressValid()
		stressQuarter = SizedPayloads.hvStressQuarterInvalid()
		stressHalf = SizedPayloads.hvStressHalfInvalid()
		stressAll = SizedPayloads.hvStressAllInvalid()

		maximumValid = SizedPayloads.hvMaximumValid()
		maximumQuarter = SizedPayloads.hvMaximumQuarterInvalid()
		maximumHalf = SizedPayloads.hvMaximumHalfInvalid()
		maximumAll = SizedPayloads.hvMaximumAllInvalid()

		harness.validate(smallValid)
		harness.validate(mediumValid)
		harness.validate(largeValid)
		harness.validate(xlargeValid)
		harness.validate(veryLargeValid)
		harness.validate(extremeValid)
		harness.validate(stressValid)
		harness.validate(maximumValid)
	}

	/** Closes the validator factory. */
	@TearDown(Level.Trial)
	fun tearDown() {
		harness.close()
	}

	@Benchmark fun hvSmallValid(bh: Blackhole) = bh.consume(harness.validate(smallValid))
	@Benchmark fun hvSmallQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(smallQuarter))
	@Benchmark fun hvSmallHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(smallHalf))
	@Benchmark fun hvSmallAllInvalid(bh: Blackhole) = bh.consume(harness.validate(smallAll))

	@Benchmark fun hvMediumValid(bh: Blackhole) = bh.consume(harness.validate(mediumValid))
	@Benchmark fun hvMediumQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(mediumQuarter))
	@Benchmark fun hvMediumHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(mediumHalf))
	@Benchmark fun hvMediumAllInvalid(bh: Blackhole) = bh.consume(harness.validate(mediumAll))

	@Benchmark fun hvLargeValid(bh: Blackhole) = bh.consume(harness.validate(largeValid))
	@Benchmark fun hvLargeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(largeQuarter))
	@Benchmark fun hvLargeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(largeHalf))
	@Benchmark fun hvLargeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(largeAll))

	@Benchmark fun hvXLargeValid(bh: Blackhole) = bh.consume(harness.validate(xlargeValid))
	@Benchmark fun hvXLargeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(xlargeQuarter))
	@Benchmark fun hvXLargeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(xlargeHalf))
	@Benchmark fun hvXLargeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(xlargeAll))

	@Benchmark fun hvVeryLargeValid(bh: Blackhole) = bh.consume(harness.validate(veryLargeValid))
	@Benchmark fun hvVeryLargeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(veryLargeQuarter))
	@Benchmark fun hvVeryLargeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(veryLargeHalf))
	@Benchmark fun hvVeryLargeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(veryLargeAll))

	@Benchmark fun hvExtremeValid(bh: Blackhole) = bh.consume(harness.validate(extremeValid))
	@Benchmark fun hvExtremeQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(extremeQuarter))
	@Benchmark fun hvExtremeHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(extremeHalf))
	@Benchmark fun hvExtremeAllInvalid(bh: Blackhole) = bh.consume(harness.validate(extremeAll))

	@Benchmark fun hvStressValid(bh: Blackhole) = bh.consume(harness.validate(stressValid))
	@Benchmark fun hvStressQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(stressQuarter))
	@Benchmark fun hvStressHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(stressHalf))
	@Benchmark fun hvStressAllInvalid(bh: Blackhole) = bh.consume(harness.validate(stressAll))

	@Benchmark fun hvMaximumValid(bh: Blackhole) = bh.consume(harness.validate(maximumValid))
	@Benchmark fun hvMaximumQuarterInvalid(bh: Blackhole) = bh.consume(harness.validate(maximumQuarter))
	@Benchmark fun hvMaximumHalfInvalid(bh: Blackhole) = bh.consume(harness.validate(maximumHalf))
	@Benchmark fun hvMaximumAllInvalid(bh: Blackhole) = bh.consume(harness.validate(maximumAll))
}

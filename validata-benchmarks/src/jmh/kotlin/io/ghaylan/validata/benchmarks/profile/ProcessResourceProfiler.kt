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
package io.ghaylan.validata.benchmarks.profile

import com.sun.management.OperatingSystemMXBean
import org.openjdk.jmh.infra.BenchmarkParams
import org.openjdk.jmh.infra.IterationParams
import org.openjdk.jmh.profile.InternalProfiler
import org.openjdk.jmh.results.AggregationPolicy
import org.openjdk.jmh.results.IterationResult
import org.openjdk.jmh.results.Result
import org.openjdk.jmh.results.ScalarResult
import java.lang.management.ManagementFactory

/**
 * Per-iteration process CPU time and heap-used snapshot for the comparison report.
 *
 * Secondary metrics:
 * - `·cpu.time` — process CPU during the iteration (ms)
 * - `·cpu.time.norm` — process CPU per operation (ns/op), estimated via score × wall time
 * - `·cpu.util` — process CPU / wall time (can exceed 1.0 on multi-core)
 * - `·heap.used` — heap used after the iteration (MiB)
 *
 * Registered from Gradle `jmh { profilers }` by fully qualified class name. Not a library SPI.*
 * 
 * @author Ghaylan Saada
 */
class ProcessResourceProfiler: InternalProfiler {
	
	/** Process CPU nanoseconds captured in [beforeIteration].	 */
	private var cpuBeforeNs: Long = 0L
	
	/** Wall-clock nanoseconds captured in [beforeIteration].	 */
	private var wallBeforeNs: Long = 0L
	
	override fun getDescription(): String = "Process CPU time + heap used"
	
	/**
	 * @param benchmarkParams unused; required by JMH
	 * @param iterationParams unused; required by JMH	 
	 */
	override fun beforeIteration(
		benchmarkParams: BenchmarkParams,
		iterationParams: IterationParams
	) {
		cpuBeforeNs = processCpuNs()
		wallBeforeNs = System.nanoTime()
	}
	
	/**
	 * @param benchmarkParams unused; required by JMH
	 * @param iterationParams unused; required by JMH
	 * @param result iteration primary score, used to estimate ops for `·cpu.time.norm`
	 * @return four scalar secondary metrics	 
	 */
	override fun afterIteration(
		benchmarkParams: BenchmarkParams,
		iterationParams: IterationParams,
		result: IterationResult,
	): Collection<Result<*>> {
		val wallDeltaNs = (System.nanoTime() - wallBeforeNs).coerceAtLeast(1L)
		val cpuDeltaNs = (processCpuNs() - cpuBeforeNs).coerceAtLeast(0L)
		val heapMiB = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used / (1024.0 * 1024.0)
		val wallSec = wallDeltaNs / 1_000_000_000.0
		val primary = result.primaryResult
		val score = primary.score
		val unit = primary.scoreUnit.orEmpty()
		val ops = when {
			unit.contains("ops/s", ignoreCase = true) || unit.contains("/s", ignoreCase = true) -> (score * wallSec).coerceAtLeast(1.0)
			else -> primary.statistics.n.toDouble()
				.coerceAtLeast(1.0)
		}
		return listOf(
			ScalarResult("·cpu.time", cpuDeltaNs / 1_000_000.0, "ms", AggregationPolicy.AVG),
			ScalarResult("·cpu.time.norm", cpuDeltaNs / ops, "ns/op", AggregationPolicy.AVG),
			ScalarResult("·cpu.util", cpuDeltaNs.toDouble() / wallDeltaNs, "·", AggregationPolicy.AVG),
			ScalarResult("·heap.used", heapMiB, "MiB", AggregationPolicy.AVG),
		)
	}
	
	/**
	 * @return process CPU time in nanoseconds, or `0` when the MXBean does not expose it
	 */
	private fun processCpuNs(): Long {
		val os = ManagementFactory.getOperatingSystemMXBean()
		return if (os is OperatingSystemMXBean) os.processCpuTime else 0L
	}
}

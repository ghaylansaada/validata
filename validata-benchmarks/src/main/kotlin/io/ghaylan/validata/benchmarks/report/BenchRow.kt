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
package io.ghaylan.validata.benchmarks.report

/**
 * One parsed JMH primary-metric row plus optional secondary profiler scores.
 *
 * @property name simple benchmark method name (`customSmallValid`, `hvMaximumAllInvalid`, …)
 * @property mode JMH mode (`thrpt`, `sample`, …)
 * @property score primary metric score
 * @property error JMH score error (≈99.9% CI when present)
 * @property unit primary unit (`ops/s`, `ms/op`, …)
 * @property threads JMH worker threads
 * @property params JMH `@Param` map (`size`, `outcome`, …)
 * @property secondary secondary metric name → (score, unit)*
 * 
 * @author Ghaylan Saada
 */
internal data class BenchRow(
	val name: String,
	val mode: String,
	val score: Double,
	val error: Double,
	val unit: String,
	val threads: Int,
	val params: Map<String, String> = emptyMap(),
	val secondary: Map<String, Pair<Double, String>> = emptyMap(),
) {
	
	/** JMH `size` param, or `null` when the benchmark has none.	 */
	val size: String? get() = params["size"]
	
	/** JMH `outcome` param, or `null` when the benchmark has none.	 */
	val outcome: String? get() = params["outcome"]
	
	/**
	 * @param key secondary metric name, with or without the JMH `·` prefix
	 * @return score or `null` when absent	 
	 */
	fun secondaryScore(key: String): Double? = secondary[key]?.first
	
	/**
	 * Lookup key used by the Markdown tables: method name plus sorted `k=v` params.
	 *
	 * @return e.g. `customSmallValid` or a param key when JMH params are present
	 */
	fun exactKey(): String {
		if (params.isEmpty()) return name
		val p = params.entries.sortedBy { it.key }
			.joinToString("|") { "${it.key}=${it.value}" }
		return "$name|$p"
	}
}

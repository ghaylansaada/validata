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

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.io.File

/**
 * Reads JMH `resultFormat=JSON` output into [BenchRow] values.
 *
 * Uses Jackson 3 (`tools.jackson`) to parse JMH JSON.*
 * 
 * @author Ghaylan Saada
 */
internal object JmhResultsParser {
	
	private val mapper: JsonMapper = JsonMapper.builder()
		.build()
	
	/**
	 * @param file JMH JSON array written by the `jmh` task
	 * @return one row per benchmark × param combination; skips entries without a primary metric
	 * @throws IllegalArgumentException when the root is not a JSON array	 
	 */
	fun parse(file: File): List<BenchRow> {
		val root: JsonNode = mapper.readTree(file)
		require(root.isArray) {
			"JMH results file '${file.absolutePath}' is not a JSON array. Re-run :validata-benchmarks:jmh so results.json is JMH resultFormat=JSON."
		}
		return root.mapNotNull { node ->
			val benchmark = node.path("benchmark")
				.asString(null)
				?: return@mapNotNull null
			val primary = node.path("primaryMetric")
			if (primary.isMissingNode) return@mapNotNull null
			val params = linkedMapOf<String, String>()
			node.path("params")
				.properties()
				.forEach { (k, v) -> params[k] = v.asString() }
			val secondary = linkedMapOf<String, Pair<Double, String>>()
			node.path("secondaryMetrics")
				.properties()
				.forEach { (k, v) ->
					if (v.has("score")) {
						secondary[k] = v.path("score")
							.asDouble() to v.path("scoreUnit")
							.asString("")
					}
				}
			BenchRow(
				name = benchmark.substringAfterLast('.'),
				mode = node.path("mode")
					.asString("thrpt"),
				score = primary.path("score")
					.asDouble(),
				error = primary.path("scoreError")
					.asDouble(0.0),
				unit = primary.path("scoreUnit")
					.asString("ops/s"),
				threads = node.path("threads")
					.asInt(1),
				params = params,
				secondary = secondary,
			)
		}
	}
}

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

import java.io.File

/**
 * CLI entry: `BenchmarkReportMain <jmh-results.json> <output.md>`
 *
 * Invoked by the Gradle `benchmarkReport` task after JMH writes JSON. The Gradle task embeds
 * the fragment into `validata-benchmarks/README.md` between Html comment markers.
 *
 * @author Ghaylan Saada
 */
object BenchmarkReportMain {

	/**
	 * @param args `[0]` JMH JSON path, `[1]` Markdown output path
	 */
	@JvmStatic
	fun main(args: Array<String>) {
		require(args.size == 2) {
			"Usage: BenchmarkReportMain <results.json> <output.md>. " +
				"Pass the JMH JSON from :validata-benchmarks:jmh and a Markdown path to write."
		}
		val jsonFile = File(args[0])
		val outFile = File(args[1])
		require(jsonFile.isFile) {
			"Missing JMH results: ${jsonFile.absolutePath}. Run :validata-benchmarks:jmh first."
		}
		val benches = JmhResultsParser.parse(jsonFile)
		outFile.parentFile?.mkdirs()
		outFile.writeText(BenchmarkReportRenderer.render(benches))
		println("Wrote ${outFile.absolutePath} (${benches.size} benchmarks)")
	}
}

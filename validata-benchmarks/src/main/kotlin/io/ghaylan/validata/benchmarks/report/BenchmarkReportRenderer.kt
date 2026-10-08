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

import io.ghaylan.validata.benchmarks.matrix.InvalidityLevel
import io.ghaylan.validata.benchmarks.matrix.PayloadSize
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/**
 * Turns parsed JMH rows into in-memory comparison Markdown.
 *
 * Reports neutral ratios (no winners / rankings / aggregate scores). Sections are grouped by
 * [InvalidityLevel] so the four invalidity levels are easy to compare.
 *
 * @author Ghaylan Saada
 */
internal object BenchmarkReportRenderer {

	/**
	 * @param rows parsed JMH results
	 * @return complete Markdown document
	 */
	fun render(rows: List<BenchRow>): String {
		val byExact = rows.associateBy { it.exactKey() }
		val ts = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
			.format(Instant.now())
		val rt = ManagementFactory.getRuntimeMXBean()
		val os = ManagementFactory.getOperatingSystemMXBean()
		val mem = ManagementFactory.getMemoryMXBean()
		val heapMb = mem.heapMemoryUsage.max / (1024.0 * 1024.0)
		val memSizes = PayloadSize.entries.map { "${it.displayName} (${it.fieldCount})" to it.benchMid }

		fun memRow(name: String): BenchRow? = byExact[name]

		fun fmt(v: Double): String = when {
			v >= 100 -> "%,d".format(v.roundToLong())
			v >= 10 -> "%.1f".format(v)
			else -> "%.3f".format(v)
		}

		fun fmtRaw(v: Double): String = "%.3f".format(v)

		fun ratio(custom: Double?, hv: Double?): String {
			if (custom == null || hv == null || hv == 0.0) return "—"
			return "%.2fx".format(custom / hv)
		}

		fun pair(c: Double?, h: Double?): String =
			"${c?.let(::fmt) ?: "—"} | ${h?.let(::fmt) ?: "—"} | ${ratio(c, h)}"

		fun alloc(row: BenchRow?): Double? = row?.secondaryScore("·gc.alloc.rate.norm")
			?: row?.secondaryScore("gc.alloc.rate.norm")

		fun cpuNorm(row: BenchRow?): Double? = row?.secondaryScore("·cpu.time.norm")
			?: row?.secondaryScore("cpu.time.norm")

		fun heapUsed(row: BenchRow?): Double? = row?.secondaryScore("·heap.used")
			?: row?.secondaryScore("heap.used")

		val hasGc = rows.any { alloc(it) != null }
		val hasCpu = rows.any { cpuNorm(it) != null || it.secondaryScore("·cpu.time") != null }

		return buildString {
			appendLine("## Benchmark results")
			appendLine()
			appendLine("> Auto-generated in-memory comparison. Re-run:")
			appendLine(">")
			appendLine("> ```bash")
			appendLine("> ./gradlew :validata-benchmarks:jmh :validata-benchmarks:benchmarkReport -PjmhPrecise")
			appendLine("> ```")
			appendLine()
			appendLine("| | |")
			appendLine("|---|---|")
			appendLine("| **Generated (UTC)** | `$ts` |")
			appendLine("| **JVM** | ${rt.vmName} `${rt.vmVersion}` |")
			appendLine("| **OS** | ${os.name} `${os.arch}` · **${os.availableProcessors}** CPUs |")
			appendLine("| **Max heap (reporter JVM)** | ${"%.0f".format(heapMb)} MiB |")
			appendLine("| **Benchmarks parsed** | **${rows.size}** |")
			appendLine("| **GC alloc metrics** | ${if (hasGc) "yes (`·gc.alloc.rate.norm`)" else "no — re-run with GC profiler"} |")
			appendLine("| **CPU / heap metrics** | ${if (hasCpu) "yes (`ProcessResourceProfiler`)" else "no"} |")
			appendLine("| **Scope** | **In-memory only** (no HTTP) |")
			appendLine()
			appendLine("---")
			appendLine()
			appendLine("## Matrix")
			appendLine()
			appendLine("| Dimension | Values |")
			appendLine("|---|---|")
			appendLine(
				"| Payload size | **${PayloadSize.entries.joinToString("** / **") { it.fieldCount.toString() }}** validated fields |",
			)
			appendLine(
				"| Invalidity | **0%** (Valid) / **25%** (Quarter) / **50%** (Half) / **100%** (All) |",
			)
			appendLine("| Stack | **Validata** vs **Hibernate Validator** |")
			appendLine("| Metrics | ops/s · alloc B/op · CPU ns/op · heap MiB (diagnostic) |")
			appendLine("| Cells | **64** (8 sizes × 4 invalidity levels × 2 engines) |")
			appendLine()
			appendLine("Ratios are Validata / Hibernate Validator:")
			appendLine()
			appendLine("- throughput ratio **> 1** → higher Validata throughput")
			appendLine("- allocation ratio **< 1** → lower Validata allocation")
			appendLine("- CPU ratio **< 1** → lower Validata CPU time")
			appendLine()
			appendLine("---")
			appendLine()
			appendLine("## 1. Throughput (ops/s)")
			appendLine()
			appendLine("Pure engine validate vs HV `Validator.validate` — no HTTP, Tomcat, or Jackson.")
			appendLine()

			fun throughputSection(section: String, level: InvalidityLevel) {
				val suffix = level.benchSuffix
				appendLine("### $section ${level.displayName}")
				appendLine()
				appendLine("| Size | Validata (ops/s) | HV (ops/s) | Throughput ratio |")
				appendLine("|---|---:|---:|---:|")
				val customScores = mutableListOf<Long>()
				val hvScores = mutableListOf<Long>()
				val axis = mutableListOf<String>()
				for ((label, mid) in memSizes) {
					val c = memRow("custom${mid}$suffix")?.score
					val h = memRow("hv$mid$suffix")?.score
					if (c == null && h == null) continue
					c?.let { customScores += it.roundToLong() }
					h?.let { hvScores += it.roundToLong() }
					axis += label.replace(" ", "_").replace("(", "").replace(")", "")
					appendLine("| $label | ${pair(c, h)} |")
				}
				appendLine()
				if (customScores.isNotEmpty() && customScores.size == hvScores.size && customScores.size == axis.size) {
					val yMax = ((customScores + hvScores).maxOrNull() ?: 1L) * 115 / 100
					appendLine("```mermaid")
					appendLine("xychart-beta")
					appendLine("  title \"In-memory ops/s — ${level.displayName}\"")
					appendLine("  x-axis [${axis.joinToString(", ")}]")
					appendLine("  y-axis \"ops/s\" 0 --> $yMax")
					appendLine("  bar [${customScores.joinToString(", ")}]")
					appendLine("  bar [${hvScores.joinToString(", ")}]")
					appendLine("```")
					appendLine()
					appendLine("Legend: **first bars** = Validata, **second** = Hibernate Validator.")
					appendLine()
				}
			}

			InvalidityLevel.entries.forEachIndexed { index, level ->
				throughputSection("1.${index + 1}", level)
			}

			if (hasGc || hasCpu) {
				appendLine("---")
				appendLine()
				appendLine("## 2. Allocation (B/op) and CPU (ns/op)")
				appendLine()
				appendLine("Heap after iteration is diagnostic only — not a retention proof.")
				appendLine()

				InvalidityLevel.entries.forEachIndexed { index, level ->
					val suffix = level.benchSuffix
					appendLine("### 2.${index + 1} ${level.displayName}")
					appendLine()
					appendLine(
						"| Size | Validata alloc | HV alloc | Alloc ratio | Validata CPU | HV CPU | CPU ratio | Validata heap | HV heap |",
					)
					appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
					for ((label, mid) in memSizes) {
						val c = memRow("custom$mid$suffix")
						val h = memRow("hv$mid$suffix")
						if (c == null && h == null) continue
						appendLine(
							"| $label | " +
								"${alloc(c)?.let { "%,.0f B/op".format(it) } ?: "—"} | " +
								"${alloc(h)?.let { "%,.0f B/op".format(it) } ?: "—"} | " +
								"${ratio(alloc(c), alloc(h))} | " +
								"${cpuNorm(c)?.let { "%,.0f ns/op".format(it) } ?: "—"} | " +
								"${cpuNorm(h)?.let { "%,.0f ns/op".format(it) } ?: "—"} | " +
								"${ratio(cpuNorm(c), cpuNorm(h))} | " +
								"${heapUsed(c)?.let { "%.0f MiB".format(it) } ?: "—"} | " +
								"${heapUsed(h)?.let { "%.0f MiB".format(it) } ?: "—"} |",
						)
					}
					appendLine()
				}
				appendLine(
					"Alloc = GC normalized allocation (`·gc.alloc.rate.norm`). " +
						"CPU = process CPU per op. Heap = used heap after iteration (not a retention proof).",
				)
				appendLine()
			}

			appendLine("---")
			appendLine()
			appendLine("## 3. Raw scores (diff-friendly)")
			appendLine()
			appendLine("| key | score | error | unit | mode | threads | alloc B/op | cpu ns/op | heap MiB |")
			appendLine("|---|---:|---:|---|---|---:|---:|---:|---:|")
			rows.sortedWith(compareBy({ it.name }, { it.threads }))
				.forEach { r ->
					appendLine(
						"| `${r.exactKey()}` | ${fmtRaw(r.score)} | ${fmtRaw(r.error)} | ${r.unit} | ${r.mode} | ${r.threads} | " +
							"${alloc(r)?.let { fmtRaw(it) } ?: "—"} | " +
							"${cpuNorm(r)?.let { fmtRaw(it) } ?: "—"} | " +
							"${heapUsed(r)?.let { fmtRaw(it) } ?: "—"} |",
					)
				}
			appendLine()
			appendLine("---")
			appendLine()
			appendLine("## Notes")
			appendLine()
			appendLine("1. Prefer **ratios on the same machine/run** over absolute numbers across hosts.")
			appendLine("2. Invalidity levels use a deterministic evenly spaced leaf-fail plan (floor percents).")
			appendLine("3. `error` is JMH score error (≈99.9% CI when present).")
			appendLine("4. This module measures the validation engine only — no Tomcat / Jackson / HTTP.")
			appendLine("5. Ratios are reported separately; there is no aggregate score or winner.")
			appendLine()
		}
	}
}

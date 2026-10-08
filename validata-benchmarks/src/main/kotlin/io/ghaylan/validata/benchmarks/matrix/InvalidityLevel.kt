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

/**
 * Deterministic invalidity level for the primary benchmark matrix.
 *
 * Every [PayloadSize] is measured at each of these four levels. JMH method names append
 * [benchSuffix] (`customSmallValid`, `hvMediumQuarterInvalid`, …).
 *
 * ## Fail-count rule
 *
 * For [fieldCount] leaves and [percent] ∈ {0, 25, 50, 100}:
 *
 * ```text
 * failCount = fieldCount                          when percent == 100
 * failCount = 0                                   when percent == 0
 * failCount = (fieldCount * percent) / 100        otherwise (integer floor)
 * ```
 *
 * Fail indices are spaced evenly across `[0, fieldCount)`:
 *
 * ```text
 * index_i = (i * fieldCount) / failCount    for i in 0 until failCount
 * ```
 *
 * Examples for 5 leaves: 0% → {}, 25% → {0}, 50% → {0, 2}, 100% → {0,1,2,3,4}.
 * Examples for 10 leaves: 25% → {0, 5}, 50% → {0, 2, 4, 6, 8}.
 *
 * No randomness — the same logical leaves fail on Validata and Hibernate Validator.
 *
 * @property percent Target invalid leaf percentage (0 / 25 / 50 / 100).
 * @property benchSuffix CamelCase suffix for JMH method names.
 * @property displayName Human label for README / report sections.
 *
 * @author Ghaylan Saada
 */
enum class InvalidityLevel(
	val percent: Int,
	val benchSuffix: String,
	val displayName: String,
) {

	/** Every leaf satisfies its constraints. */
	VALID(0, "Valid", "Valid (0% invalid)"),

	/** Approximately one quarter of leaves fail (floor). */
	QUARTER(25, "QuarterInvalid", "Quarter Invalid (25%)"),

	/** Approximately half of leaves fail (floor). */
	HALF(50, "HalfInvalid", "Half Invalid (50%)"),

	/** Every leaf fails. */
	ALL(100, "AllInvalid", "All Invalid (100%)"),
	;

	/**
	 * Deterministic fail indices for [fieldCount] leaves at this level.
	 *
	 * @param fieldCount total validated leaves in the payload
	 * @return fail indices in `[0, fieldCount)`
	 */
	fun failLeafIndices(fieldCount: Int): Set<Int> = Companion.failLeafIndices(fieldCount, percent)

	companion object {

		/**
		 * Deterministic set of leaf indices that must fail for [fieldCount] and [percent].
		 *
		 * @param fieldCount total validated leaves in the payload
		 * @param percent one of 0, 25, 50, 100
		 * @return sorted unique fail indices in `[0, fieldCount)`
		 */
		fun failLeafIndices(fieldCount: Int, percent: Int): Set<Int> {
			require(fieldCount >= 0) { "fieldCount must be >= 0" }
			require(percent == 0 || percent == 25 || percent == 50 || percent == 100) {
				"percent must be 0, 25, 50, or 100 (got $percent)"
			}
			if (percent == 0 || fieldCount == 0) return emptySet()
			if (percent == 100) return (0 until fieldCount).toSet()
			val failCount = (fieldCount * percent) / 100
			if (failCount == 0) return emptySet()
			return (0 until failCount).map { (it * fieldCount) / failCount }.toSet()
		}
	}
}

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

import io.ghaylan.validata.benchmarks.hibernate.HvFieldBlock
import io.ghaylan.validata.benchmarks.validata.ValidataFieldBlock

/**
 * Real-life payload scales used by in-memory JMH.
 *
 * [SMALL] is a heterogeneous 5-field DTO. Sizes 10–1000 are [blockCount] copies of a 5-field
 * rotating slice ([ValidataFieldBlock] / [HvFieldBlock]). [fieldCount] is the validated leaf total.
 *
 * JMH method names use [benchMid] plus [InvalidityLevel.benchSuffix]
 * (`customSmallValid`, `hvVeryLargeQuarterInvalid`, …).
 *
 * @property paramValue Stable token for docs and tooling.
 * @property displayName Human label in Markdown tables.
 * @property benchMid Camel mid-token for in-memory JMH method names.
 * @property blockCount How many nested field blocks the wrapper holds (1 for Small conceptually).
 *
 * @author Ghaylan Saada
 */
enum class PayloadSize(
	val paramValue: String,
	val displayName: String,
	val benchMid: String,
	val blockCount: Int,
) {

	/** 5 fields — heterogeneous realistic small DTO. */
	SMALL("small", "Small", "Small", 1),

	/** 10 fields — two rotating slices. */
	MEDIUM("medium", "Medium", "Medium", 2),

	/** 25 fields — mid-size resource. */
	LARGE("large", "Large", "Large", 5),

	/** 50 fields — large form / nested resource. */
	XLARGE("xlarge", "XLarge", "XLarge", 10),

	/** 100 fields — very large API body. */
	VERY_LARGE("verylarge", "Very Large", "VeryLarge", 20),

	/** 250 fields — extreme graph. */
	EXTREME("extreme", "Extreme", "Extreme", 50),

	/** 500 fields — stress. */
	STRESS("stress", "Stress", "Stress", 100),

	/** 1000 fields — maximum stress. */
	MAXIMUM("maximum", "Maximum Stress", "Maximum", 200),
	;

	/**
	 * Leaf fields under validation: [blockCount] × [FIELDS_PER_BLOCK].
	 */
	val fieldCount: Int
		get() = blockCount * FIELDS_PER_BLOCK

	companion object {

		/**
		 * Leaf properties on each concrete [ValidataFieldBlock] / [HvFieldBlock] slice
		 * (and on the Small DTO).
		 */
		const val FIELDS_PER_BLOCK = 5

		/**
		 * How many distinct 5-field constraint families cycle in [SizedPayloads] (sizes ≥ 10).
		 */
		const val SLICE_KINDS = 6

		/**
		 * Resolves a size token.
		 *
		 * @param value one of [paramValue]
		 * @return the matching size
		 * @throws IllegalArgumentException when [value] is not a known token
		 */
		fun fromParam(value: String): PayloadSize = entries.find { it.paramValue == value }
			?: throw IllegalArgumentException(
				"Unknown payload size '$value'. Use one of: ${entries.joinToString { it.paramValue }}.",
			)
	}
}

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

import io.ghaylan.validata.benchmarks.matrix.PayloadSize
import io.ghaylan.validata.benchmarks.validata.ValidataSmallRoot
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * Realistic small HV DTO — **5** heterogeneous fields ([PayloadSize.SMALL]).
 *
 * Validata twin: [ValidataSmallRoot].
 *
 * @author Ghaylan Saada
 */
data class HvSmallRoot(
	@field:NotBlank
	val name: String? = null,
	@field:Min(0)
	val age: Int? = null,
	@field:Email
	val email: String? = null,
	@field:Past
	val birthDate: LocalDate? = null,
	@field:Size(min = 1, max = 5)
	val tags: List<String>? = null,
)

/**
 * Medium HV payload — **2** blocks ([PayloadSize.MEDIUM] = 10 fields).
 *
 * @property items Exactly two field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvMediumRoot(
	@field:Valid
	@field:Size(min = 2, max = 2)
	val items: List<HvFieldBlock>,
)

/**
 * Large HV payload — **5** blocks ([PayloadSize.LARGE] = 25 fields).
 *
 * @property items Exactly five field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvLargeRoot(
	@field:Valid
	@field:Size(min = 5, max = 5)
	val items: List<HvFieldBlock>,
)

/**
 * XLarge HV payload — **10** blocks ([PayloadSize.XLARGE] = 50 fields).
 *
 * @property items Exactly ten field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvXLargeRoot(
	@field:Valid
	@field:Size(min = 10, max = 10)
	val items: List<HvFieldBlock>,
)

/**
 * Very Large HV payload — **20** blocks ([PayloadSize.VERY_LARGE] = 100 fields).
 *
 * @property items Exactly twenty field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvVeryLargeRoot(
	@field:Valid
	@field:Size(min = 20, max = 20)
	val items: List<HvFieldBlock>,
)

/**
 * Extreme HV payload — **50** blocks ([PayloadSize.EXTREME] = 250 fields).
 *
 * @property items Exactly fifty field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvExtremeRoot(
	@field:Valid
	@field:Size(min = 50, max = 50)
	val items: List<HvFieldBlock>,
)

/**
 * Stress HV payload — **100** blocks ([PayloadSize.STRESS] = 500 fields).
 *
 * @property items Exactly one hundred field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvStressRoot(
	@field:Valid
	@field:Size(min = 100, max = 100)
	val items: List<HvFieldBlock>,
)

/**
 * Maximum Stress HV payload — **200** blocks ([PayloadSize.MAXIMUM] = 1000 fields).
 *
 * @property items Exactly two hundred field blocks.
 *
 * @author Ghaylan Saada
 */
data class HvMaximumRoot(
	@field:Valid
	@field:Size(min = 200, max = 200)
	val items: List<HvFieldBlock>,
)

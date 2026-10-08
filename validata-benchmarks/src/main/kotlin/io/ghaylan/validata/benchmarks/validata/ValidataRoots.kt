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

import io.ghaylan.validata.benchmarks.hibernate.HvSmallRoot
import io.ghaylan.validata.benchmarks.matrix.PayloadSize
import io.ghaylan.validata.constraint.annotation.Email
import io.ghaylan.validata.constraint.annotation.Min
import io.ghaylan.validata.constraint.annotation.RelativeToNow
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.Size
import io.ghaylan.validata.schema.Validatable
import java.time.LocalDate

/**
 * Realistic small Validata DTO — **5** heterogeneous fields ([PayloadSize.SMALL]).
 *
 * Not built from rotating slices. HV twin: [HvSmallRoot].
 *
 * Mapping: `@Required` ≈ HV `@NotBlank` (fixture domain); `@Min` / `@Email` / `@RelativeToNow(PAST)` / `@Size` exact.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataSmallRoot(
	@field:Required
	val name: String? = null,
	@field:Min("0")
	val age: Int? = null,
	@field:Email
	val email: String? = null,
	@field:RelativeToNow(relation = RelativeToNow.Relation.LT)
	val birthDate: LocalDate? = null,
	@field:Size(min = 1, max = 5)
	val tags: List<String>? = null,
)

/**
 * Medium Validata payload — **2** blocks ([PayloadSize.MEDIUM] = 10 fields).
 *
 * @property items Exactly two field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataMediumRoot(
	@field:Size(min = 2, max = 2)
	val items: List<ValidataFieldBlock>,
)

/**
 * Large Validata payload — **5** blocks ([PayloadSize.LARGE] = 25 fields).
 *
 * @property items Exactly five field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataLargeRoot(
	@field:Size(min = 5, max = 5)
	val items: List<ValidataFieldBlock>,
)

/**
 * XLarge Validata payload — **10** blocks ([PayloadSize.XLARGE] = 50 fields).
 *
 * @property items Exactly ten field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataXLargeRoot(
	@field:Size(min = 10, max = 10)
	val items: List<ValidataFieldBlock>,
)

/**
 * Very Large Validata payload — **20** blocks ([PayloadSize.VERY_LARGE] = 100 fields).
 *
 * @property items Exactly twenty field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataVeryLargeRoot(
	@field:Size(min = 20, max = 20)
	val items: List<ValidataFieldBlock>,
)

/**
 * Extreme Validata payload — **50** blocks ([PayloadSize.EXTREME] = 250 fields).
 *
 * @property items Exactly fifty field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataExtremeRoot(
	@field:Size(min = 50, max = 50)
	val items: List<ValidataFieldBlock>,
)

/**
 * Stress Validata payload — **100** blocks ([PayloadSize.STRESS] = 500 fields).
 *
 * @property items Exactly one hundred field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataStressRoot(
	@field:Size(min = 100, max = 100)
	val items: List<ValidataFieldBlock>,
)

/**
 * Maximum Stress Validata payload — **200** blocks ([PayloadSize.MAXIMUM] = 1000 fields).
 *
 * @property items Exactly two hundred field blocks.
 *
 * @author Ghaylan Saada
 */
@Validatable
data class ValidataMaximumRoot(
	@field:Size(min = 200, max = 200)
	val items: List<ValidataFieldBlock>,
)

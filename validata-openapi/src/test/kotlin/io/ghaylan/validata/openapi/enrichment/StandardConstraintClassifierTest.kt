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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.annotation.*
import io.ghaylan.validata.groups.OnDefault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import kotlin.reflect.KClass

class StandardConstraintClassifierTest {
	
	private val groups: Set<KClass<*>> = setOf(OnDefault::class)
	
	@Test
	@DisplayName("Size, Email, and Range are standard; Compare, Phone, and DaysOfWeek are custom")
	fun classifiesStandardVsCustom() {
		assertThat(StandardConstraintClassifier.isStandard(SizeConstraint(1, 2, "", groups))).isTrue()
		assertThat(StandardConstraintClassifier.isStandard(EmailConstraint("", groups))).isTrue()
		assertThat(
			StandardConstraintClassifier.isStandard(
				RangeConstraint(
					from = "1",
					to = "10",
					fromInclusive = true,
					toInclusive = true,
					negated = false, message = "",
					groups = groups,
				),
			),
		).isTrue()
		assertThat(
			StandardConstraintClassifier.isCustom(
				CompareConstraint(ref = "other", operation = Compare.Operation.EQ, message = "", groups = groups),
			),
		).isTrue()
		assertThat(StandardConstraintClassifier.isCustom(
			PhoneConstraint(allowedTypes = emptySet(), allowedCountries = emptySet(), message = "", groups = groups),
		)).isTrue()
		assertThat(
			StandardConstraintClassifier.isCustom(
				DaysOfWeekConstraint(days = setOf(DayOfWeek.MONDAY), negated = false, message = "", groups = groups),
			),
		).isTrue()
	}
}

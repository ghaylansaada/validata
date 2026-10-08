/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.contract

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Entry names are part of the KSP / IntelliJ discovery contract — rename = SemVer break.
 *
 * Uses the shared `validata-schema` enums (same contract as schema `ConstraintArgKindTest`).
 * 
 * @author Ghaylan Saada
 */
class ConstraintArgContractTest {
	
	@Test
	@DisplayName("ConstraintArgKind entry names stay stable for annotation-argument discovery")
	fun kindEntryNamesAreStable() {
		assertThat(ConstraintArgKind.entries.map { it.name }).containsExactly(
			"NOT_BLANK",
			"NON_EMPTY",
			"TYPED_LITERAL",
			"NON_NEGATIVE",
			"POSITIVE",
			"REGEX",
		)
	}
	
	@Test
	@DisplayName("ConstraintArgTarget entry names stay stable for annotation-argument discovery")
	fun targetEntryNamesAreStable() {
		assertThat(ConstraintArgTarget.entries.map { it.name }).containsExactly(
			"VALUE",
			"ELEMENT",
		)
	}
}

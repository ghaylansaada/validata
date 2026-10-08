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

package io.ghaylan.validata.intellij.discovery.constraintarg

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Pure-unit coverage for [ConstraintArgMarkerParser] text / kind / target decoding.
 * 
 * @author Ghaylan Saada
 */
class ConstraintArgMarkerParserTest {
	
	@Test
	@DisplayName("parseHostsFromText reads VALUE kinds from stub text")
	fun parseHostsFromTextValueKinds() {
		val hosts = ConstraintArgMarkerParser.parseHostsFromText(
			"""@ConstraintArg(kinds = [NON_NEGATIVE])""",
			"version",
		)
		assertThat(hosts).hasSize(1)
		assertThat(hosts.single().parameterName).isEqualTo("version")
		assertThat(hosts.single().target).isEqualTo(ConstraintArgTarget.VALUE)
		assertThat(hosts.single().kinds).containsExactly(ConstraintArgKind.NON_NEGATIVE)
	}
	
	@Test
	@DisplayName("parseHostsFromText reads ELEMENT target from body text")
	fun parseHostsFromTextElementTarget() {
		val hosts = ConstraintArgMarkerParser.parseHostsFromText(
			"""ConstraintArg(NOT_BLANK, TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)""",
			"values",
		)
		assertThat(hosts).hasSize(1)
		assertThat(hosts.single().target).isEqualTo(ConstraintArgTarget.ELEMENT)
		assertThat(hosts.single().kinds).containsExactlyInAnyOrder(
			ConstraintArgKind.NOT_BLANK,
			ConstraintArgKind.TYPED_LITERAL,
		)
	}
	
	@Test
	@DisplayName("parseHostsFromText ignores unknown kind tokens and empty bodies")
	fun parseHostsFromTextIgnoresJunk() {
		assertThat(
			ConstraintArgMarkerParser.parseHostsFromText("@ConstraintArg()", "x"),
		).isEmpty()
		assertThat(
			ConstraintArgMarkerParser.parseHostsFromText("@ConstraintArg(UNKNOWN)", "x"),
		).isEmpty()
		assertThat(
			ConstraintArgMarkerParser.parseHostsFromText("not a marker", "x"),
		).isEmpty()
	}
	
	@Test
	@DisplayName("toKind maps every ConstraintArgKind name and rejects unknown")
	fun toKindMapsAllKnownKinds() {
		for (kind in ConstraintArgKind.entries) {
			assertThat(ConstraintArgMarkerParser.toKind(kind.name)).isEqualTo(kind)
		}
		assertThat(ConstraintArgMarkerParser.toKind("NOPE")).isNull()
	}
	
	@Test
	@DisplayName("KIND_TOKEN finds kinds inside free-form attribute text")
	fun kindTokenFindsKinds() {
		val found = ConstraintArgMarkerParser.KIND_TOKEN.findAll("kinds = [NOT_BLANK, REGEX, POSITIVE]")
			.map { it.value }
			.toList()
		assertThat(found).containsExactly("NOT_BLANK", "REGEX", "POSITIVE")
	}
}

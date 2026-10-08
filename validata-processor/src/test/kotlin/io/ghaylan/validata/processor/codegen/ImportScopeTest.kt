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
package io.ghaylan.validata.processor.codegen

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [ImportScope] short-name rewriting.
 *
 * @author Ghaylan Saada
 */
class ImportScopeTest {

	@Test
	@DisplayName("ref imports unique types and returns simple names")
	fun refImportsUniqueTypes() {
		val b = KotlinSourceBuilder("com.acme.ghaylan.validata")
		val scope = ImportScope(b)
		assertThat(scope.ref("com.acme.User")).isEqualTo("User")
		assertThat(scope.ref("com.acme.User")).isEqualTo("User")
		val source = b.build("test")
		assertThat(source).contains("import com.acme.User")
	}

	@Test
	@DisplayName("ref keeps FQCN when simple names collide")
	fun refKeepsFqcnOnCollision() {
		val b = KotlinSourceBuilder("com.acme.ghaylan.validata")
		val scope = ImportScope(b)
		assertThat(scope.ref("com.acme.User")).isEqualTo("User")
		assertThat(scope.ref("com.other.User")).isEqualTo("com.other.User")
	}

	@Test
	@DisplayName("shortenExpression rewrites nested enum constants")
	fun shortenNestedEnum() {
		val b = KotlinSourceBuilder("com.acme.ghaylan.validata")
		val scope = ImportScope(b)
		val expr = scope.shortenExpression(
			"io.ghaylan.validata.constraint.annotation.RequiredConstraint(\n" +
				"    mode = io.ghaylan.validata.constraint.annotation.Required.Mode.STRICT,\n" +
				"    groups = setOf(io.ghaylan.validata.groups.OnDefault::class),\n" +
				")",
		)
		assertThat(expr).isEqualTo(
			"RequiredConstraint(\n" +
				"    mode = Mode.STRICT,\n" +
				"    groups = setOf(OnDefault::class),\n" +
				")",
		)
		val source = b.build("test")
		assertThat(source).contains("import io.ghaylan.validata.constraint.annotation.RequiredConstraint")
		assertThat(source).contains("import io.ghaylan.validata.constraint.annotation.Required.Mode")
		assertThat(source).contains("import io.ghaylan.validata.groups.OnDefault")
	}

	@Test
	@DisplayName("trimTrailingEnumEntry strips ALL_CAPS enum constants")
	fun trimTrailingEnumEntry() {
		assertThat(ImportScope.trimTrailingEnumEntry("java.time.temporal.ChronoUnit.DAYS"))
			.isEqualTo("java.time.temporal.ChronoUnit")
		assertThat(ImportScope.trimTrailingEnumEntry("io.pkg.Required.Mode"))
			.isEqualTo("io.pkg.Required.Mode")
	}
}

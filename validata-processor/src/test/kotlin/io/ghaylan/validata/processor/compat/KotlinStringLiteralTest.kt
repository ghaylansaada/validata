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
package io.ghaylan.validata.processor.compat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks Kotlin string-literal escaping used by every codegen / analyze renderer.
 * 
 * @author Ghaylan Saada
 */
class KotlinStringLiteralTest {
	
	@Test
	@DisplayName("plain text is unchanged")
	fun plain() {
		assertThat(KotlinStringLiteral.escape("plain")).isEqualTo("plain")
	}
	
	@Test
	@DisplayName("escapes backslash, quote, dollar, and control characters")
	fun specialCharacters() {
		assertThat(KotlinStringLiteral.escape("a\"b")).isEqualTo("a\\\"b")
		assertThat(KotlinStringLiteral.escape("a\\b")).isEqualTo("a\\\\b")
		assertThat(KotlinStringLiteral.escape("a\$b")).isEqualTo("a\\\$b")
		assertThat(KotlinStringLiteral.escape("a\nb")).isEqualTo("a\\nb")
		assertThat(KotlinStringLiteral.escape("a\rb")).isEqualTo("a\\rb")
		assertThat(KotlinStringLiteral.escape("a\tb")).isEqualTo("a\\tb")
	}
	
	@Test
	@DisplayName("dollar in JsonProperty-style names is escaped for string templates")
	fun escapesDollar() {
		assertThat(KotlinStringLiteral.escape("price\$amount")).isEqualTo("price\\\$amount")
	}
}

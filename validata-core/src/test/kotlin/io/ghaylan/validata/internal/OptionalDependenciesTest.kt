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
package io.ghaylan.validata.internal

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Optional backend presence checks used by `@Html` / `@Phone` (T6).
 *
 * Absence paths need a classpath without jsoup/libphonenumber (covered in samples/integration);
 * this module's test classpath includes both jars, so we lock the present + require-* happy path.
 * 
 * @author Ghaylan Saada
 */
class OptionalDependenciesTest {
	
	@Test
	@DisplayName("jsoup and libphonenumber are present on the api test classpath")
	fun backendsPresentInTests() {
		assertThat(OptionalDependencies.jsoupPresent).isTrue()
		assertThat(OptionalDependencies.libphonenumberPresent).isTrue()
	}
	
	@Test
	@DisplayName("requireJsoup and requireLibphonenumber succeed when jars are present")
	fun requireSucceedsWhenPresent() {
		assertThatCode { OptionalDependencies.requireJsoup() }.doesNotThrowAnyException()
		assertThatCode { OptionalDependencies.requireLibphonenumber() }.doesNotThrowAnyException()
	}
}

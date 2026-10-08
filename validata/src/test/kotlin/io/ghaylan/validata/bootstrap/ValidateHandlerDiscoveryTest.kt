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
package io.ghaylan.validata.bootstrap

import io.ghaylan.validata.bootstrap.fixture.ClassLevelValidateController
import io.ghaylan.validata.bootstrap.fixture.MethodLevelValidateController
import io.ghaylan.validata.bootstrap.fixture.PlainBootstrapController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.web.method.HandlerMethod

/**
 * Unit coverage for [ValidateHandlerDiscovery] — `@Validate` discovery on method, class, and non-web contexts.
 *
 * Threat: missing class-level `@Validate` discovery would leave every handler on that controller
 * unvalidated (historical bug).
 * 
 * @author Ghaylan Saada
 */
class ValidateHandlerDiscoveryTest {
	
	@Nested
	@DisplayName("findValidate")
	inner class FindValidate {
		
		@Test
		@DisplayName("findValidate resolves method-level @Validate")
		fun findValidateMethodLevel() {
			val controller = MethodLevelValidateController()
			val method = MethodLevelValidateController::class.java.getDeclaredMethod("create", String::class.java)
			val handler = HandlerMethod(controller, method)
			
			assertThat(ValidateHandlerDiscovery.findValidate(handler)).isNotNull
		}
		
		@Test
		@DisplayName("findValidate resolves class-level @Validate when the method itself is bare")
		fun findValidateClassLevel() {
			val controller = ClassLevelValidateController()
			val method = ClassLevelValidateController::class.java.getDeclaredMethod("create", String::class.java)
			val handler = HandlerMethod(controller, method)
			
			assertThat(ValidateHandlerDiscovery.findValidate(handler)).isNotNull
		}
		
		@Test
		@DisplayName("findValidate returns null when neither method nor class carries @Validate")
		fun findValidateUnannotated() {
			val controller = PlainBootstrapController()
			val method = PlainBootstrapController::class.java.getDeclaredMethod("echo", String::class.java)
			val handler = HandlerMethod(controller, method)
			
			assertThat(ValidateHandlerDiscovery.findValidate(handler)).isNull()
		}
		
		@Test
		@DisplayName("findValidate(Method) resolves class-level @Validate via declaring class")
		fun findValidateMethodOverloadClassLevel() {
			val method = ClassLevelValidateController::class.java.getDeclaredMethod("create", String::class.java)
			
			assertThat(ValidateHandlerDiscovery.findValidate(method)).isNotNull
			assertThat(ValidateHandlerDiscovery.findValidate(method, method.declaringClass)).isNotNull
		}
	}
	
	@Nested
	@DisplayName("findRequestValidationMethods")
	inner class FindRequestValidationMethods {
		
		@Test
		@DisplayName("returns empty map when there is no requestMappingHandlerMapping")
		fun findRequestValidationMethodsNonWeb() {
			AnnotationConfigApplicationContext().use { ctx ->
				ctx.refresh()
				assertThat(ValidateHandlerDiscovery.findRequestValidationMethods(ctx)).isEmpty()
			}
		}
	}
}

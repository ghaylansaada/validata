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

import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.engine.ValidationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.support.GenericApplicationContext

/**
 * Spring lifecycle adapter tests for [ValidationRegistryInitializer].
 * 
 * @author Ghaylan Saada
 */
class ValidationRegistryInitializerTest {
	
	@Test
	@DisplayName("afterPropertiesSet registers catalog validators including Required")
	fun registersValidators() {
		AnnotationConfigApplicationContext().use { ctx ->
			ctx.refresh()
			val registry = ValidationRegistry()
			ValidationRegistryInitializer(registry).apply {
				setApplicationContext(ctx)
				afterPropertiesSet()
			}
			val byType = registry.validatorCatalog()[RequiredConstraint::class]
			assertThat(byType).isNotNull.isNotEmpty
			assertThat(byType!!.values).contains(RequiredValidator)
		}
	}
	
	@Test
	@DisplayName("ignores ContextRefreshedEvent from a different (child) context")
	fun ignoresChildContextRefresh() {
		AnnotationConfigApplicationContext().use { parent ->
			parent.refresh()
			val registry = ValidationRegistry()
			val initializer = ValidationRegistryInitializer(registry).apply {
				setApplicationContext(parent)
				afterPropertiesSet()
			}
			val child = GenericApplicationContext(parent)
			child.refresh()
			initializer.onApplicationEvent(ContextRefreshedEvent(child))
			
			assertThat(registry.staticSchemas()).isEmpty()
		}
	}
	
	@Test
	@DisplayName("non-web context registers no static endpoint schemas")
	fun nonWebRegistersNoStaticSchemas() {
		AnnotationConfigApplicationContext().use { ctx ->
			ctx.refresh()
			val registry = ValidationRegistry()
			val initializer = ValidationRegistryInitializer(registry).apply {
				setApplicationContext(ctx)
				afterPropertiesSet()
			}
			initializer.onApplicationEvent(ContextRefreshedEvent(ctx))
			
			assertThat(registry.staticSchemas()).isEmpty()
			assertThat(registry.isFrozen()).isTrue()
		}
	}
}

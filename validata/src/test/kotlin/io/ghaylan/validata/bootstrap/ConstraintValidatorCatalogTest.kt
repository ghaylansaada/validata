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

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.spi.GeneratedConstraintCatalogs
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * Host-side catalog assembly via [ConstraintValidatorCatalog] + [GeneratedConstraintCatalogs].
 *
 * Threat: ignoring a Spring `@Bean` of a validator type would leave customized validators unused.
 * 
 * @author Ghaylan Saada
 */
class ConstraintValidatorCatalogTest {
	
	@Nested
	@DisplayName("Default catalog")
	inner class DefaultCatalog {
		
		@Test
		@DisplayName("buildValidators registers built-in metadata types from the generated catalog")
		fun registersBuiltInsFromCatalog() {
			AnnotationConfigApplicationContext().use { ctx ->
				ctx.refresh()
				val catalog = ConstraintValidatorCatalog.buildValidators(ctx)
				
				assertThat(GeneratedConstraintCatalogs.all()).isNotEmpty
				assertThat(catalog.keys).contains(RequiredConstraint::class, SizeConstraint::class)
				assertThat(catalog.values.sumOf { it.size }).isGreaterThanOrEqualTo(39)
			}
		}
		
		@Test
		@DisplayName("every catalog entry produces a non-null validator instance")
		fun everyEntryResolvesAnInstance() {
			AnnotationConfigApplicationContext().use { ctx ->
				ctx.refresh()
				val catalog = ConstraintValidatorCatalog.buildValidators(ctx)
				assertThat(catalog.values.flatMap { it.values }).isNotEmpty
				assertThat(catalog.values.flatMap { it.values }).doesNotContainNull()
			}
		}
		
		@Test
		@DisplayName("RequiredConstraint maps to the shared RequiredValidator instance via default factory")
		fun requiredUsesSharedInstance() {
			AnnotationConfigApplicationContext().use { ctx ->
				ctx.refresh()
				val byType = ConstraintValidatorCatalog.buildValidators(ctx)
					.getValue(RequiredConstraint::class)
				assertThat(byType.values).contains(RequiredValidator)
			}
		}
		
		@Test
		@DisplayName("catalog size matches GeneratedConstraintCatalogs entry count")
		fun catalogSizeMatchesSpiEntries() {
			AnnotationConfigApplicationContext().use { ctx ->
				ctx.refresh()
				val entries = GeneratedConstraintCatalogs.all()
				val resolved = ConstraintValidatorCatalog.buildValidators(ctx).values.sumOf { it.size }
				assertThat(resolved).isEqualTo(entries.size)
			}
		}
	}
	
	@Nested
	@DisplayName("Spring bean override")
	inner class SpringBeanOverride {
		
		@Test
		@DisplayName("a Spring bean of the validator type wins over the default factory")
		fun springBeanWinsOverDefaultFactory() {
			@Suppress("UNCHECKED_CAST")
			val beanOverride = Mockito.mock(ConstraintValidator::class.java) as ConstraintValidator<*, *>
			val ctx = Mockito.mock(ApplicationContext::class.java)
			Mockito.`when`(ctx.getBeanProvider(Mockito.any(Class::class.java)))
				.thenAnswer { invocation ->
					@Suppress("UNCHECKED_CAST")
					val type = invocation.arguments[0] as Class<*>
					
					@Suppress("UNCHECKED_CAST")
					val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<Any>
					if (type == RequiredValidator::class.java) {
						Mockito.`when`(provider.getIfAvailable())
							.thenReturn(beanOverride)
					}
					else {
						Mockito.`when`(provider.getIfAvailable())
							.thenReturn(null)
					}
					provider
				}
			val catalog = ConstraintValidatorCatalog.buildValidators(ctx)
			val resolved = catalog.getValue(RequiredConstraint::class).values
			
			assertThat(resolved).contains(beanOverride)
			assertThat(resolved).doesNotContain(RequiredValidator)
		}
	}
}

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
package io.ghaylan.validata.config

import io.ghaylan.validata.config.fixture.AnnotatedConfigBean
import io.ghaylan.validata.config.fixture.ValidateOnlyBean
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.exception.ConfigurationValidationException
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.ObjectProvider

/**
 * Unit edges for [ConfigurationValidationPostProcessor] (no Boot ApplicationContextRunner).
 *
 * Threat: fail-open when the engine is missing would start the app with invalid configuration.
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationPostProcessorTest {
	
	@Nested
	@DisplayName("Annotation gates")
	inner class AnnotationGates {
		
		@Test
		@DisplayName("beans without @Validate are returned without consulting the engine")
		fun unannotatedBeanSkipped() {
			@Suppress("UNCHECKED_CAST")
			val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<ValidatorEngine>
			Mockito.`when`(provider.getIfAvailable())
				.thenThrow(AssertionError("engine must not be resolved"))
			val processor = ConfigurationValidationPostProcessor(provider)
			val bean = Any()
			
			assertThat(processor.postProcessAfterInitialization(bean, "plain")).isSameAs(bean)
			Mockito.verify(provider, Mockito.never())
				.getIfAvailable()
		}
		
		@Test
		@DisplayName("@Validate without @ConfigurationProperties is ignored (MVC dual-use of the marker)")
		fun validateWithoutConfigurationPropertiesSkipped() {
			@Suppress("UNCHECKED_CAST")
			val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<ValidatorEngine>
			Mockito.`when`(provider.getIfAvailable())
				.thenThrow(AssertionError("engine must not be resolved"))
			val processor = ConfigurationValidationPostProcessor(provider)
			val bean = ValidateOnlyBean()
			
			assertThat(processor.postProcessAfterInitialization(bean, "controllerLike")).isSameAs(bean)
			Mockito.verify(provider, Mockito.never())
				.getIfAvailable()
		}
	}
	
	@Nested
	@DisplayName("Engine presence and violations")
	inner class EngineAndViolations {
		
		@Test
		@DisplayName("when the engine ObjectProvider has no bean a targeted config bean fails closed")
		fun nullEngineProviderFailsClosed() {
			@Suppress("UNCHECKED_CAST")
			val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<ValidatorEngine>
			Mockito.`when`(provider.getIfAvailable())
				.thenReturn(null)
			val processor = ConfigurationValidationPostProcessor(provider)
			val bean = AnnotatedConfigBean()
			
			assertThatThrownBy {
				processor.postProcessAfterInitialization(bean, "annotatedConfigBean")
			}.isInstanceOf(IllegalStateException::class.java)
				.hasMessageContaining("annotatedConfigBean")
				.hasMessageContaining("ValidatorEngine")
				.hasMessageContaining("ValidationConfig")
		}
		
		@Test
		@DisplayName("invalid @Validate @ConfigurationProperties throws ConfigurationValidationException with prefix")
		fun invalidBeanThrowsPrefixedException() {
			val bean = AnnotatedConfigBean()
			val engine = Mockito.mock(ValidatorEngine::class.java)
			Mockito.`when`(engine.validate(bean))
				.thenReturn(
					listOf(
						ConstraintError(
							path = "host",
							code = ConstraintErrorCode.TEXT_TOO_SHORT,
							message = "too short",
						),
					),
				)
			@Suppress("UNCHECKED_CAST")
			val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<ValidatorEngine>
			Mockito.`when`(provider.getIfAvailable())
				.thenReturn(engine)
			val processor = ConfigurationValidationPostProcessor(provider)
			
			assertThatThrownBy {
				processor.postProcessAfterInitialization(bean, "mail")
			}.isInstanceOfSatisfying(ConfigurationValidationException::class.java) { failure ->
				assertThat(failure.beanName).isEqualTo("mail")
				assertThat(failure.propertyPrefix).isEqualTo("test.")
				assertThat(failure.message).contains("test.host")
			}
		}
		
		@Test
		@DisplayName("a valid targeted config bean is returned unchanged when the engine reports no errors")
		fun validBeanReturnedUnchanged() {
			val bean = AnnotatedConfigBean()
			val engine = Mockito.mock(ValidatorEngine::class.java)
			Mockito.`when`(engine.validate(bean))
				.thenReturn(emptyList())
			@Suppress("UNCHECKED_CAST")
			val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<ValidatorEngine>
			Mockito.`when`(provider.getIfAvailable())
				.thenReturn(engine)
			val processor = ConfigurationValidationPostProcessor(provider)
			
			assertThat(processor.postProcessAfterInitialization(bean, "mail")).isSameAs(bean)
			Mockito.verify(engine)
				.validate(bean)
		}
	}
}

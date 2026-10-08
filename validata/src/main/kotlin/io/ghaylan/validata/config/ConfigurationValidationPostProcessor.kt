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

import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.exception.ConfigurationValidationException
import io.ghaylan.validata.exception.ConfigurationValidationReport
import io.ghaylan.validata.schema.Validate
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.AnnotationUtils

/**
 * Validates `@ConfigurationProperties` beans that also carry [Validate], immediately after
 * initialization.
 *
 * On failure throws a structured [ConfigurationValidationException] (bean identity + errors).
 * Human-readable text is owned by [ConfigurationValidationReport].
 *
 * Controllers may also use [Validate] for HTTP; this processor ignores types that are not
 * `@ConfigurationProperties` so the same marker stays dual-purpose.
 *
 * The engine is injected as an [ObjectProvider]: a hard constructor dependency would pull
 * [ValidatorEngine] into early BeanPostProcessor instantiation and trigger the classic
 * "not eligible for getting processed by all BeanPostProcessors" warning.
 *
 * Registered as a `@Bean` from [ValidationConfig] (`@ConditionalOnMissingBean`), not as a
 * `@Component`, so it is never a component-scan target. Prefer relying on auto-configuration:
 *
 * ```kotlin
 * @Validate
 * @ConfigurationProperties(prefix = "app.mail")
 * data class MailProperties(
 *     @field:Required
 *     var host: String? = null,
 * )
 * ```
 *
 * @property validatorEngine Lazily resolved engine used to evaluate the bean's constraints.*
 * 
 * @author Ghaylan Saada
 */
class ConfigurationValidationPostProcessor(
	private val validatorEngine: ObjectProvider<ValidatorEngine>,
) : BeanPostProcessor {

	/**
	 * Runs configuration validation when the bean carries both [Validate] and
	 * `@ConfigurationProperties`.
	 *
	 * Fail closed: if the bean is a validation target and no [ValidatorEngine] is available, this
	 * method throws rather than skipping validation.
	 *
	 * @param bean bean instance created by the container
	 * @param beanName registered name in the application context
	 * @return the original [bean] when validation passes or the bean is not targeted
	 * @throws IllegalStateException when the bean is a validation target but no [ValidatorEngine] is available
	 * @throws ConfigurationValidationException when one or more constraints are violated
	 */
	override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
		// `bean::class.java` is the CGLIB subclass for a proxied bean, and annotations are not
		// inherited by generated subclasses — which silently skipped `@ConfigurationProperties`
		// beans. Resolve the target class and use AnnotatedElementUtils so @Validate works
		// as a meta-annotation too.
		//
		// Require `@ConfigurationProperties` as well: the same `@Validate` marker is used on
		// MVC controllers (HTTP), which must not be treated as config beans here.
		val targetClass = AopUtils.getTargetClass(bean)

		if (!AnnotatedElementUtils.hasAnnotation(targetClass, Validate::class.java)) return bean
		if (!AnnotatedElementUtils.hasAnnotation(targetClass, ConfigurationProperties::class.java)) return bean

		val engine = validatorEngine.getIfAvailable()
			?: error("Cannot validate @Validate @ConfigurationProperties bean '$beanName' " +
					"(${targetClass.name}): no ValidatorEngine bean is available. " +
					"Ensure Validata auto-configuration is on the classpath and not excluded " +
					"(e.g. spring.autoconfigure.exclude must not remove ValidationConfig).")

		val errors = engine.validate(bean)
		if (errors.isEmpty()) return bean

		val configProperties = AnnotationUtils.findAnnotation(targetClass, ConfigurationProperties::class.java)
		val propertyPrefix = configProperties?.prefix?.takeIf(String::isNotBlank)?.let { "$it." } ?: ""

		throw ConfigurationValidationException(
			beanName = beanName,
			targetClassName = targetClass.name,
			propertyPrefix = propertyPrefix,
			errors = errors)
	}
}

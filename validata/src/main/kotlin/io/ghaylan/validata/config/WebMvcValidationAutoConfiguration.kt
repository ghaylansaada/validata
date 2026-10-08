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
import io.ghaylan.validata.web.HandlerValidationPlan
import io.ghaylan.validata.web.ValidatedEndpointPlanCache
import io.ghaylan.validata.web.ValidatingServletInvocableHandlerMethod
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod

/**
 * Registers [ValidatingServletInvocableHandlerMethod] into Spring MVC via [WebMvcRegistrations].
 *
 * Substitutes the default invocable factory on [RequestMappingHandlerAdapter] so validation runs
 * after argument binding and before the controller method — without replacing Boot's broader MVC
 * auto-configuration.
 *
 * Ordered after [ValidationConfig] so [ValidatedEndpointPlanCache] exists before this adapter bean
 * is constructed. Gated with [ConditionalOnWebApplication] because the plan-cache bean is web-only;
 * without this condition a non-web Boot app that depends on this jar for `@ConfigurationProperties`
 * validation would fail context refresh on a missing constructor argument.
 *
 * To disable MVC substitution in a web app, exclude this auto-configuration via
 * `spring.autoconfigure.exclude` with this class's binary name.
 *
 * @property validatorEngine Engine passed into every validating invocable.
 * @property endpointPlanCache Shared plan cache (annotation + schema resolve once per method).*
 * 
 * @author Ghaylan Saada
 */
@AutoConfiguration
@AutoConfigureAfter(ValidationConfig::class)
@ConditionalOnWebApplication
class WebMvcValidationAutoConfiguration(
	private val validatorEngine: ValidatorEngine,
	private val endpointPlanCache: ValidatedEndpointPlanCache,
): WebMvcRegistrations {
	
	/**
	 * Supplies a [RequestMappingHandlerAdapter] that builds [ValidatingServletInvocableHandlerMethod]
	 * for every mapped handler.
	 *
	 * Unannotated handlers still receive the validating invocable; [ValidatedEndpointPlanCache]
	 * returns [HandlerValidationPlan.Skip] for them so the hot path stays a single cache lookup
	 * after the first resolve.
	 *
	 * @return adapter wired with the validating invocable factory	 
	 */
	override fun getRequestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
		return object: RequestMappingHandlerAdapter() {
			/**
			 * Creates a validating invocable for every mapped handler.
			 *
			 * @param handlerMethod destination handler
			 * @return invocable that consults [endpointPlanCache] before the controller body			 
			 */
			override fun createInvocableHandlerMethod(handlerMethod: HandlerMethod): ServletInvocableHandlerMethod {
				return ValidatingServletInvocableHandlerMethod(
					handlerMethod = handlerMethod,
					validatorEngine = validatorEngine,
					endpointPlanCache = endpointPlanCache)
			}
		}
	}
}

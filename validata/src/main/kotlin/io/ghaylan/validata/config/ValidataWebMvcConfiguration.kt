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

import io.ghaylan.validata.exception.ValidataExceptionResolver
import io.ghaylan.validata.exception.ValidataExceptionTranslator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver

/**
 * Boot auto-configuration that installs [ValidataExceptionResolver] into the MVC exception chain.
 *
 * Locates Boot's [ExceptionHandlerExceptionResolver] as the render delegate and prepends
 * [ValidataExceptionResolver] so Validata can normalize request failures before other resolvers run.
 *
 * Gated with [ConditionalOnWebApplication] so non-web Boot apps that depend on this jar only for
 * `@ConfigurationProperties` validation do not register a servlet MVC configurer.*
 * 
 * @author Ghaylan Saada
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ValidataWebMvcConfiguration: WebMvcConfigurer {
	
	/**
	 * Prepends a [ValidataExceptionResolver] when an [ExceptionHandlerExceptionResolver] is present.
	 *
	 * Mutates [resolvers] in place (insert at index `0`). No-op when no exception-handler resolver
	 * exists yet.
	 *
	 * @param resolvers mutable MVC exception-resolver list owned by the framework	 
	 */
	override fun extendHandlerExceptionResolvers(
		resolvers: MutableList<HandlerExceptionResolver>,
	) {
		val exceptionHandlerResolver = resolvers.filterIsInstance<ExceptionHandlerExceptionResolver>()
			.firstOrNull()
			?: return
		
		val resolver = ValidataExceptionResolver(
			translator = ValidataExceptionTranslator(),
			delegate = exceptionHandlerResolver)
		
		resolvers.add(0, resolver)
	}
}

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
package io.ghaylan.validata.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver

/**
 * [HandlerExceptionResolver] that rewrites Spring / Jackson request failures into
 * [ConstraintViolationException] and re-dispatches through Boot's exception-handler resolver.
 *
 * Runs at [Ordered.HIGHEST_PRECEDENCE]. Leaves [ConstraintViolationException] untouched so
 * `@ExceptionHandler` / later resolvers own the response.
 *
 * @property translator Maps supported Spring exceptions into [ConstraintViolationException]
 * @property delegate Boot [ExceptionHandlerExceptionResolver] that renders the translated exception*
 * 
 * @author Ghaylan Saada
 */
class ValidataExceptionResolver(
	private val translator: ValidataExceptionTranslator,
	private val delegate: ExceptionHandlerExceptionResolver,
): HandlerExceptionResolver, Ordered {
	
	/**
	 * Translates [ex] when possible, then resolves the resulting [ConstraintViolationException]
	 * via [delegate].
	 *
	 * May write the HTTP response through [delegate]. Does not mutate [ex].
	 *
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler matched handler, often a [HandlerMethod]; may be `null`
	 * @param ex exception raised while handling the request
	 * @return [ModelAndView] from [delegate], or `null` when [ex] is already a
	 *   [ConstraintViolationException] or [translator] cannot map it	 
	 */
	override fun resolveException(
		request: HttpServletRequest,
		response: HttpServletResponse,
		handler: Any?,
		ex: Exception,
	): ModelAndView? {
		if (ex is ConstraintViolationException) {
			return null
		}
		
		val violation = translator.translate(
			ex,
			handler as? HandlerMethod
		) ?: return null
		
		return delegate.resolveException(request, response, handler, violation)
	}
	
	/**
	 * Resolver order: highest precedence so translation runs before other resolvers.
	 *
	 * @return [Ordered.HIGHEST_PRECEDENCE]	 
	 */
	override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}

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

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.Ordered
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver

/**
 * Unit coverage for [ValidataExceptionResolver] — translate then re-dispatch, leave CVEs alone.
 * 
 * @author Ghaylan Saada
 */
class ValidataExceptionResolverTest {
	
	@Test
	@DisplayName("ConstraintViolationException is left for later resolvers (returns null)")
	fun leavesConstraintViolationException() {
		val translator = Mockito.mock(ValidataExceptionTranslator::class.java)
		val delegate = Mockito.mock(ExceptionHandlerExceptionResolver::class.java)
		val resolver = ValidataExceptionResolver(translator, delegate)
		val mav = resolver.resolveException(
			Mockito.mock(HttpServletRequest::class.java),
			Mockito.mock(HttpServletResponse::class.java),
			null,
			ConstraintViolationException(
				errors = listOf(ConstraintError(code = ConstraintErrorCode.VALUE_MISSING)),
			),
		)
		
		assertThat(mav).isNull()
		Mockito.verifyNoInteractions(translator)
		Mockito.verifyNoInteractions(delegate)
	}
	
	@Test
	@DisplayName("unmapped Spring exceptions return null without calling the delegate")
	fun unmappedReturnsNull() {
		val translator = Mockito.mock(ValidataExceptionTranslator::class.java)
		val delegate = Mockito.mock(ExceptionHandlerExceptionResolver::class.java)
		val resolver = ValidataExceptionResolver(translator, delegate)
		val ex = IllegalArgumentException("nope")
		Mockito.`when`(translator.translate(ex, null))
			.thenReturn(null)
		val mav = resolver.resolveException(
			Mockito.mock(HttpServletRequest::class.java),
			Mockito.mock(HttpServletResponse::class.java),
			null,
			ex,
		)
		
		assertThat(mav).isNull()
		Mockito.verify(delegate, Mockito.never())
			.resolveException(
				Mockito.any(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(),
			)
	}
	
	@Test
	@DisplayName("translated violations are re-dispatched through the exception-handler resolver")
	fun translatedIsDelegated() {
		val translator = Mockito.mock(ValidataExceptionTranslator::class.java)
		val delegate = Mockito.mock(ExceptionHandlerExceptionResolver::class.java)
		val resolver = ValidataExceptionResolver(translator, delegate)
		val request = Mockito.mock(HttpServletRequest::class.java)
		val response = Mockito.mock(HttpServletResponse::class.java)
		val handler = Mockito.mock(HandlerMethod::class.java)
		val springEx = IllegalArgumentException("binding")
		val violation = ConstraintViolationException(
			errors = listOf(ConstraintError(code = ConstraintErrorCode.VALUE_MISSING, path = "q")),
		)
		val expected = ModelAndView("ignored")
		Mockito.`when`(translator.translate(springEx, handler))
			.thenReturn(violation)
		Mockito.`when`(delegate.resolveException(request, response, handler, violation))
			.thenReturn(expected)
		val mav = resolver.resolveException(request, response, handler, springEx)
		
		assertThat(mav).isSameAs(expected)
		Mockito.verify(delegate)
			.resolveException(request, response, handler, violation)
	}
	
	@Test
	@DisplayName("runs at highest precedence so translation happens before other resolvers")
	fun highestPrecedence() {
		val resolver = ValidataExceptionResolver(
			ValidataExceptionTranslator(),
			Mockito.mock(ExceptionHandlerExceptionResolver::class.java),
		)
		assertThat(resolver.order).isEqualTo(Ordered.HIGHEST_PRECEDENCE)
	}
}

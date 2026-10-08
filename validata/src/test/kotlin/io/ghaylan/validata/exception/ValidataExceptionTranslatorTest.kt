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

import io.ghaylan.validata.exception.fixture.ClassLevelTranslatorProbeController
import io.ghaylan.validata.exception.fixture.TranslatorProbeController
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ErrorLocation
import io.ghaylan.validata.web.fixture.controller.MethodLevelController
import io.ghaylan.validata.web.fixture.dto.UserDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tools.jackson.databind.exc.MismatchedInputException
import java.io.ByteArrayInputStream
import java.lang.reflect.Method

/**
 * Unit coverage for [ValidataExceptionTranslator] — binding / Jackson rewrite into
 * [ConstraintViolationException].
 * 
 * @author Ghaylan Saada

 */
class ValidataExceptionTranslatorTest {

	private val translator = ValidataExceptionTranslator()
	private val controller = TranslatorProbeController()

	@Nested
	@DisplayName("Jackson message classification")
	inner class JacksonPrefix {

		@Test
		@DisplayName("Jackson missing-creator message prefix is recognized")
		fun missingCreatorPropertyPrefix() {
			assertThat(
				ValidataExceptionTranslator.isMissingCreatorPropertyMessage(
					"${ValidataExceptionTranslator.MISSING_CREATOR_PROPERTY_PREFIX} 'name' (index 0/1)",
				),
			).isTrue()
		}

		@Test
		@DisplayName("other Jackson mismatch messages are not treated as missing creator")
		fun otherMismatchMessages() {
			assertThat(
				ValidataExceptionTranslator.isMissingCreatorPropertyMessage(
					"Cannot deserialize value of type `int` from String \"x\"",
				),
			).isFalse()
			assertThat(ValidataExceptionTranslator.isMissingCreatorPropertyMessage(null)).isFalse()
			assertThat(ValidataExceptionTranslator.isMissingCreatorPropertyMessage("")).isFalse()
		}
	}

	@Nested
	@DisplayName("Missing required arguments")
	inner class MissingRequired {

		@Test
		@DisplayName("missing query with @Required becomes VALUE_MISSING at QUERY")
		fun missingRequiredQuery() {
			val method = method("requiredQuery", String::class.javaObjectType)
			val ex = MissingServletRequestParameterException("q", MethodParameter(method, 0), false)

			val result = translator.translate(ex, HandlerMethod(controller, method))

			assertThat(result).isNotNull
			assertThat(result!!.errors).hasSize(1)
			assertThat(result.errors[0].path).isEqualTo("q")
			assertThat(result.errors[0].location).isEqualTo(ErrorLocation.QUERY)
			assertThat(result.errors[0].code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}

		@Test
		@DisplayName("missing query without @Required is left unmapped")
		fun missingQueryWithoutRequired() {
			val method = method("optionalQuery", String::class.javaObjectType)
			val ex = MissingServletRequestParameterException("q", MethodParameter(method, 0), false)

			assertThat(translator.translate(ex, HandlerMethod(controller, method))).isNull()
		}

		@Test
		@DisplayName("missing path with @Required becomes VALUE_MISSING at PATH")
		fun missingRequiredPath() {
			val method = method("requiredPath", String::class.javaObjectType)
			val ex = MissingPathVariableException("id", MethodParameter(method, 0))

			val result = translator.translate(ex, HandlerMethod(controller, method))!!

			assertThat(result.errors.single().path).isEqualTo("id")
			assertThat(result.errors.single().location).isEqualTo(ErrorLocation.PATH)
			assertThat(result.errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}

		@Test
		@DisplayName("missing header with @Required becomes VALUE_MISSING at HEADER")
		fun missingRequiredHeader() {
			val method = method("requiredHeader", String::class.javaObjectType)
			val ex = MissingRequestHeaderException("X-Tenant", MethodParameter(method, 0))

			val result = translator.translate(ex, HandlerMethod(controller, method))!!

			assertThat(result.errors.single().path).isEqualTo("X-Tenant")
			assertThat(result.errors.single().location).isEqualTo(ErrorLocation.HEADER)
			assertThat(result.errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
		}
	}

	@Nested
	@DisplayName("Type mismatch")
	inner class TypeMismatch {

		@Test
		@DisplayName("query type mismatch with @Required becomes VALUE_TYPE_MISMATCH at QUERY")
		fun requiredQueryTypeMismatch() {
			val method = method("requiredQuery", String::class.javaObjectType)
			val parameter = MethodParameter(method, 0)
			val ex = MethodArgumentTypeMismatchException(
				"xx",
				Int::class.javaObjectType,
				"q",
				parameter,
				IllegalArgumentException("bad"),
			)

			val result = translator.translate(ex, HandlerMethod(controller, method))!!

			assertThat(result.errors.single().path).isEqualTo("q")
			assertThat(result.errors.single().location).isEqualTo(ErrorLocation.QUERY)
			assertThat(result.errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_TYPE_MISMATCH)
		}

		@Test
		@DisplayName("type mismatch without @Required is left unmapped")
		fun typeMismatchWithoutRequired() {
			val method = method("optionalQuery", String::class.javaObjectType)
			val ex = MethodArgumentTypeMismatchException(
				"xx",
				Int::class.javaObjectType,
				"q",
				MethodParameter(method, 0),
				IllegalArgumentException("bad"),
			)

			assertThat(translator.translate(ex, HandlerMethod(controller, method))).isNull()
		}
	}

	@Nested
	@DisplayName("Bean validation / Jackson")
	inner class BeanAndJackson {

		@Test
		@DisplayName("MethodArgumentNotValidException without a parameter-level marker is left unmapped")
		fun methodArgumentNotValidUnmarked() {
			// `@Validate` targets only FUNCTION/CLASS — bean-validation failures on a body
			// parameter are not rewritten by this host (parameter has no Validata marker).
			val method = MethodLevelController::class.java.getDeclaredMethod("createUser", UserDto::class.java)
			val parameter = MethodParameter(method, 0)
			val target = Any()
			val binding = BeanPropertyBindingResult(target, "user")
			binding.addError(FieldError("user", "name", null, false, arrayOf("Size"), null, "too short"))
			val ex = MethodArgumentNotValidException(parameter, binding)

			assertThat(
				translator.translate(ex, HandlerMethod(MethodLevelController(), method)),
			).isNull()
		}

		@Test
		@DisplayName("HttpMessageNotReadableException without @Validate is left unmapped")
		fun jacksonUnmarked() {
			val method = method("unmarkedMethod")
			val ex = HttpMessageNotReadableException("unreadable", emptyInputMessage())

			assertThat(translator.translate(ex, HandlerMethod(controller, method))).isNull()
		}

		@Test
		@DisplayName("class-level @Validate still maps Jackson body type mismatches")
		fun jacksonClassLevelValidate() {
			val bean = ClassLevelTranslatorProbeController()
			val method = ClassLevelTranslatorProbeController::class.java.getDeclaredMethod("ping")
			val handler = HandlerMethod(bean, method)
			val cause = mismatchedInput("age", "Cannot deserialize value of type `int` from String \"x\"")
			val ex = HttpMessageNotReadableException("unreadable", cause, emptyInputMessage())

			val result = translator.translate(ex, handler)!!

			assertThat(result.errors.single().path).isEqualTo("age")
			assertThat(result.errors.single().location).isEqualTo(ErrorLocation.BODY)
			assertThat(result.errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_TYPE_MISMATCH)
		}

		@Test
		@DisplayName("Jackson missing-creator message becomes VALUE_MISSING")
		fun jacksonMissingCreator() {
			val method = method("validatedMethod")
			val cause = mismatchedInput(
				"name",
				"${ValidataExceptionTranslator.MISSING_CREATOR_PROPERTY_PREFIX} 'name' (index 0/1)",
			)
			val ex = HttpMessageNotReadableException("unreadable", cause, emptyInputMessage())

			val result = translator.translate(ex, HandlerMethod(controller, method))!!

			assertThat(result.errors.single().code).isEqualTo(ConstraintErrorCode.VALUE_MISSING)
			assertThat(result.errors.single().path).isEqualTo("name")
		}
	}

	@Nested
	@DisplayName("Unmapped input")
	inner class Unmapped {

		@Test
		@DisplayName("unsupported exception types return null")
		fun unsupportedException() {
			assertThat(translator.translate(IllegalStateException("x"), null)).isNull()
		}
	}

	private fun method(name: String, vararg parameterTypes: Class<*>): Method =
		TranslatorProbeController::class.java.getDeclaredMethod(name, *parameterTypes)

	private fun emptyInputMessage(): HttpInputMessage =
		object : HttpInputMessage {
			override fun getBody() = ByteArrayInputStream(ByteArray(0))
			override fun getHeaders() = HttpHeaders()
		}

	private fun mismatchedInput(property: String, message: String): MismatchedInputException {
		val ex = MismatchedInputException.from(null, null as Class<*>?, message)
		ex.prependPath(Any(), property)
		return ex
	}
}

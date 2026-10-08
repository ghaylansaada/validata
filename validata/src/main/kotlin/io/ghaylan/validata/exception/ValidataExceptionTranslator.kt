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

import io.ghaylan.validata.bootstrap.ValidateHandlerDiscovery
import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ErrorLocation
import io.ghaylan.validata.schema.Validate
import java.lang.reflect.Method
import org.springframework.core.MethodParameter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tools.jackson.databind.DatabindException
import tools.jackson.databind.exc.InvalidNullException
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.UnrecognizedPropertyException

/**
 * Maps Spring MVC / Jackson request failures into [ConstraintViolationException].
 *
 * Only translates exceptions tied to Validata markers ([Required], [Validate]).
 * Class- and method-level `@Validate` detection matches [ValidateHandlerDiscovery].*
 * 
 * @author Ghaylan Saada
 */
class ValidataExceptionTranslator {
	
	/**
	 * Translates a handled Spring exception into a [ConstraintViolationException] when applicable.
	 *
	 * No I/O; does not mutate [ex].
	 *
	 * @param ex exception from the MVC pipeline
	 * @param handler current handler method, used for body / Jackson / method-validation cases; may be `null`
	 * @return violation exception with one or more [ConstraintError]s, or `null` when unmapped	 
	 */
	fun translate(
		ex: Exception,
		handler: HandlerMethod?,
	): ConstraintViolationException? {
		return when (ex) {
			is MissingServletRequestParameterException -> translate(ex)
			is MissingPathVariableException -> translate(ex)
			is MissingRequestHeaderException -> translate(ex)
			is MethodArgumentTypeMismatchException -> translate(ex)
			is MethodArgumentNotValidException -> translate(ex)
			is HandlerMethodValidationException -> translate(ex, handler)
			is HttpMessageNotReadableException -> translate(ex, handler)
			else -> null
		}?.takeIf(List<*>::isNotEmpty)?.let(::ConstraintViolationException)
	}
	
	/**
	 * Maps a missing query parameter to [ConstraintErrorCode.VALUE_MISSING] when [Required] is present.	 
	 */
	private fun translate(
		ex: MissingServletRequestParameterException
	): List<ConstraintError<*>>? = missingRequired(
		marked = ex.methodParameter?.hasAnnotation<Required>() == true,
		path = ex.parameterName,
		location = ErrorLocation.QUERY)
	
	/**
	 * Maps a missing path variable to [ConstraintErrorCode.VALUE_MISSING] when [Required] is present.	 
	 */
	private fun translate(
		ex: MissingPathVariableException
	): List<ConstraintError<*>>? = missingRequired(
		marked = ex.parameter.hasAnnotation<Required>(),
		path = ex.variableName,
		location = ErrorLocation.PATH)
	
	/**
	 * Maps a missing request header to [ConstraintErrorCode.VALUE_MISSING] when [Required] is present.	 
	 */
	private fun translate(
		ex: MissingRequestHeaderException
	): List<ConstraintError<*>>? = missingRequired(
		marked = ex.parameter.hasAnnotation<Required>(),
		path = ex.headerName,
		location = ErrorLocation.HEADER)
	
	/**
	 * Maps a path/query/header type mismatch to [ConstraintErrorCode.VALUE_TYPE_MISMATCH] when [Required] is present.	 
	 */
	private fun translate(ex: MethodArgumentTypeMismatchException): List<ConstraintError<*>>? {
		val parameter = ex.parameter
		
		if (!parameter.hasAnnotation<Required>()) return null
		
		val location = when {
			parameter.hasAnnotation<PathVariable>() -> ErrorLocation.PATH
			parameter.hasAnnotation<RequestParam>() -> ErrorLocation.QUERY
			parameter.hasAnnotation<RequestHeader>() -> ErrorLocation.HEADER
			else -> return null
		}
		
		val constraint = ConstraintError(
			path = ex.name,
			location = location,
			code = ConstraintErrorCode.VALUE_TYPE_MISMATCH,
			message = ConstraintErrorCode.VALUE_TYPE_MISMATCH.message)
		
		return listOf(constraint)
	}
	
	/**
	 * Maps `@RequestBody` bean validation failures when the parameter carries [Validate].	 
	 */
	private fun translate(
		ex: MethodArgumentNotValidException
	): List<ConstraintError<*>>? {
		if (!ex.parameter.hasAnnotation<Validate>()) return null
		
		val location = when {
			ex.parameter.hasAnnotation<RequestBody>() -> ErrorLocation.BODY
			else -> null
		}
		
		return ex.bindingResult.allErrors.mapNotNull { error ->
			val fieldError = error as? FieldError
				?: return@mapNotNull null
			
			ConstraintError(
				path = fieldError.field,
				location = location,
				code = ConstraintErrorCode.VALUE_INVALID,
				message = fieldError.defaultMessage,
				metadata = fieldError.code?.let(::listOf),
			)
		}
	}
	
	/**
	 * Maps method-parameter validation failures when the handler carries [Validate] (method or class).	 
	 */
	private fun translate(
		ex: HandlerMethodValidationException,
		handler: HandlerMethod?,
	): List<ConstraintError<*>>? {
		if (!isValidataMarked(handler, ex.method)) return null
		return toConstraintErrors(ex.valueResults)
	}
	
	/**
	 * Maps Jackson body databind failures when the handler carries [Validate] (method or class).	 
	 */
	private fun translate(
		ex: HttpMessageNotReadableException,
		handler: HandlerMethod?,
	): List<ConstraintError<*>>? {
		if (!isValidataMarked(handler, handler?.method)) return null
		val cause = ex.mostSpecificCause
		if (cause !is DatabindException) return null
		val path = cause.propertyPath().ifBlank { return null }
		
		val code = when (cause) {
			is InvalidNullException -> ConstraintErrorCode.VALUE_MISSING
			is UnrecognizedPropertyException -> ConstraintErrorCode.PROPERTY_UNKNOWN
			is MismatchedInputException -> {
				if (isMissingCreatorPropertyMessage(cause.originalMessage)) {
					ConstraintErrorCode.VALUE_MISSING
				}
				else {
					ConstraintErrorCode.VALUE_TYPE_MISMATCH
				}
			}
			
			else -> return null
		}
		
		val constraint = ConstraintError(
			path = path,
			code = code,
			message = code.message,
			location = ErrorLocation.BODY)
		
		return listOf(constraint)
	}
	
	/**
	 * Builds a single [ConstraintErrorCode.VALUE_MISSING] error when [marked] is true; otherwise `null` (leave Spring's exception).	 
	 */
	private fun missingRequired(
		marked: Boolean,
		path: String,
		location: ErrorLocation,
	): List<ConstraintError<*>>? {
		if (!marked) return null
		
		val constraint = ConstraintError(
			path = path,
			location = location,
			code = ConstraintErrorCode.VALUE_MISSING,
			message = ConstraintErrorCode.VALUE_MISSING.message)
		
		return listOf(constraint)
	}
	
	/**
	 * Flattens Spring parameter validation results into [ConstraintError] entries.	 
	 */
	private fun toConstraintErrors(
		validationResult: List<ParameterValidationResult>,
	): List<ConstraintError<*>> {
		val errors = ArrayList<ConstraintError<ConstraintErrorCode>>()
		
		for (result in validationResult) {
			val parameter = result.methodParameter
			val path = parameter.parameterName ?: parameter.parameter.name
			val location = parameter.toErrorLocationOrNull()
			
			for (error in result.resolvableErrors) {
				errors += ConstraintError(
					path = path,
					location = location,
					code = ConstraintErrorCode.VALUE_INVALID,
					message = error.defaultMessage,
					metadata = error.codes?.toList(),
				)
			}
		}
		
		return errors
	}
	
	/**
	 * Whether the handler (preferred) or reflected [method] carries effective `@Validate`.	 
	 */
	private fun isValidataMarked(
		handler: HandlerMethod?,
		method: Method?
	): Boolean {
		if (handler != null) {
			return ValidateHandlerDiscovery.findValidate(handler) != null
		}
		if (method == null) return false
		return ValidateHandlerDiscovery.findValidate(method) != null
	}
	
	/**
	 * Formats Jackson's reference path as a dotted / indexed property path string.	 
	 */
	private fun DatabindException.propertyPath(): String {
		return buildString {
			for (reference in path) {
				when {
					reference.propertyName != null -> {
						if (isNotEmpty()) append('.')
						append(reference.propertyName)
					}
					
					reference.index >= 0 -> {
						append('[')
						append(reference.index)
						append(']')
					}
				}
			}
		}
	}
	
	/**
	 * Maps a method parameter's Spring binding annotation to [ErrorLocation], or `null` when unknown.	 
	 */
	private fun MethodParameter.toErrorLocationOrNull(): ErrorLocation? = when {
		hasAnnotation<RequestBody>() -> ErrorLocation.BODY
		hasAnnotation<PathVariable>() -> ErrorLocation.PATH
		hasAnnotation<RequestParam>() -> ErrorLocation.QUERY
		hasAnnotation<RequestHeader>() -> ErrorLocation.HEADER
		else -> null
	}
	
	/**
	 * Whether this method parameter carries annotation [A].	 
	 */
	private inline fun <reified A: Annotation> MethodParameter.hasAnnotation(): Boolean {
		return hasParameterAnnotation(A::class.java)
	}
	
	companion object {
		
		/**
		 * Stable prefix of Jackson's message for missing required creator properties
		 * (tools.jackson / jackson-databind). Prefer a typed Jackson API when one exists;
		 * until then this prefix is pinned and covered by unit tests.		 
		 */
		internal const val MISSING_CREATOR_PROPERTY_PREFIX = "Missing required creator property"
		
		/**
		 * Classifies a Jackson `MismatchedInputException` message as a missing creator property.		 
		 */
		internal fun isMissingCreatorPropertyMessage(
			message: String?
		): Boolean = message?.startsWith(MISSING_CREATOR_PROPERTY_PREFIX) == true
	}
}

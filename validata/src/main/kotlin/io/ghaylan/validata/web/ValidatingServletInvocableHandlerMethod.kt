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
package io.ghaylan.validata.web

import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.exception.ConstraintViolationException
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod

/**
 * Spring MVC invocable that runs Validata validation after argument resolution and before the
 * controller method body.
 *
 * [RequestMappingHandlerAdapter] creates a new invocable per request. Cross-request reuse lives in
 * [ValidatedEndpointPlanCache], not in fields of this instance beyond the injected collaborators.
 *
 * Applications do not construct this type; `WebMvcValidationAutoConfiguration` installs it through
 * Spring MVC's [RequestMappingHandlerAdapter] factory hook.
 *
 * @param handlerMethod Spring handler describing the destination endpoint
 * @property validatorEngine Stateless validation engine.
 * @property endpointPlanCache Process-wide plan cache keyed by bridged method.*
 * 
 * @author Ghaylan Saada
 */
internal class ValidatingServletInvocableHandlerMethod(
	handlerMethod: HandlerMethod,
	private val validatorEngine: ValidatorEngine,
	private val endpointPlanCache: ValidatedEndpointPlanCache,
) : ServletInvocableHandlerMethod(handlerMethod) {

	/**
	 * Resolves arguments, optionally validates them, then invokes the controller method.
	 *
	 * @param request current native web request
	 * @param mavContainer model/view container for this invocation
	 * @param providedArgs extra arguments supplied by the dispatcher
	 * @return controller return value
	 * @throws ConstraintViolationException when validation reports one or more constraint errors
	 * @throws Exception when argument resolution or controller invocation fails
	 */
	override fun invokeForRequest(
		request: NativeWebRequest,
		mavContainer: ModelAndViewContainer?,
		vararg providedArgs: Any?,
	): Any? {
		val args = getMethodArgumentValues(request, mavContainer, *providedArgs)

		when (val plan = endpointPlanCache.planFor(this, bridgedMethod)) {
			HandlerValidationPlan.Skip -> Unit
			is HandlerValidationPlan.Active -> runValidation(plan, args)
		}

		return doInvoke(*args)
	}

	/**
	 * Assembles transport inputs from [args] and runs [ValidatorEngine.validateRequest].
	 *
	 * @param plan cached active plan for this handler
	 * @param args resolved method arguments in declaration order
	 * @throws ConstraintViolationException when the engine returns a non-empty error list
	 */
	private fun runValidation(plan: HandlerValidationPlan.Active, args: Array<Any?>) {
		val assembled = RequestArgumentAssembler.assemble(
			layout = plan.schema.argumentLayout,
			args = args,
			plan = plan)

		// Omitted optional `@RequestBody` resolves to null. Use the plan-time schema variant with
		// requestBody cleared — do not walk the body schema against null (that would treat every
		// `@Required` property as failed). Required bodies that are missing fail earlier in Spring's
		// argument resolution (HttpMessageNotReadableException).
		val schema = if (assembled.body == null) plan.schemaWhenBodyAbsent else plan.schema

		val errors = validatorEngine.validateRequest(
			schema = schema,
			body = assembled.body,
			params = assembled.queryParams,
			headers = assembled.headers,
			pathVariables = assembled.pathVariables)

		if (errors.isNotEmpty()) {
			throw ConstraintViolationException(errors = errors)
		}
	}
}

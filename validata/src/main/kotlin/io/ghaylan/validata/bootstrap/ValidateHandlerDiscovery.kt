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

import io.ghaylan.validata.schema.Validate
import java.lang.reflect.Method
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * Spring MVC discovery helpers used at registry bootstrap and plan-cache build.
 *
 * Locates handlers annotated with [Validate] (method or controller class) so schemas can be
 * registered and per-request plans can skip unannotated methods.*
 * 
 * @author Ghaylan Saada
 */
internal object ValidateHandlerDiscovery {
	
	/**
	 * Bean name of Spring MVC's [RequestMappingHandlerMapping] in a Boot web context.
	 */
	private const val HANDLER_MAPPING_BEAN = "requestMappingHandlerMapping"
	
	/**
	 * Indexes every MVC handler that carries [Validate] on the method or its controller class.
	 *
	 * Returns an empty map when the application has no `requestMappingHandlerMapping` bean so
	 * non-web contexts (for example config-only `@Validate` on `@ConfigurationProperties`) can
	 * refresh without failing.
	 *
	 * @param appContext live Spring context that may or may not include Web MVC
	 * @return reflection [Method] → merged [Validate] metadata
	 */
	fun findRequestValidationMethods(
		appContext: ApplicationContext,
	): Map<Method, Validate> {
		// A plain (non-web) application has no handler mapping — config-only `@Validate` must still
		// refresh without requiring Web MVC.
		if (!appContext.containsBean(HANDLER_MAPPING_BEAN)) return emptyMap()
		
		val handlerMapping = appContext.getBean<RequestMappingHandlerMapping>(HANDLER_MAPPING_BEAN)
		val result = HashMap<Method, Validate>(handlerMapping.handlerMethods.size)
		for (handlerMethod in handlerMapping.handlerMethods.values) {
			result[handlerMethod.method] = findValidate(handlerMethod) ?: continue
		}
		return result
	}
	
	/**
	 * Resolves the effective [Validate] for a handler, method declaration first, then the controller class.
	 *
	 * Single definition of "annotated" for bootstrap and the request interceptor. Uses
	 * [AnnotatedElementUtils] so meta-annotations and Spring proxies still resolve;
	 * [HandlerMethod.getBeanType] already yields the target class.
	 *
	 * @param handlerMethod Spring MVC handler descriptor
	 * @return merged annotation, or `null` when neither method nor class is annotated
	 */
	fun findValidate(handlerMethod: HandlerMethod): Validate? {
		return findValidate(handlerMethod.method, handlerMethod.beanType)
	}

	/**
	 * Resolves the effective [Validate] for a reflected method, then [beanType] (controller class).
	 *
	 * Prefer [findValidate] with a [HandlerMethod] when available so proxy target classes are used.
	 *
	 * @param method handler method (bridged / user method)
	 * @param beanType controller bean type used for class-level `@Validate`
	 * @return merged annotation, or `null` when neither method nor class is annotated
	 */
	fun findValidate(method: Method, beanType: Class<*> = method.declaringClass): Validate? {
		return AnnotatedElementUtils.findMergedAnnotation(method, Validate::class.java)
			?: AnnotatedElementUtils.findMergedAnnotation(beanType, Validate::class.java)
	}
}

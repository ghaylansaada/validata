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
package io.ghaylan.validata.openapi.springdoc

import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import org.springdoc.core.customizers.SpringDocCustomizers
import org.springdoc.core.discoverer.SpringDocParameterNameDiscoverer
import org.springdoc.core.extractor.MethodParameterPojoExtractor
import org.springdoc.core.service.GenericParameterService
import org.springdoc.core.service.RequestBodyService
import org.springdoc.webmvc.core.service.RequestService
import org.springframework.core.MethodParameter

/**
 * [RequestService] that strips Validata (and other non-Jakarta) annotations before springdoc
 * applies Bean Validation → schema mapping, avoiding [ClassCastException] on colliding simple names.
 *
 * @param parameterBuilder springdoc parameter builder
 * @param requestBodyService springdoc request-body service
 * @param springDocCustomizers springdoc customizer registry
 * @param localSpringDocParameterNameDiscoverer parameter name discoverer
 * @param methodParameterPojoExtractor POJO parameter extractor*
 * 
 * @author Ghaylan Saada
 */
class ValidataRequestService(
	parameterBuilder: GenericParameterService,
	requestBodyService: RequestBodyService,
	springDocCustomizers: SpringDocCustomizers,
	localSpringDocParameterNameDiscoverer: SpringDocParameterNameDiscoverer,
	methodParameterPojoExtractor: MethodParameterPojoExtractor,
): RequestService(
	parameterBuilder,
	requestBodyService,
	springDocCustomizers,
	localSpringDocParameterNameDiscoverer,
	methodParameterPojoExtractor,
) {
	
	/**
	 * Filters Validata annotations out of the parameter Bean Validation path.
	 *
	 * Mutates [parameter] through springdoc's Bean Validation mapping on the filtered set.
	 *
	 * @param methodParameter Spring method parameter being documented
	 * @param parameter OpenAPI parameter being enriched
	 * @param annotations annotations springdoc collected; may be `null`
	 * @param isParameterObject whether the parameter is a `@ParameterObject`
	 * @param openapiVersion OpenAPI version string used by springdoc	 
	 */
	override fun applyBeanValidatorAnnotations(
		methodParameter: MethodParameter,
		parameter: Parameter,
		annotations: MutableList<Annotation>?,
		isParameterObject: Boolean,
		openapiVersion: String,
	) {
		super.applyBeanValidatorAnnotations(methodParameter,
			parameter,
			JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(annotations),
			isParameterObject,
			openapiVersion)
	}
	
	/**
	 * Filters Validata annotations out of the request-body Bean Validation path.
	 *
	 * Mutates [requestBody] through springdoc's Bean Validation mapping on the filtered set.
	 *
	 * @param requestBody OpenAPI request body being enriched
	 * @param annotations annotations springdoc collected; may be `null`
	 * @param isOptional whether the request body is optional	 
	 */
	override fun applyBeanValidatorAnnotations(
		requestBody: RequestBody,
		annotations: MutableList<Annotation>?,
		isOptional: Boolean,
	) {
		super.applyBeanValidatorAnnotations(requestBody, JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(annotations), isOptional)
	}
}

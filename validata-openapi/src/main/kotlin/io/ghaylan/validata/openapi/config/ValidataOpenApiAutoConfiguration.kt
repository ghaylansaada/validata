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
package io.ghaylan.validata.openapi.config

import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.openapi.aot.ValidataOpenApiRuntimeHints
import io.ghaylan.validata.openapi.enrichment.EndpointErrorCodeCollector
import io.ghaylan.validata.openapi.enrichment.OpenApiEnrichmentCache
import io.ghaylan.validata.openapi.enrichment.SchemaErrorDocResolver
import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import io.ghaylan.validata.openapi.springdoc.ValidataModelConverter
import io.ghaylan.validata.openapi.springdoc.ValidataOperationCustomizer
import io.ghaylan.validata.openapi.springdoc.ValidataRequestService
import io.swagger.v3.core.converter.ModelConverter
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.customizers.SpringDocCustomizers
import org.springdoc.core.discoverer.SpringDocParameterNameDiscoverer
import org.springdoc.core.extractor.MethodParameterPojoExtractor
import org.springdoc.core.service.GenericParameterService
import org.springdoc.core.service.RequestBodyService
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springdoc.webmvc.core.service.RequestService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.context.annotation.Lazy

/**
 * Boot auto-configuration for Validata ↔ springdoc OpenAPI enrichment.
 *
 * Registers [ValidataRequestService], [ValidataOperationCustomizer], and [ValidataModelConverter]
 * when springdoc is present. [ValidationRegistry] is optional; error docs are emitted only when
 * apps register [ErrorDocPublisher] beans.
 * 
 * @author Ghaylan Saada
 */
@AutoConfiguration(after = [SpringDocConfiguration::class], before = [SpringDocWebMvcConfiguration::class])
@ConditionalOnWebApplication
@ConditionalOnClass(OperationCustomizer::class, RequestService::class)
@ImportRuntimeHints(ValidataOpenApiRuntimeHints::class)
class ValidataOpenApiAutoConfiguration {
	
	/**
	 * Replaces springdoc's default [RequestService] so Validata constraint annotations never
	 * reach springdoc's Bean Validation → schema cast path.
	 *
	 * The bean **method name** `requestBuilder` is springdoc's historical name and must stay:
	 * this bean replaces theirs. Do not rename it.
	 *
	 * @param parameterBuilder springdoc parameter builder
	 * @param requestBodyService springdoc request-body service
	 * @param springDocCustomizers springdoc customizer registry
	 * @param localSpringDocParameterNameDiscoverer parameter name discoverer
	 * @param methodParameterPojoExtractor POJO parameter extractor
	 * @return Validata-aware request service
	 */
	@Bean
	@ConditionalOnMissingBean(RequestService::class)
	@ConditionalOnBean(GenericParameterService::class)
	@Lazy(false)
	fun requestBuilder(
		parameterBuilder: GenericParameterService,
		requestBodyService: RequestBodyService,
		springDocCustomizers: SpringDocCustomizers,
		localSpringDocParameterNameDiscoverer: SpringDocParameterNameDiscoverer,
		methodParameterPojoExtractor: MethodParameterPojoExtractor,
	): RequestService = ValidataRequestService(
		parameterBuilder = parameterBuilder,
		requestBodyService = requestBodyService,
		springDocCustomizers = springDocCustomizers,
		localSpringDocParameterNameDiscoverer = localSpringDocParameterNameDiscoverer,
		methodParameterPojoExtractor = methodParameterPojoExtractor)
	
	/**
	 * Documents Validata parameters and runs registered [ErrorDocPublisher] beans when a
	 * [ValidationRegistry] is present.
	 *
	 * @param registry optional Validata registry from the host app
	 * @param errorDocPublishers all [ErrorDocPublisher] beans (ordered)
	 * @return operation customizer bean
	 */
	@Bean
	@ConditionalOnMissingBean(name = ["validataOperationCustomizer"])
	fun validataOperationCustomizer(
		registry: ObjectProvider<ValidationRegistry>,
		errorDocPublishers: ObjectProvider<ErrorDocPublisher>,
	): OperationCustomizer = ValidataOperationCustomizer(
		registry = registry.ifAvailable,
		errorDocPublishers = errorDocPublishers.orderedStream().toList())
	
	/**
	 * Overlays Validata IR constraints onto springdoc DTO schemas.
	 *
	 * @return model converter bean
	 */
	@Bean
	@ConditionalOnMissingBean(name = ["validataModelConverter"])
	fun validataModelConverter(): ModelConverter = ValidataModelConverter()
	
	/**
	 * Clears enrichment / error-code caches after each OpenAPI document build so identity
	 * tracking does not grow across `/v3/api-docs` regenerations.
	 *
	 * @return OpenAPI customizer that runs after converters and operation customizers
	 */
	@Bean
	@ConditionalOnMissingBean(name = ["validataOpenApiCacheResetCustomizer"])
	fun validataOpenApiCacheResetCustomizer(): OpenApiCustomizer = OpenApiCustomizer {
		OpenApiEnrichmentCache.clear()
		EndpointErrorCodeCollector.clear(clearWarnings = false)
		SchemaErrorDocResolver.clear(clearWarnings = false)
	}
}

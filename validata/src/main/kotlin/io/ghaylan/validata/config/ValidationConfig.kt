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

import io.ghaylan.validata.aot.ValidationRuntimeHints
import io.ghaylan.validata.bootstrap.ValidationRegistryInitializer
import io.ghaylan.validata.engine.ValidationLimits
import io.ghaylan.validata.engine.ValidationRegistry
import io.ghaylan.validata.engine.ValidatorEngine
import io.ghaylan.validata.web.ValidatedEndpointPlanCache
import io.ghaylan.validata.web.ValidatingServletInvocableHandlerMethod
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportRuntimeHints

/**
 * Boot auto-configuration for the Validata Spring host.
 *
 * Registers the Spring-free [ValidationRegistry] / [ValidatorEngine], populates them via
 * [ValidationRegistryInitializer], registers [ConfigurationValidationPostProcessor], and (for web
 * apps) wires [ValidatedEndpointPlanCache] for [WebMvcValidationAutoConfiguration] /
 * [ValidatingServletInvocableHandlerMethod].
 *
 * Constraint messages come from the annotation `message` attribute, or the error-code enum
 * default — there is no Spring `MessageSource` bridge.
 *
 * Apps normally depend on this jar and do nothing else; beans are replaceable via
 * [ConditionalOnMissingBean]:
 *
 * ```kotlin
 * @Bean
 * fun validationLimits(): ValidationLimits =
 *     ValidationLimits(maxDepth = 16, maxElementsPerContainer = 5_000, maxErrors = 50)
 * ```
 *
 * Or bind ceilings in configuration:
 *
 * ```yaml
 * validata:
 *   limits:
 *     max-depth: 16
 *     max-errors: 50
 *     max-elements-per-container: 5000
 * ```
 * 
 * @author Ghaylan Saada
 */
@AutoConfiguration
@ImportRuntimeHints(ValidationRuntimeHints::class)
@EnableConfigurationProperties(ValidataLimitsProperties::class)
class ValidationConfig {
	
	/**
	 * Exposes bound `validata.limits.*` as the engine-facing [ValidationLimits] value object.
	 *
	 * @param properties Boot-bound ceilings under `validata.limits`
	 * @return validated [ValidationLimits] snapshot
	 * @throws IllegalArgumentException when any bound value is non-positive
	 */
	@Bean
	@ConditionalOnMissingBean
	fun validationLimits(properties: ValidataLimitsProperties): ValidationLimits = properties.toLimits()
	
	/**
	 * Empty [ValidationRegistry] filled later by [ValidationRegistryInitializer].
	 *
	 * @return Spring-free registry bean shared by the engine and MVC integration
	 */
	@Bean
	@ConditionalOnMissingBean
	fun validationRegistry(): ValidationRegistry = ValidationRegistry()
	
	/**
	 * Populates [ValidationRegistry] from Spring beans and KSP-generated catalogs at startup.
	 *
	 * Lives in the `validata` host module because the initializer implements Spring lifecycle
	 * interfaces; the registry itself remains Spring-free for core consumers.
	 *
	 * @param validationRegistry registry bean to mutate during bootstrap
	 * @return lifecycle adapter that registers validators and request schemas
	 */
	@Bean
	@ConditionalOnMissingBean
	fun validationRegistryInitializer(
		validationRegistry: ValidationRegistry,
	): ValidationRegistryInitializer = ValidationRegistryInitializer(validationRegistry)
	
	/**
	 * Stateless validation engine shared by request and configuration validation paths.
	 *
	 * @param validationRegistry populated schema and validator catalog
	 * @param limits depth / size / error ceilings from [validationLimits]
	 * @return engine used by the MVC invocable and [ConfigurationValidationPostProcessor]
	 */
	@Bean
	@ConditionalOnMissingBean
	fun validatorEngine(
		validationRegistry: ValidationRegistry,
		limits: ValidationLimits,
	): ValidatorEngine = ValidatorEngine(validationRegistry, limits)
	
	/**
	 * Process-wide cache of per-handler validation plans.
	 *
	 * Web applications inject this into [WebMvcValidationAutoConfiguration]; non-web contexts may
	 * omit it because nothing constructs [ValidatingServletInvocableHandlerMethod].
	 *
	 * @param validationRegistry source of generated endpoint schemas
	 * @return shared plan cache keyed by bridged handler method
	 */
	@Bean
	@ConditionalOnWebApplication
	@ConditionalOnMissingBean
	fun validatedEndpointPlanCache(
		validationRegistry: ValidationRegistry,
	): ValidatedEndpointPlanCache = ValidatedEndpointPlanCache(validationRegistry)
	
	/**
	 * Validates `@Validate` `@ConfigurationProperties` beans after initialization.
	 *
	 * Declared as a `@Bean` (not `@Component`) so it is not a component-scan target. The engine
	 * is an [ObjectProvider] so this [BeanPostProcessor] can be constructed without pulling
	 * [ValidatorEngine] into early post-processor instantiation.
	 *
	 * @param validatorEngine lazily resolved engine; absent only if a consumer replaced auto-config
	 * @return post-processor that fails context refresh on constraint violations
	 */
	@Bean
	@ConditionalOnMissingBean
	fun configurationValidationPostProcessor(
		validatorEngine: ObjectProvider<ValidatorEngine>,
	): ConfigurationValidationPostProcessor = ConfigurationValidationPostProcessor(validatorEngine)
}

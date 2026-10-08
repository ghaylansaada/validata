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

import io.ghaylan.validata.constraint.spi.GeneratedConstraintCatalogs
import io.ghaylan.validata.engine.ValidationRegistry
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent

/**
 * Spring lifecycle adapter that populates a Spring-free [ValidationRegistry].
 *
 * Lives in the `validata` Spring host module — the only library module allowed to import Spring
 * types. Implements [ApplicationContextAware], [InitializingBean], and [ApplicationListener] so
 * the registry stays free of Spring types.
 *
 * Must not be used from Spring-free modules (`validata-core`, `validata-schema`).
 *
 * ### Catalog warm-up
 *
 * [afterPropertiesSet] calls [GeneratedConstraintCatalogs.all] before registering validators so the
 * `ServiceLoader` scan and collision check complete during host bootstrap — not on the first
 * concurrent request. Cold-start hosts that skip this initializer must warm the catalog themselves
 * before serving traffic (audit Task 1.2).
 *
 * @property registry Engine-side registry bean to populate (injected; never looked up statically).*
 * 
 * @author Ghaylan Saada
 */
class ValidationRegistryInitializer(
	private val registry: ValidationRegistry,
): ApplicationListener<ContextRefreshedEvent>, InitializingBean, ApplicationContextAware {
	
	/**
	 * Application context captured by [setApplicationContext] for bean and handler discovery.	 
	 */
	private lateinit var appContext: ApplicationContext
	
	/**
	 * Stores the root [ApplicationContext] for later registry population.
	 *
	 * @param applicationContext context that owns this initializer bean	 
	 */
	override fun setApplicationContext(applicationContext: ApplicationContext) {
		this.appContext = applicationContext
	}
	
	/**
	 * Eagerly loads [GeneratedConstraintCatalogs] then registers validators into [registry].
	 *
	 * Runs earlier than [onApplicationEvent] so the engine can resolve validators while other
	 * beans finish initializing. Catalog warm-up ensures first-request paths do not pay a full
	 * `ServiceLoader` scan under concurrency.	 
	 */
	override fun afterPropertiesSet() {
		GeneratedConstraintCatalogs.all()
		registry.registerValidators(ConstraintValidatorCatalog.buildValidators(appContext))
	}
	
	/**
	 * Registers static request schemas into [registry] after full context refresh.
	 *
	 * Ignores refresh events from child contexts so nested contexts do not double-register.
	 *
	 * @param event refresh event; only the context that matches [appContext] is handled
	 * @throws IllegalStateException when [EndpointSchemaIndex] cannot resolve a schema for a handler	 
	 */
	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		if (event.applicationContext !== appContext) return
		val schemas = EndpointSchemaIndex.resolveStaticSchemas(appContext = appContext)
		registry.registerStaticSchemas(schemas)
		registry.freeze()
	}
}

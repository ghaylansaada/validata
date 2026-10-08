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

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.constraint.spi.ConstraintCatalogEntry
import io.ghaylan.validata.constraint.spi.GeneratedConstraintCatalogs
import io.ghaylan.validata.internal.TypeInfo
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * Builds the runtime validator catalog from [GeneratedConstraintCatalogs] (KSP-emitted SPI).
 *
 * Instance resolution has exactly two tiers, in order:
 * 1. a Spring bean of [ConstraintCatalogEntry.validatorType], when one is registered
 * 2. [ConstraintCatalogEntry.defaultInstanceFactory] (Kotlin `object` or no-arg constructor,
 *    decided at compile time by the processor catalog model builder)
 *
 * There is no `kotlin-reflect` type walking and no `Class.forName` scanning here.
 *
 * Apps override a built-in or custom validator by registering a Spring `@Bean` of that
 * [ConstraintValidator] type; the bean wins over the compile-time default factory.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintValidatorCatalog {
	
	/**
	 * Registers every catalogued constraint validator.
	 *
	 * @param appContext active Spring context used for bean-backed validators
	 * @return metadata class → (compatible [TypeInfo] → validator instance)	 
	 */
	fun buildValidators(
		appContext: ApplicationContext,
	): Map<KClass<out ConstraintMetadata>, Map<TypeInfo, ConstraintValidator<*, *>>> {
		val validators = HashMap<KClass<out ConstraintMetadata>, HashMap<TypeInfo, ConstraintValidator<*, *>>>()
		
		for (entry in GeneratedConstraintCatalogs.all()) {
			val valueTypeMap = validators.getOrPut(entry.metadataType.kotlin) { hashMapOf() }
			valueTypeMap[entry.valueType] = resolveValidatorInstance(appContext, entry)
		}
		
		return validators
	}
	
	/**
	 * Resolves a validator instance using the two-tier rule documented on [ConstraintValidatorCatalog].
	 *
	 * @param appContext Spring context used for the bean-provider lookup
	 * @param entry catalog entry describing validator type and default factory
	 * @return Spring bean when registered, otherwise the compile-time default instance	 
	 */
	private fun resolveValidatorInstance(
		appContext: ApplicationContext,
		entry: ConstraintCatalogEntry,
	): ConstraintValidator<*, *> {
		appContext.getBeanProvider(entry.validatorType)
			.getIfAvailable()
			?.let { return it }
		return entry.defaultInstanceFactory()
	}
}

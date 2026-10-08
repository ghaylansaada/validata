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
package io.ghaylan.validata.aot

import io.ghaylan.validata.constraint.spi.ConstraintCatalog
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.request.spi.RequestSchemaModule
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.schema.spi.ObjectSchemaModule
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.registerType

/**
 * Residual AOT / GraalVM hints for the closed-world (KSP-only) validation path.
 *
 * Schema graphs and [ValueReader]s are ordinary bytecode from KSP / [ObjectSchemaModule].
 * This registrar covers error JSON reflection and ServiceLoader resources only —
 * including [RequestSchemaModule] / [ConstraintCatalog] providers required by native-image.*
 * 
 * @author Ghaylan Saada
 */
class ValidationRuntimeHints: RuntimeHintsRegistrar {
	
	/**
	 * Registers reflection categories and ServiceLoader resource patterns for native image.
	 *
	 * @param hints Spring AOT hint collector
	 * @param classLoader optional loader (unused; ServiceLoader patterns are name-based)	 
	 */
	override fun registerHints(
		hints: RuntimeHints,
		classLoader: ClassLoader?
	) {
		hints.reflection()
			.registerType<ConstraintErrorCode>(*ENUM_MEMBERS)
			.registerType<ConstraintError<*>>(*JSON_TYPE_MEMBERS)
			.registerType<ObjectSchema>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<GeneratedSchemas>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<ConstraintCatalog>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<ObjectSchemaModule>(MemberCategory.INVOKE_PUBLIC_METHODS)
			.registerType<RequestSchemaModule>(MemberCategory.INVOKE_PUBLIC_METHODS)
		
		hints.resources()
			.registerPattern("META-INF/services/" + ConstraintCatalog::class.java.name)
			.registerPattern("META-INF/services/" + ObjectSchemaModule::class.java.name)
			.registerPattern("META-INF/services/" + RequestSchemaModule::class.java.name)
	}
	
	/**
	 * Member-category arrays shared by [registerHints] reflection registrations.
	 */
	companion object {
		
		/**
		 * Constructor + public methods for JSON-serializable error payloads.
		 */
		private val JSON_TYPE_MEMBERS = arrayOf(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
		
		/**
		 * Public methods for error-code enums exposed in JSON.
		 */
		private val ENUM_MEMBERS = arrayOf(MemberCategory.INVOKE_PUBLIC_METHODS)
	}
}

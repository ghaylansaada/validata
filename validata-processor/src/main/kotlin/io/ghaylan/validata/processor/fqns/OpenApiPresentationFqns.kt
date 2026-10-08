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
package io.ghaylan.validata.processor.fqns

/**
 * Fully qualified names and argument contracts for OpenAPI presentation annotations.
 *
 * Types live in `:validata-openapi`, which is not on the processor main compile classpath.
 * Do not add them to [ProcessorFqns.ALL_LOADABLE]. When openapi is absent from a consumer
 * compilation, symbols never match these FQCNs and no docs IR is emitted.
 * 
 * @author Ghaylan Saada
 */
internal object OpenApiPresentationFqns {
	
	/**
	 * Docs-only `@ApiError` annotation FQCN ([API_ERROR]).
	 */
	const val API_ERROR: String = "io.ghaylan.validata.openapi.presentation.ApiError"
	
	/**
	 * Required catalog interface FQCN ([CONSTRAINT_ERROR_DEFINITION]).
	 */
	const val CONSTRAINT_ERROR_DEFINITION: String = "io.ghaylan.validata.model.ConstraintErrorDefinition"
	
	/**
	 * `@ApiError` parameter names and positional indices when KSP omits the name.
	 *
	 * Indices match the annotation declaration order.
	 */
	object Attr {
		
		/**
		 * Machine code string. Declaration index `0`.
		 */
		const val CODE: String = "code"
		
		/**
		 * Documentation message. Declaration index `1`.
		 */
		const val MESSAGE: String = "message"
		
		/**
		 * Catalog `KClass` (enum implementing [CONSTRAINT_ERROR_DEFINITION]). Declaration index `2`.
		 */
		const val CATALOG: String = "catalog"
		
		/**
		 * Declaration index of [CODE] when the argument name is absent.
		 */
		const val CODE_INDEX: Int = 0
		
		/**
		 * Declaration index of [MESSAGE] when the argument name is absent.
		 */
		const val MESSAGE_INDEX: Int = 1
		
		/**
		 * Declaration index of [CATALOG] when the argument name is absent.
		 */
		const val CATALOG_INDEX: Int = 2
	}
}

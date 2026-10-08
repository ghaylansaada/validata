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
package io.ghaylan.validata.engine.support

import io.ghaylan.validata.exception.ConstraintViolationException
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition

/**
 * Fluent collector for business or domain constraint errors after binding.
 *
 * Not for the schema-walk hot path. Register via [error], then call [throwIfNotEmpty].
 *
 * ```kotlin
 * val collector = RequestErrorCollector()
 *
 * collector.error("user.email", ConstraintErrorCode.VALUE_MISSING)
 *     .message("Email is required")
 *
 * collector.throwIfNotEmpty()
 * ```*
 * 
 * @author Ghaylan Saada
 */
class RequestErrorCollector {
	
	/**
	 * Registered builders not yet materialized by [collect].
	 */
	private val errorsBuilder = mutableListOf<ConstraintErrorBuilder<*>>()
	
	/**
	 * Registers a new error and returns its fluent builder.
	 *
	 * Mutates this collector; the returned builder is already stored for [collect].
	 *
	 * @param CodeT Enum that implements [ConstraintErrorDefinition].
	 * @param field Path segment such as `user.email` or a flat query name.
	 * @param code Violated constraint or business error code.
	 * @return Builder already stored in this collector; chain to enrich before [collect].	 
	 */
	fun <CodeT> error(
		field: String,
		code: CodeT
	): ConstraintErrorBuilder<CodeT> where CodeT: Enum<CodeT>, CodeT: ConstraintErrorDefinition {
		return ConstraintErrorBuilder(field, code).also(errorsBuilder::add)
	}
	
	/**
	 * Throws [ConstraintViolationException] when at least one error was registered.
	 *
	 * Side effect: throws when non-empty; no-op when empty.
	 *
	 * @throws ConstraintViolationException When the collector holds one or more errors.	 
	 */
	fun throwIfNotEmpty() {
		if (errorsBuilder.isEmpty()) return
		throw ConstraintViolationException(errors = collect())
	}
	
	/**
	 * Builds every registered error without clearing the collector.
	 *
	 * No I/O; does not mutate registration order beyond reading builders.
	 *
	 * @return Immutable snapshot of built errors, or empty when nothing was registered.	 
	 */
	fun collect(): List<ConstraintError<*>> {
		val size = errorsBuilder.size
		if (size == 0) return emptyList()
		val allErrors = ArrayList<ConstraintError<*>>(size)
		
		for (builder in errorsBuilder) {
			allErrors.add(builder.build())
		}
		
		return allErrors
	}
	
	/**
	 * Whether no errors have been registered yet.
	 *
	 * @return `true` when [error] has never been called or the builder list is empty.	 
	 */
	fun isEmpty(): Boolean {
		return errorsBuilder.isEmpty()
	}
}

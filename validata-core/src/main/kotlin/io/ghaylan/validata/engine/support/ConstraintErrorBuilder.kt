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

import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.model.ErrorLocation

/**
 * Fluent builder for hand-built [ConstraintError]s on the business-error path.
 *
 * Pass wire or logical paths via [field]. Schema-driven request validation stamps paths elsewhere.
 *
 * @param CodeT Enum error code that also implements [ConstraintErrorDefinition].
 * @property field Path segment such as `user.email` or a flat query name.
 * @property code Violated constraint or business error code.
 *
 * @author Ghaylan Saada
 */
class ConstraintErrorBuilder<CodeT>(
	private val field: String,
	private val code: CodeT,
) where CodeT: Enum<CodeT>, CodeT: ConstraintErrorDefinition {

	/**
	 * Optional message override; `null` leaves [ConstraintError.message] unset for callers to fill.
	 */
	private var message: String? = null

	/**
	 * Optional HTTP section for [ConstraintError.location].
	 */
	private var location: ErrorLocation? = null

	/**
	 * Map-style metadata entries; used when no opaque [metadataPayload] is set.
	 */
	private var metadataMap: LinkedHashMap<String, Any?>? = null

	/**
	 * Opaque metadata payload; when set, wins over [metadataMap].
	 */
	private var metadataPayload: Any? = null

	/**
	 * Adds one map entry under [key] for [ConstraintError.metadata].
	 *
	 * Mutates this builder’s map-style payload.
	 *
	 * @param key Key exposed to API clients.
	 * @param value Associated value; may be `null`.
	 * @return This builder for chaining.
	 */
	fun metadata(
		key: String,
		value: Any?,
	): ConstraintErrorBuilder<CodeT> {
		val map = metadataMap ?: LinkedHashMap<String, Any?>().also { metadataMap = it }
		map[key] = value
		return this
	}

	/**
	 * Replaces map-style entries with a single opaque [metadata] payload.
	 *
	 * Mutates this builder; opaque payload wins over map entries on [build].
	 *
	 * @param metadata Arbitrary object stored as [ConstraintError.metadata].
	 * @return This builder for chaining.
	 */
	fun metadata(metadata: Any): ConstraintErrorBuilder<CodeT> {
		this.metadataPayload = metadata
		return this
	}

	/**
	 * Merges all [entries] into the metadata map.
	 *
	 * Mutates this builder’s map-style payload.
	 *
	 * @param entries Key/value pairs to attach.
	 * @return This builder for chaining.
	 */
	fun metadata(entries: Map<String, Any?>): ConstraintErrorBuilder<CodeT> {
		val map = metadataMap ?: LinkedHashMap<String, Any?>().also { metadataMap = it }
		map.putAll(entries)
		return this
	}

	/**
	 * Invokes [entries] immediately and merges the result into the metadata map.
	 *
	 * Mutates this builder’s map-style payload.
	 *
	 * @param entries Supplier of key/value pairs.
	 * @return This builder for chaining.
	 */
	fun metadata(entries: () -> Map<String, Any?>): ConstraintErrorBuilder<CodeT> {
		val map = metadataMap ?: LinkedHashMap<String, Any?>().also { metadataMap = it }
		map.putAll(entries.invoke())
		return this
	}

	/**
	 * Overrides the default message from [code].
	 *
	 * Mutates this builder’s message override.
	 *
	 * @param message Human-readable detail for this violation.
	 * @return This builder for chaining.
	 */
	fun message(message: String): ConstraintErrorBuilder<CodeT> {
		this.message = message
		return this
	}

	/**
	 * Sets the request section for the invalid input.
	 *
	 * Mutates this builder’s [location].
	 *
	 * @param location HTTP-scoped section ([ErrorLocation]), including `OTHER`.
	 * @return This builder for chaining.
	 */
	fun location(location: ErrorLocation): ConstraintErrorBuilder<CodeT> {
		this.location = location
		return this
	}

	/**
	 * Materializes an immutable [ConstraintError] from the current builder state.
	 *
	 * No I/O; does not clear builder state.
	 *
	 * @return Finished error; metadata is opaque [metadataPayload] if set, otherwise the
	 *   non-empty map.
	 */
	internal fun build(): ConstraintError<CodeT> = ConstraintError(
		path = field,
		code = code,
		location = location,
		message = message,
		metadata = metadataPayload ?: metadataMap?.takeIf(Map<*, *>::isNotEmpty),
	)
}

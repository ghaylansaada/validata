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
package io.ghaylan.validata.constraint.validator.string.url

import io.ghaylan.validata.constraint.annotation.Url
import io.ghaylan.validata.constraint.annotation.UrlConstraint
import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache
import io.ghaylan.validata.model.ConstraintError
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.model.ConstraintErrorDefinition
import java.net.URI

/**
 * Shared URL / URI policy checks for [UrlValidator] and [UriValidator].
 *
 * Applies length, host (website type), protocol, port, query, and extension rules from
 * [UrlConstraint]. Policy objects are memoized per constraint via [ConstraintLiteralCache].
 *
 * @author Ghaylan Saada
 */
internal object UrlSupport {

	/**
	 * Error codes [UrlValidator] / [UriValidator] may emit for [constraint].
	 *
	 * Side effects: none.
	 *
	 * @param constraint URL policy metadata.
	 * @param includeFormatInvalid When `true`, includes [ConstraintErrorCode.VALUE_FORMAT_INVALID]
	 *   (CharSequence parse path only).
	 * @return Codes that can fire for this policy configuration.
	 */
	fun possibleErrorCodes(
		constraint: UrlConstraint,
		includeFormatInvalid: Boolean,
	): Set<ConstraintErrorDefinition> {
		val codes = linkedSetOf<ConstraintErrorDefinition>()
		if (constraint.maxLength < Int.MAX_VALUE) {
			codes += ConstraintErrorCode.TEXT_TOO_LONG
		}
		if (includeFormatInvalid) {
			codes += ConstraintErrorCode.VALUE_FORMAT_INVALID
		}
		if (constraint.type == Url.Type.WEBSITE) {
			codes += ConstraintErrorCode.VALUE_INVALID
		}
		val filters =
			!constraint.allowedProtocols.contains("*") ||
				!constraint.allowedPorts.contains("*") ||
				!constraint.allowedParams.contains("*") ||
				constraint.type.isMedia ||
				!constraint.allowedExtensions.contains("*")
		if (filters) {
			codes += ConstraintErrorCode.VALUE_NOT_ALLOWED
		}
		return codes
	}

	/**
	 * Checks [raw] length then applies host / protocol / port / query / extension policy to [uri].
	 *
	 * Side effects: may populate [ConstraintLiteralCache] for [constraint].
	 *
	 * @param raw Original URL text (used for length).
	 * @param uri Parsed [URI] to inspect.
	 * @param constraint URL constraint arguments.
	 * @return Path-free violation with [constraint] attached when applicable, or `null` when
	 *   valid.
	 */
	fun validateParsed(
		raw: String,
		uri: URI,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		lengthError(raw, constraint)?.let { return it }
		hostError(uri, constraint)?.let { return it }
		val policy = ConstraintLiteralCache.getOrCompute(constraint) { UrlPolicy.from(constraint) }
		protocolError(uri, policy, constraint)?.let { return it }
		portError(uri, policy, constraint)?.let { return it }
		queryError(uri, policy, constraint)?.let { return it }
		return extensionError(uri, policy, constraint)
	}

	private fun lengthError(
		rawUrl: String,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		if (rawUrl.length <= constraint.maxLength) return null
		return ConstraintError(
			code = ConstraintErrorCode.TEXT_TOO_LONG,
			message = "URL must be at most ${constraint.maxLength} characters long.",
			metadata = constraint,
		)
	}

	private fun hostError(
		uri: URI,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		if (constraint.type != Url.Type.WEBSITE || !uri.host.isNullOrBlank()) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_INVALID,
			message = "URL must include a host.",
			metadata = constraint,
		)
	}

	private fun protocolError(
		uri: URI,
		policy: UrlPolicy,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		if (policy.allowAnyProtocol) return null
		val scheme = uri.scheme?.lowercase()
		if (scheme in policy.protocols) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = "URL scheme is not allowed; permitted schemes: ${constraint.allowedProtocols.joinToString(", ")}.",
			metadata = constraint,
		)
	}

	private fun portError(
		uri: URI,
		policy: UrlPolicy,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		val port = uri.port
		if (port == -1 || policy.allowAnyPort || port.toString() in policy.ports) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = "URL port is not allowed; permitted ports: ${constraint.allowedPorts.joinToString(", ")}.",
			metadata = constraint,
		)
	}

	private fun queryError(
		uri: URI,
		policy: UrlPolicy,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		val query = uri.query
		if (query.isNullOrEmpty()) return null
		return when {
			policy.rejectAllQuery ->
				ConstraintError(
					code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
					message = "URL must not include a query string.",
					metadata = constraint,
				)
			!policy.allowAnyParam && hasUnpermittedQueryKey(query, policy.params) ->
				ConstraintError(
					code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
					message = "URL query contains a parameter that is not allowed; permitted parameters: ${constraint.allowedParams.joinToString(", ")}.",
					metadata = constraint,
				)
			else -> null
		}
	}

	private fun extensionError(
		uri: URI,
		policy: UrlPolicy,
		constraint: UrlConstraint,
	): ConstraintError<*>? {
		if (!policy.checkExtension) return null
		val extension = uri.path.orEmpty().substringAfterLast('.', "").lowercase()
		if (extension.isNotBlank() && extension in policy.extensions) return null
		return ConstraintError(
			code = ConstraintErrorCode.VALUE_NOT_ALLOWED,
			message = "URL path must end in one of these extensions: ${policy.extensions.joinToString(", ")}.",
			metadata = constraint,
		)
	}

	private fun hasUnpermittedQueryKey(
		query: String,
		allowed: Set<String>,
	): Boolean {
		var start = 0
		while (start <= query.length) {
			val amp = query.indexOf('&', start).let { if (it < 0) query.length else it }
			val eq = query.indexOf('=', start).let { if (it !in 0..amp) amp else it }
			val key = query.substring(start, eq)
			if (key !in allowed) return true
			if (amp == query.length) break
			start = amp + 1
		}
		return false
	}

	private data class UrlPolicy(
		val allowAnyProtocol: Boolean,
		val protocols: Set<String>,
		val allowAnyPort: Boolean,
		val ports: Set<String>,
		val allowAnyParam: Boolean,
		val rejectAllQuery: Boolean,
		val params: Set<String>,
		val checkExtension: Boolean,
		val extensions: Set<String>,
	) {
		companion object {
			fun from(constraint: UrlConstraint): UrlPolicy {
				val allowAnyProtocol = constraint.allowedProtocols.contains("*")
				val allowAnyPort = constraint.allowedPorts.contains("*")
				val allowAnyParam = constraint.allowedParams.contains("*")
				val rejectAllQuery =
					constraint.allowedParams.isEmpty() ||
						(constraint.allowedParams.size == 1 && constraint.allowedParams.contains(""))

				val hasCustomExtensions = !constraint.allowedExtensions.contains("*")
				val checkExtension = constraint.type.isMedia || hasCustomExtensions
				val extensions = when {
					!checkExtension -> emptySet()
					hasCustomExtensions -> constraint.allowedExtensions.mapTo(HashSet()) { it.lowercase() }
					else -> constraint.type.extensions.toHashSet()
				}

				return UrlPolicy(
					allowAnyProtocol = allowAnyProtocol,
					protocols = if (allowAnyProtocol) emptySet() else constraint.allowedProtocols.mapTo(HashSet()) { it.lowercase() },
					allowAnyPort = allowAnyPort,
					ports = if (allowAnyPort) emptySet() else constraint.allowedPorts,
					allowAnyParam = allowAnyParam,
					rejectAllQuery = rejectAllQuery,
					params = if (allowAnyParam || rejectAllQuery) emptySet() else constraint.allowedParams,
					checkExtension = checkExtension,
					extensions = extensions,
				)
			}
		}
	}
}

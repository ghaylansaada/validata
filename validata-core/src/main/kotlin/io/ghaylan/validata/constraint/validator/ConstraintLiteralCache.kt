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
package io.ghaylan.validata.constraint.validator

import io.ghaylan.validata.constraint.validator.ConstraintLiteralCache.cache
import java.util.concurrent.ConcurrentHashMap

/**
 * Memoizes expensive parses of **immutable** constraint literals across requests.
 *
 * Generated schemas keep one constraint-metadata instance per annotation site for the life of the
 * JVM. Re-parsing `constraint.value` / patterns on every call is pure waste on the success path.
 *
 * Keys are usually those metadata instances (stable identity + value equality for data classes).
 * Failed parses are not cached — a bad literal stays a loud per-call failure.
 *
 * ### Lifetime / bounds contract
 *
 * This cache is intentionally **unbounded** and process-scoped. It is correct only when keys are
 * static-schema identities (KSP-emitted metadata singletons or equivalent process-wide instances).
 * Do not key by per-request or throwaway constraint objects — that would grow without bound.
 * Dynamic-schema hosts that mint new metadata instances per request must not rely on this cache
 * without an eviction strategy.
 *
 * @author Ghaylan Saada
 */
internal object ConstraintLiteralCache {
	
	/**
	 * Concurrent map of constraint-site keys to memoized parsed values.
	 */
	private val cache = ConcurrentHashMap<Any, Any>(64)
	
	/**
	 * Returns a previously computed value for [key], or computes and stores [parse]'s non-null result.
	 *
	 * Side effects: may insert into [cache] when [parse] returns non-null and the key was absent.
	 *
	 * @param T Non-null memoized result type.
	 * @param key Usually the constraint metadata instance whose literal is being parsed.
	 * @param parse Pure parse of the literal; must not depend on the request value.
	 * @return Cached or freshly parsed value, or `null` when [parse] fails.	 
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T: Any> getOrParse(
		key: Any,
		parse: () -> T?,
	): T? {
		val cached = cache[key]
		if (cached != null) return cached as T
		val parsed = parse() ?: return null
		cache.putIfAbsent(key, parsed)
		return parsed
	}
	
	/**
	 * Like [getOrParse] when the compute step cannot fail with `null`
	 * (e.g. building a policy object or parsing a temporal that throws on bad input).
	 *
	 * Side effects: may insert into [cache] when the key was absent.
	 *
	 * @param T Non-null memoized result type.
	 * @param key Usually the constraint metadata instance whose literal is being parsed.
	 * @param compute Pure compute of the value; must not depend on the request value.
	 * @return Cached or freshly computed value.	 
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T: Any> getOrCompute(
		key: Any,
		compute: () -> T,
	): T {
		val cached = cache[key]
		if (cached != null) return cached as T
		val computed = compute()
		cache.putIfAbsent(key, computed)
		return computed
	}
	
	/**
	 * Clears memoized literals between characterization runs.
	 *
	 * Side effects: empties [cache].	 
	 */
	internal fun clearForTests() {
		cache.clear()
	}
}

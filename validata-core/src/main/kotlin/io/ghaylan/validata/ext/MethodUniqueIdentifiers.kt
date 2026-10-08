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
package io.ghaylan.validata.ext

import io.ghaylan.validata.ext.MethodUniqueIdentifiers.buildIdentifier
import io.ghaylan.validata.ext.MethodUniqueIdentifiers.cache
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Builds and caches endpoint identifiers that must match KSP-generated request-schema keys
 * byte-for-byte.
 *
 * Format: `package.ClassName#methodName(pkg.ParamType1,pkg.ParamType2)`. Parameter types use
 * [Class.getTypeName] so overloaded handlers stay distinct. Kotlin's synthetic `Continuation` on
 * suspend handlers is omitted; nested classes use JVM names (`Outer$Inner`), matching the
 * processor's `EndpointIdentifier`.
 *
 * Lives in `:validata-core` (Spring-free) so WebMVC (`:validata`) and optional `:validata-openapi`
 * share one lookup-key implementation.
 *
 * ### Cache contract
 *
 * Memoized by [Method] identity so a hit skips [buildIdentifier]. Unbounded under the
 * static-schema / JVM-lifetime contract: Spring (and similar) keep handler [Method] instances for
 * the process — same assumption as registry schema caches keyed by [Class]. Hot ClassLoader
 * redeploy without process restart is not a supported scenario for this library.*
 * 
 * @author Ghaylan Saada
 */
object MethodUniqueIdentifiers {

	/**
	 * Memoized endpoint ids keyed by reflective [Method] (process-lifetime handler identities).
	 */
	private val cache = ConcurrentHashMap<Method, String>()

	/**
	 * Returns the stable endpoint id for this reflective [Method], computing it once per method.
	 *
	 * Side effect: may insert into [cache] on first lookup for this [Method] instance.
	 *
	 * @receiver Handler method whose signature forms the identifier.
	 * @return Unique string token matching compile-time generated request-schema maps.
	 */
	fun Method.getUniqueIdentifier(): String =
		cache.computeIfAbsent(this) { buildIdentifier(it) }

	/**
	 * Builds the endpoint id string without touching [cache].
	 *
	 * @param method Handler method whose declaring class, name, and parameter types form the id.
	 * @return Unique string token matching compile-time generated request-schema maps.
	 */
	private fun buildIdentifier(method: Method): String = buildString {
		append(method.declaringClass.name)
		append('#')
		append(method.name)
		append('(')
		// Avoid filterNot's intermediate List — append types in one pass (cache miss only).
		var first = true
		for (type in method.parameterTypes) {
			if (type.name == "kotlin.coroutines.Continuation") continue
			if (!first) append(',')
			append(type.typeName)
			first = false
		}
		append(')')
	}
}

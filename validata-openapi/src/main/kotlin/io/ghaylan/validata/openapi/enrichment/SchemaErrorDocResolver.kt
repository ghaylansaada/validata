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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.model.ConstraintErrorDefinition
import io.ghaylan.validata.openapi.enrichment.SchemaErrorDocResolver.MISS
import io.ghaylan.validata.schema.docs.SchemaErrorDoc
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves effective [SchemaErrorDoc] code/message from a [ConstraintErrorDefinition] catalog
 * when the baked IR left [SchemaErrorDoc.message] blank (or when publishing prefers catalog `code`).
 *
 * Annotation / IR [SchemaErrorDoc.message] wins when non-blank. Lookup uses the enum constant
 * named [SchemaErrorDoc.code] on [SchemaErrorDoc.catalogFqcn]. Catalog loads are cached.
 *
 * Lives in `enrichment` (not `presentation`) so property writers can resolve catalogs without a
 * package cycle back into the error-doc SPI layer.*
 * 
 * @author Ghaylan Saada
 */
object SchemaErrorDocResolver {

	private val log = LoggerFactory.getLogger(SchemaErrorDocResolver::class.java)

	/**
	 * Sentinel for a cached miss (ConcurrentHashMap disallows null values).
	 */
	private val MISS: Any = Any()

	/**
	 * Catalog FQCNs already logged as unloadable (one warn per class).
	 */
	private val warnedCatalogs = ConcurrentHashMap.newKeySet<String>()

	/**
	 * Cache of catalog FQCN + code → [ConstraintErrorDefinition] or [MISS].
	 */
	private val entryCache = ConcurrentHashMap<CatalogKey, Any>()

	/**
	 * Effective machine code for OpenAPI publishing.
	 *
	 * @param doc baked schema error doc
	 * @return catalog entry [ConstraintErrorDefinition.code] when loadable; otherwise [SchemaErrorDoc.code]
	 */
	fun effectiveCode(doc: SchemaErrorDoc): String =
		loadEntry(doc)?.code ?: doc.code

	/**
	 * Effective documentation message for OpenAPI publishing.
	 *
	 * @param doc baked schema error doc
	 * @return non-blank [SchemaErrorDoc.message], else catalog [ConstraintErrorDefinition.message], else `""`
	 */
	fun effectiveMessage(doc: SchemaErrorDoc): String {
		if (doc.message.isNotBlank()) return doc.message
		return loadEntry(doc)?.message.orEmpty()
	}

	/**
	 * Resolved `code` + `message` for publishers and `x-validata-errors`.
	 *
	 * @param doc baked schema error doc
	 * @return pair of effective code and message
	 */
	fun resolve(doc: SchemaErrorDoc): Pair<String, String> =
		effectiveCode(doc) to effectiveMessage(doc)

	/**
	 * Clears the catalog entry cache (OpenAPI rebuilds / tests).
	 *
	 * @param clearWarnings when `true`, also clears unloadable-catalog warn state (tests)
	 */
	fun clear(clearWarnings: Boolean = false) {
		entryCache.clear()
		if (clearWarnings) warnedCatalogs.clear()
	}

	/**
	 * Clears the catalog entry cache and warning state so tests can assert resolve behavior in isolation.
	 */
	fun resetForTests() {
		clear(clearWarnings = true)
	}

	/**
	 * Loads the catalog enum constant named [SchemaErrorDoc.code] when [SchemaErrorDoc.catalogFqcn]
	 * is set and the class is a [ConstraintErrorDefinition] enum.
	 *
	 * @param doc baked schema error doc
	 * @return catalog entry, or `null` when missing / unloadable / not an enum definition
	 */
	private fun loadEntry(doc: SchemaErrorDoc): ConstraintErrorDefinition? {
		val fqcn = doc.catalogFqcn ?: return null
		if (doc.code.isBlank()) return null
		val key = CatalogKey(fqcn, doc.code)
		val cached = entryCache[key]
		if (cached != null) {
			return if (cached === MISS) null else cached as ConstraintErrorDefinition
		}
		val loaded = loadEntryUncached(fqcn, doc.code)
		entryCache.putIfAbsent(key, loaded ?: MISS)
		val again = entryCache[key]
		return if (again === MISS || again == null) null else again as ConstraintErrorDefinition
	}

	/**
	 * Uncached [Class.forName] + enum constant lookup.
	 *
	 * Catches only reflective / value failures — does not swallow [Error].
	 */
	private fun loadEntryUncached(fqcn: String, code: String): ConstraintErrorDefinition? =
		try {
			@Suppress("UNCHECKED_CAST")
			val clazz = Class.forName(fqcn) as Class<out Enum<*>>
			val constant = java.lang.Enum.valueOf(clazz, code)
			constant as? ConstraintErrorDefinition
		} catch (e: ReflectiveOperationException) {
			warnCatalogOnce(fqcn, e)
			null
		} catch (e: ClassCastException) {
			warnCatalogOnce(fqcn, e)
			null
		} catch (e: IllegalArgumentException) {
			// Enum.valueOf: no constant named [code]
			warnCatalogOnce(fqcn, e)
			null
		}

	private fun warnCatalogOnce(fqcn: String, cause: Exception) {
		if (!warnedCatalogs.add(fqcn)) return
		log.warn(
			"Could not load error catalog {}; OpenAPI will keep baked SchemaErrorDoc code/message. ({})",
			fqcn,
			cause.toString())
	}

	/**
	 * Cache key for one catalog enum constant.
	 *
	 * @property catalogFqcn fully qualified enum class name
	 * @property code enum constant name
	 */
	private data class CatalogKey(
		val catalogFqcn: String,
		val code: String,
	)
}

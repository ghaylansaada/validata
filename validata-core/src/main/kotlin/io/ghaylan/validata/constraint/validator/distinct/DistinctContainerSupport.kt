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
package io.ghaylan.validata.constraint.validator.distinct

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertyPath
import io.ghaylan.validata.schema.ValueReader

/**
 * Shared helpers for [DistinctValidator] element-context uniqueness.
 *
 * `@Distinct` is type-use on the collection element only; the engine stamps indexed paths
 * (e.g. `users[1]`). Messages describe the **item** (not the container) and never repeat the
 * index — path already locates the element. Key field names may appear when `by` is non-empty;
 * element payloads and key values never reach the message.
 *
 * @author Ghaylan Saada
 */
internal object DistinctContainerSupport {

	/**
	 * Builds the message for whole-element uniqueness (`by` empty).
	 *
	 * Side effects: none.
	 *
	 * @return Item-scoped rule sentence; path carries the element index.
	 */
	fun unkeyedMessage(): String = "Item is duplicated in the collection."

	/**
	 * Builds the message for keyed uniqueness (`by` non-empty).
	 *
	 * Names only the declared key fields — never their values.
	 *
	 * Side effects: none.
	 *
	 * @param fields Key field names in declaration order; must be non-empty.
	 * @return Item-scoped rule sentence naming the uniqueness key fields.
	 */
	fun keyedMessage(fields: List<String>): String {
		val fieldsPart = fields.joinToString(", ")
		return "Item is duplicated in the collection by fields: $fieldsPart."
	}

	/**
	 * Full duplicate-value set for element-context membership tests.
	 *
	 * Side effects: none.
	 *
	 * @param list Sibling elements from the array context.
	 * @param extractor Projection per element; identity by default.
	 * @return Set of projected values that occur at least twice; [emptySet] when all are unique.
	 */
	inline fun allDuplicates(
		list: List<Any?>,
		extractor: (Any?) -> Any? = { it },
	): Set<Any?> {
		val seen = HashSet<Any?>(list.size)
		var duplicates: HashSet<Any?>? = null
		for (i in list.indices) {
			val projected = extractor(list[i])
			if (!seen.add(projected)) {
				val bag = duplicates ?: HashSet<Any?>().also { duplicates = it }
				bag.add(projected)
			}
		}
		return duplicates ?: emptySet()
	}

	/**
	 * Builds [FieldExtractor]s that close over [ValueReader]s resolved against [schema].
	 *
	 * Side effects: none.
	 *
	 * @param fields Declared `by` field names; each must resolve on [schema].
	 * @param objectSchema Element schema used to resolve readers.
	 * @return One extractor per name, in [fields] iteration order.
	 * @throws IllegalStateException when a name has no matching property on [objectSchema].
	 */
	fun objectFieldExtractors(fields: Set<String>, objectSchema: ObjectSchema): Array<FieldExtractor> {
		val result = arrayOfNulls<FieldExtractor>(fields.size)
		var i = 0
		for (field in fields) {
			val reader: ValueReader = PropertyPath.findProperty(objectSchema, field)?.read
				?: error("Unknown Distinct.by field '$field' on ${objectSchema.type.name}")
			result[i++] = FieldExtractor(field) { item ->
				if (item == null) null else reader.read(item)
			}
		}
		@Suppress("UNCHECKED_CAST")
		return result as Array<FieldExtractor>
	}

	/**
	 * Builds [FieldExtractor]s that read map entries by key name.
	 *
	 * Side effects: none.
	 *
	 * @param fields Declared `by` key names.
	 * @return One extractor per name, in [fields] iteration order; each yields `null` for
	 *   non-map elements and absent keys.
	 */
	fun mapFieldExtractors(fields: Set<String>): Array<FieldExtractor> {
		val result = arrayOfNulls<FieldExtractor>(fields.size)
		var i = 0
		for (field in fields) {
			result[i++] = FieldExtractor(field) { item -> (item as? Map<*, *>)?.get(field) }
		}
		@Suppress("UNCHECKED_CAST")
		return result as Array<FieldExtractor>
	}

	/**
	 * Materializes extractor field names once for error metadata.
	 *
	 * Side effects: none.
	 *
	 * @param extractors Key-field extractors in declaration order.
	 * @return Field names in the same order.
	 */
	fun fieldNames(extractors: Array<FieldExtractor>): List<String> {
		val names = ArrayList<String>(extractors.size)
		for (extractor in extractors) names.add(extractor.field)
		return names
	}

	/**
	 * Named projection of one Distinct `by` field from a list element.
	 *
	 * @property field Declared field or map-key name this extractor reads.
	 * @property read Projection applied to an element; must tolerate `null` elements.
	 */
	class FieldExtractor(
		val field: String,
		private val read: (Any?) -> Any?) {
		/**
		 * Projects [item] to its key-field value.
		 *
		 * Side effects: none beyond whatever the underlying reader performs.
		 *
		 * @param item List element; `null` is allowed.
		 * @return Extracted field value, or `null` when absent or when [item] is `null`.
		 */
		operator fun invoke(item: Any?): Any? = read(item)
	}

	/**
	 * Content-keyed tuple for keyed uniqueness.
	 *
	 * Immutable instances from [Companion.from] go into HashSets. [Companion.probe] and [fill]
	 * support allocation-free membership checks; a probe must **never** be inserted into a set.
	 *
	 * @property parts Backing slots for extracted key values; grown on demand by [fill].
	 * @property length Number of meaningful slots in [parts]; equality and hashing read only these.
	 */
	class ComboKey private constructor(
		private var parts: Array<Any?>,
		private var length: Int) {
		/**
		 * Overwrites this key with [item]'s projections.
		 *
		 * Mutates [parts] and [length]; grows [parts] when [extractors] is longer than the
		 * current capacity. Invalidates any earlier comparison result.
		 *
		 * @param extractors Key-field extractors in declaration order.
		 * @param item Element to project; `null` is allowed.
		 */
		fun fill(extractors: Array<FieldExtractor>, item: Any?) {
			val n = extractors.size
			if (parts.size < n) parts = arrayOfNulls(n)
			length = n
			for (i in 0 until n) {
				parts[i] = extractors[i](item)
			}
		}

		/**
		 * Structural equality over the first [length] slots.
		 *
		 * @param other Candidate; only another [ComboKey] can match.
		 * @return `true` when arity and every slot are equal.
		 */
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is ComboKey) return false
			if (length != other.length) return false
			for (i in 0 until length) {
				if (parts[i] != other.parts[i]) return false
			}
			return true
		}

		/**
		 * Order-sensitive hash over the first [length] slots.
		 *
		 * @return Hash consistent with [equals]; changes after [fill].
		 */
		override fun hashCode(): Int {
			var result = 1
			for (i in 0 until length) {
				val e = parts[i]
				result = 31 * result + (e?.hashCode() ?: 0)
			}
			return result
		}

		/**
		 * Factories for stored keys and reusable probes.
		 */
		companion object {

			/**
			 * Builds an immutable key safe to insert into a HashSet.
			 *
			 * Side effects: none.
			 *
			 * @param extractors Key-field extractors in declaration order.
			 * @param item Element to project; `null` is allowed.
			 * @return Key holding one slot per extractor.
			 */
			fun from(extractors: Array<FieldExtractor>, item: Any?): ComboKey {
				val parts = arrayOfNulls<Any?>(extractors.size)
				for (i in extractors.indices) {
					parts[i] = extractors[i](item)
				}
				return ComboKey(parts, extractors.size)
			}

			/**
			 * Builds an empty reusable key for membership probing only.
			 *
			 * Side effects: none.
			 *
			 * @param capacity Initial slot count; [fill] grows it when a site declares more fields.
			 * @return Zero-arity key to be populated by [fill] before each probe.
			 */
			fun probe(capacity: Int): ComboKey = ComboKey(arrayOfNulls(capacity), 0)
		}
	}
}

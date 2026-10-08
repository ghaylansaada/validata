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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.schema.types.TypeNames

/**
 * Collision-aware FQCN → short-name mapping for one generated Kotlin file.
 *
 * Registers imports on [builder] and rewrites expressions so generated sources prefer
 * `RequiredConstraint(…)` over fully qualified constructor calls. When two distinct types
 * share a simple name, the second keeps its FQCN (no silent wrong import).
 *
 * @param builder Destination source assembler that owns the import set.
 *
 * @author Ghaylan Saada
 */
internal class ImportScope(
	private val builder: KotlinSourceBuilder,
) {

	private val simpleToFqcn = LinkedHashMap<String, String>()

	/**
	 * Returns a short name for [fqcn] when safe, otherwise [fqcn] unchanged.
	 *
	 * Side effects: may call [KotlinSourceBuilder.addImport].
	 *
	 * @param fqcn Fully qualified type name (nested types allowed, e.g. `…Required.Mode`).
	 * @return Simple name when unique in this file; FQCN on collision or when unimportable.
	 */
	fun ref(fqcn: String): String {
		if (fqcn.isBlank() || !fqcn.contains('.')) return fqcn
		if (fqcn.startsWith(TypeNames.KOTLIN_PACKAGE_PREFIX) ||
			fqcn.startsWith(TypeNames.JAVA_LANG_PACKAGE_PREFIX)
		) {
			return fqcn.substringAfterLast('.')
		}
		val simple = fqcn.substringAfterLast('.')
		val existing = simpleToFqcn[simple]
		when (existing) {
			null -> {
				simpleToFqcn[simple] = fqcn
				builder.addImport(fqcn)
				return simple
			}
			fqcn -> return simple
			else -> return fqcn
		}
	}

	/**
	 * Rewrites [expr] by replacing embedded type FQCNs with [ref] short names (longest first).
	 *
	 * Enum-style trailing segments (`STRICT`, `DAYS`) stay attached to the shortened type
	 * (`Mode.STRICT`, `ChronoUnit.DAYS`).
	 *
	 * Side effects: may register imports via [ref].
	 *
	 * @param expr Kotlin expression that may contain fully qualified type names.
	 * @return Expression with short names where imports were registered.
	 */
	fun shortenExpression(expr: String): String {
		if (expr.isEmpty() || !expr.contains('.')) return expr
		val typeFqcn = LinkedHashSet<String>()
		for (match in TYPE_FQCN_REGEX.findAll(expr)) {
			typeFqcn += trimTrailingEnumEntry(match.value)
		}
		var result = expr
		for (fqcn in typeFqcn.sortedByDescending { it.length }) {
			val short = ref(fqcn)
			if (short == fqcn) continue
			result = result.replace(fqcn, short)
		}
		return result
	}

	companion object {

		/**
		 * Package segments (lowercase start) + one or more PascalCase / nested type segments.
		 * May include a trailing ALL_CAPS enum entry; [trimTrailingEnumEntry] strips that.
		 */
		private val TYPE_FQCN_REGEX =
			Regex("""\b(?:[a-z_]\w*\.)+[A-Z]\w*(?:\.[A-Z]\w*)*""")

		/**
		 * Drops a trailing ALL_CAPS enum constant so the import targets the enum type.
		 *
		 * `…Required.Mode.STRICT` → `…Required.Mode`; `…ChronoUnit.DAYS` → `…ChronoUnit`.
		 * Leaves PascalCase tails alone (`…OnDefault`, `…Required.Mode`).
		 */
		internal fun trimTrailingEnumEntry(fqcn: String): String {
			val lastDot = fqcn.lastIndexOf('.')
			if (lastDot <= 0) return fqcn
			val last = fqcn.substring(lastDot + 1)
			if (last.isEmpty() || !last.all { it.isUpperCase() || it.isDigit() || it == '_' }) {
				return fqcn
			}
			// Require at least one more class segment in the parent (avoid stripping `com.FOO`).
			val parent = fqcn.substring(0, lastDot)
			if (!parent.any { it.isLowerCase() }) return fqcn
			val parentSimple = parent.substringAfterLast('.')
			if (parentSimple.isEmpty() || parentSimple[0].isLowerCase()) return fqcn
			return parent
		}
	}
}

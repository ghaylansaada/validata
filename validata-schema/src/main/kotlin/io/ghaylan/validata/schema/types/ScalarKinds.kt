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
package io.ghaylan.validata.schema.types

import io.ghaylan.validata.schema.shape.ScalarKind

/**
 * Maps leaf FQCNs to [ScalarKind] — shared by KSP, IntelliJ, and runtime classifiers.
 *
 * Unknown leaves become [ScalarKind.OTHER] so generation / checks still proceed.
 * Enum membership is a host concern (`ClassKind.ENUM` / PSI); pass [isEnum] when known.
 *
 * @author Ghaylan Saada
 */
object ScalarKinds {

	/**
	 * Whether [qName] is a known scalar leaf (stdlib / JDK / `java.time.*` / Date / Calendar).
	 */
	fun isScalarLeaf(qName: String): Boolean =
		qName in TypeTables.SCALAR_LEAF_FQCNS || qName.startsWith(TypeNames.JAVA_TIME_PACKAGE_PREFIX)

	/**
	 * Whether [qName] is a JDK / Kotlin platform type that must not be treated as a user DTO.
	 *
	 * `kotlin.Any` is excluded so unmarked `Any` stays dynamic rather than a platform leaf.
	 *
	 * @param qName Fully qualified type name, or `null` when unresolved.
	 */
	fun isPlatformLeaf(qName: String?): Boolean {
		if (qName == null) return true
		if (qName.startsWith(TypeNames.JAVA_PACKAGE_PREFIX)) return true
		if (qName.startsWith(TypeNames.JAVAX_PACKAGE_PREFIX)) return true
		if (qName.startsWith(TypeNames.JAKARTA_PACKAGE_PREFIX)) return true
		return qName.startsWith(TypeNames.KOTLIN_PACKAGE_PREFIX) && qName != TypeNames.ANY_KOTLIN
	}

	/**
	 * Maps a leaf FQCN to a [ScalarKind].
	 *
	 * @param qName Fully qualified leaf type name (boxed or unboxed).
	 * @param isEnum When `true`, returns [ScalarKind.ENUM] regardless of [qName].
	 */
	fun of(qName: String, isEnum: Boolean = false): ScalarKind {
		if (isEnum) return ScalarKind.ENUM
		return when (qName) {
			TypeNames.BOOLEAN_KOTLIN, TypeNames.BOOLEAN_JAVA -> ScalarKind.BOOLEAN
			TypeNames.CHAR_KOTLIN, TypeNames.CHARACTER_JAVA -> ScalarKind.CHAR
			TypeNames.STRING_KOTLIN, TypeNames.STRING_JAVA -> ScalarKind.STRING
			TypeNames.INT_KOTLIN,
			TypeNames.LONG_KOTLIN,
			TypeNames.SHORT_KOTLIN,
			TypeNames.BYTE_KOTLIN,
			TypeNames.INTEGER_JAVA,
			TypeNames.LONG_JAVA,
			TypeNames.SHORT_JAVA,
			TypeNames.BYTE_JAVA,
			TypeNames.BIG_INTEGER -> ScalarKind.INTEGRAL
			TypeNames.FLOAT_KOTLIN,
			TypeNames.DOUBLE_KOTLIN,
			TypeNames.FLOAT_JAVA,
			TypeNames.DOUBLE_JAVA,
			TypeNames.BIG_DECIMAL -> ScalarKind.DECIMAL
			TypeNames.UUID -> ScalarKind.UUID
			else -> if (
				qName.startsWith(TypeNames.JAVA_TIME_PACKAGE_PREFIX) ||
				qName == TypeNames.DATE ||
				qName == TypeNames.CALENDAR
			) {
				ScalarKind.TEMPORAL
			} else {
				ScalarKind.OTHER
			}
		}
	}
}

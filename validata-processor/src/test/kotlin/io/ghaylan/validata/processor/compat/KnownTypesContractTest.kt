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
package io.ghaylan.validata.processor.compat

import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

/**
 * Contract tests for schema [KnownTypes] / [TypeNames], plus processor [TypeView] map/collection shape.
 *
 * JVM `java.*` strings are checked with [Class.forName]. Kotlin stdlib names are checked against
 * [KClass.qualifiedName] — the same names KSP emits for Kotlin declarations.
 *
 * @author Ghaylan Saada
 */
class KnownTypesContractTest {

	@Test
	@DisplayName("canonicalize maps java.lang boxed types onto kotlin.* equivalents")
	fun canonicalizeJavaLangAliases() {
		assertThat(KnownTypes.canonicalize(TypeNames.INTEGER_JAVA)).isEqualTo(TypeNames.INT_KOTLIN)
		assertThat(KnownTypes.canonicalize(TypeNames.STRING_JAVA)).isEqualTo(TypeNames.STRING_KOTLIN)
		assertThat(KnownTypes.canonicalize(TypeNames.OBJECT_JAVA)).isEqualTo(TypeNames.ANY_KOTLIN)
		assertThat(KnownTypes.canonicalize(TypeNames.CHAR_SEQUENCE_JAVA)).isEqualTo(TypeNames.CHAR_SEQUENCE_KOTLIN)
		assertThat(KnownTypes.canonicalize(TypeNames.COMPARABLE_JAVA)).isEqualTo(TypeNames.COMPARABLE_KOTLIN)
		assertThat(KnownTypes.canonicalize("com.acme.User")).isEqualTo("com.acme.User")
	}

	@Test
	@DisplayName("primitiveOrBoxedMatch is true for kotlin.Int vs java.lang.Integer either way")
	fun primitiveOrBoxedMatchBothDirections() {
		assertThat(KnownTypes.primitiveOrBoxedMatch(TypeNames.INT_KOTLIN, TypeNames.INTEGER_JAVA)).isTrue()
		assertThat(KnownTypes.primitiveOrBoxedMatch(TypeNames.INTEGER_JAVA, TypeNames.INT_KOTLIN)).isTrue()
		assertThat(KnownTypes.primitiveOrBoxedMatch(TypeNames.INT_KOTLIN, TypeNames.LONG_KOTLIN)).isFalse()
	}

	@Test
	@DisplayName("isNumeric accepts BigDecimal and rejects String")
	fun isNumericBoundaries() {
		assertThat(KnownTypes.isNumeric(TypeNames.BIG_DECIMAL)).isTrue()
		assertThat(KnownTypes.isNumeric(TypeNames.INT_KOTLIN)).isTrue()
		assertThat(KnownTypes.isNumeric(TypeNames.STRING_KOTLIN)).isFalse()
	}

	@Test
	@DisplayName("isTemporal allowlist includes LocalDate but not Month or Duration")
	fun isTemporalAllowlist() {
		assertThat(KnownTypes.isTemporal(TypeNames.LOCAL_DATE)).isTrue()
		assertThat(KnownTypes.isTemporal(TypeNames.TEMPORAL)).isTrue()
		assertThat(KnownTypes.isTemporal(TypeNames.MONTH)).isFalse()
		assertThat(KnownTypes.isTemporal(TypeNames.DURATION)).isFalse()
	}

	@Test
	@DisplayName("TypeView.isCollectionLike is true for List and false for arrays")
	fun isCollectionLikeExcludesArrays() {
		assertThat(TypeView(TypeNames.LIST_KOTLIN).isCollectionLike()).isTrue()
		assertThat(
			TypeView(
				qualifiedName = TypeNames.ARRAY_KOTLIN,
				isArray = true,
				arrayElement = TypeView(TypeNames.STRING_KOTLIN),
			).isCollectionLike(),
		).isFalse()
	}

	@Test
	@DisplayName("TypeView.isMapLike recognizes MutableMap and java.util.Map; HashMap needs Map in assignableSupertypes")
	fun isMapLike() {
		assertThat(TypeView(TypeNames.MUTABLE_MAP_KOTLIN).isMapLike()).isTrue()
		assertThat(TypeView(TypeNames.MAP_JAVA).isMapLike()).isTrue()
		assertThat(TypeView(TypeNames.STRING_KOTLIN).isMapLike()).isFalse()
		assertThat(TypeView("java.util.HashMap").isMapLike()).isFalse()
		assertThat(
			TypeView(
				qualifiedName = "java.util.HashMap",
				assignableSupertypes = setOf(TypeNames.MAP_JAVA),
			).isMapLike(),
		).isTrue()
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("jvmLoadableFqcn")
	@DisplayName("KnownTypes ALL_JVM_LOADABLE entry resolves via Class.forName")
	fun jvmLoadableFqcnExists(fqcn: String) {
		try {
			Class.forName(fqcn, false, KnownTypesContractTest::class.java.classLoader)
		} catch (e: ClassNotFoundException) {
			throw AssertionError(
				"Hardcoded JVM FQCN '$fqcn' in KnownTypes.ALL_JVM_LOADABLE does not resolve. " +
					"Update schema TypeNames / KnownTypes (or restore the renamed type).",
				e,
			)
		}
	}

	@Test
	@DisplayName("ALL_JVM_LOADABLE has no duplicates and only java.* names")
	fun jvmLoadableListShape() {
		assertThat(KnownTypes.ALL_JVM_LOADABLE).doesNotHaveDuplicates()
		assertThat(KnownTypes.ALL_JVM_LOADABLE).isNotEmpty
		assertThat(KnownTypes.ALL_JVM_LOADABLE).allMatch { it.startsWith(TypeNames.JAVA_PACKAGE_PREFIX) }
	}

	@Test
	@DisplayName("ALL_KOTLIN_FQCN has no duplicates and only kotlin.* names")
	fun kotlinFqcnListShape() {
		assertThat(KnownTypes.ALL_KOTLIN_FQCN).doesNotHaveDuplicates()
		assertThat(KnownTypes.ALL_KOTLIN_FQCN).isNotEmpty
		assertThat(KnownTypes.ALL_KOTLIN_FQCN).allMatch { it.startsWith(TypeNames.KOTLIN_PACKAGE_PREFIX) }
	}

	@Test
	@DisplayName("hardcoded kotlin.* FQCNs match KClass.qualifiedName for stdlib types")
	fun kotlinFqcnMatchReflectionQualifiedNames() {
		val exact = mapOf(
			TypeNames.STRING_KOTLIN to String::class,
			TypeNames.INT_KOTLIN to Int::class,
			TypeNames.LONG_KOTLIN to Long::class,
			TypeNames.SHORT_KOTLIN to Short::class,
			TypeNames.BYTE_KOTLIN to Byte::class,
			TypeNames.FLOAT_KOTLIN to Float::class,
			TypeNames.DOUBLE_KOTLIN to Double::class,
			TypeNames.BOOLEAN_KOTLIN to Boolean::class,
			TypeNames.CHAR_KOTLIN to Char::class,
			TypeNames.NUMBER_KOTLIN to Number::class,
			TypeNames.ANY_KOTLIN to Any::class,
			TypeNames.CHAR_SEQUENCE_KOTLIN to CharSequence::class,
			TypeNames.COMPARABLE_KOTLIN to Comparable::class,
			TypeNames.LIST_KOTLIN to List::class,
			TypeNames.SET_KOTLIN to Set::class,
			TypeNames.COLLECTION_KOTLIN to Collection::class,
			TypeNames.MAP_KOTLIN to Map::class,
		)
		for ((fqcn, kClass) in exact) {
			assertThat(KnownTypes.ALL_KOTLIN_FQCN).contains(fqcn)
			assertThat(kClass.qualifiedName)
				.withFailMessage { "KClass for $fqcn reported qualifiedName=${kClass.qualifiedName}" }
				.isEqualTo(fqcn)
		}

		assertThat(KnownTypes.ALL_KOTLIN_FQCN).contains(
			TypeNames.MUTABLE_LIST_KOTLIN,
			TypeNames.MUTABLE_SET_KOTLIN,
			TypeNames.MUTABLE_COLLECTION_KOTLIN,
			TypeNames.MUTABLE_MAP_KOTLIN,
		)
		assertThat(MutableList::class.qualifiedName).isEqualTo(TypeNames.LIST_KOTLIN)
		assertThat(MutableSet::class.qualifiedName).isEqualTo(TypeNames.SET_KOTLIN)
		assertThat(MutableCollection::class.qualifiedName).isEqualTo(TypeNames.COLLECTION_KOTLIN)
		assertThat(MutableMap::class.qualifiedName).isEqualTo(TypeNames.MAP_KOTLIN)

		assertThat(KnownTypes.ALL_KOTLIN_FQCN).contains(TypeNames.CLONEABLE_KOTLIN)
		assertThat(Cloneable::class.qualifiedName).isEqualTo(TypeNames.CLONEABLE_KOTLIN)
		assertThat(KnownTypes.ALL_JVM_LOADABLE).contains(TypeNames.CLONEABLE_JAVA)
	}

	@Test
	@DisplayName("schema types sources: java.* literals are listed in ALL_JVM_LOADABLE")
	fun sourceJavaLiteralsAreCoveredByAllJvmLoadable() {
		val source = readSchemaTypeSources()
		val literals = JAVA_FQCN_LITERAL_REGEX.findAll(source).map { it.groupValues[1] }.toSet()
		assertThat(literals)
			.withFailMessage { "Expected to find java.* string literals in schema types sources" }
			.isNotEmpty()
		val missing = literals.filter { it !in KnownTypes.ALL_JVM_LOADABLE.toSet() }
		assertThat(missing)
			.withFailMessage {
				"schema types sources contain java.* literals not listed in ALL_JVM_LOADABLE:\n" +
					missing.joinToString("\n")
			}
			.isEmpty()
	}

	@Test
	@DisplayName("schema types sources: kotlin.* literals are listed in ALL_KOTLIN_FQCN")
	fun sourceKotlinLiteralsAreCoveredByAllKotlinFqcn() {
		val source = readSchemaTypeSources()
		val literals = KOTLIN_FQCN_LITERAL_REGEX.findAll(source).map { it.groupValues[1] }.toSet()
		assertThat(literals)
			.withFailMessage { "Expected to find kotlin.* string literals in schema types sources" }
			.isNotEmpty()
		val missing = literals.filter { it !in KnownTypes.ALL_KOTLIN_FQCN.toSet() }
		assertThat(missing)
			.withFailMessage {
				"schema types sources contain kotlin.* literals not listed in ALL_KOTLIN_FQCN:\n" +
					missing.joinToString("\n")
			}
			.isEmpty()
	}

	companion object {
		private val JAVA_FQCN_LITERAL_REGEX =
			Regex(""""(java\.[A-Za-z0-9_.]*[A-Za-z0-9_])"""")

		private val KOTLIN_FQCN_LITERAL_REGEX =
			Regex(""""(kotlin\.[A-Za-z0-9_.]*[A-Za-z0-9_])"""")

		@JvmStatic
		fun jvmLoadableFqcn(): List<String> = KnownTypes.ALL_JVM_LOADABLE

		private fun readSchemaTypeSources(): String {
			val schemaRoots = listOf(
				File("src/main/kotlin/io/ghaylan/validata/schema/types"),
				File("validata-schema/src/main/kotlin/io/ghaylan/validata/schema/types"),
				File("../validata-schema/src/main/kotlin/io/ghaylan/validata/schema/types"),
			)
			val schemaRoot = schemaRoots.firstOrNull { it.isDirectory }
				?: error("Cannot locate validata-schema types/ for FQCN literal scan")
			val files = schemaRoot.listFiles { f ->
				f.isFile && f.name.endsWith(".kt") && f.name != "BuiltinTypeShortNames.kt"
			}.orEmpty().toList()
			check(files.isNotEmpty()) { "No schema types sources found" }
			return files.joinToString("\n") { it.readText() }
		}
	}
}

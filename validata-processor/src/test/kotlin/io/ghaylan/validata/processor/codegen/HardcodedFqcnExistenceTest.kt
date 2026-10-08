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

import io.ghaylan.validata.processor.compat.ConstraintValidatorTypeResolver
import io.ghaylan.validata.processor.fqns.CodegenFqns
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.Modifier
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Guards hardcoded FQCNs used by analysis and codegen.
 *
 * The processor cannot always import consumer / root-library types (classpath cycle), so it
 * compares and emits string FQCNs. This suite fails the build when a constant drifts from a
 * real class on the test classpath (`validata-core` + Spring Web annotations).
 *
 * JVM type catalogs come from schema `KnownTypes` (from schema `KnownTypes`). Kotlin stdlib
 * names are covered in `KnownTypesContractTest` (not Class.forName-able on the JVM).*
 * 
 * @author Ghaylan Saada
 */
class HardcodedFqcnExistenceTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("codegenFqns")
	@DisplayName("CodegenFqns entry resolves via Class.forName")
	fun codegenFqcnExists(fqcn: String) {
		assertLoadable(fqcn)
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("processorFqns")
	@DisplayName("ProcessorFqns entry resolves via Class.forName")
	fun processorFqcnExists(fqcn: String) {
		assertLoadable(fqcn)
	}

	@Test
	@DisplayName("ConstraintValidator base FQCN resolves via Class.forName")
	fun constraintValidatorFqcnExists() {
		assertLoadable(ConstraintValidatorTypeResolver.CONSTRAINT_VALIDATOR_FQN)
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typeFqcnJvmLoadable")
	@DisplayName("KnownTypes ALL_JVM_LOADABLE entry resolves via Class.forName")
	fun typeFqcnJvmLoadableExists(fqcn: String) {
		assertLoadable(fqcn)
	}

	@Test
	@DisplayName("CodegenFqns.ALL_LOADABLE lists every String const exactly once")
	fun codegenAllLoadableIsComplete() {
		assertThat(CodegenFqns.ALL_LOADABLE).doesNotHaveDuplicates()
		assertThat(CodegenFqns.ALL_LOADABLE)
			.containsExactlyInAnyOrderElementsOf(stringConstsOn(CodegenFqns::class.java))
	}

	@Test
	@DisplayName("ProcessorFqns.ALL_LOADABLE lists every type FQCN const (plus Spring mappings)")
	fun processorAllLoadableIsComplete() {
		assertThat(ProcessorFqns.ALL_LOADABLE).doesNotHaveDuplicates()
		val expected = stringConstsOn(ProcessorFqns::class.java)
			.filter { it != ProcessorFqns.GENERATED_FALLBACK_PACKAGE }
			.toMutableSet()
		expected += ProcessorFqns.SPRING_MAPPING_ANNOTATIONS
		assertThat(ProcessorFqns.ALL_LOADABLE.toSet()).isEqualTo(expected)
	}

	@Test
	@DisplayName("codegen writers do not invent io.ghaylan.validata string literals outside CodegenFqns")
	fun writersUseOnlyCodegenFqns() {
		// Guard against the regex going silent after a package rename.
		assertThat(LITERAL_FQCN_REGEX.containsMatchIn("\"${CodegenFqns.OBJECT_SCHEMA}\"")).isTrue()
		assertThat(LITERAL_FQCN_REGEX.find("\"${CodegenFqns.OBJECT_SCHEMA}\"")!!.groupValues[1])
			.isEqualTo(CodegenFqns.OBJECT_SCHEMA)

		val allowed = CodegenFqns.ALL_LOADABLE.toSet()
		val root = locateCodegenDir()
		val writerFiles = listOf(
			root.resolve("EndpointSchemaCodeWriter.kt"),
			root.resolve("SchemaCodeWriter.kt"),
			root.resolve("ConstraintCatalogCodeWriter.kt"),
			root.resolve("ConstraintMetadataCodeWriter.kt"),
			root.resolve("ConstraintSourceRenderer.kt"),
		)
		val orphaned = mutableListOf<String>()
		for (file in writerFiles) {
			assertThat(file).exists()
			val text = file.readText()
			for (match in LITERAL_FQCN_REGEX.findAll(text)) {
				val fqcn = match.groupValues[1]
				if (fqcn !in allowed) {
					orphaned += "${file.name} → $fqcn"
				}
			}
		}
		assertThat(orphaned)
			.withFailMessage {
				"Writers contain hardcoded FQCNs not listed in CodegenFqns.ALL_LOADABLE:\n" +
					orphaned.joinToString("\n")
			}
			.isEmpty()
	}

	companion object {
		/**
		 * Kotlin / Java string literals that look like framework FQCNs.
		 *
		 * Package prefix must stay `io.ghaylan.validata` — rename requires updating this regex.
		 */
		private val LITERAL_FQCN_REGEX =
			Regex(""""(io\.ghaylan\.validata\.[A-Za-z0-9_.]+)"""")

		@JvmStatic
		fun codegenFqns(): List<String> = CodegenFqns.ALL_LOADABLE

		@JvmStatic
		fun processorFqns(): List<String> = ProcessorFqns.ALL_LOADABLE

		@JvmStatic
		fun typeFqcnJvmLoadable(): List<String> = KnownTypes.ALL_JVM_LOADABLE

		private fun assertLoadable(fqcn: String) {
			try {
				Class.forName(fqcn, false, HardcodedFqcnExistenceTest::class.java.classLoader)
			} catch (e: ClassNotFoundException) {
				throw AssertionError(
					"Hardcoded FQCN '$fqcn' does not resolve on the processor test classpath. " +
						"Update CodegenFqns / ProcessorFqns / schema KnownTypes (or restore the renamed type).",
					e,
				)
			}
		}

		/** Static `String` fields on a Kotlin `object` (includes `const val`). */
		private fun stringConstsOn(type: Class<*>): List<String> =
			type.declaredFields
				.filter { it.type == String::class.java && Modifier.isStatic(it.modifiers) }
				.mapNotNull { field ->
					field.isAccessible = true
					field.get(null) as? String
				}
				.filter { it.isNotBlank() }
				.distinct()

		private fun locateCodegenDir(): java.io.File {
			val candidates = listOf(
				java.io.File("src/main/kotlin/io/ghaylan/validata/processor/codegen"),
				java.io.File("validata-processor/src/main/kotlin/io/ghaylan/validata/processor/codegen"),
			)
			return candidates.firstOrNull { it.isDirectory }
				?: error("Cannot locate codegen sources directory for FQCN scan")
		}
	}
}

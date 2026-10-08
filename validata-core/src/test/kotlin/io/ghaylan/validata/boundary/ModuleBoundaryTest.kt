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
package io.ghaylan.validata.boundary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.asSequence

/**
 * Lightweight substitute for ArchUnit: `:validata-core` main sources must stay free of Spring,
 * Jackson, and servlet APIs, and must respect ADR 0001 package DAG edges.
 *
 * Scans main sources under `src/main/kotlin` relative to the Gradle module working directory.*
 * 
 * @author Ghaylan Saada
 */
class ModuleBoundaryTest {

	@Test
	@DisplayName("main sources do not import Spring, Jackson, or servlet packages")
	fun mainSourcesForbidFrameworkImports() {
		val violations = scanImports { _, line ->
			FORBIDDEN_FRAMEWORK.any { prefix -> line.contains(prefix) }
		}

		assertThat(violations)
			.withFailMessage {
				"validata-core must not depend on Spring/Jackson/servlet:\n" +
					violations.joinToString("\n")
			}
			.isEmpty()
	}

	@Test
	@DisplayName("package DAG forbids schema→runtime and other ADR-critical cycles")
	fun packageDagForbidsCriticalCycles() {
		val dagViolations = scanImports { pkg, line ->
			if (pkg to line.removePrefix("import ") in ALLOWED_DAG_EDGES) return@scanImports false
			FORBIDDEN_DAG_EDGES.any { (fromPrefix, importPrefix) ->
				pkg.startsWith(fromPrefix) && line.startsWith("import $importPrefix")
			}
		}

		assertThat(dagViolations)
			.withFailMessage {
				"ADR 0001 package DAG violated:\n" + dagViolations.joinToString("\n")
			}
			.isEmpty()
	}

	/**
	 * Walks main sources and returns `"file:line [pkg]: import …"` for matching import lines.
	 */
	private fun scanImports(predicate: (pkg: String, importLine: String) -> Boolean): List<String> {
		val sourceRoot = locateSourceRoot()
		return Files.walk(sourceRoot).use { stream ->
			stream.asSequence()
				.filter { it.isRegularFile() && it.toString().endsWith(".kt") }
				.flatMap { path ->
					val text = path.readText()
					val pkg = text.lineSequence()
						.map { it.trim() }
						.firstOrNull { it.startsWith("package ") }
						?.removePrefix("package ")
						?.trim()
						?: return@flatMap emptySequence()

					text.lineSequence()
						.mapIndexed { index, line -> Triple(path, index + 1, line.trim()) }
						.filter { (_, _, line) -> line.startsWith("import ") }
						.filter { (_, _, line) -> predicate(pkg, line) }
						.map { (path, lineNo, line) -> "${path.fileName}:$lineNo [$pkg]: $line" }
				}
				.toList()
		}
	}

	private fun locateSourceRoot(): Path {
		val roots = listOf(
			Path.of("src/main/kotlin"),
			Path.of("validata-core/src/main/kotlin"),
		)
		return roots.firstOrNull { Files.isDirectory(it) }
			?: error("Could not locate validata-core main sources (cwd=${Path.of("").toAbsolutePath()})")
	}

	private companion object {
		val FORBIDDEN_FRAMEWORK = listOf(
			"org.springframework",
			// Full Jackson stacks stay out; jackson-annotations alone remains allowed for wire-name APIs.
			"com.fasterxml.jackson.databind",
			"com.fasterxml.jackson.core",
			"com.fasterxml.jackson.module",
			"tools.jackson",
			"jakarta.servlet",
			"javax.servlet",
		)

		/**
		 * Forbidden type-use edges (importer package prefix → forbidden import prefix).
		 *
		 * Named exception: `constraint` may import `schema.ref` annotation-facing enums shared with
		 * the processor (ADR 0001). `runtime` may import `schema.shape` / `schema.ObjectSchema` for
		 * IR ports on the context.

		 */
		val FORBIDDEN_DAG_EDGES = listOf(
			"io.ghaylan.validata.schema" to "io.ghaylan.validata.runtime",
			"io.ghaylan.validata.schema" to "io.ghaylan.validata.engine",
			"io.ghaylan.validata.runtime" to "io.ghaylan.validata.engine",
			"io.ghaylan.validata.runtime" to "io.ghaylan.validata.constraint",
			"io.ghaylan.validata.model" to "io.ghaylan.validata.constraint.annotation",
			"io.ghaylan.validata.model" to "io.ghaylan.validata.engine",
			"io.ghaylan.validata.exception" to "io.ghaylan.validata.engine",
		)

		/**
		 * Exact (package → import) pairs that override [FORBIDDEN_DAG_EDGES].
		 *
		 * Kept empty after deletion of `model.context.*` (typed error payloads). Add a named-type
		 * exception only when a `model` type must import a single annotation vocabulary type.
		 */
		val ALLOWED_DAG_EDGES = emptySet<Pair<String, String>>()
	}
}

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
 * Keeps the Spring host package DAG acyclic:
 * - `config` may depend on `web` / `exception` / `bootstrap` / `aot`
 * - `web` must not depend on `config`
 * - `exception` must not depend on `web`
 * 
 * @author Ghaylan Saada
 */
class HostPackageBoundaryTest {
	
	@Test
	@DisplayName("web does not import config (avoids config ↔ web cycle)")
	fun webDoesNotImportConfig() {
		val violations = scanForbidden(
			importerPrefix = "io.ghaylan.validata.web",
			forbiddenImportPrefix = "io.ghaylan.validata.config")
		assertThat(violations).withFailMessage { "web → config forbidden:\n" + violations.joinToString("\n") }
			.isEmpty()
	}
	
	@Test
	@DisplayName("exception does not import web (avoids config → exception → web → config)")
	fun exceptionDoesNotImportWeb() {
		val violations = scanForbidden(
			importerPrefix = "io.ghaylan.validata.exception",
			forbiddenImportPrefix = "io.ghaylan.validata.web",
		)
		assertThat(violations).withFailMessage { "exception → web forbidden:\n" + violations.joinToString("\n") }
			.isEmpty()
	}
	
	private fun scanForbidden(
		importerPrefix: String,
		forbiddenImportPrefix: String
	): List<String> {
		val sourceRoot = locateSourceRoot()
		return Files.walk(sourceRoot)
			.use { stream ->
				stream.asSequence()
					.filter {
						it.isRegularFile() && it.toString()
							.endsWith(".kt")
					}
					.flatMap { path ->
						val text = path.readText()
						val pkg = text.lineSequence()
							.map { it.trim() }
							.firstOrNull { it.startsWith("package ") }
							?.removePrefix("package ")
							?.trim()
							?: return@flatMap emptySequence()
						
						if (!pkg.startsWith(importerPrefix)) return@flatMap emptySequence()
						
						text.lineSequence()
							.mapIndexed { index, line -> Triple(path, index + 1, line.trim()) }
							.filter { (_, _, line) -> line.startsWith("import $forbiddenImportPrefix") }
							.map { (path, lineNo, line) -> "${path.fileName}:$lineNo [$pkg]: $line" }
					}
					.toList()
			}
	}
	
	private fun locateSourceRoot(): Path {
		val roots = listOf(
			Path.of("src/main/kotlin"),
			Path.of("validata/src/main/kotlin"),
		)
		return roots.firstOrNull { Files.isDirectory(it) }
			?: error("Could not locate validata main sources (cwd=${
				Path.of("")
					.toAbsolutePath()
			})")
	}
}

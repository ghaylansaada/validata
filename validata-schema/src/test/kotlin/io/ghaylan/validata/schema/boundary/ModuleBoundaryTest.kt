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
package io.ghaylan.validata.schema.boundary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.asSequence

/**
 * Lightweight substitute for ArchUnit: this module must stay free of Spring / Jackson / servlet types.
 *
 * Scans main sources under `src/main/kotlin` relative to the Gradle module working directory.
 * 
 * @author Ghaylan Saada
 */
class ModuleBoundaryTest {
	
	@Test
	@DisplayName("main sources do not import Spring, Jackson, or servlet packages")
	fun mainSourcesForbidFrameworkImports() {
		val roots = listOf(
			Path.of("src/main/kotlin"),
			Path.of("validata-schema/src/main/kotlin"))
		
		val sourceRoot = roots.firstOrNull { Files.isDirectory(it) }
			?: error("Could not locate validata-schema main sources (cwd=${Path.of("").toAbsolutePath()})")
		
		val violations = Files.walk(sourceRoot).use { stream ->
			stream.asSequence().filter {
				it.isRegularFile() && it.toString().endsWith(".kt")
			}.flatMap { path ->
				path.readText()
					.lineSequence()
					.mapIndexed { index, line -> Triple(path, index + 1, line.trim()) }
					.filter { (_, _, line) -> line.startsWith("import ") }
					.filter { (_, _, line) -> FORBIDDEN.any { prefix -> line.contains(prefix) } }
					.map { (path, lineNo, line) -> "${path.fileName}:$lineNo: $line" }
			}.toList()
		}
		
		assertThat(violations).withFailMessage {
			"validata-schema must not depend on Spring/Jackson/servlet:\n" + violations.joinToString("\n")
		}.isEmpty()
	}
	
	private companion object {
		
		val FORBIDDEN = listOf(
			"org.springframework",
			"com.fasterxml.jackson",
			"tools.jackson",
			"jakarta.servlet",
			"javax.servlet")
	}
}

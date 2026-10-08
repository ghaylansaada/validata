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
package io.ghaylan.validata.openapi.boundary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.asSequence

/**
 * Lightweight substitute for ArchUnit: forbids the enrichment ↔ presentation package cycle and
 * KDoc-only springdoc edges from presentation.
 * 
 * @author Ghaylan Saada
 */
class ModuleBoundaryTest {
	
	@Test
	@DisplayName("enrichment must not import presentation")
	fun enrichmentDoesNotDependOnPresentation() {
		val violations = scanImports { pkg, line ->
			pkg.startsWith("io.ghaylan.validata.openapi.enrichment") && line.startsWith("import io.ghaylan.validata.openapi.presentation")
		}
		assertThat(violations).withFailMessage {
			"enrichment → presentation cycle edge:\n" + violations.joinToString("\n")
		}
			.isEmpty()
	}
	
	@Test
	@DisplayName("docs must not import enrichment or mapper (KDoc FQCN only)")
	fun docsDoesNotDependOnEnrichmentOrMapper() {
		val violations = scanImports { pkg, line ->
			pkg.startsWith("io.ghaylan.validata.openapi.docs") && (line.startsWith("import io.ghaylan.validata.openapi.enrichment") || line.startsWith(
				"import io.ghaylan.validata.openapi.mapper"))
		}
		assertThat(violations).withFailMessage {
			"docs package must not import enrichment/mapper:\n" + violations.joinToString("\n")
		}
			.isEmpty()
	}
	
	@Test
	@DisplayName("presentation must not import springdoc")
	fun presentationDoesNotDependOnSpringdoc() {
		val violations = scanImports { pkg, line ->
			pkg.startsWith("io.ghaylan.validata.openapi.presentation") && line.startsWith("import io.ghaylan.validata.openapi.springdoc")
		}
		assertThat(violations).withFailMessage {
			"presentation → springdoc edge:\n" + violations.joinToString("\n")
		}
			.isEmpty()
	}
	
	private fun scanImports(predicate: (pkg: String, importLine: String) -> Boolean): List<String> {
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
			Path.of("validata-openapi/src/main/kotlin"),
		)
		return roots.firstOrNull { Files.isDirectory(it) }
			?: error("Could not locate validata-openapi main sources (cwd=${
				Path.of("")
					.toAbsolutePath()
			})")
	}
}

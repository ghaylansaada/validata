/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Fails when leaf packages import upward toward editor/discovery (KDoc reverse-import cycles).
 * 
 * @author Ghaylan Saada
 */
class PackageBoundaryTest {
	
	private val mainRoot: Path = Path.of("src/main/kotlin/io/ghaylan/validata/intellij")
	
	@Test
	@DisplayName("model, path, contract, and editor.color do not import editor or discovery")
	fun leafPackagesDoNotImportEditorOrDiscovery() {
		val forbiddenPrefix = "import io.ghaylan.validata.intellij."
		val violations = mutableListOf<String>()
		for (leaf in listOf("model", "path", "contract", "editor/color")) {
			val dir = mainRoot.resolve(leaf)
			if (!Files.isDirectory(dir)) continue
			Files.walk(dir)
				.use { stream ->
					stream.filter {
						it.toString()
							.endsWith(".kt")
					}
						.forEach { file ->
							Files.readAllLines(file)
								.forEachIndexed { i, line ->
									val trimmed = line.trim()
									if (!trimmed.startsWith(forbiddenPrefix)) return@forEachIndexed
									val rest = trimmed.removePrefix(forbiddenPrefix)
									val bad = rest.startsWith("editor.annotator") || rest.startsWith("editor.reference") || rest.startsWith("editor.completion") || rest.startsWith(
										"editor.inject") || rest.startsWith("discovery.")
									if (bad) {
										violations += "${file.toAbsolutePath()}:${i + 1}: $trimmed"
									}
								}
						}
				}
		}
		assertThat(violations).withFailMessage { violations.joinToString("\n") }
			.isEmpty()
	}
	
	@Test
	@DisplayName("analysis does not import editor packages")
	fun analysisDoesNotImportEditor() {
		val dir = mainRoot.resolve("analysis")
		val violations = mutableListOf<String>()
		Files.walk(dir)
			.use { stream ->
				stream.filter {
					it.toString()
						.endsWith(".kt")
				}
					.forEach { file ->
						Files.readAllLines(file)
							.forEachIndexed { i, line ->
								val trimmed = line.trim()
								if (trimmed.startsWith("import io.ghaylan.validata.intellij.editor.")) {
									violations += "${file.toAbsolutePath()}:${i + 1}: $trimmed"
								}
							}
					}
			}
		assertThat(violations).withFailMessage { violations.joinToString("\n") }
			.isEmpty()
	}
}

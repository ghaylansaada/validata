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
package io.ghaylan.validata.processor.support

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.*
import io.ghaylan.validata.processor.ConstraintCatalogProcessorProvider
import io.ghaylan.validata.processor.ConstraintMetadataProcessorProvider
import io.ghaylan.validata.processor.SchemaProcessorProvider
import io.ghaylan.validata.processor.support.KspCompileSupport.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * Shared compile-testing harness for processor integration tests.
 *
 * Every KSP round-trip needs the same boilerplate: inherit the test classpath (so
 * `validata-schema` + `validata-core` / `validata-core` resolve), enable KSP2, and register
 * the right [SymbolProcessorProvider]s. Tests focus on source snippets and assertions.*
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
internal object KspCompileSupport {
	
	/**
	 * Which processor providers to register for a compilation.
	 */
	enum class Providers {
		
		/** No built-in providers — supply everything via [compile]'s `extraProviders`.		 */
		NONE,
		
		/** [SchemaProcessorProvider] only — `@Validatable` / `@Validate` pipelines.		 */
		SCHEMA,
		
		/** Schema + catalog providers — needed when asserting `@Constraint` catalog output.		 */
		SCHEMA_AND_CATALOG,
		
		/** Metadata generator + schema (no catalog) — Option-2 IR without duplicate-binding checks.		 */
		METADATA_AND_SCHEMA,
		
		/** Metadata generator + schema + catalog (Option 2 GenDemo / role-marker tests).		 */
		ALL,
	}
	
	/**
	 * Compiles [source] as a single file `Sample.kt` with the selected KSP providers.
	 *
	 * @param source complete Kotlin source
	 * @param providers which processors to run (defaults to schema-only)
	 * @param extraProviders additional providers prepended before the selected set (e.g. probes)
	 * @param kspOptions forwarded as KSP `processorOptions`	 
	 */
	fun compile(
		source: String,
		providers: Providers = Providers.SCHEMA,
		kspOptions: Map<String, String> = emptyMap(),
		extraProviders: List<SymbolProcessorProvider> = emptyList(),
	): JvmCompilationResult = KotlinCompilation().apply {
		sources = listOf(SourceFile.kotlin("Sample.kt", source))
		inheritClassPath = true
		useKsp2()
		configureKsp {
			for (provider in extraProviders + providersFor(providers)) {
				symbolProcessorProviders += provider
			}
			for ((key, value) in kspOptions) {
				processorOptions[key] = value
			}
		}
	}
		.compile()
	
	/**
	 * Lists Kotlin sources KSP wrote for this compilation.
	 */
	fun generatedSources(result: JvmCompilationResult): List<File> {
		val ksp = result.outputDirectory.parentFile?.resolve("ksp/sources")
			?: return emptyList()
		return ksp.walkTopDown()
			.filter { it.isFile && it.extension == "kt" }
			.toList()
	}
	
	/**
	 * Concatenates all generated `.kt` contents (handy for substring assertions).
	 */
	fun generatedSourceText(result: JvmCompilationResult): String = generatedSources(result).joinToString("\n") { it.readText() }
	
	private fun providersFor(providers: Providers): List<SymbolProcessorProvider> = when (providers) {
		Providers.NONE -> emptyList()
		Providers.SCHEMA -> listOf(SchemaProcessorProvider())
		Providers.SCHEMA_AND_CATALOG -> listOf(
			SchemaProcessorProvider(),
			ConstraintCatalogProcessorProvider(),
		)
		
		Providers.METADATA_AND_SCHEMA -> listOf(
			ConstraintMetadataProcessorProvider(),
			SchemaProcessorProvider(),
		)
		
		Providers.ALL -> listOf(
			ConstraintMetadataProcessorProvider(),
			SchemaProcessorProvider(),
			ConstraintCatalogProcessorProvider(),
		)
	}
}

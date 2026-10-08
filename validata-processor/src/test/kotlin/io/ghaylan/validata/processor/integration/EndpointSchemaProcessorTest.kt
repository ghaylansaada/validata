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
package io.ghaylan.validata.processor.integration

import com.tschuchort.compiletesting.KotlinCompilation
import io.ghaylan.validata.processor.support.KspCompileSupport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Endpoint IR mechanisms (layout, fail-fast, body lookup, transport rules) — not built-in constraints.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class EndpointSchemaProcessorTest {
	
	@Test
	@DisplayName("class-level-only @Validate generates schemas for every mapped method")
	fun classLevelOnlyGeneratesEndpoints() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import io.ghaylan.validata.schema.Validatable
			import org.springframework.web.bind.annotation.GetMapping
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RequestParam
			import org.springframework.web.bind.annotation.RestController

			@Validatable
			data class BodyDto(val name: String?)

			@RestController
			@RequestMapping("/api")
			@Validate
			class ClassOnlyController {
				@PostMapping("/users")
				fun create(@RequestBody body: BodyDto): String = "ok"

				@GetMapping("/search")
				fun search(@RequestParam("q") q: String?): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val sources = KspCompileSupport.generatedSources(result)
		val aggregator = sources.first { it.name.contains("RequestSchemasModule") }
			.readText()
		assertThat(aggregator).contains("ClassOnlyController#create(")
		assertThat(aggregator).contains("ClassOnlyController#search(")
		val searchConsts = sources.first {
			it.name.contains("ClassOnlyController_search_") &&
				!it.name.contains("Endpoint") &&
				it.name.endsWith("_.kt")
		}.readText()
		assertThat(searchConsts).contains("object ClassOnlyController_search_")
		assertThat(searchConsts).contains("const val Q: String = \"q\"")
		assertThat(
			sources.none {
				it.name.contains("ClassOnlyController_create_") &&
					!it.name.contains("Endpoint") &&
					it.name.endsWith("_.kt")
			},
		).isTrue()
	}
	
	@Test
	@DisplayName("method-level @Validate overrides class-level fail-fast for that method only")
	fun methodLevelOverridesClassLevel() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import io.ghaylan.validata.schema.Validatable
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RestController

			@Validatable
			data class NoteDto(val text: String?)

			@RestController
			@RequestMapping("/api")
			@Validate
			class OverrideController {
				@PostMapping("/notes")
				@Validate(failFast = true, oneErrorPerParam = false)
				fun createNote(@RequestBody note: NoteDto): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val endpoint = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("createNote") }
			.readText()
		assertThat(endpoint).contains("failFast = true")
		assertThat(endpoint).contains("oneErrorPerParam = false")
		assertThat(endpoint).contains("requestBody = GeneratedSchemaLookup.requireGeneratedSchema")
	}
	
	@Test
	@DisplayName("present @RequestHeader params always emit headers ObjectSchema")
	fun headersSectionAlwaysEmittedWhenPresent() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import org.springframework.web.bind.annotation.GetMapping
			import org.springframework.web.bind.annotation.RequestHeader
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RequestParam
			import org.springframework.web.bind.annotation.RestController

			@RestController
			@RequestMapping("/api")
			class HeaderController {
				@GetMapping("/x")
				@Validate
				fun x(
					@RequestParam("q") q: String?,
					@RequestHeader("X-Tenant") tenant: String?,
				): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val endpoint = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("HeaderController") && it.name.contains("Endpoint") }
			.readText()
		assertThat(endpoint).contains("headers = ObjectSchema(")
		assertThat(endpoint).contains("queryParams = ObjectSchema(")
	}
	
	@Test
	@DisplayName("parameter with two transport annotations fails the build")
	fun dualTransportAnnotationFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import org.springframework.web.bind.annotation.GetMapping
			import org.springframework.web.bind.annotation.RequestHeader
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RequestParam
			import org.springframework.web.bind.annotation.RestController

			@RestController
			@RequestMapping("/api")
			class DualController {
				@GetMapping("/x")
				@Validate
				fun x(@RequestParam("x") @RequestHeader("X") x: String?): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("cannot be both")
	}
	
	@Test
	@DisplayName("nested request DTO uses JVM $ binary name in the endpoint id")
	fun nestedDtoUsesJvmBinaryNameInEndpointId() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import io.ghaylan.validata.schema.Validatable
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RestController

			@RestController
			@RequestMapping("/api")
			class NestedDtoController {
				@Validatable
				data class NestedDto(val name: String?)

				@PostMapping("/nested")
				@Validate
				fun create(@RequestBody body: NestedDto): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
		val aggregator = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("RequestSchemasModule") }
			.readText()
		assertThat(aggregator).contains(
			"sample.NestedDtoController#create(sample.NestedDtoController\\\$NestedDto)",
		)
		assertThat(aggregator).doesNotContain(
			"sample.NestedDtoController#create(sample.NestedDtoController.NestedDto)",
		)
	}
	
	@Test
	@DisplayName("same-module @RequestBody type without @Validatable fails the build")
	fun unmarkedSameModuleBodyFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RestController

			data class UnmarkedBody(val name: String?)

			@RestController
			@RequestMapping("/api")
			class UnmarkedBodyController {
				@PostMapping("/x")
				@Validate
				fun create(@RequestBody body: UnmarkedBody): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("not @Validatable")
			.contains("UnmarkedBody")
	}
	
	@Test
	@DisplayName("@RequestBody List<Dto> emits IterableShape ObjectRefShape in requestBody")
	fun requestBodyListDto_emitsIterableObjectRef() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import io.ghaylan.validata.schema.Validatable
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RestController

			@Validatable
			data class ItemDto(val name: String?)

			@RestController
			@RequestMapping("/api")
			class ListBodyController {
				@PostMapping("/items")
				@Validate
				fun create(@RequestBody items: List<ItemDto>): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("IterableShape(ObjectRefShape(lazy { GeneratedSchemaLookup.requireGeneratedSchema(ItemDto::class.java) }))")
		assertThat(text).contains("import sample.ItemDto")
	}
	
	@Test
	@DisplayName("@RequestBody Map fails KSP")
	fun requestBodyMap_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RestController

			@RestController
			@RequestMapping("/api")
			class MapBodyController {
				@PostMapping("/x")
				@Validate
				fun create(@RequestBody body: Map<String, Any?>): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("Map")
	}
	
	@Test
	@DisplayName("multiple @RequestBody parameters fail KSP")
	fun multipleRequestBody_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import io.ghaylan.validata.schema.Validatable
			import org.springframework.web.bind.annotation.PostMapping
			import org.springframework.web.bind.annotation.RequestBody
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RestController

			@Validatable
			data class A(val x: String?)

			@Validatable
			data class B(val y: String?)

			@RestController
			@RequestMapping("/api")
			class MultiBodyController {
				@PostMapping("/x")
				@Validate
				fun create(@RequestBody a: A, @RequestBody b: B): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("Multiple @RequestBody")
	}
	
	@Test
	@DisplayName("flat endpoint Compare self-reference fails KSP")
	fun endpointFlatSelfReference_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.annotation.Compare
			import io.ghaylan.validata.schema.Validate
			import org.springframework.web.bind.annotation.GetMapping
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RequestParam
			import org.springframework.web.bind.annotation.RestController

			@RestController
			@RequestMapping("/api")
			class SelfRefController {
				@GetMapping("/x")
				@Validate
				fun search(
					@RequestParam("q")
					@Compare(ref = "q", operation = Compare.Operation.EQ)
					q: String?,
				): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).containsIgnoringCase("cannot reference itself")
	}
	
	@Test
	@DisplayName("suspend handler endpoint id omits synthetic Continuation parameter")
	fun suspendEndpoint_omitsContinuationFromIdentifier() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.schema.Validate
			import org.springframework.web.bind.annotation.GetMapping
			import org.springframework.web.bind.annotation.RequestMapping
			import org.springframework.web.bind.annotation.RequestParam
			import org.springframework.web.bind.annotation.RestController

			@RestController
			@RequestMapping("/api")
			class SuspendController {
				@GetMapping("/ping")
				@Validate
				suspend fun ping(@RequestParam("q") q: String?): String = "ok"
			}
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val aggregator = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("RequestSchemasModule") }
			.readText()
		assertThat(aggregator).contains("SuspendController#ping(")
		assertThat(aggregator).contains("java.lang.String")
		assertThat(aggregator).doesNotContain("Continuation")
	}
}

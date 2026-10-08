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
import io.ghaylan.validata.processor.support.fixtures.FixtureSnippets
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * ConstraintModelBuilder mechanisms: presence ordering, override dedup, composition.
 *
 * `@Required` is used only where the processor special-cases presence by simple name.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class ConstraintModelBuilderProcessorTest {
	
	@Test
	@DisplayName("presence annotations are emitted before other constraints on the same property")
	fun presenceSortedBeforeFormat() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Required
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			data class OrderDto(
				@field:FormatOk
				@field:Required
				val email: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val src = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("OrderDtoSchema") }
			.readText()
		val requiredIdx = src.indexOf("RequiredConstraint")
		val formatIdx = src.indexOf("FormatOkConstraint")
		assertThat(requiredIdx).isGreaterThanOrEqualTo(0)
		assertThat(formatIdx).isGreaterThanOrEqualTo(0)
		assertThat(requiredIdx).withFailMessage("RequiredConstraint should appear before FormatOkConstraint in generated IR")
			.isLessThan(formatIdx)
	}
	
	@Test
	@DisplayName("subclass redeclaring @Required does not emit two RequiredConstraint runners")
	fun overrideDoesNotDuplicateRequired() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Required
			import io.ghaylan.validata.schema.Validatable

			@Validatable
			open class BaseDto(
				@field:Required
				open val name: String?,
			)

			@Validatable
			class ChildDto(
				@field:Required
				override val name: String?,
			) : BaseDto(name)
			""".trimIndent(),
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val childSrc = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("ChildDtoSchema") }
			.readText()
		assertThat(Regex("""RequiredConstraint\(""").findAll(childSrc).count())
			.withFailMessage("expected a single RequiredConstraint for ChildDto.name, got:\n$childSrc")
			.isEqualTo(1)
	}
	
	@Test
	@DisplayName("subclass inherits parent @Required when it does not redeclare it")
	fun overrideInheritsParentRequired() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Required
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Validatable
			open class BaseDto(
				@field:Required
				open val name: String?,
			)

			@Validatable
			class ChildDto(
				@field:FormatOk
				override val name: String?,
			) : BaseDto(name)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val childSrc = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("ChildDtoSchema") }
			.readText()
		assertThat(childSrc).contains("RequiredConstraint")
		assertThat(childSrc).contains("FormatOkConstraint")
	}
	
	@Test
	@DisplayName("composed annotation expands nested @Constraint meta-annotations")
	fun composedAnnotationExpands() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.annotation.Required
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Required
			@FormatOk
			annotation class Username

			@Validatable
			data class UserDto(
				@field:Username
				val username: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val src = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("UserDtoSchema") }
			.readText()
		assertThat(src).contains("RequiredConstraint")
		assertThat(src).contains("FormatOkConstraint")
		assertThat(src).doesNotContain("CompositionConstraint(")
		assertThat(src).doesNotContain("CompiledConstraints.or(")
		assertThat(src).doesNotContain("CompositionOrRunner(")
	}
	
	@Test
	@DisplayName("OR composition emits CompositionConstraint instead of flattening")
	fun composedAnnotationOrExpands() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@FormatOk
			@TokenCheck
			@ConstraintComposition(Mode.OR)
			annotation class FormatOrToken

			@Validatable
			data class ContactDto(
				@field:FormatOrToken
				val contact: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val src = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("ContactDtoSchema") }
			.readText()
		assertThat(src).contains("CompositionConstraint(")
		assertThat(src).contains("CompiledConstraints.or(")
		assertThat(src).contains("FormatOkConstraint")
		assertThat(src).contains("TokenCheckConstraint")
		assertThat(src).doesNotContain("CompositionOrRunner(")
	}
	
	@Test
	@DisplayName("OR composition with Required member fails KSP")
	fun orCompositionRejectsPresence() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			import io.ghaylan.validata.constraint.annotation.Required
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@Required
			@FormatOk
			@ConstraintComposition(Mode.OR)
			annotation class RequiredOrFormat

			@Validatable
			data class BadDto(
				@field:RequiredOrFormat
				val value: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("Presence must stay outside OR")
	}
	
	@Test
	@DisplayName("OR composition with a single leaf fails KSP")
	fun orCompositionRequiresTwoLeaves() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@FormatOk
			@ConstraintComposition(Mode.OR)
			annotation class LonelyOr

			@Validatable
			data class BadDto(
				@field:LonelyOr
				val value: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("OR requires at least 2")
	}
	
	@Test
	@DisplayName("OR composition rejects nested composed members")
	fun orCompositionRejectsNestedComposedMember() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			import io.ghaylan.validata.constraint.ConstraintComposition
			import io.ghaylan.validata.constraint.ConstraintComposition.Mode
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@FormatOk
			@TokenCheck
			annotation class NestedAnd

			@NestedAnd
			@FormatOk
			@ConstraintComposition(Mode.OR)
			annotation class NestedInOr

			@Validatable
			data class BadDto(
				@field:NestedInOr
				val value: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("nests composed annotation")
	}
	
	@Test
	@DisplayName("AND composition depth cap stops expanding beyond MAX_COMPOSITION_DEPTH")
	fun compositionDepthCap_stopsAtMax() {
		val result = KspCompileSupport.compile(
			"""
			package sample
			${FixtureSnippets.MECHANISM_CONSTRAINTS}

			@FormatOk
			annotation class L4

			@L4
			annotation class L3

			@L3
			annotation class L2

			@L2
			annotation class L1

			@L1
			annotation class L0

			@Validatable
			data class DeepDto(
				@field:L0
				val value: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val src = KspCompileSupport.generatedSources(result)
			.first { it.name.contains("DeepDtoSchema") }
			.readText()
		assertThat(src).withFailMessage("FormatOk at depth beyond cap must not appear in IR:\n$src")
			.doesNotContain("FormatOkConstraint")
	}
}

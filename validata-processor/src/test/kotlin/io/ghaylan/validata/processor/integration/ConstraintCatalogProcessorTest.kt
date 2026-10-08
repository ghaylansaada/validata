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
 * Compile-testing coverage for [ConstraintCatalogProcessor].
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class ConstraintCatalogProcessorTest {
	
	@Test
	@DisplayName("a custom @Constraint annotation emits catalog entries with the right types")
	fun customConstraintEmitsCatalog() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [FlagValidator::class])
			annotation class Flag(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object FlagValidator : ConstraintValidator<String, FlagConstraint>() {
				override fun validate(value: String, constraint: FlagConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val sources = KspCompileSupport.generatedSources(result)
		assertThat(sources.map { it.name }).anyMatch { it.contains("FlagConstraintEntries") }
		assertThat(sources.map { it.name }).anyMatch { it.contains("ConstraintCatalogModule") }
		val text = sources.joinToString("\n") { it.readText() }
		assertThat(text).contains("FlagConstraint::class.java")
		assertThat(text).contains("infoFromClass(String::class.java)")
		assertThat(text).contains("FlagValidator")
		assertThat(text).contains("class ConstraintCatalogModule")
	}
	
	@Test
	@DisplayName("constraints-only module (no @Validatable) still emits a catalog")
	fun constraintsOnlyModuleEmitsCatalog() {
		val result = KspCompileSupport.compile(
			"""
			package onlyconstraints

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [AloneValidator::class])
			annotation class Alone(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object AloneValidator : ConstraintValidator<Any, AloneConstraint>() {
				override fun validate(value: Any, constraint: AloneConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("ConstraintCatalogModule")
		assertThat(text).contains("AloneConstraint")
		assertThat(text).doesNotContain("ObjectSchemasModule")
	}
	
	@Test
	@DisplayName("catalog generation is deterministic across two identical compilations")
	fun catalogGenerationIsDeterministic() {
		val source = """
			package det

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [DetValidator::class])
			annotation class Det(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object DetValidator : ConstraintValidator<Int, DetConstraint>() {
				override fun validate(value: Int, constraint: DetConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
		""".trimIndent()
		val first = KspCompileSupport.generatedSources(
			KspCompileSupport.compile(source, KspCompileSupport.Providers.ALL),
		)
			.associate { it.name to it.readText() }
		val second = KspCompileSupport.generatedSources(
			KspCompileSupport.compile(source, KspCompileSupport.Providers.ALL),
		)
			.associate { it.name to it.readText() }
		assertThat(first.keys).isEqualTo(second.keys)
		for (name in first.keys) {
			assertThat(first[name]).isEqualTo(second[name])
		}
	}
	
	@Test
	@DisplayName("Option-2 generated metadata is visible to the catalog in the same compile")
	fun option2MetadataVisibleToCatalog() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [RoundTripValidator::class])
			annotation class RoundTrip(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object RoundTripValidator : ConstraintValidator<String, RoundTripConstraint>() {
				override fun validate(
					value: String,
					constraint: RoundTripConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("class RoundTripConstraint")
		assertThat(text).contains("RoundTripConstraint::class.java")
		assertThat(text).contains("ConstraintCatalogModule")
	}
	
	@Test
	@DisplayName("catalog accumulates @Constraint annotations across KSP rounds")
	fun accumulatesAcrossRounds() {
		val result = KspCompileSupport.compile(
			source = """
				package multi

				import io.ghaylan.validata.constraint.Constraint
				import io.ghaylan.validata.constraint.ConstraintGroups
				import io.ghaylan.validata.constraint.ConstraintMessage
				import io.ghaylan.validata.constraint.ConstraintValidator
				import io.ghaylan.validata.runtime.ValidationContext
				import io.ghaylan.validata.groups.OnDefault
				import io.ghaylan.validata.model.ConstraintError
				import kotlin.reflect.KClass

				@Constraint(validatedBy = [FirstValidator::class])
				annotation class First(
					@ConstraintMessage
					val message: String = "",
					@ConstraintGroups
					val groups: Array<KClass<*>> = [OnDefault::class],
				)

				object FirstValidator : ConstraintValidator<String, FirstConstraint>() {
					override fun validate(
						value: String,
						constraint: FirstConstraint,
						context: ValidationContext,
					): ConstraintError<*>? = null
				}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
			extraProviders = listOf(LateConstraintStubProcessorProvider()),
		)
		
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("FirstConstraintEntries")
		assertThat(text).contains("SecondConstraintEntries")
		assertThat(text).contains("ConstraintCatalogModule")
		assertThat(text).contains("FirstConstraint::class.java")
		assertThat(text).contains("SecondConstraint::class.java")
	}
	
	@Test
	@DisplayName("empty validatedBy fails KSP")
	fun emptyValidatedBy_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.groups.OnDefault
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [])
			annotation class EmptyBy(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("empty 'validatedBy'")
	}
	
	@Test
	@DisplayName("duplicate catalog binding for same metadata+valueType fails KSP")
	fun duplicateCatalogBinding_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [FirstValidator::class, SecondValidator::class])
			annotation class Dup(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object FirstValidator : ConstraintValidator<String, DupConstraint>() {
				override fun validate(
					value: String,
					constraint: DupConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}

			object SecondValidator : ConstraintValidator<String, DupConstraint>() {
				override fun validate(
					value: String,
					constraint: DupConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("Duplicate constraint catalog binding")
	}
	
	@Test
	@DisplayName("non-object validator without public no-arg constructor fails KSP")
	fun nonObjectValidatorWithoutNoArgCtor_failsKsp() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [NeedsArgValidator::class])
			annotation class NeedsArg(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			class NeedsArgValidator(
				private val flag: Boolean,
			) : ConstraintValidator<String, NeedsArgConstraint>() {
				override fun validate(
					value: String,
					constraint: NeedsArgConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		assertThat(result.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(result.messages).contains("no public no-arg constructor")
	}
}

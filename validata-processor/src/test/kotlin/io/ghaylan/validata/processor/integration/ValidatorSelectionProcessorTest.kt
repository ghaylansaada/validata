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
import io.ghaylan.validata.processor.analyze.ConstraintModelBuilder
import io.ghaylan.validata.processor.support.KspCompileSupport
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Compile-time fixtures proving [ConstraintModelBuilder]
 * emits the most specific validator FQCN into generated schemas.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class ValidatorSelectionProcessorTest {
	
	@Test
	@DisplayName("String property picks CharSequence validator; Int picks Number; enum falls to Any")
	fun multiValidatorSelectionInGeneratedSource() {
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
			import io.ghaylan.validata.schema.Validatable
			import kotlin.reflect.KClass

			object AnyV : ConstraintValidator<Any, FlagConstraint>() {
				override fun validate(value: Any, constraint: FlagConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
			object TextV : ConstraintValidator<CharSequence, FlagConstraint>() {
				override fun validate(value: CharSequence, constraint: FlagConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
			object NumberV : ConstraintValidator<Number, FlagConstraint>() {
				override fun validate(value: Number, constraint: FlagConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [AnyV::class, TextV::class, NumberV::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
			annotation class Flag(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			@Validatable
			data class FlaggedDto(
				@field:Flag val text: String?,
				@field:Flag val count: Int?,
				@field:Flag val other: java.time.DayOfWeek?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val schemaSource = KspCompileSupport.generatedSources(result)
			.filter { it.name.contains("Schema") && !it.name.contains("Fields") }
			.joinToString("\n") { it.readText() }
		assertThat(schemaSource).contains("TextV")
		assertThat(schemaSource).contains("NumberV")
		assertThat(schemaSource).contains("AnyV")
		val textBlock = schemaSource.substringAfter("\"text\"")
			.substringBefore("\"count\"")
		assertThat(textBlock).contains("TextV")
		assertThat(textBlock).doesNotContain("AnyV")
		val countBlock = schemaSource.substringAfter("\"count\"")
			.substringBefore("\"other\"")
		assertThat(countBlock).contains("NumberV")
		assertThat(countBlock).doesNotContain("AnyV")
		val otherBlock = schemaSource.substringAfter("\"other\"")
		assertThat(otherBlock).contains("AnyV")
		assertThat(schemaSource).contains("import sample.TextV")
		assertThat(schemaSource).contains("import sample.NumberV")
		assertThat(schemaSource).contains("import sample.AnyV")
	}
	
	@Test
	@DisplayName("two CharSequence validators at the same rank: lexicographically first FQCN wins")
	fun tieBreakByFqcn() {
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
			import io.ghaylan.validata.schema.Validatable
			import kotlin.reflect.KClass

			object ZebraV : ConstraintValidator<CharSequence, TiedConstraint>() {
				override fun validate(value: CharSequence, constraint: TiedConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
			object AlphaV : ConstraintValidator<CharSequence, TiedConstraint>() {
				override fun validate(value: CharSequence, constraint: TiedConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [ZebraV::class, AlphaV::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
			annotation class Tied(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			@Validatable
			data class TiedDto(@field:Tied val name: String?)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val sources = KspCompileSupport.generatedSources(result)
			.joinToString("\n") { it.readText() }
		assertThat(sources).contains("CompiledConstraints.of(")
		assertThat(sources).contains("AlphaV,")
		assertThat(sources).doesNotContain("ZebraV,")
		assertThat(sources).contains("import sample.AlphaV")
	}
}

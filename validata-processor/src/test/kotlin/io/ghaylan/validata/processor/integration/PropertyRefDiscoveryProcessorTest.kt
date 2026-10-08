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
 * Property-path hosts and compatibility kinds come from annotation-param `@PropertyRef`
 * (copied onto generated metadata), not hard-coded annotation argument names.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class PropertyRefDiscoveryProcessorTest {
	
	@Test
	@DisplayName("custom @PropertyRef on annotation param: bad path fails; good path compiles; type mismatch fails")
	fun customConstraintPropertyRefDiscovery() {
		val preamble = """
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.constraint.PropertyRef
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
			import io.ghaylan.validata.schema.Validatable
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [MatchesSiblingValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
			annotation class MatchesSibling(
				@PropertyRef(compatibility = PropertyRefCompatibilityKind.SAME_SCALAR_KIND)
				val property: String,
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object MatchesSiblingValidator : ConstraintValidator<Any, MatchesSiblingConstraint>() {
				override fun validate(
					value: Any,
					constraint: MatchesSiblingConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
		""".trimIndent()
		val ok = KspCompileSupport.compile(
			"""
			$preamble

			@Validatable
			data class User(
				val password: String?,
				@field:MatchesSibling(property = "password")
				val confirm: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(ok.exitCode).withFailMessage { ok.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val unknown = KspCompileSupport.compile(
			"""
			$preamble

			@Validatable
			data class User(
				val password: String?,
				@field:MatchesSibling(property = "passwrd")
				val confirm: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(unknown.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(unknown.messages).contains("passwrd")
		val typeMismatch = KspCompileSupport.compile(
			"""
			$preamble

			@Validatable
			data class User(
				val age: Int?,
				@field:MatchesSibling(property = "age")
				val confirm: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(typeMismatch.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
		assertThat(typeMismatch.messages).containsIgnoringCase("cannot equal")
	}
	
	@Test
	@DisplayName("string annotation param without @PropertyRef is not treated as a path host")
	fun unmarkedStringFieldIsNotAPathHost() {
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

			@Constraint(validatedBy = [PatternHintValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
			annotation class PatternHint(
				val property: String,
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object PatternHintValidator : ConstraintValidator<Any, PatternHintConstraint>() {
				override fun validate(
					value: Any,
					constraint: PatternHintConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}

			@Validatable
			data class User(
				@field:PatternHint(property = "this_is_not_a_path_and_must_not_fail_existence")
				val name: String?,
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.METADATA_AND_SCHEMA,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
	}
}

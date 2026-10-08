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
 * Option 2 — KSP generates `*Constraint` when `@ConstraintMessage` / `@ConstraintGroups` are present.
 * 
 * @author Ghaylan Saada
 */
@OptIn(ExperimentalCompilerApi::class)
class ConstraintMetadataProcessorTest {
	
	@Test
	@DisplayName("convention generates CrossPkgConstraint in the annotation package")
	fun crossPackageMetadataImport() {
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

			@Constraint(validatedBy = [CrossPkgValidator::class])
			annotation class CrossPkg(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object CrossPkgValidator : ConstraintValidator<String, CrossPkgConstraint>() {
				override fun validate(
					value: String,
					constraint: CrossPkgConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val generated = KspCompileSupport.generatedSources(result)
			.firstOrNull { it.name == "CrossPkgConstraint.kt" }
		assertThat(generated).isNotNull()
		assertThat(generated!!.readText()).contains("package sample")
	}
	
	@Test
	@DisplayName("GenDemo with role markers generates GenDemoConstraint")
	fun genDemoGeneratesMetadata() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [GenDemoValidator::class])
			annotation class GenDemo(
				@ConstraintArg(ConstraintArgKind.NOT_BLANK)
				val token: String = "x",
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object GenDemoValidator : ConstraintValidator<String, GenDemoConstraint>() {
				override fun validate(
					value: String,
					constraint: GenDemoConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).withFailMessage { result.messages }
			.isEqualTo(KotlinCompilation.ExitCode.OK)
		val text = KspCompileSupport.generatedSourceText(result)
		assertThat(text).contains("data class GenDemoConstraint(")
		assertThat(text).contains("Generated metadata for [GenDemo].")
		assertThat(text).doesNotContain("Generated metadata for [sample.GenDemo].")
		assertThat(text).contains("@ConstraintArg(ConstraintArgKind.NOT_BLANK)")
		assertThat(text).contains("val token: String")
		assertThat(text).contains("override val message: String")
		assertThat(text).contains("override val groups: Set<KClass<*>>")
		assertThat(text).contains("import kotlin.reflect.KClass")
		assertThat(text).doesNotContain("Set<kotlin.reflect.KClass<*>>")
		assertThat(text).contains("Do not edit")
	}
	
	@Test
	@DisplayName("missing @ConstraintGroups is a compile error when @ConstraintMessage is present")
	fun missingGroupsRoleMarkerFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [BrokenValidator::class])
			annotation class Broken(
				@ConstraintMessage
				val message: String = "",
				val groups: Array<KClass<*>> = [],
			)

			data class BrokenMeta(
				override val message: String,
				override val groups: Set<KClass<*>>,
			) : ConstraintMetadata()

			object BrokenValidator : ConstraintValidator<String, BrokenMeta>() {
				override fun validate(
					value: String,
					constraint: BrokenMeta,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
		assertThat(result.messages).containsIgnoringCase("ConstraintGroups")
	}
	
	@Test
	@DisplayName("@ConstraintGroups on a non-Array type is a compile error")
	fun wrongGroupsTypeFails() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.model.ConstraintError
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [WrongValidator::class])
			annotation class Wrong(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: String = "",
			)

			data class WrongMeta(
				override val message: String,
				override val groups: Set<KClass<*>>,
			) : ConstraintMetadata()

			object WrongValidator : ConstraintValidator<String, WrongMeta>() {
				override fun validate(
					value: String,
					constraint: WrongMeta,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
		assertThat(result.messages).containsIgnoringCase("Array")
	}
	
	@Test
	@DisplayName("annotation-param @ConstraintArg NOT_BLANK fails blank use-site (dual-read)")
	fun annotationParamArgMarkersWork() {
		val result = KspCompileSupport.compile(
			"""
			package sample

			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import io.ghaylan.validata.schema.Validatable
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import kotlin.reflect.KClass

			@Constraint(validatedBy = [TokenValidator::class])
			annotation class Token(
				@ConstraintArg(ConstraintArgKind.NOT_BLANK)
				val value: String = "ok",
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object TokenValidator : ConstraintValidator<String, TokenConstraint>() {
				override fun validate(
					value: String,
					constraint: TokenConstraint,
					context: ValidationContext,
				): ConstraintError<*>? = null
			}

			@Validatable
			data class Dto(
				@field:Token(value = "")
				val name: String?
			)
			""".trimIndent(),
			providers = KspCompileSupport.Providers.ALL,
		)
		
		assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
		assertThat(result.messages).containsIgnoringCase("blank")
	}
}

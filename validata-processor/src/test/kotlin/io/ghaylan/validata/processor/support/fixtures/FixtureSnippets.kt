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
package io.ghaylan.validata.processor.support.fixtures

/**
 * Shared Kotlin source snippets for mechanism-first KSP tests.
 *
 * Prefer these **custom** `@Constraint` fixtures over built-in annotations
 * (`@Email`, `@Min`, …) so the processor suite scales with authoring mechanisms,
 * not with the built-in catalog.
 * 
 * @author Ghaylan Saada
 */
internal object FixtureSnippets {
	
	/**
	 * Imports shared by most custom-constraint snippets.
	 */
	val CORE_IMPORTS = """
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintGroups
			import io.ghaylan.validata.constraint.ConstraintMessage
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.constraint.PropertyRef
			import io.ghaylan.validata.runtime.ValidationContext
			import io.ghaylan.validata.groups.OnDefault
			import io.ghaylan.validata.model.ConstraintError
			import io.ghaylan.validata.schema.Validatable
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
			import kotlin.reflect.KClass
	""".trimIndent()
	
	/**
	 * Custom constraints covering `@ConstraintArg` kinds and `@PropertyRef` discovery.
	 * Generated metadata types: `TokenCheckConstraint`, `BoundCheckConstraint`,
	 * `ValuesCheckConstraint`, `PatternCheckConstraint`, `MatchesSiblingConstraint`,
	 * `AfterSiblingConstraint`, `FormatOkConstraint`.
	 */
	val MECHANISM_CONSTRAINTS = """
			$CORE_IMPORTS

			@Constraint(validatedBy = [TokenCheckValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.ANNOTATION_CLASS)
			annotation class TokenCheck(
				@ConstraintArg(ConstraintArgKind.NOT_BLANK)
				val token: String = "ok",
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object TokenCheckValidator : ConstraintValidator<Any, TokenCheckConstraint>() {
				override fun validate(value: Any, constraint: TokenCheckConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [BoundCheckValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
			annotation class BoundCheck(
				@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
				val value: String = "0",
				@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
				val min: Int = 0,
				@ConstraintArg(ConstraintArgKind.POSITIVE)
				val factor: Int = 1,
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object BoundCheckValidator : ConstraintValidator<Any, BoundCheckConstraint>() {
				override fun validate(value: Any, constraint: BoundCheckConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [ValuesCheckValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
			annotation class ValuesCheck(
				@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
				@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
				val values: Array<String> = ["A"],
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object ValuesCheckValidator : ConstraintValidator<Any, ValuesCheckConstraint>() {
				override fun validate(value: Any, constraint: ValuesCheckConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [PatternCheckValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
			annotation class PatternCheck(
				@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.REGEX)
				val pattern: String = ".+",
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object PatternCheckValidator : ConstraintValidator<CharSequence, PatternCheckConstraint>() {
				override fun validate(value: CharSequence, constraint: PatternCheckConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

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
				override fun validate(value: Any, constraint: MatchesSiblingConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [AfterSiblingValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
			annotation class AfterSibling(
				@PropertyRef(compatibility = PropertyRefCompatibilityKind.COMPARABLE_FAMILY)
				val property: String,
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object AfterSiblingValidator : ConstraintValidator<Any, AfterSiblingConstraint>() {
				override fun validate(value: Any, constraint: AfterSiblingConstraint, context: ValidationContext): ConstraintError<*>? = null
			}

			@Constraint(validatedBy = [FormatOkValidator::class])
			@Retention(AnnotationRetention.RUNTIME)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
			annotation class FormatOk(
				@ConstraintMessage
				val message: String = "",
				@ConstraintGroups
				val groups: Array<KClass<*>> = [OnDefault::class],
			)

			object FormatOkValidator : ConstraintValidator<Any, FormatOkConstraint>() {
				override fun validate(value: Any, constraint: FormatOkConstraint, context: ValidationContext): ConstraintError<*>? = null
			}
	""".trimIndent()
}

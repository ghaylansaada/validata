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
package io.ghaylan.validata.constraint.composition

import io.ghaylan.validata.constraint.annotation.EmailConstraint
import io.ghaylan.validata.constraint.annotation.RegexConstraint
import io.ghaylan.validata.constraint.validator.string.email.EmailValidator
import io.ghaylan.validata.constraint.validator.string.regex.RegexValidator
import io.ghaylan.validata.engine.ValidatorBackedRunner
import io.ghaylan.validata.groups.OnCreate
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.support.EngineTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Hand-built OR composition: Email ∨ Regex("^VD-") via [CompositionOrRunner].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("Composition OR")
class CompositionOrRunnerTest {
	
	private val engine = EngineTestSupport.engine()
	private val defaultGroups = setOf(OnDefault::class)
	
	data class ContactBody(val contact: String?)
	
	private fun emailChild(groups: Set<KClass<*>> = defaultGroups): CompiledConstraint {
		val meta = EmailConstraint("", groups)
		return CompiledConstraint(meta, ValidatorBackedRunner(EmailValidator, meta), 0)
	}
	
	private fun vdPrefixChild(groups: Set<KClass<*>> = defaultGroups): CompiledConstraint {
		val meta = RegexConstraint(pattern = "^VD-.*", name = "VD_PREFIX", message = "", groups = groups)
		return CompiledConstraint(meta, ValidatorBackedRunner(RegexValidator, meta), 1)
	}
	
	private fun orConstraint(
		message: String = "",
		groups: Set<KClass<*>> = defaultGroups,
		children: List<CompiledConstraint> = listOf(emailChild(), vdPrefixChild()),
	): CompiledConstraint {
		val composition = CompositionConstraint(message, groups, children)
		return CompiledConstraint(composition, CompositionOrRunner(composition), 0)
	}
	
	private fun bodySchema(constraint: CompiledConstraint): ObjectSchema = ObjectSchema(
		type = ContactBody::class.java,
		properties = listOf(
			PropertySpec(
				declaredName = "contact",
				externalName = "contact",
				shape = ScalarShape(ScalarKind.STRING),
				read = { (it as ContactBody).contact },
				constraints = listOf(constraint),
			),
		),
	)
	
	private fun validate(
		value: String?,
		constraint: CompiledConstraint = orConstraint(),
		groups: Set<KClass<*>> = defaultGroups,
		oneErrorPerParam: Boolean = true,
	) = engine.validateRequest(
		schema = EngineTestSupport.bodyRequestSchema(
			body = bodySchema(constraint),
			oneErrorPerParam = oneErrorPerParam,
			groups = groups,
			id = "composition-or-${System.identityHashCode(constraint)}-$oneErrorPerParam",
		),
		body = ContactBody(value),
		params = null,
		headers = null,
		pathVariables = null,
	)
	
	@Nested
	@DisplayName("OR semantics")
	inner class OrSemantics {
		
		@Test
		@DisplayName("email-only value passes")
		fun emailOnlyPasses() {
			assertThat(validate("a@b.com")).isEmpty()
		}
		
		@Test
		@DisplayName("prefix-only value passes")
		fun prefixOnlyPasses() {
			assertThat(validate("VD-42")).isEmpty()
		}
		
		@Test
		@DisplayName("neither side passes → CONSTRAINT_UNSATISFIABLE")
		fun neitherFails() {
			val errors = validate("nope")
			assertThat(errors).hasSize(1)
			assertThat(errors[0].code).isEqualTo(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE)
			assertThat(errors[0].path).isEqualTo("contact")
		}
		
		@Test
		@DisplayName("custom outer message is used on failure")
		fun customMessage() {
			val errors = validate("nope", orConstraint(message = "email or VD- code required"))
			assertThat(errors).hasSize(1)
			assertThat(errors[0].message).isEqualTo("email or VD- code required")
		}
		
		@Test
		@DisplayName("null is skipped by leaf validators → composition passes")
		fun nullSkipped() {
			assertThat(validate(null)).isEmpty()
		}
	}
	
	@Nested
	@DisplayName("groups")
	inner class Groups {
		
		@Test
		@DisplayName("inactive outer groups skip the whole OR")
		fun outerGroupsSkip() {
			val constraint = orConstraint(groups = setOf(OnCreate::class))
			assertThat(validate("nope", constraint, groups = defaultGroups)).isEmpty()
		}
		
		@Test
		@DisplayName("only active children participate; one active success passes")
		fun oneActiveChildSuccess() {
			val constraint = orConstraint(
				children = listOf(
					emailChild(setOf(OnCreate::class)),
					vdPrefixChild(defaultGroups),
				),
			)
			assertThat(validate("VD-1", constraint)).isEmpty()
		}
		
		@Test
		@DisplayName("only active children participate; one active failure fails composition")
		fun oneActiveChildFailure() {
			val constraint = orConstraint(
				children = listOf(
					emailChild(setOf(OnCreate::class)),
					vdPrefixChild(defaultGroups),
				),
			)
			val errors = validate("a@b.com", constraint)
			assertThat(errors).hasSize(1)
			assertThat(errors[0].code).isEqualTo(ConstraintErrorCode.CONSTRAINT_UNSATISFIABLE)
		}
	}
	
	@Nested
	@DisplayName("oneErrorPerParam")
	inner class OneErrorPerParam {
		
		@Test
		@DisplayName("failed OR yields a single path error under oneErrorPerParam")
		fun singlePathError() {
			val errors = validate("nope", oneErrorPerParam = true)
			assertThat(errors).hasSize(1)
			assertThat(errors[0].path).isEqualTo("contact")
		}
	}
}

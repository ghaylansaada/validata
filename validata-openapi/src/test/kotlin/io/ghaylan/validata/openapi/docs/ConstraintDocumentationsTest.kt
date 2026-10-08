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
package io.ghaylan.validata.openapi.docs

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.SizeConstraint
import io.ghaylan.validata.constraint.composition.CompositionConstraint
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.openapi.aot.ValidataOpenApiRuntimeHints
import io.ghaylan.validata.openapi.docs.builtin.*
import io.ghaylan.validata.openapi.support.UnknownProbeConstraint
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

class ConstraintDocumentationsTest {
	
	@BeforeEach
	fun reset() {
		ConstraintDocumentations.resetForTests()
	}
	
	@Test
	@DisplayName("ServiceLoader resolves Size string vs iterable facets")
	fun sizeShapeSensitive() {
		val size = SizeConstraint(min = 2, max = 10, message = "", groups = setOf(OnDefault::class))
		val stringHints = ConstraintDocumentations.resolve(size, ScalarShape(ScalarKind.STRING))
		assertThat(stringHints.facets).anyMatch { it is JsonSchemaFacet.MinLength }
		val listHints = ConstraintDocumentations.resolve(
			size,
			IterableShape(ScalarShape(ScalarKind.STRING)),
		)
		assertThat(listHints.facets).anyMatch { it is JsonSchemaFacet.MinItems }
		assertThat(listHints.facets).noneMatch { it is JsonSchemaFacet.MinLength }
	}
	
	@Test
	@DisplayName("Size, DayOfWeekIn, and split custom documenters are registered")
	fun sizeDocumenterPresent() {
		assertThat(ConstraintDocumentations.all()
			.map { it::class.java }).contains(
			SizeConstraintDocumentation::class.java,
			DaysOfWeekConstraintDocumentation::class.java,
			NumberCustomConstraintDocumentation::class.java,
			StringCustomConstraintDocumentation::class.java,
			MiscCustomConstraintDocumentation::class.java,
		)
	}
	
	@Test
	@DisplayName("resolve returns EMPTY when no documenter supports the metadata")
	fun resolveReturnsEmptyForUnknownMetadata() {
		assertThat(ConstraintDocumentations.resolve(UnknownProbeConstraint(), null).isEmpty).isTrue()
	}
	
	@Test
	@DisplayName("every AOT-registered built-in constraint metadata has an owning documenter")
	fun everyBuiltinConstraintIsOwned() {
		val uncovered = ValidataOpenApiRuntimeHints.BUILTIN_CONSTRAINT_METADATA.mapNotNull { type ->
			val instance = syntheticInstance(type)
			if (ConstraintDocumentations.supporting(instance) == null) type.name else null
		}
		assertThat(uncovered).describedAs("Add ConstraintDocumentation ownership for new core constraints")
			.isEmpty()
	}
	
	/**
	 * Builds a throwaway [ConstraintMetadata] instance for [type] via its primary constructor,
	 * filling args with zeros / empty strings / first enum constants / empty collections.	 
	 */
	private fun syntheticInstance(type: Class<*>): ConstraintMetadata {
		if (type == CompositionConstraint::class.java) {
			return CompositionConstraint(message = "", groups = setOf(OnDefault::class), children = emptyList())
		}
		val ctor = type.kotlin.primaryConstructor
			?: error("No primary constructor for ${type.name}")
		val args = ctor.parameters.associateWith { param ->
			val erasure = param.type.jvmErasure
			when {
				erasure == Boolean::class -> false
				erasure == Int::class -> 0
				erasure == Long::class -> 0L
				erasure == Double::class -> 0.0
				erasure == Float::class -> 0f
				erasure == String::class -> ""
				erasure.isSubclassOf(KClass::class) -> OnDefault::class
				erasure.java.isEnum -> erasure.java.enumConstants.first()
				erasure.isSubclassOf(Set::class) -> if (param.name == "groups") setOf(OnDefault::class) else emptySet<Any>()
				erasure.isSubclassOf(List::class) -> emptyList<Any>()
				erasure.isSubclassOf(Collection::class) -> emptyList<Any>()
				else -> error("Unsupported ctor arg ${param.name}: ${param.type} on ${type.simpleName}")
			}
		}
		return ctor.callBy(args) as ConstraintMetadata
	}
}

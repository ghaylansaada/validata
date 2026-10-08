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

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import io.ghaylan.validata.processor.ConstraintCatalogProcessor

/**
 * Emits a second Option-2 `@Constraint` annotation on round 2 so [ConstraintCatalogProcessor]
 * must accumulate across rounds (T7.4).
 * 
 * @author Ghaylan Saada
 */
internal class LateConstraintStubProcessorProvider: SymbolProcessorProvider {
	
	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = object: SymbolProcessor {
		private var round = 0
		
		override fun process(resolver: Resolver): List<KSAnnotated> {
			round++
			if (round != 2) return emptyList()
			environment.codeGenerator.createNewFile(
				Dependencies(aggregating = false),
				"multi",
				"Second",
			)
				.bufferedWriter()
				.use { out ->
					out.write(
						"""
						package multi

						import io.ghaylan.validata.constraint.Constraint
						import io.ghaylan.validata.constraint.ConstraintGroups
						import io.ghaylan.validata.constraint.ConstraintMessage
						import io.ghaylan.validata.constraint.ConstraintValidator
						import io.ghaylan.validata.runtime.ValidationContext
						import io.ghaylan.validata.groups.OnDefault
						import io.ghaylan.validata.model.ConstraintError
						import kotlin.reflect.KClass

						@Constraint(validatedBy = [SecondValidator::class])
						annotation class Second(
							@ConstraintMessage
							val message: String = "",
							@ConstraintGroups
							val groups: Array<KClass<*>> = [OnDefault::class],
						)

						object SecondValidator : ConstraintValidator<Int, SecondConstraint>() {
							override fun validate(
								value: Int,
								constraint: SecondConstraint,
								context: ValidationContext,
							): ConstraintError<*>? = null
						}
						""".trimIndent(),
					)
				}
			return emptyList()
		}
	}
}

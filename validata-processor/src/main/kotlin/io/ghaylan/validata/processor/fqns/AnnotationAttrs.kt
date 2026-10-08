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
package io.ghaylan.validata.processor.fqns

import io.ghaylan.validata.processor.model.ConstraintModel
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope

/**
 * KSP argument-name and annotation simple-name contracts matched by string.
 *
 * Parameter names are not compile-time references when the declaring type is off the processor
 * classpath or when KSP exposes only `KSValueArgument.name`. Nested objects mirror one annotation
 * (or a tightly related pair). Type FQCNs live in [ProcessorFqns], [CodegenFqns],
 * [OpenApiPresentationFqns], or schema `TypeNames`/`KnownTypes` — not here.
 * 
 * @author Ghaylan Saada
 */
internal object AnnotationAttrs {
	
	/**
	 * `@Constraint` meta-annotation parameter names.
	 */
	object Constraint {
		
		/**
		 * Validator class list argument (`validatedBy = […]`).
		 */
		const val VALIDATED_BY: String = "validatedBy"
	}
	
	/**
	 * `@ConstraintComposition` parameter names.
	 */
	object ConstraintComposition {
		
		/**
		 * Combination mode (`value = ConstraintComposition.Mode.OR`).
		 */
		const val VALUE: String = "value"
	}
	
	/**
	 * `@ConstraintArg` parameter names (and the repeatable `@ConstraintArgs` container).
	 */
	object ConstraintArg {
		
		/**
		 * Kind-rules vararg (`kinds = […]`).
		 */
		const val KINDS: String = "kinds"
		
		/**
		 * Positional vararg name when KSP omits `kinds` (Kotlin often surfaces the first vararg as `value`).
		 */
		const val VALUE: String = "value"
		
		/**
		 * `ConstraintArgTarget` argument name.
		 */
		const val TARGET: String = "target"
		
		/**
		 * Optional diagnostic-prefix argument name.
		 */
		const val MESSAGE: String = "message"
	}
	
	/**
	 * `@ConstraintArgs` container parameter names.
	 */
	object ConstraintArgs {
		
		/**
		 * Nested `@ConstraintArg` array.
		 */
		const val VALUE: String = "value"
	}
	
	/**
	 * `@PropertyRef` parameter names.
	 */
	object PropertyRef {
		
		/**
		 * [PropertyRefScope] argument name.
		 */
		const val SCOPE: String = "scope"
		
		/**
		 * [PropertyRefCompatibilityKind] argument name.
		 */
		const val COMPATIBILITY: String = "compatibility"
	}
	
	/**
	 * Shared constraint-annotation parameter names (not every constraint declares all of them).
	 */
	object ConstraintPayload {
		
		/**
		 * Group-class array (`@ConstraintGroups` role).
		 */
		const val GROUPS: String = "groups"
		
		/**
		 * Human message (`@ConstraintMessage` role).
		 */
		const val MESSAGE: String = "message"
		
		/**
		 * Single typed literal or gate value (e.g. `@Min`, `@RequiredWhen`).
		 */
		const val VALUE: String = "value"
		
		/**
		 * Multi typed literals (e.g. `@In`, `@NotIn`, `@RequiredWhen`).
		 */
		const val VALUES: String = "values"
		
		/**
		 * Legacy sibling property path argument name, kept for constraints that still spell the
		 * reference as `property`.
		 */
		const val PROPERTY: String = "property"
		
		/**
		 * Sibling property path (e.g. `@Compare`, `@RequiredWhen`).
		 */
		const val REF: String = "ref"
	}
	
	/**
	 * `@RequiredWhen` parameter names beyond [ConstraintPayload].
	 */
	object RequiredWhen {
		
		/**
		 * Gate condition enum argument name.
		 */
		const val CONDITION: String = "condition"
		
		/**
		 * Presence mode when the gate is a presence condition.
		 */
		const val MODE: String = "mode"
	}
	
	/**
	 * Jackson `@JsonProperty` argument names used for wire naming.
	 */
	object Jackson {
		
		/**
		 * Explicit external name on `@JsonProperty`.
		 */
		const val VALUE: String = "value"
	}
	
	/**
	 * Spring Web binding annotation argument names (`@RequestParam`, `@PathVariable`, …).
	 *
	 * Effective name order: [NAME], then [VALUE], then the Kotlin parameter name.
	 */
	object SpringBinding {
		
		/**
		 * Explicit binding name.
		 */
		const val NAME: String = "name"
		
		/**
		 * Alias for [NAME] on Spring annotations.
		 */
		const val VALUE: String = "value"
	}
	
	/**
	 * `@Validate` parameters the processor reads.
	 */
	object Validate {
		
		/**
		 * Validation groups for the handler.
		 */
		const val GROUPS: String = "groups"
		
		/**
		 * Stop after the first error per parameter.
		 */
		const val ONE_ERROR_PER_PARAM: String = "oneErrorPerParam"
		
		/**
		 * Abort the whole request after the first failing parameter.
		 */
		const val FAIL_FAST: String = "failFast"
	}
	
	/**
	 * `@Validatable` polymorphism parameters.
	 */
	object Validatable {
		
		/**
		 * Explicit subtype list.
		 */
		const val SUBTYPES: String = "subtypes"
		
		/**
		 * Discriminator property name.
		 */
		const val DISCRIMINATOR: String = "discriminator"
	}
	
	/**
	 * `@Validatable.Subtype` nested annotation parameters.
	 */
	object ValidatableSubtype {
		
		/**
		 * Wire / discriminator value for this subtype.
		 */
		const val NAME: String = "name"
		
		/**
		 * Concrete subtype class (`type = SomeDto::class`).
		 */
		const val TYPE: String = "type"
	}
	
	/**
	 * Annotation simple names used for presence-first constraint ordering.
	 *
	 * Matched against [ConstraintModel.annotationSimpleName],
	 * not FQCNs.
	 */
	object PresenceSimpleNames {
		
		/**
		 * `@Required` annotation simple name.
		 */
		const val REQUIRED: String = "Required"
		
		/**
		 * `@RequiredWhen` annotation simple name.
		 */
		const val REQUIRED_WHEN: String = "RequiredWhen"
		
		/**
		 * Simple names sorted to the front of constraint execution order.
		 *
		 * Side effects: none (immutable set).
		 */
		val ALL: Set<String> = setOf(REQUIRED, REQUIRED_WHEN)
	}
}

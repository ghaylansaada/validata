/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.contract

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.ref.PropertyRefScope

/**
 * Fully-qualified names of library markers the plugin discovers via the **user project** classpath.
 *
 * The IntelliJ module compiles against `validata-schema` for shared contracts (enums, path helpers,
 * scalar compatibility) but still discovers **annotations** (`@PropertyRef`, `@Constraint`, …) by
 * these FQCN strings against whatever the open project has on its classpath — never via
 * `validata-processor`, and never against the Spring host `validata` / `validata.web` modules.
 *
 * Keep aligned with types in:
 * - **validata-core** — `io.ghaylan.validata.constraint.*` ([CONSTRAINT], [PROPERTY_REF], …)
 *   and `io.ghaylan.validata.schema.Validatable` ([VALIDATABLE])
 * - **validata-schema** — `io.ghaylan.validata.schema.ref.*` ([COMPATIBILITY_KIND], …)
 *
 * Locked by `PropertyRefLibraryFqnsTest`. Do not invent alternate packages; renaming a library
 * type without updating these strings silently disables IDE discovery for that marker.
 *
 * Not a bootstrap allowlist of constraint annotation FQCNs — custom constraints light up when
 * authors mark metadata with `@PropertyRef` / `@ConstraintArg`. Not the Marketplace plugin id
 * (`io.ghaylan.validata.intellij.PluginConstants.PLUGIN_ID`).
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefLibraryFqns {
	
	/**
	 * FQCN of `@Constraint` — the meta-annotation on constraint annotation classes
	 * (`io.ghaylan.validata.constraint.Constraint`). Used to locate the `validatedBy` attribute
	 * during property-ref and validated-by discovery. Generated `{Name}Constraint` metadata is
	 * resolved by naming convention, not via a meta-annotation attribute.
	 */
	const val CONSTRAINT: String = "io.ghaylan.validata.constraint.Constraint"
	
	/**
	 * FQCN of `ConstraintValidator` — base type for `@Constraint(validatedBy = …)` implementations
	 * (`io.ghaylan.validata.constraint.ConstraintValidator`). Used when verifying / resolving the
	 * validator class named by the meta-annotation.
	 */
	const val CONSTRAINT_VALIDATOR: String = "io.ghaylan.validata.constraint.ConstraintValidator"
	
	/**
	 * FQCN of `@PropertyRef` — marks metadata / annotation parameters that hold property-path
	 * strings (`io.ghaylan.validata.constraint.PropertyRef`). Primary discovery hook for path hosts.
	 */
	const val PROPERTY_REF: String = "io.ghaylan.validata.constraint.PropertyRef"
	
	/**
	 * FQCN of [PropertyRefScope] (`SIBLING` / `ELEMENT`). Discovery compares argument text
	 * / resolved enum names against this type’s entries.
	 */
	const val PROPERTY_REF_SCOPE: String = "io.ghaylan.validata.schema.ref.PropertyRefScope"
	
	/**
	 * FQCN of `@ConstraintArg` — marks metadata / annotation parameters whose literals tooling
	 * must check (`io.ghaylan.validata.constraint.ConstraintArg`). Repeatable; see [CONSTRAINT_ARGS].
	 */
	const val CONSTRAINT_ARG: String = "io.ghaylan.validata.constraint.ConstraintArg"
	
	/**
	 * FQCN of `@ConstraintArgs` — JVM repeatable container for [CONSTRAINT_ARG]
	 * (`io.ghaylan.validata.constraint.ConstraintArgs`). Discovery expands the container the same
	 * way KSP does; authors still only write `@ConstraintArg`.
	 */
	const val CONSTRAINT_ARGS: String = "io.ghaylan.validata.constraint.ConstraintArgs"
	
	/**
	 * FQCN of schema enum [PropertyRefCompatibilityKind]. Authors reference it from
	 * `@PropertyRef(compatibility = …)`; the plugin matches by entry **name**.
	 */
	const val COMPATIBILITY_KIND: String = "io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind"
	
	/**
	 * FQCN of schema enum [ConstraintArgKind]. Authors pass it as `@ConstraintArg(…)` vararg kinds.
	 */
	const val CONSTRAINT_ARG_KIND: String = "io.ghaylan.validata.schema.ref.ConstraintArgKind"
	
	/**
	 * FQCN of schema enum [ConstraintArgTarget]. Authors pass it as `@ConstraintArg(target = …)`.
	 */
	const val CONSTRAINT_ARG_TARGET: String = "io.ghaylan.validata.schema.ref.ConstraintArgTarget"
	
	/**
	 * FQCN of `@Validatable` — marks a type as a schema root
	 * (`io.ghaylan.validata.schema.Validatable` in **validata-core**). Used by polymorphism
	 * diagnostics (discriminator property presence and subtype assignability).
	 */
	const val VALIDATABLE: String = "io.ghaylan.validata.schema.Validatable"
	
	/**
	 * FQCN of `@RequiredWhen` — conditional presence constraint
	 * (`io.ghaylan.validata.constraint.annotation.RequiredWhen`). Used so IDE literal checks
	 * match the real library annotation, not an unrelated short name.
	 */
	const val REQUIRED_WHEN: String = "io.ghaylan.validata.constraint.annotation.RequiredWhen"
	
	/**
	 * FQCN of `@Required` — unconditional presence constraint
	 * (`io.ghaylan.validata.constraint.annotation.Required`).
	 */
	const val REQUIRED: String = "io.ghaylan.validata.constraint.annotation.Required"
	
	/**
	 * FQCN of `@ConstraintComposition` — marks a composed (non-`@Constraint`) annotation and
	 * selects AND / OR expand (`io.ghaylan.validata.constraint.ConstraintComposition`).
	 */
	const val CONSTRAINT_COMPOSITION: String = "io.ghaylan.validata.constraint.ConstraintComposition"
	
	/**
	 * FQCN of nested `ConstraintComposition.Mode` — AND / OR enum for
	 * [CONSTRAINT_COMPOSITION].
	 */
	const val COMPOSITION_MODE: String = "io.ghaylan.validata.constraint.ConstraintComposition.Mode"
}

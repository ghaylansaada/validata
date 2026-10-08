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

package io.ghaylan.validata.intellij.discovery.cache

import io.ghaylan.validata.intellij.typing.ValidatorTypeView

/**
 * Outcome of reading `@Constraint(validatedBy = […])` for a usage-site constraint annotation.
 *
 * Produced by `ConstraintValidatedByDiscovery` (and memoized by `ConstraintDiscoveryCache`).
 * Callers — subject-type inspections, compatibility ranking — branch on the sealed variants instead of
 * treating “empty list” as ambiguous.
 *
 * Mirrors the author contract KSP uses: accepted subject types come from each validator’s
 * `ConstraintValidator<V, C>` binding, not from an IntelliJ FQCN allowlist.
 *
 * This is **not** a runtime validation result and **not** a schema IR type — IDE-only analysis
 * of library / consumer constraint declarations on the classpath.
 * 
 * @author Ghaylan Saada
 */
internal sealed class ConstraintValidatedByResult

/**
 * At least one validator class in `validatedBy` resolved to a usable value-type parameter `V`.
 *
 * [valueTypes] may contain several views when multiple validators are declared (union of
 * accepted subjects). Ranking / assignability code decides how to combine them.
 *
 * @property valueTypes Resolved `ConstraintValidator` value-type views, one per successfully
 *   analyzed validator (order follows discovery; duplicates are not stripped here).
 * 
 * @author Ghaylan Saada
 */
internal data class ConstraintValidatedByOk(
	val valueTypes: List<ValidatorTypeView>,
): ConstraintValidatedByResult()

/**
 * The annotation is meta-annotated with `@Constraint`, but `validatedBy` is missing, empty,
 * or none of the listed classes resolve to a readable `ConstraintValidator<V, C>` binding.
 *
 * Distinct from [ConstraintValidatedByNotAConstraint]: tooling can still treat the type as a
 * Validata constraint and surface “validators unresolved / regenerate / check classpath”
 * diagnostics rather than ignoring the annotation entirely.
 * 
 * @author Ghaylan Saada
 */
internal data object ConstraintValidatedByMissingValidatedBy: ConstraintValidatedByResult()

/**
 * The resolved annotation declaration is not meta-annotated with `@Constraint`
 * (`io.ghaylan.validata…Constraint` or an unresolved short name `"Constraint"` when PSI
 * cannot bind FQCNs in light tests).
 *
 * Callers should skip Validata-specific subject-type checks for this annotation.
 * 
 * @author Ghaylan Saada
 */
internal data object ConstraintValidatedByNotAConstraint: ConstraintValidatedByResult()

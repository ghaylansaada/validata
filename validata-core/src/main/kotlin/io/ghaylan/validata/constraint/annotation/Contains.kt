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
package io.ghaylan.validata.constraint.annotation

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.collection.contains.ArrayContainsValidator
import io.ghaylan.validata.constraint.validator.collection.contains.CollectionContainsValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import kotlin.reflect.KClass

/**
 * Membership rules over a collection or array subject against [values].
 *
 * Compares each element's `toString()` to the configured literals ([ConstraintArgKind.TYPED_LITERAL]
 * on elements). String scalars are not valid subjects — use [Regex], [In], or [NotIn] instead.
 */
@MustBeDocumented
@Constraint(validatedBy = [
	CollectionContainsValidator::class,
	ArrayContainsValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Contains(
	@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
	val values: Array<String>,
	val mode: Mode = Mode.ANY,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	enum class Mode {
		ANY,
		ALL,
		NONE,
	}
}

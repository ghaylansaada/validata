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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.schema.shape.DynamicShape
import io.ghaylan.validata.schema.shape.IterableShape
import io.ghaylan.validata.schema.shape.MapShape
import io.ghaylan.validata.schema.shape.ObjectRefShape
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape
import io.ghaylan.validata.schema.shape.TypeShape
import java.time.temporal.Temporal
import java.util.UUID

/**
 * Maps Validata [TypeShape] IR to an approximate runtime [Class] for
 * [ConstraintValidator.possibleErrorCodes].
 *
 * IR often carries only [ScalarKind]; when a precise JVM type is unavailable the nearest
 * representative class is used (`String`, `Number`, `Temporal`, …).*
 * 
 * @author Ghaylan Saada
 */
internal object TypeShapeJavaTypes {
	
	/**
	 * Best-effort subject class for [shape].
	 *
	 * @param shape property or type-use shape from Validata IR
	 * @return representative runtime class for docs tooling	 
	 */
	fun resolve(shape: TypeShape): Class<*> = when (shape) {
		is ScalarShape -> scalarClass(shape.kind)
		is ObjectRefShape -> shape.ref.value.type
		is IterableShape -> Collection::class.java
		is MapShape -> Map::class.java
		is DynamicShape -> Any::class.java
	}
	
	/**
	 * Maps a coarse [ScalarKind] to a representative JVM class.
	 */
	private fun scalarClass(kind: ScalarKind): Class<*> = when (kind) {
		ScalarKind.BOOLEAN -> Boolean::class.javaObjectType
		ScalarKind.CHAR -> Char::class.javaObjectType
		ScalarKind.STRING -> String::class.java
		ScalarKind.INTEGRAL, ScalarKind.DECIMAL -> Number::class.java
		ScalarKind.TEMPORAL -> Temporal::class.java
		ScalarKind.ENUM -> Enum::class.java
		ScalarKind.UUID -> UUID::class.java
		ScalarKind.OTHER -> Any::class.java
	}
}

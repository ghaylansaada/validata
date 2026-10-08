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
package io.ghaylan.validata.internal

import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.types.TypeTables
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoField
import java.time.temporal.Temporal
import java.util.UUID

/**
 * Classifies subject [Class] values passed to [ConstraintValidator.possibleErrorCodes].
 *
 * OpenAPI / IR often supply coarse types (`String`, `Number`, `Temporal`, `Collection`,
 * [Any]); helpers treat those conservatively so docs never omit a code that can still fire.
 *
 * Complements [StructureClassifier], which answers the same family questions for `KClass`
 * values while building [TypeInfo]: that one decides the IR shape of a declared type, this one
 * narrows an already-erased runtime [Class] so error-code documentation stays honest. Reflection
 * over Kotlin classes is deliberately avoided here — `possibleErrorCodes` runs per constraint at
 * compile time.*
 * 
 * @author Ghaylan Saada
 */
internal object SubjectTypes {
	
	/**
	 * `true` when [type] is unknown / opaque (`Any` / `Object`) — callers should keep the full
	 * code set.
	 */
	fun isUnknown(type: Class<*>): Boolean =
		type == Any::class.java || type == Object::class.java
	
	/**
	 * `true` when [type] is [CharSequence] or a concrete string-like leaf (incl. Kotlin/Java
	 * `String`).
	 */
	fun isCharSequence(type: Class<*>): Boolean {
		val boxed = box(type)
		return CharSequence::class.java.isAssignableFrom(boxed)
	}
	
	/**
	 * `true` when [type] is `char` / [Char].
	 */
	fun isChar(type: Class<*>): Boolean {
		val boxed = box(type)
		return boxed == Char::class.javaObjectType
	}
	
	/**
	 * `true` when [type] is a [Collection], [Iterable], or JVM array.
	 */
	fun isCollectionLike(type: Class<*>): Boolean {
		if (type.isArray) return true
		val boxed = box(type)
		return Collection::class.java.isAssignableFrom(boxed) || Iterable::class.java.isAssignableFrom(boxed)
	}
	
	/**
	 * `true` when [type] is a [Map].
	 */
	fun isMap(type: Class<*>): Boolean =
		Map::class.java.isAssignableFrom(box(type))
	
	/**
	 * `true` when [type] is a numeric JVM type (boxed/primitive) or the coarse [Number] carrier.
	 */
	fun isNumber(type: Class<*>): Boolean {
		if (type.isPrimitive) {
			return type != Boolean::class.javaPrimitiveType && type != Char::class.javaPrimitiveType
		}
		return Number::class.java.isAssignableFrom(type)
	}
	
	/**
	 * `true` when [type] is an integral number that never takes the finite-decimal parse path
	 * (byte/short/int/long/[BigInteger], or unknown).
	 *
	 * [Float]/[Double]/[BigDecimal]/coarse [Number] return `false` — those can emit
	 * [ConstraintErrorCode.VALUE_PARSING_FAILED].
	 */
	fun isStrictIntegralNumber(type: Class<*>): Boolean {
		if (isUnknown(type)) return false
		val boxed = box(type)
		return when (boxed) {
			Byte::class.javaObjectType,
			Short::class.javaObjectType,
			Int::class.javaObjectType,
			Long::class.javaObjectType,
			BigInteger::class.java,
			-> true
			else -> false
		}
	}
	
	/**
	 * `true` when [type] is a [Temporal] (or the coarse temporal carrier).
	 */
	fun isTemporal(type: Class<*>): Boolean =
		Temporal::class.java.isAssignableFrom(box(type))
	
	/**
	 * `true` when [type] is [Duration] (or unknown / coarse temporal, which may be a duration).
	 */
	fun isDurationCompatible(type: Class<*>): Boolean =
		isUnknown(type) ||
			Duration::class.java.isAssignableFrom(box(type)) ||
			type == Temporal::class.java
	
	/**
	 * `true` when [type] is [Period] (or unknown / coarse temporal).
	 */
	fun isPeriodCompatible(type: Class<*>): Boolean =
		isUnknown(type) ||
			Period::class.java.isAssignableFrom(box(type)) ||
			type == Temporal::class.java
	
	/**
	 * `true` when [type] may expose a [DayOfWeek] (date-bearing temporals).
	 *
	 * Time-only / instant / year-month leaves skip `@DaysOfWeek` at runtime; unknown / coarse
	 * [Temporal] stay optimistic so docs never omit a code that can still fire.
	 */
	fun supportsDayOfWeek(type: Class<*>): Boolean {
		if (isUnknown(type) || type == Temporal::class.java) return true
		if (!isTemporal(type)) return false
		val boxed = box(type)
		return boxed != LocalTime::class.java &&
			boxed != OffsetTime::class.java &&
			boxed != Instant::class.java &&
			boxed != Year::class.java &&
			boxed != YearMonth::class.java &&
			boxed != MonthDay::class.java &&
			!Duration::class.java.isAssignableFrom(boxed) &&
			!Period::class.java.isAssignableFrom(boxed)
	}
	
	/**
	 * `true` when [type] may expose [ChronoField.DAY_OF_MONTH].
	 */
	fun supportsDayOfMonth(type: Class<*>): Boolean {
		if (isUnknown(type) || type == Temporal::class.java) return true
		if (!isTemporal(type)) return false
		val boxed = box(type)
		return boxed != LocalTime::class.java &&
			boxed != OffsetTime::class.java &&
			boxed != Instant::class.java &&
			boxed != Year::class.java &&
			boxed != YearMonth::class.java &&
			!Duration::class.java.isAssignableFrom(boxed) &&
			!Period::class.java.isAssignableFrom(boxed)
	}
	
	/**
	 * `true` when [type] may expose [ChronoField.MONTH_OF_YEAR].
	 */
	fun supportsMonthOfYear(type: Class<*>): Boolean {
		if (isUnknown(type) || type == Temporal::class.java) return true
		if (!isTemporal(type)) return false
		val boxed = box(type)
		return boxed != LocalTime::class.java &&
			boxed != OffsetTime::class.java &&
			boxed != Instant::class.java &&
			boxed != Year::class.java &&
			!Duration::class.java.isAssignableFrom(boxed) &&
			!Period::class.java.isAssignableFrom(boxed)
	}
	
	/**
	 * `true` when [type] may be a structured bean / object walked by deep-empty presence.
	 */
	fun mayBeDeepEmptyStructured(type: Class<*>): Boolean {
		if (isUnknown(type)) return true
		if (isCharSequence(type) || isChar(type)) return false
		if (isCollectionLike(type) || isMap(type)) return true
		if (isNumber(type) || isTemporal(type)) return false
		if (type.isEnum || type == Boolean::class.javaObjectType || type == Boolean::class.javaPrimitiveType) {
			return false
		}
		if (type == UUID::class.java) return false
		if (Duration::class.java.isAssignableFrom(type) || Period::class.java.isAssignableFrom(type)) {
			return false
		}
		return !type.isPrimitive
	}
	
	/**
	 * JVM primitive → wrapper pairs, derived from schema [TypeTables] so boxing cannot drift
	 * from the shared FQCN catalog.
	 */
	private val boxedByPrimitive: Map<Class<*>, Class<*>> = TypeTables.KOTLIN_PRIMITIVE_JVM
		.mapNotNull { (kotlinFqcn, jvmPrim) ->
			val boxedFqcn = TypeTables.PRIMITIVE_TO_BOXED[kotlinFqcn] ?: return@mapNotNull null
			val primClass = jvmPrimitiveClass(jvmPrim) ?: return@mapNotNull null
			val boxedClass = Class.forName(boxedFqcn)
			primClass to boxedClass
		}
		.toMap()

	private fun jvmPrimitiveClass(name: String): Class<*>? = when (name) {
		"boolean" -> Boolean::class.javaPrimitiveType
		"byte" -> Byte::class.javaPrimitiveType
		"short" -> Short::class.javaPrimitiveType
		"int" -> Int::class.javaPrimitiveType
		"long" -> Long::class.javaPrimitiveType
		"float" -> Float::class.javaPrimitiveType
		"double" -> Double::class.javaPrimitiveType
		"char" -> Char::class.javaPrimitiveType
		else -> null
	}

	/**
	 * Boxes primitive [type] to its wrapper; otherwise returns [type].
	 */
	fun box(type: Class<*>): Class<*> {
		if (!type.isPrimitive) return type
		return boxedByPrimitive[type] ?: type
	}
}

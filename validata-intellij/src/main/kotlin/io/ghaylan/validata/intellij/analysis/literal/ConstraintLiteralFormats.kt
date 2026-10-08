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

package io.ghaylan.validata.intellij.analysis.literal

import io.ghaylan.validata.schema.types.ConstraintLiteralRules
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypedLiteralResult

/**
 * Parse rules for constraint string literals — IntelliJ adapter over schema
 * [ConstraintLiteralRules] (shared with KSP and runtime).
 *
 * Formats inspection messages; parse / classification logic lives in schema.
 *
 * @author Ghaylan Saada
 */
internal object ConstraintLiteralFormats {

	fun isTemporalType(typeQualifiedName: String): Boolean =
		KnownTypes.isTemporalLiteralHost(typeQualifiedName)

	fun isNumericType(typeQualifiedName: String): Boolean =
		KnownTypes.isNumeric(typeQualifiedName)

	fun isStringType(typeQualifiedName: String): Boolean =
		KnownTypes.isCharSequenceLike(typeQualifiedName)

	fun isValidNumber(raw: String): Boolean =
		ConstraintLiteralRules.isValidNumber(raw)

	fun isValidTemporal(raw: String, typeQualifiedName: String): Boolean =
		ConstraintLiteralRules.isValidTemporal(raw, typeQualifiedName)

	/**
	 * Validates a string against the direct annotated subject type for `TYPED_LITERAL`.
	 *
	 * @return `null` if OK; else a short error message for the inspection highlight
	 */
	fun typedLiteralError(
		raw: String,
		typeQualifiedName: String?,
		enumConstantNames: Set<String>? = null,
	): String? =
		when (val result = ConstraintLiteralRules.checkTypedLiteral(raw, typeQualifiedName, enumConstantNames)) {
			TypedLiteralResult.Ok -> null
			TypedLiteralResult.BlankNotAllowed -> "must not be blank"
			TypedLiteralResult.InvalidNumber ->
				"expects a decimal number string (e.g. \"18\" or \"1_000\")"
			is TypedLiteralResult.InvalidTemporal ->
				"invalid literal for ${result.typeFqcn}"
			is TypedLiteralResult.UnknownEnumConstant ->
				"unknown enum constant '${result.raw}' (expected one of ${result.expected.sorted()})"
		}
}

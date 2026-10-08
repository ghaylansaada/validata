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
package io.ghaylan.validata.processor.verify

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.schema.types.ConstraintLiteralRules
import io.ghaylan.validata.schema.types.TypedLiteralResult

/**
 * Shared numeric / temporal / typed-literal helpers for constraint string literals.
 *
 * Parse rules live in schema [ConstraintLiteralRules]; this object adapts KSP [KSType] and
 * formats KSP diagnostics.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintLiteralVerifier {

	/**
	 * Whether [raw] parses as a decimal number (Kotlin-style underscores allowed).
	 */
	fun isValidNumberLiteral(raw: String): Boolean =
		ConstraintLiteralRules.isValidNumber(raw)

	/**
	 * Whether [raw] parses as `java.time` type named by [typeQualifiedName].
	 */
	fun isValidTemporalLiteral(raw: String, typeQualifiedName: String): Boolean =
		ConstraintLiteralRules.isValidTemporal(raw, typeQualifiedName)

	/**
	 * Typed-literal check shared by `@ConstraintArg(TYPED_LITERAL)` and
	 * `@Validatable.Subtype(name = …)` against discriminator / subject [valueType].
	 *
	 * @return `null` when OK; else short phrase for diagnostic suffix.
	 */
	fun typedLiteralError(raw: String, valueType: KSType): String? {
		val notNull = valueType.makeNotNullable()
		val typeQ = notNull.declaration.qualifiedName?.asString()
		val enumNames = enumConstantNames(notNull.declaration as? KSClassDeclaration)
		return when (val result = ConstraintLiteralRules.checkTypedLiteral(raw, typeQ, enumNames)) {
			TypedLiteralResult.Ok -> null
			TypedLiteralResult.BlankNotAllowed ->
				"has invalid value '$raw' — expected a non-blank literal."
			TypedLiteralResult.InvalidNumber ->
				"has invalid value '$raw' — expected a decimal number string (e.g. \"18\", \"0.5\", or \"1_000\")."
			is TypedLiteralResult.InvalidTemporal ->
				"has invalid value '$raw' for ${result.typeFqcn} — expected an ISO-8601 / type-specific literal."
			is TypedLiteralResult.UnknownEnumConstant ->
				"has invalid value '$raw' — not a constant of ${result.typeFqcn} " +
					"(expected one of ${result.expected.sorted().joinToString()})"
		}
	}

	private fun enumConstantNames(declaration: KSClassDeclaration?): Set<String>? {
		if (declaration == null) return null
		if (declaration.classKind != ClassKind.ENUM_CLASS) return null
		return declaration.declarations
			.filterIsInstance<KSClassDeclaration>()
			.filter { it.classKind == ClassKind.ENUM_ENTRY }
			.map { it.simpleName.asString() }
			.toSet()
			.takeIf { it.isNotEmpty() }
	}
}

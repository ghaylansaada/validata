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
package io.ghaylan.validata.schema.types

/**
 * Result of a typed-literal check against a subject / gate FQCN.
 *
 * Hosts map these to KSP / IntelliJ diagnostic wording; the verdict stays shared.
 *
 * @author Ghaylan Saada
 */
sealed interface TypedLiteralResult {

	/** Literal is acceptable for the subject type. */
	data object Ok : TypedLiteralResult

	/** Blank literal on a non-string subject. */
	data object BlankNotAllowed : TypedLiteralResult

	/** Subject is numeric but [raw] is not a decimal number string. */
	data object InvalidNumber : TypedLiteralResult

	/**
	 * Subject is a temporal literal host but [raw] does not parse as [typeFqcn].
	 *
	 * @property typeFqcn Temporal host FQCN used for the check.
	 */
	data class InvalidTemporal(val typeFqcn: String) : TypedLiteralResult

	/**
	 * Subject is an enum and [raw] is not among [expected].
	 *
	 * @property raw Rejected literal text.
	 * @property typeFqcn Enum type FQCN.
	 * @property expected Known enum constant names.
	 */
	data class UnknownEnumConstant(
		val raw: String,
		val typeFqcn: String,
		val expected: Set<String>,
	) : TypedLiteralResult
}

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
package io.ghaylan.validata.contract

import io.ghaylan.validata.model.ConstraintErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Freezes [ConstraintErrorCode] names, declaration order, and default messages.
 *
 * Renaming, removing, reordering, or changing a default message is a SemVer break
 * (see root README Stability & SemVer). The root README must document every entry.
 *
 * @author Ghaylan Saada
 */
class ConstraintErrorCodeContractTest {

	@Test
	@DisplayName("ConstraintErrorCode entries, order, and default messages stay frozen")
	fun entriesAndMessagesAreFrozen() {
		assertThat(ConstraintErrorCode.entries.map { it.name to it.message })
			.containsExactlyElementsOf(FROZEN)
	}

	@Test
	@DisplayName("ConstraintErrorCode.code equals the enum constant name")
	fun codeEqualsName() {
		for (entry in ConstraintErrorCode.entries) {
			assertThat(entry.code).isEqualTo(entry.name)
		}
	}

	@Test
	@DisplayName("root README documents every ConstraintErrorCode name")
	fun rootReadmeDocumentsEveryCode() {
		val readme = rootReadme().toFile().readText()
		val missing = ConstraintErrorCode.entries
			.map { it.name }
			.filterNot { name -> readme.contains("`$name`") }
		assertThat(missing)
			.withFailMessage {
				"Root README Built-in codes must list every ConstraintErrorCode:\n" +
					missing.joinToString("\n")
			}
			.isEmpty()
	}

	private fun rootReadme(): Path {
		val moduleReadme = Path.of("README.md")
		val fromModule = Path.of("..", "README.md")
		return when {
			moduleReadme.toFile().isFile &&
				moduleReadme.toFile().readText().contains("# Validata") -> moduleReadme
			fromModule.toFile().isFile -> fromModule
			else -> error("Could not locate root README.md (cwd=${Path.of("").toAbsolutePath()})")
		}
	}

	companion object {
		/**
		 * Authoritative freeze list — keep in sync with [ConstraintErrorCode] and root README.
		 */
		private val FROZEN = listOf(
			"VALUE_MISSING" to "Value must not be null.",
			"VALUE_EMPTY" to "Value must not be empty.",
			"VALUE_TYPE_MISMATCH" to "Value has an unexpected data type.",
			"VALUE_PARSING_FAILED" to "Value cannot be parsed.",
			"VALUE_FORMAT_INVALID" to "Value has an invalid format.",
			"VALUE_INVALID" to "Value is invalid.",
			"VALUE_UNSUPPORTED" to "Value is not supported.",
			"VALUE_NOT_ALLOWED" to "Value is not allowed.",
			"PROPERTY_UNKNOWN" to "Property is not recognized.",
			"NUMBER_TOO_SMALL" to "Number is smaller than the permitted minimum.",
			"NUMBER_TOO_LARGE" to "Number is larger than the permitted maximum.",
			"NUMBER_NOT_POSITIVE" to "Number must be greater than zero.",
			"NUMBER_NOT_NEGATIVE" to "Number must be less than zero.",
			"NUMBER_ZERO_NOT_ALLOWED" to "Number must not be zero.",
			"NUMBER_NOT_INTEGER" to "Number must be an integer.",
			"NUMBER_NOT_FINITE" to "Number must be finite.",
			"NUMBER_NOT_MULTIPLE" to "Number must be an exact multiple of the required factor.",
			"NUMBER_NOT_EVEN" to "Number must be even.",
			"NUMBER_NOT_ODD" to "Number must be odd.",
			"NUMBER_OUT_OF_RANGE" to "Number is outside the valid range.",
			"NUMBER_PRECISION_EXCEEDED" to "Number exceeds the permitted precision.",
			"NUMBER_SCALE_EXCEEDED" to "Number exceeds the permitted scale.",
			"NUMBER_OVERFLOW" to "Number exceeds the representable range.",
			"NUMBER_UNDERFLOW" to "Number falls below the representable range.",
			"TEXT_BLANK" to "Text must not be blank.",
			"TEXT_TOO_SHORT" to "Text is shorter than the permitted minimum length.",
			"TEXT_TOO_LONG" to "Text exceeds the permitted maximum length.",
			"TEXT_PATTERN_MISMATCH" to "Text does not match the required pattern.",
			"TEMPORAL_NOT_IN_PAST" to "Temporal value must be in the past.",
			"TEMPORAL_NOT_IN_FUTURE" to "Temporal value must be in the future.",
			"TEMPORAL_TOO_EARLY" to "Temporal value is earlier than permitted.",
			"TEMPORAL_TOO_LATE" to "Temporal value is later than permitted.",
			"TEMPORAL_OUT_OF_RANGE" to "Temporal value is outside the valid range.",
			"TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED" to "Day of the month is not allowed.",
			"TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED" to "Day of the week is not allowed.",
			"TEMPORAL_MONTH_NOT_ALLOWED" to "Month is not allowed.",
			"TEMPORAL_QUARTER_NOT_ALLOWED" to "Calendar quarter is not allowed.",
			"TEMPORAL_YEAR_NOT_ALLOWED" to "Year is not allowed.",
			"TEMPORAL_HOUR_NOT_ALLOWED" to "Hour is not allowed.",
			"TEMPORAL_MINUTE_NOT_ALLOWED" to "Minute is not allowed.",
			"TEMPORAL_SECOND_NOT_ALLOWED" to "Second is not allowed.",
			"TEMPORAL_TIMEZONE_MISSING" to "Timezone information is missing.",
			"TEMPORAL_TIMEZONE_NOT_ALLOWED" to "Timezone is not allowed.",
			"TEMPORAL_OFFSET_NOT_ALLOWED" to "UTC offset is not allowed.",
			"TEMPORAL_DURATION_TOO_SHORT" to "Temporal duration is shorter than permitted.",
			"TEMPORAL_DURATION_TOO_LONG" to "Temporal duration exceeds the permitted maximum.",
			"COLLECTION_TOO_SMALL" to "Collection contains fewer items than permitted.",
			"COLLECTION_TOO_LARGE" to "Collection contains more items than permitted.",
			"COLLECTION_DUPLICATE" to "Item is duplicated in the collection.",
			"COLLECTION_ITEM_MISSING" to "Required collection item is missing.",
			"COLLECTION_SUBSET_MISMATCH" to "Collection is not a subset of the permitted values.",
			"COLLECTION_OVERLAP" to "Collections contain overlapping items where they must be disjoint.",
			"OBJECT_TOO_SMALL" to "Object contains fewer properties than permitted.",
			"OBJECT_TOO_LARGE" to "Object contains more properties than permitted.",
			"STRUCTURE_DEPTH_EXCEEDED" to "Structure exceeds the permitted nesting depth.",
			"RELATIONSHIP_REFERENCE_MISMATCH" to "Value does not match its referenced value.",
			"RELATIONSHIP_REFERENCE_MISSING" to "Required dependent value is missing.",
			"RELATIONSHIP_REFERENCE_INVALID" to "Dependent value is invalid.",
			"COMPARISON_UNSATISFIED_EQUAL" to "Value must not be equal to the reference value.",
			"COMPARISON_UNSATISFIED_NOT_EQUAL" to "Value must be equal to the reference value.",
			"COMPARISON_UNSATISFIED_LESS_THAN" to "Value must be greater than or equal to the reference value.",
			"COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL" to "Value must be greater than the reference value.",
			"COMPARISON_UNSATISFIED_GREATER_THAN" to "Value must be less than or equal to the reference value.",
			"COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL" to "Value must be less than the reference value.",
			"COMPARISON_NOT_ORDERABLE" to "Value cannot be compared with the reference value.",
			"VALUE_CHECKSUM_INVALID" to "Value failed checksum verification.",
			"VALUE_SIGNATURE_INVALID" to "Value failed signature verification.",
			"VALUE_SANITIZATION_MISMATCH" to "Value does not match its sanitized form.",
			"CONSTRAINT_UNSATISFIABLE" to "Validation constraints cannot be satisfied.",
		)
	}
}

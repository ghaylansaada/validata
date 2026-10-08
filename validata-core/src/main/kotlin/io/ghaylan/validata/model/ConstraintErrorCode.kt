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
package io.ghaylan.validata.model

/**
 * Built-in validation error codes and default user-facing messages.
 *
 * Codes are intentionally **abstract and reusable** — domain-specific detail belongs in
 * [ConstraintError.metadata] (the failing constraint metadata) and a precise
 * [ConstraintError.message], not in the enum constant name. Prefer adding a new code when no
 * existing constant fits the failure; do not stretch an incompatible code onto a validator.
 * Custom enums implement [ConstraintErrorDefinition] the same way ([code] typically equals the
 * constant name).
 *
 * Never put secrets in these strings — validators must redact before building [ConstraintError]s.
 *
 * @property message Default user-facing text for this code.
 *
 * @author Ghaylan Saada
 */
enum class ConstraintErrorCode(override val message: String) : ConstraintErrorDefinition {

	// -------------------------------------------------------------------------
	// Presence / nullability
	// -------------------------------------------------------------------------

	/**
	 * Value is absent or explicitly null where null is not permitted
	 * (including omitted required query/path/header params and missing JSON creator properties).
	 */
	VALUE_MISSING("Value must not be null."),

	/**
	 * Value contains no elements or meaningful content.
	 */
	VALUE_EMPTY("Value must not be empty."),

	// -------------------------------------------------------------------------
	// Mapping / input resolution
	// -------------------------------------------------------------------------

	/**
	 * Supplied value does not match the expected data type.
	 */
	VALUE_TYPE_MISMATCH("Value has an unexpected data type."),

	/**
	 * Value cannot be parsed according to the expected representation.
	 */
	VALUE_PARSING_FAILED("Value cannot be parsed."),

	/**
	 * Value does not conform to the expected formal format.
	 */
	VALUE_FORMAT_INVALID("Value has an invalid format."),

	/**
	 * Value is intrinsically invalid for its expected domain.
	 */
	VALUE_INVALID("Value is invalid."),

	/**
	 * Value or representation is valid but not supported by the active validator or policy.
	 */
	VALUE_UNSUPPORTED("Value is not supported."),

	/**
	 * Value is valid in itself but forbidden by the active constraint or policy.
	 */
	VALUE_NOT_ALLOWED("Value is not allowed."),

	/**
	 * Input contains a property that is not declared by the target structure.
	 */
	PROPERTY_UNKNOWN("Property is not recognized."),

	// -------------------------------------------------------------------------
	// Numbers
	// -------------------------------------------------------------------------

	/**
	 * Number is below the configured minimum.
	 */
	NUMBER_TOO_SMALL("Number is smaller than the permitted minimum."),

	/**
	 * Number exceeds the configured maximum.
	 */
	NUMBER_TOO_LARGE("Number is larger than the permitted maximum."),

	/**
	 * Number is zero or negative where a strictly positive value is required.
	 */
	NUMBER_NOT_POSITIVE("Number must be greater than zero."),

	/**
	 * Number is zero or positive where a strictly negative value is required.
	 */
	NUMBER_NOT_NEGATIVE("Number must not be negative."),

	/**
	 * Number is zero where a non-zero value is required.
	 */
	NUMBER_ZERO_NOT_ALLOWED("Number must not be zero."),

	/**
	 * Number contains a fractional component where an integer is required.
	 */
	NUMBER_NOT_INTEGER("Number must be an integer."),

	/**
	 * Number is NaN or infinite where a finite value is required.
	 */
	NUMBER_NOT_FINITE("Number must be finite."),

	/**
	 * Number is not evenly divisible by the required factor.
	 */
	NUMBER_NOT_MULTIPLE("Number must be an exact multiple of the required factor."),

	/**
	 * Number is not even where an even value is required.
	 */
	NUMBER_NOT_EVEN("Number must be even."),

	/**
	 * Number is not odd where an odd value is required.
	 */
	NUMBER_NOT_ODD("Number must be odd."),

	/**
	 * Number lies outside its intrinsic valid domain rather than a configured constraint bound.
	 */
	NUMBER_OUT_OF_RANGE("Number is outside the valid range."),

	/**
	 * Number exceeds the permitted decimal precision.
	 */
	NUMBER_PRECISION_EXCEEDED("Number exceeds the permitted precision."),

	/**
	 * Number exceeds the permitted decimal scale.
	 */
	NUMBER_SCALE_EXCEEDED("Number exceeds the permitted scale."),

	/**
	 * Numeric conversion or representation exceeds the maximum representable value.
	 */
	NUMBER_OVERFLOW("Number exceeds the representable range."),

	/**
	 * Numeric conversion or representation falls below the minimum representable value.
	 */
	NUMBER_UNDERFLOW("Number falls below the representable range."),
	
	// -------------------------------------------------------------------------
	// Text / strings
	// -------------------------------------------------------------------------
	
	/**
	 * Text contains only whitespace.
	 */
	TEXT_BLANK("Text must not be blank."),

	/**
	 * Text is shorter than the configured minimum length.
	 */
	TEXT_TOO_SHORT("Text is shorter than the permitted minimum length."),

	/**
	 * Text exceeds the configured maximum length.
	 */
	TEXT_TOO_LONG("Text exceeds the permitted maximum length."),

	/**
	 * Text does not match the configured pattern.
	 */
	TEXT_PATTERN_MISMATCH("Text does not match the required pattern."),

	// -------------------------------------------------------------------------
	// Temporals
	// -------------------------------------------------------------------------

	/**
	 * Temporal value is not strictly before the current reference time.
	 */
	TEMPORAL_NOT_IN_PAST("Temporal value must be in the past."),

	/**
	 * Temporal value is not strictly after the current reference time.
	 */
	TEMPORAL_NOT_IN_FUTURE("Temporal value must be in the future."),

	/**
	 * Temporal value is earlier than the permitted lower temporal bound.
	 */
	TEMPORAL_TOO_EARLY("Temporal value is earlier than permitted."),

	/**
	 * Temporal value is later than the permitted upper temporal bound.
	 */
	TEMPORAL_TOO_LATE("Temporal value is later than permitted."),

	/**
	 * Temporal value lies outside the permitted interval (including negated range checks).
	 */
	TEMPORAL_OUT_OF_RANGE("Temporal value is outside the valid range."),

	/**
	 * Day of the month is not permitted.
	 */
	TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED("Day of the month is not allowed."),

	/**
	 * Day of the week is not permitted.
	 */
	TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED("Day of the week is not allowed."),

	/**
	 * Month is not permitted.
	 */
	TEMPORAL_MONTH_NOT_ALLOWED("Month is not allowed."),

	/**
	 * Quarter is not permitted.
	 */
	TEMPORAL_QUARTER_NOT_ALLOWED("Calendar quarter is not allowed."),

	/**
	 * Year is not permitted.
	 */
	TEMPORAL_YEAR_NOT_ALLOWED("Year is not allowed."),

	/**
	 * Hour component is not permitted.
	 */
	TEMPORAL_HOUR_NOT_ALLOWED("Hour is not allowed."),

	/**
	 * Minute component is not permitted.
	 */
	TEMPORAL_MINUTE_NOT_ALLOWED("Minute is not allowed."),

	/**
	 * Second component is not permitted.
	 */
	TEMPORAL_SECOND_NOT_ALLOWED("Second is not allowed."),

	/**
	 * Required timezone information is absent.
	 */
	TEMPORAL_TIMEZONE_MISSING("Timezone information is missing."),

	/**
	 * Supplied timezone is not permitted.
	 */
	TEMPORAL_TIMEZONE_NOT_ALLOWED("Timezone is not allowed."),

	/**
	 * Supplied UTC offset is not permitted.
	 */
	TEMPORAL_OFFSET_NOT_ALLOWED("UTC offset is not allowed."),

	/**
	 * Duration is shorter than the configured minimum.
	 */
	TEMPORAL_DURATION_TOO_SHORT("Temporal duration is shorter than permitted."),

	/**
	 * Duration exceeds the configured maximum.
	 */
	TEMPORAL_DURATION_TOO_LONG("Temporal duration exceeds the permitted maximum."),
	
	// -------------------------------------------------------------------------
	// Collections
	// -------------------------------------------------------------------------

	/**
	 * Collection contains fewer items than permitted.
	 */
	COLLECTION_TOO_SMALL("Collection contains fewer items than permitted."),

	/**
	 * Collection contains more items than permitted.
	 */
	COLLECTION_TOO_LARGE("Collection contains more items than permitted."),

	/**
	 * An item is duplicated in its collection where uniqueness is required.
	 *
	 * Reported on the duplicate element's indexed path (e.g. `tags[1]`); used by `@Distinct`.
	 */
	COLLECTION_DUPLICATE("Item is duplicated in the collection."),

	/**
	 * Required item is absent from the collection.
	 */
	COLLECTION_ITEM_MISSING("Required collection item is missing."),

	/**
	 * Collection contains a value that must belong to a required subset.
	 */
	COLLECTION_SUBSET_MISMATCH("Collection is not a subset of the permitted values."),

	/**
	 * Collections or sets contain overlapping members where disjointness is required.
	 */
	COLLECTION_OVERLAP("Collections contain overlapping items where they must be disjoint."),
	
	// -------------------------------------------------------------------------
	// Objects / structure
	// -------------------------------------------------------------------------

	/**
	 * Object contains fewer populated properties than permitted.
	 */
	OBJECT_TOO_SMALL("Object contains fewer properties than permitted."),

	/**
	 * Object contains more populated properties than permitted.
	 */
	OBJECT_TOO_LARGE("Object contains more properties than permitted."),

	/**
	 * Nested object structure exceeds the configured maximum depth.
	 */
	STRUCTURE_DEPTH_EXCEEDED("Structure exceeds the permitted nesting depth."),

	// -------------------------------------------------------------------------
	// Relationships / dependencies
	// -------------------------------------------------------------------------

	/**
	 * Value does not match the value it is required to reference.
	 */
	RELATIONSHIP_REFERENCE_MISMATCH("Value does not match its referenced value."),

	/**
	 * Required dependent value is absent.
	 */
	RELATIONSHIP_REFERENCE_MISSING("Required dependent value is missing."),

	/**
	 * Dependent value exists but does not satisfy the required dependency.
	 */
	RELATIONSHIP_REFERENCE_INVALID("Dependent value is invalid."),
	
	// -------------------------------------------------------------------------
	// Comparison
	// -------------------------------------------------------------------------
	
	/**
	 * Compared values are equal when they must be different.
	 */
	COMPARISON_UNSATISFIED_EQUAL("Value must not be equal to the reference value."),
	
	/**
	 * Compared values are different when they must be equal.
	 */
	COMPARISON_UNSATISFIED_NOT_EQUAL("Value must be equal to the reference value."),
	
	/**
	 * Left value is less than the right value when it must be greater than or equal.
	 */
	COMPARISON_UNSATISFIED_LESS_THAN("Value must be greater than or equal to the reference value."),
	
	/**
	 * Left value is less than or equal to the right value when it must be greater.
	 */
	COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL("Value must be greater than the reference value."),
	
	/**
	 * Left value is greater than the right value when it must be less than or equal.
	 */
	COMPARISON_UNSATISFIED_GREATER_THAN("Value must be less than or equal to the reference value."),
	
	/**
	 * Left value is greater than or equal to the right value when it must be less.
	 */
	COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL("Value must be less than the reference value."),

	/**
	 * Compared values cannot be ordered using the required comparison semantics.
	 */
	COMPARISON_NOT_ORDERABLE("Value cannot be compared with the reference value."),
	
	// -------------------------------------------------------------------------
	// Integrity / transformation
	// -------------------------------------------------------------------------

	/**
	 * Value fails its integrity checksum verification.
	 */
	VALUE_CHECKSUM_INVALID("Value failed checksum verification."),

	/**
	 * Value fails its cryptographic signature verification.
	 */
	VALUE_SIGNATURE_INVALID("Value failed signature verification."),

	/**
	 * Supplied value differs from the value produced by the required sanitization process.
	 */
	VALUE_SANITIZATION_MISMATCH("Value does not match its sanitized form."),

	// -------------------------------------------------------------------------
	// General structural relationships
	// -------------------------------------------------------------------------

	/**
	 * No value can satisfy the configured validation conditions.
	 */
	CONSTRAINT_UNSATISFIABLE("Validation constraints cannot be satisfied.");


	/**
	 * Machine-readable code; equals the enum constant [name].
	 */
	override val code: String get() = name
}

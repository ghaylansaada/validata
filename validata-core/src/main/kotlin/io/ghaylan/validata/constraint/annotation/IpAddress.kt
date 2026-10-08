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
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.string.ipaddress.IpAddressValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import kotlin.reflect.KClass

/**
 * Asserts that a [CharSequence] is a valid IPv4 or IPv6 address literal.
 *
 * Validation is DNS-free: dotted-decimal IPv4 is range-checked; IPv6 accepts standard compressed
 * forms, embedded IPv4 tails, and optional zone identifiers (`%interface`). `null` values are
 * skipped; combine with [Required] when the field must also be present.
 *
 * ### Example
 *
 * ```kotlin
 * @field:IpAddress(type = IpAddress.Type.V4)
 * val gateway: String
 *
 * @field:IpAddress
 * val peer: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 *
 * @property type Address family to accept. Defaults to [Type.ANY].
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 * 
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [IpAddressValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class IpAddress(
	val type: Type = Type.ANY,
	
	@ConstraintMessage
	val message: String = "",
	
	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
) {
	
	/**
	 * Supported IP address families.
	 */
	enum class Type {
		
		/**
		 * Dotted-decimal IPv4 only.
		 */
		V4,
		
		/**
		 * Colon-hex IPv6 only (including embedded IPv4 and zone IDs).
		 */
		V6,
		
		/**
		 * Either IPv4 or IPv6.
		 */
		ANY,
	}
}

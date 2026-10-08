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
package io.ghaylan.validata.openapi.docs.builtin

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.BarcodeConstraint
import io.ghaylan.validata.constraint.annotation.Base64Constraint
import io.ghaylan.validata.constraint.annotation.CreditCardConstraint
import io.ghaylan.validata.constraint.annotation.FilePathConstraint
import io.ghaylan.validata.constraint.annotation.FinancialCodeConstraint
import io.ghaylan.validata.constraint.annotation.HexColorConstraint
import io.ghaylan.validata.constraint.annotation.IpAddress
import io.ghaylan.validata.constraint.annotation.IpAddressConstraint
import io.ghaylan.validata.constraint.annotation.IsoCountryConstraint
import io.ghaylan.validata.constraint.annotation.IsoCurrencyConstraint
import io.ghaylan.validata.constraint.annotation.IsoLanguageConstraint
import io.ghaylan.validata.constraint.annotation.PasswordConstraint
import io.ghaylan.validata.constraint.annotation.PhoneConstraint
import io.ghaylan.validata.constraint.validator.string.hexcolor.HexColorValidator
import io.ghaylan.validata.openapi.docs.ConstraintDocHints
import io.ghaylan.validata.openapi.docs.ConstraintDocumentation
import io.ghaylan.validata.openapi.docs.JsonSchemaFacet
import io.ghaylan.validata.schema.shape.TypeShape

/**
 * Native OpenAPI facets for string / identity custom constraints.
 *
 * Typed formats (`ean`, `iban`, `password`, …), HexColor `pattern`, Password length bounds,
 * and typed IpAddress `format`. Args still appear under `x-validata-constraints`.*
 * 
 * @author Ghaylan Saada
 */
class StringCustomConstraintDocumentation: ConstraintDocumentation {
	
	override fun supports(metadata: ConstraintMetadata): Boolean = metadata.javaClass in HANDLED
	
	override fun hints(
		metadata: ConstraintMetadata,
		shape: TypeShape?
	): ConstraintDocHints = when (metadata) {
		is HexColorConstraint -> {
			val facet = JsonSchemaFacet.Pattern(HEX_COLOR_PATTERN)
			ConstraintDocHints(facets = setOf(facet))
		}
		is IpAddressConstraint -> when (metadata.type) {
			IpAddress.Type.V4 -> ConstraintDocHints(facets = setOf(JsonSchemaFacet.Format("ipv4")))
			IpAddress.Type.V6 -> ConstraintDocHints(facets = setOf(JsonSchemaFacet.Format("ipv6")))
			IpAddress.Type.ANY -> ConstraintDocHints.EMPTY
		}
		is BarcodeConstraint -> {
			val format = metadata.type.name.lowercase()
			val facet = JsonSchemaFacet.Format(format)
			ConstraintDocHints(facets = setOf(facet))
		}
		is FinancialCodeConstraint -> {
			val format = metadata.type.name.lowercase()
			val facet = JsonSchemaFacet.Format(format)
			ConstraintDocHints(facets = setOf(facet))
		}
		is CreditCardConstraint -> {
			val facet = JsonSchemaFacet.Format("credit-card")
			ConstraintDocHints(facets = setOf(facet))
		}
		is Base64Constraint -> {
			val facet = JsonSchemaFacet.Format("byte")
			ConstraintDocHints(facets = setOf(facet))
		}
		is PasswordConstraint -> {
			val facets = buildSet {
				add(JsonSchemaFacet.Format("password"))
				add(JsonSchemaFacet.MinLength(metadata.minLength))
				if (metadata.maxLength != Int.MAX_VALUE) {
					add(JsonSchemaFacet.MaxLength(metadata.maxLength))
				}
			}
			ConstraintDocHints(facets = facets)
		}
		is PhoneConstraint -> {
			val facet = JsonSchemaFacet.Format("phone")
			ConstraintDocHints(facets = setOf(facet))
		}
		is IsoCountryConstraint -> {
			val facet = JsonSchemaFacet.Format("iso3166-1-alpha-2")
			ConstraintDocHints(facets = setOf(facet))
		}
		is IsoCurrencyConstraint -> {
			val facet = JsonSchemaFacet.Format("iso4217")
			ConstraintDocHints(facets = setOf(facet))
		}
		is IsoLanguageConstraint -> {
			val facet = JsonSchemaFacet.Format("iso639")
			ConstraintDocHints(facets = setOf(facet))
		}
		is FilePathConstraint -> ConstraintDocHints.EMPTY
		else -> ConstraintDocHints.EMPTY
	}
	
	companion object {
		
		/**
		 * Must match [HexColorValidator].
		 */
		private const val HEX_COLOR_PATTERN: String = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"
		
		private val HANDLED: Set<Class<*>> = setOf(
			HexColorConstraint::class.java,
			IpAddressConstraint::class.java,
			BarcodeConstraint::class.java,
			FinancialCodeConstraint::class.java,
			CreditCardConstraint::class.java,
			Base64Constraint::class.java,
			PasswordConstraint::class.java,
			PhoneConstraint::class.java,
			IsoCountryConstraint::class.java,
			IsoCurrencyConstraint::class.java,
			IsoLanguageConstraint::class.java,
			FilePathConstraint::class.java)
	}
}

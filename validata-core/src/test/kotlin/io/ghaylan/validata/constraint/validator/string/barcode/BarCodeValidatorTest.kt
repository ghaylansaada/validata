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
package io.ghaylan.validata.constraint.validator.string.barcode

import io.ghaylan.validata.constraint.annotation.Barcode
import io.ghaylan.validata.constraint.annotation.BarcodeConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [BarcodeValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("BarcodeValidator")
class BarcodeValidatorTest {
	
	private fun c(type: Barcode.Type) = BarcodeConstraint(
		type = type,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(BarcodeValidator, c(Barcode.Type.EAN))
		}
	}
	
	@Nested
	@DisplayName("EAN")
	inner class Ean {
		
		private val constraint = c(Barcode.Type.EAN)
		
		@Test
		@DisplayName("valid EAN-8 passes")
		fun validEan8() {
			assertValid(BarcodeValidator, "96385074", constraint)
		}
		
		@Test
		@DisplayName("valid EAN-13 with separators passes")
		fun validEan13() {
			assertValid(BarcodeValidator, "004-2100005264", constraint)
		}
		
		@Test
		@DisplayName("wrong length fails with VALUE_FORMAT_INVALID")
		fun badFormat() {
			assertInvalid(
				BarcodeValidator,
				"12345",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("failed GS1 checksum fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				BarcodeValidator,
				"96385075",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("UPC")
	inner class Upc {
		
		private val constraint = c(Barcode.Type.UPC)
		
		@Test
		@DisplayName("valid UPC-A passes")
		fun validUpc() {
			assertValid(BarcodeValidator, "042100005264", constraint)
		}
		
		@Test
		@DisplayName("wrong length fails with VALUE_FORMAT_INVALID")
		fun badFormat() {
			assertInvalid(
				BarcodeValidator,
				"04210000526",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("failed GS1 checksum fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				BarcodeValidator,
				"042100005265",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("GTIN")
	inner class Gtin {
		
		private val constraint = c(Barcode.Type.GTIN)
		
		@Test
		@DisplayName("valid GTIN-14 passes")
		fun validGtin14() {
			assertValid(BarcodeValidator, "10421000052641", constraint)
		}
		
		@Test
		@DisplayName("wrong length fails with VALUE_FORMAT_INVALID")
		fun badFormat() {
			assertInvalid(
				BarcodeValidator,
				"10421000052",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("failed GS1 checksum fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				BarcodeValidator,
				"10421000052642",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("ISBN")
	inner class Isbn {
		
		private val constraint = c(Barcode.Type.ISBN)
		
		@Test
		@DisplayName("valid ISBN-10 passes")
		fun validIsbn10() {
			assertValid(BarcodeValidator, "0-306-40615-2", constraint)
		}
		
		@Test
		@DisplayName("valid ISBN-13 passes")
		fun validIsbn13() {
			assertValid(BarcodeValidator, "9780306406157", constraint)
		}
		
		@Test
		@DisplayName("wrong layout fails with VALUE_FORMAT_INVALID")
		fun badFormat() {
			assertInvalid(
				BarcodeValidator,
				"1234567890123",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("failed checksum fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				BarcodeValidator,
				"0306406153",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("ISSN")
	inner class Issn {
		
		private val constraint = c(Barcode.Type.ISSN)
		
		@Test
		@DisplayName("valid ISSN with separators passes")
		fun validIssn() {
			assertValid(BarcodeValidator, "2049-3630", constraint)
		}
		
		@Test
		@DisplayName("valid ISSN with X check digit passes")
		fun validIssnWithX() {
			assertValid(BarcodeValidator, "0000-006X", constraint)
		}
		
		@Test
		@DisplayName("wrong length fails with VALUE_FORMAT_INVALID")
		fun badFormat() {
			assertInvalid(
				BarcodeValidator,
				"2049363",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("failed mod-11 checksum fails with VALUE_CHECKSUM_INVALID")
		fun badChecksum() {
			assertInvalid(
				BarcodeValidator,
				"2049-3631",
				constraint,
				ConstraintErrorCode.VALUE_CHECKSUM_INVALID,
			)
		}
	}
}

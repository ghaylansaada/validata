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
package io.ghaylan.validata.constraint.validator.string.ipaddress

import io.ghaylan.validata.constraint.annotation.IpAddress
import io.ghaylan.validata.constraint.annotation.IpAddressConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [IpAddressValidator].
 * 
 * @author Ghaylan Saada
 */
@DisplayName("IpAddressValidator")
class IpAddressValidatorTest {
	
	private fun c(type: IpAddress.Type = IpAddress.Type.ANY) = IpAddressConstraint(
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
			assertSkipsNull(IpAddressValidator, c())
		}
	}
	
	@Nested
	@DisplayName("IPv4")
	inner class Ipv4 {
		
		@Test
		@DisplayName("valid dotted quad passes")
		fun validV4() {
			assertValid(IpAddressValidator, "192.168.0.1", c(IpAddress.Type.V4))
		}
		
		@Test
		@DisplayName("octet above 255 fails with VALUE_FORMAT_INVALID")
		fun octetOutOfRange() {
			assertInvalid(IpAddressValidator, "256.0.0.1", c(IpAddress.Type.V4), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("leading zero octet fails with VALUE_FORMAT_INVALID")
		fun leadingZero() {
			assertInvalid(IpAddressValidator, "192.168.001.1", c(IpAddress.Type.V4), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("IPv6 address fails V4-only constraint")
		fun v6RejectedForV4() {
			assertInvalid(
				IpAddressValidator,
				"2001:db8::1",
				c(IpAddress.Type.V4),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("IPv6")
	inner class Ipv6 {
		
		@Test
		@DisplayName("compressed IPv6 passes")
		fun compressed() {
			assertValid(IpAddressValidator, "2001:db8::1", c(IpAddress.Type.V6))
		}
		
		@Test
		@DisplayName("full IPv6 passes")
		fun full() {
			assertValid(IpAddressValidator, "2001:0db8:0000:0000:0000:0000:0000:0001", c(IpAddress.Type.V6))
		}
		
		@Test
		@DisplayName("embedded IPv4 tail passes")
		fun embeddedV4() {
			assertValid(IpAddressValidator, "::ffff:192.168.0.1", c(IpAddress.Type.V6))
		}
		
		@Test
		@DisplayName("zone identifier passes")
		fun zoneId() {
			assertValid(IpAddressValidator, "fe80::1%eth0", c(IpAddress.Type.V6))
		}
		
		@Test
		@DisplayName("IPv4 address fails V6-only constraint")
		fun v4RejectedForV6() {
			assertInvalid(
				IpAddressValidator,
				"192.168.0.1",
				c(IpAddress.Type.V6),
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("ANY type")
	inner class AnyType {
		
		@Test
		@DisplayName("IPv4 passes")
		fun v4() {
			assertValid(IpAddressValidator, "10.0.0.1", c())
		}
		
		@Test
		@DisplayName("IPv6 passes")
		fun v6() {
			assertValid(IpAddressValidator, "::1", c())
		}
		
		@Test
		@DisplayName("hostname fails with VALUE_FORMAT_INVALID")
		fun hostname() {
			assertInvalid(IpAddressValidator, "example.com", c(), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
		
		@Test
		@DisplayName("whitespace-only string fails with VALUE_FORMAT_INVALID")
		fun whitespaceOnly() {
			assertInvalid(IpAddressValidator, "   ", c(), ConstraintErrorCode.VALUE_FORMAT_INVALID)
		}
	}
}

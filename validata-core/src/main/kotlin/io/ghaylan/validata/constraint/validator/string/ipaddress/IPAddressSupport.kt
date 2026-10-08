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

/**
 * DNS-free IPv4 / IPv6 literal checks for [IpAddressValidator].
 *
 * No DNS or network lookup — only structural parse of the literal.
 *
 * @author Ghaylan Saada
 */
internal object IpAddressSupport {
	
	/**
	 * Whether [value] satisfies [type].
	 *
	 * Side effects: none — no DNS or network lookup.
	 *
	 * @param value Candidate address literal.
	 * @param type Accepted address family.
	 * @return `true` when the literal parses as the requested family.	 
	 */
	fun isValid(
		value: CharSequence,
		type: IpAddress.Type,
	): Boolean = when (type) {
		IpAddress.Type.V4 -> isValidV4(value)
		IpAddress.Type.V6 -> isValidV6(value.toString())
		IpAddress.Type.ANY -> isValidV4(value) || isValidV6(value.toString())
	}
	
	/**
	 * Dotted-decimal IPv4 with each octet in `0..255` and no leading-zero padding (except `"0"`).
	 *
	 * Side effects: none. Scans [value] in place — no [String.split].
	 *
	 * @param value Candidate literal; anything containing `:` is rejected.
	 * @return `true` when the literal is a valid IPv4 address.	 
	 */
	fun isValidV4(value: CharSequence): Boolean {
		val n = value.length
		if (n == 0) return false
		var i = 0
		repeat(4) { part ->
			if (i >= n) return false
			val start = i
			var octet = 0
			var digits = 0
			while (i < n) {
				val c = value[i]
				if (c == '.') break
				if (c == ':') return false
				if (c !in '0'..'9') return false
				digits++
				if (digits > 3) return false
				octet = octet * 10 + (c - '0')
				if (octet > 255) return false
				i++
			}
			if (digits == 0) return false
			if (digits > 1 && value[start] == '0') return false
			if (part < 3) {
				if (i >= n || value[i] != '.') return false
				i++
			}
		}
		return i == n
	}
	
	/**
	 * Colon-hex IPv6, including compressed `::`, embedded IPv4 tails, and `%zone` suffixes.
	 *
	 * Side effects: none.
	 *
	 * @param value Candidate literal; anything without `:` is rejected.
	 * @return `true` when the literal is a valid IPv6 address.	 
	 */
	fun isValidV6(value: String): Boolean {
		if (value.isEmpty() || !value.contains(':')) return false
		if (value.count { it == ':' } > 7 && !value.contains("::")) return false
		val zoneIndex = value.indexOf('%')
		val addressPart = if (zoneIndex >= 0) {
			if (zoneIndex == value.length - 1) return false
			val zone = value.substring(zoneIndex + 1)
			if (zone.isEmpty() || !zone.all(::isZoneCharacter)) return false
			value.substring(0, zoneIndex)
		}
		else {
			value
		}
		val normalized = normalizeEmbeddedIpv4(addressPart)
			?: return false
		val groups = expandIpv6Groups(normalized)
			?: return false
		return groups.size == 8 && groups.all(::isValidHexGroup)
	}
	
	/**
	 * Rewrites a trailing IPv4-mapped tail (`::ffff:192.0.2.1`) into two hex groups.
	 *
	 * Side effects: none.
	 *
	 * @param addressPart IPv6 literal with the zone identifier already removed.
	 * @return Literal with the IPv4 tail replaced, [addressPart] unchanged when it holds no `.`,
	 *   or `null` when the tail is not a valid IPv4 address.	 
	 */
	private fun normalizeEmbeddedIpv4(addressPart: String): String? {
		if (!addressPart.contains('.')) return addressPart
		val lastColon = addressPart.lastIndexOf(':')
		if (lastColon < 0) return null
		val ipv4Part = addressPart.substring(lastColon + 1)
		if (!isValidV4(ipv4Part)) return null
		val octets = ipv4Part.split('.')
			.map { it.toInt() }
		val high = ((octets[0] shl 8) or octets[1]).toString(16)
		val low = ((octets[2] shl 8) or octets[3]).toString(16)
		return addressPart.substring(0, lastColon + 1) + high + ":" + low
	}
	
	/**
	 * Expands at most one `::` run into explicit zero groups.
	 *
	 * Side effects: none.
	 *
	 * @param addressPart IPv6 literal with any IPv4 tail already normalized.
	 * @return Exactly the groups the literal declares, or `null` when `::` appears more than once,
	 *   a group is empty, or the literal declares more than eight groups.	 
	 */
	private fun expandIpv6Groups(addressPart: String): List<String>? {
		if (addressPart == "::") return List(8) { "0" }
		val doubleColonStart = addressPart.indexOf("::")
		val doubleColonEnd = addressPart.lastIndexOf("::")
		if (doubleColonStart != doubleColonEnd) return null
		val parts = if (doubleColonStart >= 0) {
			val left = if (doubleColonStart == 0) emptyList()
			else addressPart.substring(0, doubleColonStart)
				.split(':')
			val rightStart = doubleColonStart + 2
			val right = if (rightStart == addressPart.length) {
				emptyList()
			}
			else {
				addressPart.substring(rightStart)
					.split(':')
			}
			if (left.any { it.isEmpty() } || right.any { it.isEmpty() }) return null
			val missing = 8 - left.size - right.size
			if (missing < 0) return null
			left + List(missing) { "0" } + right
		}
		else {
			addressPart.split(':')
		}
		
		return parts
	}
	
	/**
	 * Whether [group] is one to four hexadecimal digits.
	 *
	 * Side effects: none.
	 *
	 * @param group Single IPv6 group.
	 * @return `true` when the group is a valid hextet.	 
	 */
	private fun isValidHexGroup(group: String): Boolean =
		group.isNotEmpty() && group.length <= 4 && group.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
	
	/**
	 * Whether [ch] may appear in an IPv6 zone identifier (the part after `%`).
	 *
	 * Side effects: none.
	 *
	 * @param ch Candidate character.
	 * @return `true` for letters, digits, and `.`, `_`, `-`, `+`.	 
	 */
	private fun isZoneCharacter(ch: Char): Boolean = ch.isLetterOrDigit() || ch in "._-+"
}

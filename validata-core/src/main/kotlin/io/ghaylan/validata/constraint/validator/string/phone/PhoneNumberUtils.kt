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
package io.ghaylan.validata.constraint.validator.string.phone

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import io.ghaylan.validata.constraint.validator.string.phone.PhoneNumberUtils.phoneNumberUtil
import io.ghaylan.validata.internal.OptionalDependencies

/**
 * Phone-number helpers backed by libphonenumber (T-51).
 *
 * Callers must ensure [OptionalDependencies.requireLibphonenumber] has succeeded before use; this
 * object is only loaded when `@Phone` validation actually runs.
 *
 * @author Ghaylan Saada
 */
internal object PhoneNumberUtils {
	
	/** Digits-and-optional-sign keep filter; compiled once for the `@Phone` hot path.	 */
	private val NON_DIGIT = Regex("[^-?0-9]+")
	
	/**
	 * Shared [PhoneNumberUtil] singleton.
	 *
	 * Lazy: first access calls [OptionalDependencies.requireLibphonenumber] then
	 * [PhoneNumberUtil.getInstance].	 
	 */
	private val phoneNumberUtil: PhoneNumberUtil by lazy {
		OptionalDependencies.requireLibphonenumber()
		PhoneNumberUtil.getInstance()
	}
	
	/**
	 * Whether [internationalPhoneNumber] parses and passes libphonenumber validity checks.
	 *
	 * May trigger lazy [phoneNumberUtil] initialization (classpath check + singleton load).
	 *
	 * @param internationalPhoneNumber Raw number string (digits preferred; formatting stripped).
	 * @return `true` when parse succeeds and [PhoneNumberUtil.isValidNumber] accepts it.	 
	 */
	fun isValidNumber(internationalPhoneNumber: CharSequence): Boolean {
		val phone = getPhoneNumber(internationalPhoneNumber)
			?: return false
		return phoneNumberUtil.isValidNumber(phone)
	}
	
	/**
	 * Resolves the libphonenumber [PhoneNumberType] for [internationalPhoneNumber].
	 *
	 * May trigger lazy [phoneNumberUtil] initialization.
	 *
	 * @param internationalPhoneNumber Raw number string.
	 * @return Detected type, or `null` when parsing fails.	 
	 */
	fun getNumberType(internationalPhoneNumber: CharSequence): PhoneNumberType? {
		val phone = getPhoneNumber(internationalPhoneNumber)
			?: return null
		return phoneNumberUtil.getNumberType(phone)
	}
	
	/**
	 * Resolves the ISO 3166-1 alpha-2 region for [internationalPhoneNumber].
	 *
	 * May trigger lazy [phoneNumberUtil] initialization.
	 *
	 * @param internationalPhoneNumber Raw number string.
	 * @return Region code, or `null` when parsing fails.	 
	 */
	fun getCountryISOCode(internationalPhoneNumber: CharSequence): String? {
		val phone = getPhoneNumber(internationalPhoneNumber)
			?: return null
		return phoneNumberUtil.getRegionCodeForCountryCode(phone.countryCode)
	}
	
	/**
	 * Parses [internationalPhoneNumber] into a [PhoneNumber].
	 *
	 * Strips formatting via [getCleanNumber], prefixes `+`, then calls [PhoneNumberUtil.parse].
	 * Side effects: may trigger lazy [phoneNumberUtil] initialization.
	 *
	 * @param internationalPhoneNumber Raw number string.
	 * @return Parsed number, or `null` on parse failure.	 
	 */
	private fun getPhoneNumber(internationalPhoneNumber: CharSequence): PhoneNumber? {
		return try {
			val cleanNumber = getCleanNumber(internationalPhoneNumber)
			phoneNumberUtil.parse("+$cleanNumber", "")
		}
		catch (_: Exception) {
			null
		}
	}
	
	/**
	 * Strips non-digit formatting and normalizes international dial prefixes for parsing.
	 *
	 * Side effects: none.
	 *
	 * @param internationalPhoneNumber Raw number string (may contain `+`, spaces, punctuation).
	 * @return Digits-only string with `+00` / `00` / `+` prefixes removed.	 
	 */
	fun getCleanNumber(internationalPhoneNumber: CharSequence): String {
		val number = internationalPhoneNumber.replace(NON_DIGIT, "")
		return when {
			number.startsWith("+00") -> number.replaceFirst("+00", "")
			number.startsWith("00") -> number.replaceFirst("00", "")
			number.startsWith("+") -> number.replaceFirst("+", "")
			else -> number
		}
	}
}

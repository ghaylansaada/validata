/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.discovery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Ensures every built-in `@Constraint` parameter the IDE can usefully check carries
 * `@ConstraintArg` / `@PropertyRef` in validata-core sources (marker-driven discovery).
 *
 * Parses annotation sources under `validata-core` because that module is not on the
 * IntelliJ plugin test classpath.
 *
 * @author Ghaylan Saada
 */
class BuiltinConstraintParameterCoverageTest {

	/** Same 38 annotations as core `BuiltInCatalogCompletenessTest`. */
	private val builtInAnnotations: List<String> = listOf(
		"Assert",
		"Barcode",
		"Base64",
		"Contains",
		"RelativeToNow",
		"Compare",
		"Coordinate",
		"CreditCard",
		"DaysOfMonth",
		"DaysOfWeek",
		"Digits",
		"Distinct",
		"Email",
		"FilePath",
		"HexColor",
		"Html",
		"FinancialCode",
		"Checksum",
		"IpAddress",
		"IsoCountry",
		"IsoCurrency",
		"IsoLanguage",
		"Max",
		"Min",
		"Months",
		"MultipleOf",
		"NumberSign",
		"NumberParity",
		"Password",
		"Phone",
		"Range",
		"Regex",
		"Required",
		"RequiredWhen",
		"Size",
		"Url",
		"In",
		"NotIn",
	)

	/**
	 * Params that are String / Array&lt;String&gt; / IntArray / Int / Long but intentionally
	 * unmarked (IDE completes Booleans/enums natively; -1 sentinel indexes).
	 */
	private val intentionallyUnmarked: Set<Pair<String, String>> = setOf(
		"Checksum" to "checkDigitIndex",
		"Checksum" to "endIndex",
	)

	private val markerRequiredTypes: Set<String> = setOf(
		"String",
		"Array<String>",
		"kotlin.Array<String>",
		"IntArray",
		"Int",
		"Long",
	)

	/** Nested / external enums and booleans — IDE completes these without markers. */
	private val intentionallyUnmarkedTypeNames: Set<String> = setOf(
		"Boolean",
		"Mode",
		"Operation",
		"Type",
		"Axis",
		"Sign",
		"Value",
		"Relation",
		"ChronoUnit",
		"Algorithm",
		"PhoneNumberType",
		"Month",
		"DayOfWeek",
		"Required.Mode",
		"RequiredWhen.Condition",
		"Array<PhoneNumberType>",
		"Array<Month>",
		"Array<DayOfWeek>",
	)

	private val paramDeclPattern = Regex(
		"""(?:@(?:ConstraintArg|PropertyRef|ConstraintMessage|ConstraintGroups)\b[^\n]*\n\s*)*val\s+(\w+)\s*:\s*([^=,\n]+)""",
		RegexOption.MULTILINE,
	)

	@Test
	@DisplayName("exactly 38 built-in @Constraint annotation sources exist")
	fun everyBuiltInAnnotationSourceExists() {
		val dir = annotationSourceDir()
		assertThat(builtInAnnotations).hasSize(38)
		for (name in builtInAnnotations) {
			assertThat(dir.resolve("$name.kt")).withFailMessage { "missing $name.kt under $dir" }
				.exists()
		}
	}

	@Test
	@DisplayName("marker-checkable params carry @ConstraintArg or @PropertyRef")
	fun markerCheckableParamsAreMarked() {
		val dir = annotationSourceDir()
		val missing = mutableListOf<String>()

		for (name in builtInAnnotations) {
			val source = Files.readString(dir.resolve("$name.kt"))
			val body = annotationPrimaryConstructorBody(source, name) ?: continue
			for (match in paramDeclPattern.findAll(body)) {
				val paramName = match.groupValues[1]
				val typeText = match.groupValues[2].trim()
				val preceding = match.value

				if (paramName == "message" || paramName == "groups") continue
				if (name to paramName in intentionallyUnmarked) continue

				val simpleType = typeText.removeSuffix("?").trim()
				if (isIntentionallyUnmarkedType(simpleType)) continue
				if (simpleType !in markerRequiredTypes) continue

				val hasMarker =
					preceding.contains("@ConstraintArg") || preceding.contains("@PropertyRef")
				if (!hasMarker) {
					missing += "$name.$paramName : $simpleType"
				}
			}
		}

		assertThat(missing).withFailMessage {
			"params needing @ConstraintArg/@PropertyRef:\n${missing.joinToString("\n")}"
		}
			.isEmpty()
	}

	private fun isIntentionallyUnmarkedType(typeText: String): Boolean {
		if (typeText in intentionallyUnmarkedTypeNames) return true
		if (typeText.startsWith("Array<") && typeText != "Array<String>" && typeText != "kotlin.Array<String>") {
			return true
		}
		// Qualified nested enums e.g. Compare.Operation, Url.Type, Checksum.Algorithm
		val leaf = typeText.substringAfterLast('.')
		return leaf in intentionallyUnmarkedTypeNames
	}

	/**
	 * Extracts the primary annotation constructor parameter list source.
	 */
	private fun annotationPrimaryConstructorBody(
		source: String,
		simpleName: String,
	): String? {
		val header = Regex("""annotation\s+class\s+$simpleName\s*\(""")
		val start = header.find(source)?.range?.last?.plus(1) ?: return null
		var depth = 1
		var i = start
		while (i < source.length && depth > 0) {
			when (source[i]) {
				'(' -> depth++
				')' -> depth--
			}
			i++
		}
		return source.substring(start, i - 1)
	}

	private fun annotationSourceDir(): Path {
		val candidates = listOf(
			Path.of("../validata-core/src/main/kotlin/io/ghaylan/validata/constraint/annotation"),
			Path.of("validata-core/src/main/kotlin/io/ghaylan/validata/constraint/annotation"),
		)
		return candidates.firstOrNull { Files.isDirectory(it) }
			?: error("validata-core annotation sources not found; tried $candidates")
	}
}

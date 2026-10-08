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

package io.ghaylan.validata.intellij.analysis.compat

import io.ghaylan.validata.intellij.typing.ValidatorTypeView
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks IDE validator-fit ranking to KSP `ValidatorCompatibility` semantics.
 *
 * Operates on [ValidatorTypeView] / [SubjectTypeViews] — no PSI required.
 * 
 * @author Ghaylan Saada
 */
class ValidatorTypeCompatibilityTest {
	
	@Test
	@DisplayName("exact V match ranks 0")
	fun exactMatchRanksZero() {
		val subject = SubjectTypeViews.typeView("kotlin.String")
		val validator = SubjectTypeViews.typeView("kotlin.String")
		assertThat(ValidatorTypeCompatibility.scoreFit(subject, validator)).isEqualTo(0)
	}
	
	@Test
	@DisplayName("java.lang.Integer matches kotlin.Int")
	fun boxedIntegerMatchesKotlinInt() {
		val subject = SubjectTypeViews.typeView("kotlin.Int")
		val validator = SubjectTypeViews.typeView("java.lang.Integer")
		assertThat(ValidatorTypeCompatibility.scoreFit(subject, validator)).isNotNull()
	}
	
	@Test
	@DisplayName("CharSequence accepts String and rejects Int")
	fun charSequenceAcceptsStringRejectsInt() {
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.String"),
				SubjectTypeViews.typeView("kotlin.CharSequence"),
			),
		).isNotNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.Int"),
				SubjectTypeViews.typeView("kotlin.CharSequence"),
			),
		).isNull()
	}
	
	@Test
	@DisplayName("Number accepts Int and rejects String")
	fun numberAcceptsIntRejectsString() {
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.Int"),
				SubjectTypeViews.typeView("kotlin.Number"),
			),
		).isNotNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.String"),
				SubjectTypeViews.typeView("kotlin.Number"),
			),
		).isNull()
	}
	
	@Test
	@DisplayName("Any validator V accepts every subject")
	fun anyAcceptsEverything() {
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.String"),
				SubjectTypeViews.typeView("kotlin.Any"),
			),
		).isNotNull()
	}
	
	@Test
	@DisplayName("Comparable<*> accepts Int via wildcard type arg")
	fun comparableAcceptsIntViaWildcardArg() {
		val subject = SubjectTypeViews.typeView("kotlin.Int")
		val validator = ValidatorTypeView(
			qualifiedName = "kotlin.Comparable",
			typeArguments = listOf(ValidatorTypeView.WILDCARD),
		)
		assertThat(ValidatorTypeCompatibility.scoreFit(subject, validator)).isNotNull()
	}
	
	@Test
	@DisplayName("anyFits is true when at least one validator V matches")
	fun anyFitsRequiresOneMatch() {
		val subject = SubjectTypeViews.typeView("kotlin.Int")
		assertThat(
			ValidatorTypeCompatibility.anyFits(
				subject,
				listOf(
					SubjectTypeViews.typeView("kotlin.CharSequence"),
					SubjectTypeViews.typeView("kotlin.Number"),
				),
			),
		).isTrue()
		assertThat(
			ValidatorTypeCompatibility.anyFits(
				subject,
				listOf(SubjectTypeViews.typeView("kotlin.CharSequence")),
			),
		).isFalse()
	}
	
	@Test
	@DisplayName("IntArray matches IntArray but not scalar Int")
	fun intArrayMatchesIntArrayNotScalar() {
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.IntArray"),
				SubjectTypeViews.typeView("kotlin.IntArray"),
			),
		).isNotNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.IntArray"),
				SubjectTypeViews.typeView("kotlin.Int"),
			),
		).isNull()
	}
	
	@Test
	@DisplayName("Collection<*> validator accepts Collection and List subjects; rejects String")
	fun collectionStarAcceptsCollectionsRejectsString() {
		val collectionV = ValidatorTypeView(
			qualifiedName = "kotlin.collections.Collection",
			typeArguments = listOf(ValidatorTypeView.WILDCARD),
		)
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView(
					"kotlin.collections.Collection",
					listOf(SubjectTypeViews.typeView("kotlin.String")),
				),
				collectionV,
			),
		).isNotNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView(
					"kotlin.collections.List",
					listOf(SubjectTypeViews.typeView("kotlin.String")),
				),
				collectionV,
			),
		).isNotNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.String"),
				collectionV,
			),
		).isNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("kotlin.String"),
				SubjectTypeViews.typeView("kotlin.Cloneable"),
			),
		).isNull()
	}

	@Test
	@DisplayName("collection/map wildcard validators accept concrete subjects")
	fun collectionAndMapWildcardValidators() {
		val listSubject = SubjectTypeViews.typeView(
			"kotlin.collections.List",
			listOf(SubjectTypeViews.typeView("kotlin.String")),
		)
		val listValidator = ValidatorTypeView(
			qualifiedName = "kotlin.collections.List",
			typeArguments = listOf(ValidatorTypeView.WILDCARD),
			assignableSupertypes = setOf("kotlin.collections.Collection"),
		)
		assertThat(ValidatorTypeCompatibility.scoreFit(listSubject, listValidator)).isNotNull()
		val mapSubject = SubjectTypeViews.typeView(
			"kotlin.collections.Map",
			listOf(
				SubjectTypeViews.typeView("kotlin.String"),
				SubjectTypeViews.typeView("kotlin.Int"),
			),
		)
		val mapValidator = ValidatorTypeView(
			qualifiedName = "kotlin.collections.Map",
			typeArguments = listOf(ValidatorTypeView.WILDCARD, ValidatorTypeView.WILDCARD),
		)
		assertThat(ValidatorTypeCompatibility.scoreFit(mapSubject, mapValidator)).isNotNull()
	}
	
	@Test
	@DisplayName("typeArgsMatch requires arity unless expected args are empty")
	fun typeArgsMatchRequiresArityUnlessExpectedEmpty() {
		assertThat(
			ValidatorTypeCompatibility.typeArgsMatch(
				listOf(SubjectTypeViews.typeView("kotlin.String")),
				emptyList(),
			),
		).isTrue()
		assertThat(
			ValidatorTypeCompatibility.typeArgsMatch(
				listOf(SubjectTypeViews.typeView("kotlin.String")),
				listOf(
					SubjectTypeViews.typeView("kotlin.String"),
					SubjectTypeViews.typeView("kotlin.Int"),
				),
			),
		).isFalse()
	}
	
	@Test
	@DisplayName("canonicalize maps java.lang aliases to kotlin.*")
	fun canonicalizeMapsJavaLangAliases() {
		assertThat(ValidatorTypeCompatibility.canonicalize("java.lang.String")).isEqualTo("kotlin.String")
		assertThat(ValidatorTypeCompatibility.canonicalize("java.lang.Object")).isEqualTo("kotlin.Any")
		assertThat(ValidatorTypeCompatibility.canonicalize("java.lang.Integer")).isEqualTo("kotlin.Int")
	}
	
	@Test
	@DisplayName("displayName prefers simple type names")
	fun displayNamesPreferSimpleNames() {
		assertThat(SubjectTypeViews.displayName(SubjectTypeViews.typeView("kotlin.String"))).isEqualTo("String")
		assertThat(
			SubjectTypeViews.displayName(
				SubjectTypeViews.typeView(
					"kotlin.collections.List",
					listOf(SubjectTypeViews.typeView("kotlin.String")),
				),
			),
		).isEqualTo("List<String>")
		assertThat(SubjectTypeViews.displayName(SubjectTypeViews.typeView("kotlin.IntArray"))).isEqualTo("Array<Int>")
		assertThat(SubjectTypeViews.displayName(ValidatorTypeView.WILDCARD)).isEqualTo("*")
	}

	@Test
	@DisplayName("IntArray collapses to Array + Int element; PhotoArray is not an array")
	fun arrayCollapseAndFalsePositiveGuard() {
		val intArray = SubjectTypeViews.typeView("kotlin.IntArray")
		assertThat(intArray.isArray).isTrue()
		assertThat(intArray.qualifiedName).isEqualTo("kotlin.Array")
		assertThat(intArray.arrayElement?.qualifiedName).isEqualTo("kotlin.Int")

		val photoArray = SubjectTypeViews.typeView("com.acme.PhotoArray")
		assertThat(photoArray.isArray).isFalse()
		assertThat(photoArray.qualifiedName).isEqualTo("com.acme.PhotoArray")
	}

	@Test
	@DisplayName("LocalDate fits Temporal via builtin supers; Month does not")
	fun temporalSoftFitDoesNotAcceptMonth() {
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("java.time.LocalDate"),
				SubjectTypeViews.typeView("java.time.temporal.Temporal"),
			),
		).isEqualTo(4)
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("java.time.Month"),
				SubjectTypeViews.typeView("java.time.temporal.Temporal"),
			),
		).isNull()
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				SubjectTypeViews.typeView("java.time.Duration"),
				SubjectTypeViews.typeView("java.time.temporal.Temporal"),
			),
		).isNull()
	}

	@Test
	@DisplayName("String subject gets CharSequence soft-supers from schema matrix")
	fun stringBuiltinSupersEnableCharSequenceFit() {
		val stringView = SubjectTypeViews.typeView("kotlin.String")
		assertThat(stringView.assignableSupertypes).contains("kotlin.CharSequence", "kotlin.Comparable")
		assertThat(
			ValidatorTypeCompatibility.scoreFit(
				stringView,
				SubjectTypeViews.typeView("kotlin.CharSequence"),
			),
		).isNotNull()
	}
}

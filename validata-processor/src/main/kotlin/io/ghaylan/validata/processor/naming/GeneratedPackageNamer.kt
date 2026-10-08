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
package io.ghaylan.validata.processor.naming

import io.ghaylan.validata.processor.fqns.ProcessorFqns

/**
 * Package placement for generated sources.
 *
 * ## Rules
 * - **Per-type / per-handler / per-annotation:** `<sourcePackage>.ghaylan.validata`
 * - **SPI aggregators (one per compilation unit):** `<commonPrefix>.ghaylan.validata`
 * - **No shared prefix:** [ProcessorFqns.GENERATED_FALLBACK_PACKAGE]
 *
 * The [LIBRARY_SEGMENT] segment is library-owned so output does not collide with other tools’
 * bare `*.generated` packages.*
 * 
 * @author Ghaylan Saada
 */
internal object GeneratedPackageNamer {
	
	/**
	 * Package segment appended under every source package that owns generated artifacts.
	 */
	const val LIBRARY_SEGMENT = "ghaylan.validata"
	
	/**
	 * Package for a single type's schema / Fields (or a controller's endpoint factories).
	 *
	 * Side effects: none.
	 *
	 * @param sourcePackage Kotlin package of the annotated declaration; blank → [fallbackModulePackage].
	 * @param fallbackModulePackage Module package used when [sourcePackage] is empty.
	 * @return Generated package `<sourcePackage>.ghaylan.validata` or [fallbackModulePackage].	 
	 */
	fun typePackage(
		sourcePackage: String,
		fallbackModulePackage: String
	): String =
		if (sourcePackage.isBlank()) fallbackModulePackage
		else "$sourcePackage.$LIBRARY_SEGMENT"
	
	/**
	 * Package for SPI aggregators (object schemas, request modules, constraint catalogs).
	 *
	 * Side effects: none.
	 *
	 * @param sourcePackages Packages of annotated declarations in this compilation unit.
	 * @return Longest shared prefix plus [LIBRARY_SEGMENT], or [ProcessorFqns.GENERATED_FALLBACK_PACKAGE].	 
	 */
	fun modulePackage(sourcePackages: List<String>): String {
		val prefix = commonPrefix(sourcePackages)
			?: return ProcessorFqns.GENERATED_FALLBACK_PACKAGE
		return "$prefix.$LIBRARY_SEGMENT"
	}
	
	/**
	 * Longest shared dot-separated prefix among [packages], or `null` when empty or disjoint.
	 *
	 * Side effects: none.
	 *
	 * @param packages Package names to compare.
	 * @return Shared prefix without trailing segment, or `null` when none.	 
	 */
	fun commonPrefix(packages: List<String>): String? {
		if (packages.isEmpty()) return null
		val parts = packages.map {
			it.split('.').filter { seg -> seg.isNotEmpty() }
		}
		if (parts.any { it.isEmpty() }) return null
		val min = parts.minOf { it.size }
		val shared = mutableListOf<String>()
		for (i in 0 until min) {
			val token = parts[0][i]
			if (parts.all { it[i] == token }) shared += token else break
		}
		return if (shared.isEmpty()) null else shared.joinToString(".")
	}
}

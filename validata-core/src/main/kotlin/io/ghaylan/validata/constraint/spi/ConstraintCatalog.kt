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
package io.ghaylan.validata.constraint.spi

/**
 * SPI implemented by generated code. Each compiled module that declares `@Constraint` annotations
 * (this library's own built-ins, or a consuming application's custom constraints) contributes
 * exactly one implementation, registered under `META-INF/services`.
 *
 * Discovery is "every jar announces itself" — there is no classpath scanning. A module that
 * authors custom constraints must apply the KSP processor to itself so this SPI entry is emitted.
 *
 * ### Example
 *
 * ```kotlin
 * // Hand-written test double (production catalogs are KSP-generated):
 * class TestCatalog : ConstraintCatalog {
 *   override fun entries(): List<ConstraintCatalogEntry> = listOf(
 *     ConstraintCatalogEntry(
 *       annotationType = Email::class.java,
 *       metadataType = EmailConstraint::class.java,
 *       valueType = TypeInfo(...),
 *       validatorType = EmailValidator::class.java,
 *       defaultInstanceFactory = { EmailValidator },
 *     ),
 *   )
 * }
 * ```
 *
 * Prefer [GeneratedConstraintCatalogs.all] at runtime; implement this interface only in tests or
 * when authoring a custom KSP emitter.
 * */
fun interface ConstraintCatalog {
	
	/**
	 * All constraint bindings contributed by this module.
	 *
	 * No side effects expected from generated implementations.
	 *
	 * @return Catalog entries in a stable, deterministic order.	 
	 */
	fun entries(): List<ConstraintCatalogEntry>
}

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
package io.ghaylan.validata.processor.analyze

import java.lang.reflect.InvocationTargetException

/**
 * Reflective lookup of catalog enum entries that expose `code` / `message` Kotlin properties
 * (the catalog enum contract used by `@ApiError`).
 *
 * Used when KSP can load the catalog class (dependency / prior compilation). Same-round enums
 * often fail to load; callers then leave message blank and OpenAPI resolves at docs time.
 *
 * **Null-on-miss contract:** returns `null` only for expected reflective unavailability
 * (missing class/constant/property, access/cast failures). JVM [Error] / [LinkageError]
 * types (other than [ExceptionInInitializerError], treated as class-init miss) propagate.*
 * 
 * @author Ghaylan Saada
 */
internal object CatalogErrorDefinitionLookup {

	/**
	 * Loads [catalogFqcn] and returns the message of the enum constant named [entryName].
	 *
	 * @param catalogFqcn binary name of the catalog enum
	 * @param entryName enum constant name (annotation `code`)
	 * @return catalog message, or `null` when the class/constant cannot be loaded
	 */
	fun messageOf(catalogFqcn: String, entryName: String): String? =
		loadEntry(catalogFqcn, entryName)?.let { readStringProperty(it, "message", "getMessage") }

	/**
	 * Loads [catalogFqcn] and returns the code of the enum constant named [entryName].
	 *
	 * @param catalogFqcn binary name of the catalog enum
	 * @param entryName enum constant name (annotation `code`)
	 * @return catalog code, or `null` when the class/constant cannot be loaded
	 */
	fun codeOf(catalogFqcn: String, entryName: String): String? =
		loadEntry(catalogFqcn, entryName)?.let { readStringProperty(it, "code", "getCode") }

	/**
	 * @param catalogFqcn binary name of the catalog enum
	 * @param entryName enum constant name
	 * @return enum constant instance, or `null` on reflective unavailability
	 */
	private fun loadEntry(catalogFqcn: String, entryName: String): Any? =
		try {
			@Suppress("UNCHECKED_CAST")
			val clazz = Class.forName(catalogFqcn) as Class<out Enum<*>>
			java.lang.Enum.valueOf(clazz, entryName)
		} catch (_: ClassNotFoundException) {
			null
		} catch (_: NoClassDefFoundError) {
			null
		} catch (_: ExceptionInInitializerError) {
			null
		} catch (_: ClassCastException) {
			null
		} catch (_: IllegalArgumentException) {
			// Enum.valueOf: no such constant
			null
		}

	/**
	 * Reads a Kotlin/Java string property from [instance] (`getX` then field fallback).
	 *
	 * @param instance enum constant
	 * @param kotlinName Kotlin property name (`message` / `code`)
	 * @param javaGetter Java bean getter name
	 * @return property string, or `null` when absent / not a String
	 */
	private fun readStringProperty(instance: Any, kotlinName: String, javaGetter: String): String? {
		val clazz = instance.javaClass
		clazz.methods.firstOrNull {
			it.name == javaGetter && it.parameterCount == 0
		}?.let { method ->
			return try {
				method.invoke(instance) as? String
			} catch (e: InvocationTargetException) {
				val cause = e.cause
				if (cause is Error) throw cause
				null
			} catch (_: IllegalAccessException) {
				null
			} catch (_: ClassCastException) {
				null
			}
		}
		return try {
			clazz.getDeclaredField(kotlinName).apply {
				isAccessible = true
			}.get(instance) as? String
		} catch (_: NoSuchFieldException) {
			null
		} catch (_: IllegalAccessException) {
			null
		} catch (_: ClassCastException) {
			null
		} catch (_: SecurityException) {
			null
		}
	}
}

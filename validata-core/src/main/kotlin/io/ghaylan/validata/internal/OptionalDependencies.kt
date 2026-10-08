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
package io.ghaylan.validata.internal

/**
 * Presence checks for optional third-party libraries (T-51).
 *
 * `@Html` needs jsoup; `@Phone` needs libphonenumber. Both are `compileOnly` for consumers who never
 * use those constraints. Validators fail with an actionable message when the jar is absent.*
 * 
 * @author Ghaylan Saada
 */
internal object OptionalDependencies {
	
	/**
	 * `true` when jsoup is on the runtime classpath.
	 */
	val jsoupPresent: Boolean by lazy { classPresent("org.jsoup.Jsoup") }
	
	/**
	 * `true` when libphonenumber is on the runtime classpath.
	 */
	val libphonenumberPresent: Boolean by lazy {
		classPresent("com.google.i18n.phonenumbers.PhoneNumberUtil")
	}
	
	/**
	 * Fails when jsoup is absent.
	 *
	 * Side effect: may load jsoup via [Class.forName] on first [jsoupPresent] read.
	 *
	 * @param feature Human-readable feature name for the error (default `"@Html"`).
	 * @throws IllegalStateException When jsoup is not on the classpath.	 
	 */
	fun requireJsoup(feature: String = "@Html") {
		if (!jsoupPresent) {
			error("$feature requires org.jsoup:jsoup on the classpath. Add the dependency to your build, or remove $feature from your model.")
		}
	}
	
	/**
	 * Fails when libphonenumber is absent.
	 *
	 * Side effect: may load libphonenumber via [Class.forName] on first [libphonenumberPresent] read.
	 *
	 * @param feature Human-readable feature name for the error (default `"@Phone"`).
	 * @throws IllegalStateException When libphonenumber is not on the classpath.	 
	 */
	fun requireLibphonenumber(feature: String = "@Phone") {
		if (!libphonenumberPresent) {
			error("$feature requires com.googlecode.libphonenumber:libphonenumber on the classpath. Add the dependency to your build, or remove $feature from your model.")
		}
	}
	
	/**
	 * Returns whether [name] resolves on this object's class loader.
	 *
	 * Side effect: attempts [Class.forName] without initializing the class.
	 *
	 * @param name Fully-qualified class name to probe.
	 * @return `true` when the class is present.	 
	 */
	private fun classPresent(name: String): Boolean {
		return runCatching {
			Class.forName(name, false, OptionalDependencies::class.java.classLoader)
		}.isSuccess
	}
}

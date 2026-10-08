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

package io.ghaylan.validata.intellij

import com.intellij.openapi.extensions.PluginId

/**
 * Stable identifiers for the Validata IntelliJ plugin.
 *
 * Centralizes strings that must stay aligned with packaging and the IDE plugin manager so call
 * sites do not hard-code marketplace ids. Values are patched into `META-INF/plugin.xml` by the
 * `intellijPlatform.pluginConfiguration` block in `validata-intellij/build.gradle.kts` (the source
 * `plugin.xml` leaves `<id>` / version / vendor to that patch).
 *
 * Not a registry of extension FQCNs — those live as `implementation` / `implementationClass`
 * attributes in `plugin.xml`. Not related to library marker FQCNs in
 * `PropertyRefLibraryFqns` (`io.ghaylan.validata.constraint.*` / `schema.ref.*`).
 * 
 * @author Ghaylan Saada
 */
object PluginConstants {
	
	/**
	 * Marketplace / [PluginId] string for this plugin.
	 *
	 * Must match:
	 * - `intellijPlatform.pluginConfiguration { id = "…" }` in `build.gradle.kts`
	 * - the patched `<id>` written into `META-INF/plugin.xml` at build time
	 *
	 * Used wherever code needs the same id the Platform uses to look up the installed plugin
	 * (diagnostics, optional feature gates, tests). Changing this breaks Marketplace updates and
	 * any stored settings keyed by plugin id.
	 */
	const val PLUGIN_ID: String = "io.ghaylansaada.validata"
}

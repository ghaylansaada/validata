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

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.DisplayName

/**
 * Smoke test: the plugin descriptor loads and is enabled in the test IDE.
 *
 * Proves the composite build produces a valid IntelliJ plugin artifact with
 * [PluginConstants.PLUGIN_ID].
 * 
 * @author Ghaylan Saada
 */
class PluginLoadTest: BasePlatformTestCase() {
	
	@DisplayName("plugin descriptor loads and is enabled in the test IDE")
	fun testPluginIsLoaded() {
		val plugin = PluginManagerCore.getPlugin(PluginId.getId(PluginConstants.PLUGIN_ID))
		assertNotNull("Expected plugin ${PluginConstants.PLUGIN_ID} to be present on the classpath", plugin)
		assertTrue(plugin!!.isEnabled)
	}
}

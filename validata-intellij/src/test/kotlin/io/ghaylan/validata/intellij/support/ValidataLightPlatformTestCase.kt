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

package io.ghaylan.validata.intellij.support

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.io.File

/**
 * Base for Validata IntelliJ platform tests.
 *
 * - Installs library marker stubs via [PropertyRefLightFixtures]
 * - Defaults [getTestDataPath] to `src/test/testData/flat` (constraint annotation stubs)
 * - Exposes [findExactStringLiteral] / [findStringLiteralContaining] helpers
 *
 * Pure logic (no PSI) stays in plain JUnit 4 classes under the mirrored production package —
 * do not extend this for matrix / FQCN / path-dialect tests.
 * 
 * @author Ghaylan Saada
 */
abstract class ValidataLightPlatformTestCase: BasePlatformTestCase() {
	
	override fun getTestDataPath(): String = File("src/test/testData/flat").absolutePath
	
	protected fun addLibraryMarkers() {
		PropertyRefLightFixtures.addLibraryMarkers(myFixture)
	}
	
	protected fun findExactStringLiteral(content: String): KtStringTemplateExpression = PlatformTestSupport.findExactStringLiteral(myFixture, content)
	
	protected fun findStringLiteralContaining(fragment: String): KtStringTemplateExpression =
		PlatformTestSupport.findStringLiteralContaining(myFixture, fragment)
}

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

import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * Light-test stubs for OpenAPI presentation annotations (`@ApiError`, …).
 *
 * Installed under the openapi presentation package so FQCN discovery matches production.*
 * 
 * @author Ghaylan Saada
 */
internal object ApiErrorLightFixtures {
	
	/**
	 * Installs minimal `@ApiError` + `ConstraintErrorDefinition` stubs under the production
	 * packages so FQCN discovery matches a real classpath.	 
	 */
	fun addPresentationMarkers(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/model/ConstraintErrorDefinition.kt",
			"""
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

package io.ghaylan.validata.model
			interface ConstraintErrorDefinition {
			  val code: String
			  val message: String
			}
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/openapi/presentation/ApiError.kt",
			"""
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

package io.ghaylan.validata.openapi.presentation
			import io.ghaylan.validata.model.ConstraintErrorDefinition
			import kotlin.reflect.KClass
			@Repeatable
			@Retention(AnnotationRetention.RUNTIME)
			@Target(
			  AnnotationTarget.FIELD,
			  AnnotationTarget.PROPERTY,
			  AnnotationTarget.VALUE_PARAMETER,
			  AnnotationTarget.ANNOTATION_CLASS)
			annotation class ApiError(
			  val code: String,
			  val message: String = "",
			  val catalog: KClass<out ConstraintErrorDefinition>)
			""".trimIndent(),
		)
	}
}

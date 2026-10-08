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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.model.DynamicShapeModel
import io.ghaylan.validata.processor.model.EndpointModel
import io.ghaylan.validata.processor.model.EndpointParameterModel
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [EndpointPathConstantsCodeWriter].
 *
 * @author Ghaylan Saada
 */
class EndpointPathConstantsCodeWriterTest {

	@Test
	@DisplayName("shouldWrite is false when only BODY params exist")
	fun bodyOnlySkipped() {
		val model = endpoint(
			parameters = listOf(
				param(EndpointArgumentKind.BODY, "body", "body", bodyType = "sample.Dto"),
			),
		)
		assertThat(EndpointPathConstantsCodeWriter.shouldWrite(model)).isFalse()
		assertThat(EndpointPathConstantsCodeWriter.write("sample.ghaylan.validata", model)).isNull()
	}

	@Test
	@DisplayName("emits flat path/query/header consts with Spring wire values")
	fun flatTransportConsts() {
		val model = endpoint(
			functionQualifiedName = "sample.UserController.lookup",
			identifier = "sample.UserController#lookup(java.lang.String,java.lang.String,java.lang.String)",
			parameters = listOf(
				param(EndpointArgumentKind.PATH, "userId", "userId"),
				param(EndpointArgumentKind.QUERY, "q", "q"),
				param(EndpointArgumentKind.HEADER, "tenant", "X-Tenant"),
				param(EndpointArgumentKind.BODY, "body", "body", bodyType = "sample.Dto"),
			),
		)
		val src = EndpointPathConstantsCodeWriter.write("sample.ghaylan.validata", model)!!
		assertThat(src).contains("import sample.UserController")
		assertThat(src).contains(" * Generated wire-path constants for [UserController.lookup]")
		assertThat(src).contains("object ${EndpointPathConstantsCodeWriter.objectName(model)}")
		assertThat(src).contains("const val USER_ID: String = \"userId\"")
		assertThat(src).contains("const val Q: String = \"q\"")
		assertThat(src).contains("const val TENANT: String = \"X-Tenant\"")
		assertThat(src).doesNotContain("object Path")
		assertThat(src).doesNotContain("BODY")
	}

	private fun endpoint(
		functionQualifiedName: String = "sample.UserController.lookup",
		identifier: String = "sample.UserController#lookup()",
		parameters: List<EndpointParameterModel>,
	): EndpointModel = EndpointModel(
		identifier = identifier,
		packageName = "sample",
		functionQualifiedName = functionQualifiedName,
		sourceFilePath = null,
		oneErrorPerParam = false,
		failFast = false,
		groupsFqcn = emptyList(),
		parameters = parameters,
	)

	private fun param(
		kind: EndpointArgumentKind,
		declared: String,
		resolved: String,
		bodyType: String? = null,
	): EndpointParameterModel = EndpointParameterModel(
		kind = kind,
		declaredName = declared,
		resolvedName = resolved,
		shape = DynamicShapeModel(),
		bodyTypeQualifiedName = bodyType,
	)
}

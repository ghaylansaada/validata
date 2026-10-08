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
package io.ghaylan.validata.web

import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.request.EndpointArgumentSlot
import io.ghaylan.validata.schema.request.EndpointSchema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [RequestArgumentAssembler]: allocate only the section maps the plan needs.
 *
 * Threat: layout/args skew or always-on map allocation would mis-pack sections or burn GC on
 * body-only endpoints.
 * 
 * @author Ghaylan Saada
 */
class RequestArgumentAssemblerTest {
	
	@Nested
	@DisplayName("Section allocation")
	inner class SectionAllocation {
		
		@Test
		@DisplayName("body-only layout with all section flags false leaves maps null")
		fun bodyOnlyAllocatesNoMaps() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = false,
				needsPath = false,
				layout = listOf(EndpointArgumentSlot(EndpointArgumentKind.BODY)),
			)
			val body = Any()
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf(body),
				plan = plan,
			)
			
			assertThat(assembled.body).isSameAs(body)
			assertThat(assembled.queryParams).isNull()
			assertThat(assembled.headers).isNull()
			assertThat(assembled.pathVariables).isNull()
		}
		
		@Test
		@DisplayName("query slots populate only the query map when needsQuery is true")
		fun queryOnlyPopulatesQueryMap() {
			val plan = activePlan(
				needsQuery = true,
				needsHeader = false,
				needsPath = false,
				layout = listOf(
					EndpointArgumentSlot(EndpointArgumentKind.QUERY, "q"),
					EndpointArgumentSlot(EndpointArgumentKind.QUERY, "page"),
				),
			)
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf("kotlin", 1),
				plan = plan,
			)
			
			assertThat(assembled.body).isNull()
			assertThat(assembled.queryParams).containsExactlyEntriesOf(mapOf("q" to "kotlin", "page" to 1))
			assertThat(assembled.headers).isNull()
			assertThat(assembled.pathVariables).isNull()
		}
		
		@Test
		@DisplayName("header and path slots populate only those maps when their plan flags are true")
		fun headerAndPathMapsPopulated() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = true,
				needsPath = true,
				layout = listOf(
					EndpointArgumentSlot(EndpointArgumentKind.HEADER, "X-Trace"),
					EndpointArgumentSlot(EndpointArgumentKind.PATH, "id"),
				),
			)
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf("abc", 42),
				plan = plan,
			)
			
			assertThat(assembled.body).isNull()
			assertThat(assembled.queryParams).isNull()
			assertThat(assembled.headers).containsExactlyEntriesOf(mapOf("X-Trace" to "abc"))
			assertThat(assembled.pathVariables).containsExactlyEntriesOf(mapOf("id" to 42))
		}
		
		@Test
		@DisplayName("OTHER slots are ignored and do not force map allocation")
		fun otherSlotsIgnored() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = false,
				needsPath = false,
				layout = listOf(
					EndpointArgumentSlot(EndpointArgumentKind.BODY),
					EndpointArgumentSlot(EndpointArgumentKind.OTHER),
				),
			)
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf("payload", "servletRequest"),
				plan = plan,
			)
			
			assertThat(assembled.body).isEqualTo("payload")
			assertThat(assembled.queryParams).isNull()
		}
		
		@Test
		@DisplayName("header slots are ignored when needsHeader is false so no map is allocated")
		fun headerFlagFalseSkipsAllocation() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = false,
				needsPath = false,
				layout = listOf(EndpointArgumentSlot(EndpointArgumentKind.HEADER, "X-Trace")),
			)
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf("abc"),
				plan = plan,
			)
			assertThat(assembled.headers).isNull()
		}
	}
	
	@Nested
	@DisplayName("Body selection and mismatches")
	inner class BodyAndMismatch {
		
		@Test
		@DisplayName("multiple BODY slots keep the first non-null value")
		fun firstNonNullBodyWins() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = false,
				needsPath = false,
				layout = listOf(
					EndpointArgumentSlot(EndpointArgumentKind.BODY),
					EndpointArgumentSlot(EndpointArgumentKind.BODY),
				),
			)
			val first = Any()
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf(first, Any()),
				plan = plan,
			)
			assertThat(assembled.body).isSameAs(first)
		}
		
		@Test
		@DisplayName("multiple BODY slots skip a null first body and take the next non-null")
		fun skipsNullFirstBody() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = false,
				needsPath = false,
				layout = listOf(
					EndpointArgumentSlot(EndpointArgumentKind.BODY),
					EndpointArgumentSlot(EndpointArgumentKind.BODY),
				),
			)
			val second = Any()
			val assembled = RequestArgumentAssembler.assemble(
				layout = plan.schema.argumentLayout,
				args = arrayOf(null, second),
				plan = plan,
			)
			assertThat(assembled.body).isSameAs(second)
		}
		
		@Test
		@DisplayName("layout/args length mismatch throws IllegalArgumentException naming the endpoint id")
		fun layoutArgsLengthMismatchThrows() {
			val plan = activePlan(
				needsQuery = false,
				needsHeader = false,
				needsPath = false,
				layout = listOf(EndpointArgumentSlot(EndpointArgumentKind.BODY)),
			)
			assertThatThrownBy {
				RequestArgumentAssembler.assemble(
					layout = plan.schema.argumentLayout,
					args = arrayOf("a", "b"),
					plan = plan,
				)
			}.isInstanceOf(IllegalArgumentException::class.java)
				.hasMessageContaining(plan.endpointId)
				.hasMessageContaining("does not match")
		}
	}
	
	private fun activePlan(
		needsQuery: Boolean,
		needsHeader: Boolean,
		needsPath: Boolean,
		layout: List<EndpointArgumentSlot>,
	): HandlerValidationPlan.Active {
		val schema = EndpointSchema(
			id = "test#method()",
			oneErrorPerParam = true,
			groups = setOf(OnDefault::class),
			argumentLayout = layout,
		)
		return HandlerValidationPlan.Active(
			endpointId = schema.id,
			schema = schema,
			schemaWhenBodyAbsent = schema,
			needsQuery = needsQuery,
			needsHeader = needsHeader,
			needsPath = needsPath,
		)
	}
}

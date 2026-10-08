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
package io.ghaylan.validata.samples.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * springdoc `/v3/api-docs` with Validata OpenAPI enrichment.*
 * 
 * @author Ghaylan Saada
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsIT(
	/** MockMvc against the sample Boot application.	 */
	@Autowired
	val mockMvc: MockMvc,
) {
	
	@Test
	@DisplayName("/v3/api-docs loads with validata-openapi on the classpath")
			/** /v3/api-docs loads with validata-openapi on the classpath			 */
	fun apiDocsLoads() {
		mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
				jsonPath("$.openapi") { exists() }
				jsonPath("$.paths") { exists() }
			}
	}
	
	@Test
	@DisplayName("CreateUserRequest: Size, OddYears, SampleFloor minimum, no x-validata-unmapped")
			/** CreateUserRequest: Size, OddYears, SampleFloor minimum, no x-validata-unmapped			 */
	fun createUserSchemaUsesJsonPropertyAndSize() {
		mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
				jsonPath("$.components.schemas.CreateUserRequest.properties.first_name") { exists() }
				jsonPath("$.components.schemas.CreateUserRequest.properties.first_name.minLength") { value(2) }
				jsonPath("$.components.schemas.CreateUserRequest.properties.first_name.maxLength") { value(40) }
				jsonPath("$.components.schemas.CreateUserRequest.properties.first_name['x-validata-errors']") { isArray() }
				jsonPath("$.components.schemas.CreateUserRequest.properties.first_name['x-validata-errors'][?(@.code == 'NAME_INVALID')].message") {
					value("Display name failed a business rule")
				}
				jsonPath("$.components.schemas.CreateUserRequest.properties.min_age['x-validata-constraints'][?(@._constraint == 'OddYears')]") {
					exists()
				}
				jsonPath("$.components.schemas.CreateUserRequest.properties.min_age['x-validata-constraints'][?(@._constraint == 'SampleFloor')]") {
					exists()
				}
				jsonPath("$.components.schemas.CreateUserRequest.properties.min_age.minimum") {
					value(3)
				}
				jsonPath("$.components.schemas.CreateUserRequest.properties.min_age['x-validata-unmapped']") {
					doesNotExist()
				}
				jsonPath(
					"$.components.schemas.CreateUserRequest.properties.max_age" +
						"['x-validata-constraints'][?(@._constraint == 'Compare' && @.operation == 'GT')].ref",
				) {
					value("min_age")
				}
			}
	}
	
	@Test
	@DisplayName("lookup params carry Size; sample ErrorDocPublisher docs 400 SampleErrorBody")
			/** lookup params carry Size; sample ErrorDocPublisher docs 400 SampleErrorBody			 */
	fun lookupOperationDocumented() {
		mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
				jsonPath("$.paths['/api/users/{userId}'].get.parameters[?(@.name == 'userId')].schema.minLength") {
					value(3)
				}
				jsonPath("$.paths['/api/users/{userId}'].get.parameters[?(@.name == 'q')].schema.minLength") {
					value(2)
				}
				jsonPath("$.paths['/api/users/{userId}'].get.parameters[?(@.name == 'X-Tenant')].schema.minLength") {
					value(2)
				}
				jsonPath("$.paths['/api/users/{userId}'].get.responses['400']") { exists() }
				jsonPath(
					"$.paths['/api/users/{userId}'].get.responses['400'].content['application/json']" + ".schema.properties.errors.items.properties.code.enum",
				) { isArray() }
				jsonPath("$.paths['/api/users'].post.responses['400']") { exists() }
				jsonPath("$.paths['/api/users/{userId}'].get.parameters[?(@.name == 'userId')].schema['x-validata-errors']") {
					isArray()
				}
			}
	}
	
	@Test
	@DisplayName("AllConstraintsRequest.phoneBackup documents PHONE_BACKUP_REQUIRED")
			/** AllConstraintsRequest.phoneBackup documents PHONE_BACKUP_REQUIRED			 */
	fun phoneBackupErrorDoc() {
		mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
				jsonPath("$.components.schemas.AllConstraintsRequest.properties.phoneBackup['x-validata-errors'][?(@.code == 'PHONE_BACKUP_REQUIRED')]") {
					exists()
				}
			}
	}
	
	@Test
	@DisplayName("AllConstraintsRequest.contact documents EmailOrPhone as OR composition")
			/** AllConstraintsRequest.contact documents EmailOrPhone as OR composition			 */
	fun contactEmailOrPhoneCompositionDoc() {
		mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
				jsonPath(
					"$.components.schemas.AllConstraintsRequest.properties.contact" + "['x-validata-constraints'][?(@._constraint == 'Composition')].composition",
				) {
					value("OR")
				}
				jsonPath(
					"$.components.schemas.AllConstraintsRequest.properties.contact" + "['x-validata-constraints'][?(@._constraint == 'Composition')].members",
				) {
					isArray()
				}
				jsonPath("$.components.schemas.AllConstraintsRequest.properties.contact.format") {
					doesNotExist()
				}
			}
	}
	
	@Test
	@DisplayName("AllConstraintsRequest Compare EQ uses confirm_secret wire name")
			/** AllConstraintsRequest Compare EQ uses confirm_secret wire name			 */
	fun compareEqualUsesJsonPropertyWireName() {
		mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
				jsonPath("$.components.schemas.AllConstraintsRequest.properties.confirm_secret") { exists() }
				jsonPath(
					"$.components.schemas.AllConstraintsRequest.properties.secret" +
						"['x-validata-constraints'][?(@._constraint == 'Compare' && @.operation == 'EQ')].ref",
				) {
					value("confirm_secret")
				}
			}
	}
}

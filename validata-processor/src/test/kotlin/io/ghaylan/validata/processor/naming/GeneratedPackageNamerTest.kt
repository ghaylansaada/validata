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
package io.ghaylan.validata.processor.naming

import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.EndpointModel
import io.ghaylan.validata.processor.model.SchemaModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GeneratedPackageNamer] and [GeneratedNames].
 *
 * ## Why this suite exists
 *
 * Generated package placement and type names are a public contract for consumers and IDE
 * navigation. Drift here breaks SPI lookups and Fields autocomplete.
 * 
 * @author Ghaylan Saada
 */
class GeneratedPackageNamerTest {
	
	@Nested
	@DisplayName("GeneratedPackageNamer")
	inner class Packages {
		
		@Test
		@DisplayName("shared prefix becomes package.ghaylan.validata")
		fun sharedPrefix() {
			assertThat(
				GeneratedPackageNamer.modulePackage(listOf("com.acme.app.dto", "com.acme.app.api")),
			).isEqualTo("com.acme.app.ghaylan.validata")
		}
		
		@Test
		@DisplayName("typePackage appends the library segment")
		fun typePackage() {
			assertThat(GeneratedPackageNamer.typePackage("com.acme.dto", "fallback")).isEqualTo("com.acme.dto.ghaylan.validata")
			assertThat(GeneratedPackageNamer.typePackage("", "io.fallback")).isEqualTo("io.fallback")
		}
		
		@Test
		@DisplayName("empty or disjoint packages fall back to the processor default")
		fun fallback() {
			assertThat(GeneratedPackageNamer.modulePackage(emptyList())).isEqualTo(ProcessorFqns.GENERATED_FALLBACK_PACKAGE)
			assertThat(GeneratedPackageNamer.modulePackage(listOf("com.acme", "org.other"))).isEqualTo(ProcessorFqns.GENERATED_FALLBACK_PACKAGE)
		}
		
		@Test
		@DisplayName("a single package becomes that package plus the library segment")
		fun singlePackage() {
			assertThat(GeneratedPackageNamer.modulePackage(listOf("com.acme.app"))).isEqualTo("com.acme.app.ghaylan.validata")
		}
		
		@Test
		@DisplayName("identical packages still share that package as the module prefix")
		fun identicalPackages() {
			assertThat(
				GeneratedPackageNamer.modulePackage(listOf("com.acme.dto", "com.acme.dto")),
			).isEqualTo("com.acme.dto.ghaylan.validata")
		}
	}
	
	@Nested
	@DisplayName("GeneratedNames")
	inner class Names {
		
		@Test
		@DisplayName("schema and fields names are readable for nested types")
		fun schemaAndFieldsNames() {
			val nested = SchemaModel(
				packageName = "com.acme",
				simpleName = "Inner",
				qualifiedName = "com.acme.Outer.Inner",
				properties = emptyList(),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			assertThat(GeneratedNames.schemaObjectName(nested.packageName, nested.qualifiedName)).isEqualTo("Outer_InnerSchema")
			assertThat(GeneratedNames.fieldsObjectName(nested.packageName, nested.qualifiedName)).isEqualTo("Outer_Inner_")
		}
		
		@Test
		@DisplayName("endpoint base name fingerprints overloads with parameters")
		fun endpointBaseNameWithParams() {
			val model = EndpointModel(
				identifier = "com.acme.UserController#create(java.lang.String,int)",
				packageName = "com.acme",
				functionQualifiedName = "com.acme.UserController.create",
				sourceFilePath = null,
				oneErrorPerParam = false,
				failFast = false,
				groupsFqcn = emptyList(),
				parameters = emptyList(),
			)
			val base = GeneratedNames.endpointBaseName(model.functionQualifiedName, model.identifier)
			assertThat(base).startsWith("UserController_create_")
			assertThat(base.length).isGreaterThan("UserController_create_".length)
			assertThat(GeneratedNames.endpointFactoryName(model.functionQualifiedName, model.identifier)).startsWith("buildUserController_create_")
		}
		
		@Test
		@DisplayName("endpoint base name has no fingerprint when signature has no params")
		fun endpointBaseNameNoParams() {
			val model = EndpointModel(
				identifier = "com.acme.HealthController#ping()",
				packageName = "com.acme",
				functionQualifiedName = "com.acme.HealthController.ping",
				sourceFilePath = null,
				oneErrorPerParam = false,
				failFast = false,
				groupsFqcn = emptyList(),
				parameters = emptyList(),
			)
			assertThat(GeneratedNames.endpointBaseName(model.functionQualifiedName, model.identifier)).isEqualTo("HealthController_ping")
		}

		@Test
		@DisplayName("endpoint path-constants object is base name plus trailing underscore")
		fun endpointPathConstantsObjectName() {
			val withParams = EndpointModel(
				identifier = "com.acme.UserController#lookup(java.lang.String)",
				packageName = "com.acme",
				functionQualifiedName = "com.acme.UserController.lookup",
				sourceFilePath = null,
				oneErrorPerParam = false,
				failFast = false,
				groupsFqcn = emptyList(),
				parameters = emptyList(),
			)
			val name = GeneratedNames.endpointPathConstantsObjectName(
				withParams.functionQualifiedName,
				withParams.identifier,
			)
			assertThat(name).startsWith("UserController_lookup_")
			assertThat(name).endsWith("_")
			assertThat(name).isEqualTo(
				GeneratedNames.endpointBaseName(withParams.functionQualifiedName, withParams.identifier) + "_",
			)
		}
		
		@Test
		@DisplayName("shortFingerprint is stable and zero-padded")
		fun shortFingerprintStable() {
			assertThat(GeneratedNames.shortFingerprint("a")).isEqualTo(GeneratedNames.shortFingerprint("a"))
			assertThat(GeneratedNames.shortFingerprint("a")).hasSize(8)
			assertThat(GeneratedNames.shortFingerprint("a")).isNotEqualTo(GeneratedNames.shortFingerprint("b"))
		}
		
		@Test
		@DisplayName("catalog helper names use annotation simple name")
		fun catalogNames() {
			assertThat(GeneratedNames.catalogEntriesFunctionName("Required")).isEqualTo("RequiredConstraintEntries")
			assertThat(GeneratedNames.catalogEntriesFileName("Required")).isEqualTo("RequiredConstraintEntries")
		}
	}
}

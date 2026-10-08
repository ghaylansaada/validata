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

import io.ghaylan.validata.processor.model.ObjectRefShapeModel
import io.ghaylan.validata.processor.model.PropertyModel
import io.ghaylan.validata.processor.model.ScalarShapeModel
import io.ghaylan.validata.processor.model.SchemaModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [SchemaCodeWriter] emission shape (no KSP round-trip).
 *
 * Locks factory/aggregator structure and the hybrid peer / GeneratedSchemaLookup emission policy.
 * 
 * @author Ghaylan Saada
 */
class SchemaCodeWriterTest {
	
	@Test
	@DisplayName("writeSchema emits ObjectSchema factory with type and properties")
	fun writeSchemaFactory() {
		val model = SchemaModel(
			packageName = "com.acme",
			simpleName = "User",
			qualifiedName = "com.acme.User",
			properties = listOf(
				PropertyModel(
					declaredName = "email",
					externalName = "email",
					readerExpr = "{ (it as com.acme.User).email }",
					shape = ScalarShapeModel(ScalarKind.STRING),
					constraints = emptyList(),
					noCascade = false,
				),
			),
			isPolymorphicRoot = false,
			subtypeQualifiedNames = emptyList(),
		)
		val source = SchemaCodeWriter.writeSchema(
			packageName = "com.acme.ghaylan.validata",
			model = model,
			schemasByQualifiedName = mapOf(model.qualifiedName to model),
			moduleFallback = "com.acme.ghaylan.validata",
		)
		assertThat(source).contains("object ${GeneratedNames.schemaObjectName(model.packageName, model.qualifiedName)}")
		assertThat(source).contains("val schema: ObjectSchema by lazy")
		assertThat(source).contains("fun build(): ObjectSchema = schema")
		assertThat(source).contains("import com.acme.User")
		assertThat(source).contains("type = User::class.java")
		assertThat(source).contains("Generated ObjectSchema factory for [User].")
		assertThat(source).contains("declaredName = \"email\"")
		assertThat(source).contains("read = { (it as User).email }")
		assertThat(source).doesNotContain("ValueReader")
		assertThat(source).doesNotContain("import io.ghaylan.validata.schema.ValueReader")
	}
	
	@Test
	@DisplayName("in-module object peer uses PeerSchema.build(); never empty ObjectSchema")
	fun inModulePeerUsesFactoryBuild() {
		val address = SchemaModel(
			packageName = "com.acme",
			simpleName = "Address",
			qualifiedName = "com.acme.Address",
			properties = emptyList(),
			isPolymorphicRoot = false,
			subtypeQualifiedNames = emptyList(),
		)
		val user = SchemaModel(
			packageName = "com.acme",
			simpleName = "User",
			qualifiedName = "com.acme.User",
			properties = listOf(
				PropertyModel(
					declaredName = "address",
					externalName = "address",
					readerExpr = "{ (it as com.acme.User).address }",
					shape = ObjectRefShapeModel("com.acme.Address"),
					constraints = emptyList(),
					noCascade = false,
				),
			),
			isPolymorphicRoot = false,
			subtypeQualifiedNames = emptyList(),
		)
		val byQualified = mapOf(
			user.qualifiedName to user,
			address.qualifiedName to address,
		)
		val source = SchemaCodeWriter.writeSchema(
			packageName = "com.acme.ghaylan.validata",
			model = user,
			schemasByQualifiedName = byQualified,
			moduleFallback = "com.acme.ghaylan.validata",
		)
		assertThat(source).contains("AddressSchema.build()")
		assertThat(source).doesNotContain("GeneratedSchemaLookup.requireGeneratedSchema")
		assertThat(source).doesNotContain("ObjectSchema(type = com.acme.Address::class.java, properties = emptyList())")
	}
	
	@Test
	@DisplayName("missing object peer uses GeneratedSchemaLookup — never empty ObjectSchema")
	fun missingPeerUsesGeneratedSchemaLookup() {
		val user = SchemaModel(
			packageName = "com.acme",
			simpleName = "User",
			qualifiedName = "com.acme.User",
			properties = listOf(
				PropertyModel(
					declaredName = "address",
					externalName = "home\$address",
					readerExpr = "{ (it as com.acme.User).address }",
					shape = ObjectRefShapeModel("com.other.Address"),
					constraints = emptyList(),
					noCascade = false,
				),
			),
			isPolymorphicRoot = false,
			subtypeQualifiedNames = emptyList(),
		)
		val source = SchemaCodeWriter.writeSchema(
			packageName = "com.acme.ghaylan.validata",
			model = user,
			schemasByQualifiedName = mapOf(user.qualifiedName to user),
			moduleFallback = "com.acme.ghaylan.validata",
		)
		assertThat(source).contains("GeneratedSchemaLookup.requireGeneratedSchema(Address::class.java)")
		assertThat(source).contains("import com.other.Address")
		assertThat(source).contains("import ${io.ghaylan.validata.processor.fqns.CodegenFqns.GENERATED_SCHEMA_LOOKUP}")
		assertThat(source).contains("externalName = \"home\\\$address\"")
		assertThat(source).doesNotContain("ObjectSchema(type = com.other.Address::class.java, properties = emptyList())")
	}
	
	@Test
	@DisplayName("missing polymorphic subtype uses GeneratedSchemaLookup — never empty ObjectSchema")
	fun missingSubtypeUsesGeneratedSchemaLookup() {
		val root = SchemaModel(
			packageName = "com.acme",
			simpleName = "Payload",
			qualifiedName = "com.acme.Payload",
			properties = emptyList(),
			isPolymorphicRoot = true,
			subtypeQualifiedNames = listOf("com.other.ProfilePayload"),
		)
		val source = SchemaCodeWriter.writeSchema(
			packageName = "com.acme.ghaylan.validata",
			model = root,
			schemasByQualifiedName = mapOf(root.qualifiedName to root),
			moduleFallback = "com.acme.ghaylan.validata",
		)
		assertThat(source).contains(
			"GeneratedSchemaLookup.requireGeneratedSchema(ProfilePayload::class.java)",
		)
		assertThat(source).contains("import com.other.ProfilePayload")
		assertThat(source).doesNotContain(
			"ObjectSchema(type = com.other.ProfilePayload::class.java, properties = emptyList())",
		)
	}
	
	@Test
	@DisplayName("writeAggregator emits ObjectSchemaModule mapping every factory")
	fun writeAggregator() {
		val model = SchemaModel(
			packageName = "com.acme",
			simpleName = "User",
			qualifiedName = "com.acme.User",
			properties = emptyList(),
			isPolymorphicRoot = false,
			subtypeQualifiedNames = emptyList(),
		)
		val source = SchemaCodeWriter.writeAggregator(
			packageName = "com.acme.ghaylan.validata",
			models = listOf(model),
			moduleFallback = "com.acme.ghaylan.validata",
		)
		assertThat(source).contains("class ${GeneratedNames.OBJECT_SCHEMAS_MODULE} : ObjectSchemaModule")
		assertThat(source).contains("import com.acme.User")
		assertThat(source).contains("User::class.java to UserSchema.build()")
	}
}

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

import io.ghaylan.validata.processor.integration.NestedPropertyReferenceProcessorTest
import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.schema.PropertyPath
import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [FieldsObjectCodeWriter] naming rules and emitted source shape.
 *
 * KSP round-trips live in [NestedPropertyReferenceProcessorTest]; this class exercises the writer
 * with hand-built [SchemaModel]s (no compiler).
 * 
 * @author Ghaylan Saada

 */
class FieldsObjectCodeWriterTest {

	@Nested
	@DisplayName("Naming helpers")
	inner class Naming {

		@Test
		@DisplayName("constName camelCase → SCREAMING_SNAKE")
		fun constNameCamelCase() {
			assertThat(FieldsObjectCodeWriter.constName("minAge")).isEqualTo("MIN_AGE")
			assertThat(FieldsObjectCodeWriter.constName("firstName")).isEqualTo("FIRST_NAME")
			assertThat(FieldsObjectCodeWriter.constName("URL")).isEqualTo("URL")
			assertThat(FieldsObjectCodeWriter.constName("b")).isEqualTo("B")
		}

		@Test
		@DisplayName("uniqueConstName disambiguates colliding SCREAMING_SNAKE names")
		fun uniqueConstNameCollision() {
			val used = mutableSetOf<String>()
			assertThat(FieldsObjectCodeWriter.uniqueConstName("minAge", used)).isEqualTo("MIN_AGE")
			assertThat(FieldsObjectCodeWriter.uniqueConstName("min_age", used)).isEqualTo("MIN_AGE_2")
			assertThat(FieldsObjectCodeWriter.uniqueConstName("url", used)).isEqualTo("URL")
			assertThat(FieldsObjectCodeWriter.uniqueConstName("URL", used)).isEqualTo("URL_2")
		}

		@Test
		@DisplayName("nestedObjectName titles the property unless it collides with the const")
		fun nestedObjectNameCollision() {
			assertThat(FieldsObjectCodeWriter.nestedObjectName("address")).isEqualTo("Address")
			assertThat(FieldsObjectCodeWriter.nestedObjectName("b")).isEqualTo("b")
		}

		@Test
		@DisplayName("packageNameFor appends .ghaylan.validata or uses fallback when blank")
		fun packageNameFor() {
			val withPkg = schema("com.acme", "User", "com.acme.User")
			assertThat(FieldsObjectCodeWriter.packageNameFor(withPkg, "fallback"))
				.isEqualTo("com.acme.ghaylan.validata")

			val blankPkg = schema("", "User", "User")
			assertThat(FieldsObjectCodeWriter.packageNameFor(blankPkg, "io.fallback.ghaylan.validata"))
				.isEqualTo("io.fallback.ghaylan.validata")
		}

		@Test
		@DisplayName("objectName / fileName stay unique for nested types")
		fun objectAndFileNames() {
			val nested = schema("com.acme", "Inner", "com.acme.Outer.Inner")
			assertThat(FieldsObjectCodeWriter.objectName(nested)).isEqualTo("Outer_Inner_")
			assertThat(FieldsObjectCodeWriter.fileName(nested)).isEqualTo("Outer_Inner_")

			val top = schema("com.acme", "User", "com.acme.User")
			assertThat(FieldsObjectCodeWriter.objectName(top)).isEqualTo("User_")
			assertThat(FieldsObjectCodeWriter.fileName(top)).isEqualTo("User_")
		}
	}

	@Nested
	@DisplayName("write()")
	inner class Write {

		@Test
		@DisplayName("emits flat and nested path constants")
		fun nestedPaths() {
			val address = schema(
				packageName = "sample",
				simpleName = "Address",
				qualifiedName = "sample.Address",
				properties = listOf(prop("city", ScalarShapeModel(ScalarKind.STRING))),
			)
			val user = schema(
				packageName = "sample",
				simpleName = "UserRequest",
				qualifiedName = "sample.UserRequest",
				properties = listOf(
					prop("minAge", ScalarShapeModel(ScalarKind.INTEGRAL)),
					prop("address", ObjectRefShapeModel("sample.Address")),
				),
			)

			val src = FieldsObjectCodeWriter.write(
				packageName = "sample.ghaylan.validata",
				model = user,
				schemasByQualifiedName = mapOf(
					user.qualifiedName to user,
					address.qualifiedName to address,
				),
			)

			assertThat(src).contains("package sample.ghaylan.validata")
			assertThat(src).contains("import sample.UserRequest")
			assertThat(src).contains("import io.ghaylan.validata.schema.PropertySpec")
			assertThat(src).contains("import sample.Address")
			assertThat(src).contains("object UserRequest_")
			assertThat(src).contains(" * Generated wire-path constants for [UserRequest].")
			assertThat(src).doesNotContain("JPA-style")
			assertThat(src).doesNotContain("[sample.UserRequest]")
			assertThat(src).contains("const val MIN_AGE: String = \"minAge\"")
			assertThat(src).contains("const val ADDRESS: String = \"address\"")
			assertThat(src).contains("object Address")
			assertThat(src).contains("const val CITY: String = \"address.city\"")
			assertThat(src).contains("([Address])")
		}

		@Test
		@DisplayName("const values use externalName wire segments")
		fun externalNamePaths() {
			val user = schema(
				packageName = "sample",
				simpleName = "UserRequest",
				qualifiedName = "sample.UserRequest",
				properties = listOf(
					prop("minAge", ScalarShapeModel(ScalarKind.INTEGRAL), externalName = "min_age"),
					prop("confirmSecret", ScalarShapeModel(ScalarKind.STRING), externalName = "confirm_secret"),
				),
			)
			val src = FieldsObjectCodeWriter.write(
				packageName = "sample.ghaylan.validata",
				model = user,
				schemasByQualifiedName = mapOf(user.qualifiedName to user),
			)
			assertThat(src).contains("object UserRequest_")
			assertThat(src).contains("const val MIN_AGE: String = \"min_age\"")
			assertThat(src).contains("const val CONFIRM_SECRET: String = \"confirm_secret\"")
		}

		@Test
		@DisplayName("polymorphic roots with no properties emit a comment only")
		fun polymorphicEmpty() {
			val root = SchemaModel(
				packageName = "sample",
				simpleName = "Animal",
				qualifiedName = "sample.Animal",
				properties = emptyList(),
				isPolymorphicRoot = true,
				subtypeQualifiedNames = listOf("sample.Cat"),
			)
			val src = FieldsObjectCodeWriter.write("sample.ghaylan.validata", root, mapOf(root.qualifiedName to root))
			assertThat(src).contains("object Animal_")
			assertThat(src).contains("Polymorphic root")
			assertThat(src).doesNotContain("const val")
		}

		@Test
		@DisplayName("cycles on the current path are omitted with a comment")
		fun cycleOmitted() {
			val a = schema(
				packageName = "sample",
				simpleName = "NodeA",
				qualifiedName = "sample.NodeA",
				properties = listOf(prop("b", ObjectRefShapeModel("sample.NodeB"))),
			)
			val b = schema(
				packageName = "sample",
				simpleName = "NodeB",
				qualifiedName = "sample.NodeB",
				properties = listOf(prop("a", ObjectRefShapeModel("sample.NodeA"))),
			)
			val src = FieldsObjectCodeWriter.write(
				"sample.ghaylan.validata",
				a,
				mapOf(a.qualifiedName to a, b.qualifiedName to b),
			)
			// Property `b` collides with const `B`, so the nested object keeps camelCase `b`.
			assertThat(src).contains("object b {")
			assertThat(src).containsIgnoringCase("cycle")
		}

		@Test
		@DisplayName("nesting stops at PropertyPath.MAX_REFERENCE_PATH_DEPTH")
		fun maxDepth() {
			val schemas = LinkedHashMap<String, SchemaModel>()
			// Build N1 → N2 → … → N(MAX+1) so the last object-ref is encountered at depth == MAX.
			var childFqcn: String? = null
			for (depth in (PropertyPath.MAX_REFERENCE_PATH_DEPTH + 1) downTo 1) {
				val fqcn = "sample.N$depth"
				val props = if (childFqcn == null) {
					listOf(prop("leaf", ScalarShapeModel(ScalarKind.STRING)))
				} else {
					listOf(prop("next", ObjectRefShapeModel(childFqcn)))
				}
				schemas[fqcn] = schema("sample", "N$depth", fqcn, props)
				childFqcn = fqcn
			}
			val root = schemas.getValue("sample.N1")
			val src = FieldsObjectCodeWriter.write("sample.ghaylan.validata", root, schemas)
			assertThat(src).contains("max reference depth ${PropertyPath.MAX_REFERENCE_PATH_DEPTH}")
		}
	}

	private fun schema(
		packageName: String,
		simpleName: String,
		qualifiedName: String,
		properties: List<PropertyModel> = emptyList(),
	): SchemaModel = SchemaModel(
		packageName = packageName,
		simpleName = simpleName,
		qualifiedName = qualifiedName,
		properties = properties,
		isPolymorphicRoot = false,
		subtypeQualifiedNames = emptyList(),
	)

	private fun prop(
		declaredName: String,
		shape: ShapeModel,
		externalName: String = declaredName,
	): PropertyModel =
		PropertyModel(
			declaredName = declaredName,
			externalName = externalName,
			readerExpr = "{ it }",
			shape = shape,
			constraints = emptyList(),
			noCascade = false,
		)
}

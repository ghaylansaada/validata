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
package io.ghaylan.validata.processor.verify

import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.processor.support.RecordingKspLogger
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.request.EndpointArgumentKind
import io.ghaylan.validata.schema.shape.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit coverage for [PropertyReferenceVerifier] against hand-built [SchemaModel] graphs.
 *
 * Nested suites mirror the verifier’s three entry paths: schema sibling paths, element
 * (`@Distinct`) refs, and depth / diagnostic-site guards.
 * 
 * @author Ghaylan Saada
 */
class PropertyReferenceVerifierTest {
	
	private lateinit var logger: RecordingKspLogger
	private lateinit var verifier: PropertyReferenceVerifier
	
	@BeforeEach
	fun setUp() {
		logger = RecordingKspLogger()
		verifier = PropertyReferenceVerifier(logger)
	}
	
	@Nested
	@DisplayName("Schema sibling paths")
	inner class SchemaPaths {
		
		@Test
		@DisplayName("unknown sibling path fails with known-property list")
		fun unknownSiblingPath() {
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "User",
				qualifiedName = "t.User",
				properties = listOf(
					prop("email", ScalarShapeModel(ScalarKind.STRING), sibling = "noSuchField"),
					prop("password", ScalarShapeModel(ScalarKind.STRING)),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors.joinToString()).contains("unknown property 'noSuchField'")
		}
		
		@Test
		@DisplayName("nested sibling paths are rejected")
		fun nestedSiblingPathRejected() {
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "Root",
				qualifiedName = "t.Root",
				properties = listOf(
					prop("a", ScalarShapeModel(ScalarKind.INTEGRAL), sibling = "address.city"),
					prop("address", ScalarShapeModel(ScalarKind.OTHER)),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors.joinToString()).contains("nested path")
		}
		
		@Test
		@DisplayName("self-reference is a hard error")
		fun selfReferenceErrors() {
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "Root",
				qualifiedName = "t.Root",
				properties = listOf(prop("age", ScalarShapeModel(ScalarKind.INTEGRAL), sibling = "age")),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors.joinToString()).contains("cannot reference itself")
				.contains("different sibling property")
			assertThat(logger.warnings).isEmpty()
		}
		
		@Test
		@DisplayName("wide DTO with many sibling refs resolves via property name index")
		fun wideDtoSiblingRefs() {
			val props = (1..40).map { i ->
				prop("f$i", ScalarShapeModel(ScalarKind.STRING))
			} + listOf(
				prop("anchor", ScalarShapeModel(ScalarKind.STRING), sibling = "f20"),
			)
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "Wide",
				qualifiedName = "t.Wide",
				properties = props,
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors).isEmpty()
		}
	}
	
	@Nested
	@DisplayName("Element (@Distinct) refs")
	inner class ElementRefs {
		
		@Test
		@DisplayName("Distinct on List property is rejected (must be element type-use)")
		fun distinctOnListPropertyRejected() {
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "Root",
				qualifiedName = "t.Root",
				properties = listOf(
					PropertyModel(
						declaredName = "tags",
						externalName = "tags",
						readerExpr = "{ null }",
						shape = IterableShapeModel(element = ScalarShapeModel(ScalarKind.STRING)),
						constraints = listOf(
							ConstraintModel(
								metadataConstructorCall = "DistinctConstraint()",
								validatorExpression = "t.DistinctValidator",
								order = 0,
								annotationSimpleName = "Distinct",
							),
						),
						noCascade = false,
					),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors.joinToString()).contains("must be type-use on the collection element")
		}
		
		@Test
		@DisplayName("element refs on scalar type-use fail with precise diagnostic")
		fun elementRefsOnScalarTypeUse() {
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "Root",
				qualifiedName = "t.Root",
				properties = listOf(
					PropertyModel(
						declaredName = "tags",
						externalName = "tags",
						readerExpr = "{ null }",
						shape = IterableShapeModel(
							element = ScalarShapeModel(
								kind = ScalarKind.STRING,
								constraints = listOf(
									ConstraintModel(
										metadataConstructorCall = "DistinctConstraint()",
										validatorExpression = "t.DistinctValidator",
										order = 0,
										elementRefs = listOf("name"),
										annotationSimpleName = "Distinct",
									),
								),
							),
						),
						constraints = emptyList(),
						noCascade = false,
					),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors.joinToString()).contains("not an object element type-use")
		}
		
		@Test
		@DisplayName("nested element paths on Distinct are rejected")
		fun nestedElementPathRejected() {
			val address = SchemaModel(
				packageName = "t",
				simpleName = "Address",
				qualifiedName = "t.Address",
				properties = listOf(prop("city", ScalarShapeModel(ScalarKind.STRING))),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			val user = SchemaModel(
				packageName = "t",
				simpleName = "User",
				qualifiedName = "t.User",
				properties = listOf(
					PropertyModel(
						declaredName = "addresses",
						externalName = "addresses",
						readerExpr = "{ null }",
						shape = IterableShapeModel(
							element = ObjectRefShapeModel(
								typeQualifiedName = "t.Address",
								constraints = listOf(
									ConstraintModel(
										metadataConstructorCall = "DistinctConstraint()",
										validatorExpression = "t.DistinctValidator",
										order = 0,
										elementRefs = listOf("address.city"),
										annotationSimpleName = "Distinct",
									),
								),
							),
						),
						constraints = emptyList(),
						noCascade = false,
					),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(user, mapOf(user.qualifiedName to user, address.qualifiedName to address))
			assertThat(logger.errors.joinToString()).contains("element path")
		}
		
		@Test
		@DisplayName("type-use Distinct(by) on list element DTO resolves against that DTO")
		fun typeUseDistinctByOnObjectElementOk() {
			val user = SchemaModel(
				packageName = "t",
				simpleName = "User",
				qualifiedName = "t.User",
				properties = listOf(prop("id", ScalarShapeModel(ScalarKind.STRING))),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			val batch = SchemaModel(
				packageName = "t",
				simpleName = "Batch",
				qualifiedName = "t.Batch",
				properties = listOf(
					PropertyModel(
						declaredName = "users",
						externalName = "users",
						readerExpr = "{ null }",
						shape = IterableShapeModel(
							element = ObjectRefShapeModel(
								typeQualifiedName = "t.User",
								constraints = listOf(
									ConstraintModel(
										metadataConstructorCall = "DistinctConstraint()",
										validatorExpression = "t.DistinctValidator",
										order = 0,
										elementRefs = listOf("id"),
										annotationSimpleName = "Distinct",
									),
								),
							),
						),
						constraints = emptyList(),
						noCascade = false,
					),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(batch, mapOf(batch.qualifiedName to batch, user.qualifiedName to user))
			assertThat(logger.errors).isEmpty()
		}
		
		@Test
		@DisplayName("type-use Distinct(by) on list element DTO rejects unknown field")
		fun typeUseDistinctByOnObjectElementUnknownField() {
			val user = SchemaModel(
				packageName = "t",
				simpleName = "User",
				qualifiedName = "t.User",
				properties = listOf(prop("id", ScalarShapeModel(ScalarKind.STRING))),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			val batch = SchemaModel(
				packageName = "t",
				simpleName = "Batch",
				qualifiedName = "t.Batch",
				properties = listOf(
					PropertyModel(
						declaredName = "users",
						externalName = "users",
						readerExpr = "{ null }",
						shape = IterableShapeModel(
							element = ObjectRefShapeModel(
								typeQualifiedName = "t.User",
								constraints = listOf(
									ConstraintModel(
										metadataConstructorCall = "DistinctConstraint()",
										validatorExpression = "t.DistinctValidator",
										order = 0,
										elementRefs = listOf("noSuch"),
										annotationSimpleName = "Distinct",
									),
								),
							),
						),
						constraints = emptyList(),
						noCascade = false,
					),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(batch, mapOf(batch.qualifiedName to batch, user.qualifiedName to user))
			assertThat(logger.errors.joinToString()).contains("noSuch")
		}
	}
	
	@Nested
	@DisplayName("Endpoint flat refs")
	inner class EndpointFlatRefs {
		
		@Test
		@DisplayName("unknown flat sibling parameter is an error")
		fun unknownFlatSibling() {
			val endpoint = EndpointModel(
				identifier = "C#m()",
				packageName = "t",
				functionQualifiedName = "t.C.m",
				sourceFilePath = null,
				oneErrorPerParam = true,
				failFast = false,
				groupsFqcn = emptyList(),
				parameters = listOf(
					EndpointParameterModel(
						kind = EndpointArgumentKind.QUERY,
						declaredName = "from",
						resolvedName = "from",
						shape = ScalarShapeModel(ScalarKind.STRING),
						constraints = listOf(
							ConstraintModel(
								metadataConstructorCall = "CompareConstraint()",
								validatorExpression = "t.CompareValidator",
								order = 0,
								siblingRefs = listOf("missing"),
								compatibilityKind = PropertyRefCompatibilityKind.NONE,
								annotationSimpleName = "Compare",
							),
						),
					),
					EndpointParameterModel(
						kind = EndpointArgumentKind.QUERY,
						declaredName = "to",
						resolvedName = "to",
						shape = ScalarShapeModel(ScalarKind.STRING),
					),
				),
			)
			verifier.verifyEndpoint(endpoint, emptyMap())
			assertThat(logger.errors.joinToString()).contains("unknown sibling 'missing'")
		}
		
		@Test
		@DisplayName("nested path on flat endpoint params is rejected")
		fun nestedFlatPathRejected() {
			val endpoint = EndpointModel(
				identifier = "C#m()",
				packageName = "t",
				functionQualifiedName = "t.C.m",
				sourceFilePath = null,
				oneErrorPerParam = true,
				failFast = false,
				groupsFqcn = emptyList(),
				parameters = listOf(
					EndpointParameterModel(
						kind = EndpointArgumentKind.QUERY,
						declaredName = "a",
						resolvedName = "a",
						shape = ScalarShapeModel(ScalarKind.STRING),
						constraints = listOf(
							ConstraintModel(
								metadataConstructorCall = "CompareConstraint()",
								validatorExpression = "t.CompareValidator",
								order = 0,
								siblingRefs = listOf("b.c"),
								compatibilityKind = PropertyRefCompatibilityKind.NONE,
								annotationSimpleName = "Compare",
							),
						),
					),
					EndpointParameterModel(
						kind = EndpointArgumentKind.QUERY,
						declaredName = "b",
						resolvedName = "b",
						shape = ScalarShapeModel(ScalarKind.STRING),
					),
				),
			)
			verifier.verifyEndpoint(endpoint, emptyMap())
			assertThat(logger.errors.joinToString()).contains("flat query/header/path")
		}
	}
	
	@Nested
	@DisplayName("Depth guards and diagnostic sites")
	inner class DepthAndSites {
		
		@Test
		@DisplayName("shape nesting beyond max depth emits an error instead of overflowing")
		fun shapeNestingDepthExceeded() {
			val deep = (1..4).fold(ScalarShapeModel(ScalarKind.STRING) as ShapeModel) { acc, _ ->
				IterableShapeModel(element = acc)
			}
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "Root",
				qualifiedName = "t.Root",
				properties = listOf(
					PropertyModel(
						declaredName = "nested",
						externalName = "nested",
						readerExpr = "{ null }",
						shape = deep,
						constraints = emptyList(),
						noCascade = false,
					),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			val shallow = PropertyReferenceVerifier(logger, maxShapeNestingDepth = 2)
			shallow.verify(schema, mapOf(schema.qualifiedName to schema))
			assertThat(logger.errors.joinToString()).contains("exceeds max depth 2")
		}
		
		@Test
		@DisplayName("verify attaches the provided diagnostic site to errors")
		fun diagnosticSiteAttached() {
			val site = object: com.google.devtools.ksp.symbol.KSNode {
				override val location = com.google.devtools.ksp.symbol.NonExistLocation
				override val origin = com.google.devtools.ksp.symbol.Origin.KOTLIN
				override val parent: com.google.devtools.ksp.symbol.KSNode? = null
				override fun <D, R> accept(
					visitor: com.google.devtools.ksp.symbol.KSVisitor<D, R>,
					data: D,
				): R = error("not used")
			}
			val schema = SchemaModel(
				packageName = "t",
				simpleName = "User",
				qualifiedName = "t.User",
				properties = listOf(
					prop("email", ScalarShapeModel(ScalarKind.STRING), sibling = "noSuchField"),
					prop("password", ScalarShapeModel(ScalarKind.STRING)),
				),
				isPolymorphicRoot = false,
				subtypeQualifiedNames = emptyList(),
			)
			verifier.verify(schema, mapOf(schema.qualifiedName to schema), site = site)
			assertThat(logger.errorSymbols).isNotEmpty()
			assertThat(logger.errorSymbols).contains(site)
		}
	}
	
	private fun prop(
		name: String,
		shape: ShapeModel,
		sibling: String? = null,
		noCascade: Boolean = false,
	): PropertyModel = PropertyModel(
		declaredName = name,
		externalName = name,
		readerExpr = "{ null }",
		shape = shape,
		constraints = if (sibling == null) {
			emptyList()
		}
		else {
			listOf(
				ConstraintModel(
					metadataConstructorCall = "CompareConstraint()",
					validatorExpression = "t.CompareValidator",
					order = 0,
					siblingRefs = listOf(sibling),
					compatibilityKind = PropertyRefCompatibilityKind.NONE,
					annotationSimpleName = "Compare",
				),
			)
		},
		noCascade = noCascade,
	)
}

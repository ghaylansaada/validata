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
package io.ghaylan.validata.schema

import io.ghaylan.validata.schema.shape.*
import io.ghaylan.validata.schema.support.dto.Address
import io.ghaylan.validata.schema.support.dto.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Shared dotted-path grammar for KSP, runtime sibling reads, and the IntelliJ plugin.
 *
 * Drift here shows up as “valid in IDE, rejected by KSP” bugs. Locks split rules, depth ceiling,
 * declared-over-external lookup, and failure modes when a path cannot be walked on the IR graph.
 *
 * Nested groups organize scenarios (split / lookup / nested reads / invalid paths).*
 * 
 * @author Ghaylan Saada
 */
class PropertyPathTest {
	
	/** User → Address.city graph used by nested / failure cases.	 */
	private fun userWithAddressSchema(): ObjectSchema {
		val addressSchema = ObjectSchema(
			type = Address::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "city",
					externalName = "city",
					shape = ScalarShape(ScalarKind.STRING),
					read = { (it as Address).city },
				),
			),
		)
		return ObjectSchema(
			type = User::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "address",
					externalName = "address",
					shape = ObjectRefShape(lazyOf(addressSchema)),
					read = { (it as User).address },
				),
				PropertySpec(
					declaredName = "name",
					externalName = "name",
					shape = ScalarShape(ScalarKind.STRING),
					read = { (it as User).name },
				),
			),
		)
	}
	
	/**
	 * Split rules and the shared depth constant.
	 */
	@Nested
	@DisplayName("Given path splitting")
	inner class PathSplitting {
		
		@Test
		@DisplayName("MAX_REFERENCE_PATH_DEPTH is 6 — must match KSP Fields nesting and IDE mirrors")
		fun depthConstantIsSix() {
			assertThat(PropertyPath.MAX_REFERENCE_PATH_DEPTH).isEqualTo(6)
		}
		
		@Test
		@DisplayName("split trims segments, drops blanks, and handles dots / whitespace")
		fun splitGrammar() {
			assertThat(PropertyPath.split("address.city")).containsExactly("address", "city")
			assertThat(PropertyPath.split("  password  ")).containsExactly("password")
			assertThat(PropertyPath.split("")).isEmpty()
			assertThat(PropertyPath.split("a..b")).containsExactly("a", "b")
			assertThat(PropertyPath.split("a . b")).containsExactly("a", "b")
			assertThat(PropertyPath.split(" .a. ")).containsExactly("a")
			assertThat(PropertyPath.split(".")).isEmpty()
			assertThat(PropertyPath.split("..")).isEmpty()
		}
	}
	
	/**
	 * Declared-over-external resolution and miss cases.
	 */
	@Nested
	@DisplayName("Given property lookup")
	inner class PropertyLookup {
		
		@Test
		@DisplayName("findProperty prefers declaredName when it conflicts with another property's externalName")
		fun findPropertyPrefersDeclaredOverExternal() {
			val schema = ObjectSchema(
				type = Address::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "city",
						externalName = "town",
						shape = ScalarShape(ScalarKind.STRING),
						read = { (it as Address).city },
					),
					PropertySpec(
						declaredName = "alias",
						externalName = "city",
						shape = ScalarShape(ScalarKind.STRING),
						read = { "alias-value" },
					),
				),
			)
			
			assertThat(PropertyPath.findProperty(schema, "city")?.declaredName).isEqualTo("city")
			assertThat(PropertyPath.findProperty(schema, "town")?.declaredName).isEqualTo("city")
			assertThat(PropertyPath.findProperty(schema, "alias")?.declaredName).isEqualTo("alias")
		}
		
		@Test
		@DisplayName("findProperty returns null when neither declared nor external name matches")
		fun findPropertyMissesUnknown() {
			val schema = userWithAddressSchema()
			assertThat(PropertyPath.findProperty(schema, "nope")).isNull()
		}
		
		@Test
		@DisplayName("findProperty / read resolve @JsonProperty-style external names")
		fun readViaExternalName() {
			val schema = ObjectSchema(
				type = Address::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "city",
						externalName = "town",
						shape = ScalarShape(ScalarKind.STRING),
						read = { (it as Address).city },
					),
				),
			)
			assertThat(PropertyPath.findProperty(schema, "town")?.declaredName).isEqualTo("city")
			assertThat(PropertyPath.read(schema, Address("Sfax"), "town")).isEqualTo("Sfax")
		}
	}
	
	/**
	 * Successful nested walks and null-root short-circuit.
	 */
	@Nested
	@DisplayName("Given nested reads")
	inner class NestedReads {
		
		@Test
		@DisplayName("read walks nested ObjectRefShape properties")
		fun readNested() {
			val userSchema = userWithAddressSchema()
			val user = User(Address("Tunis"), "Ghaylan")
			assertThat(PropertyPath.read(userSchema, user, "address.city")).isEqualTo("Tunis")
			assertThat(PropertyPath.read(userSchema, user, "name")).isEqualTo("Ghaylan")
			assertThat(PropertyPath.read(userSchema, User(null, "x"), "address.city")).isNull()
		}
		
		@Test
		@DisplayName("null rootInstance yields null without walking")
		fun nullRootReturnsNull() {
			val schema = userWithAddressSchema()
			assertThat(PropertyPath.read(schema, null, "address.city")).isNull()
			assertThat(PropertyPath.read(schema, null, "name")).isNull()
		}
	}
	
	/**
	 * Blank paths, depth ceiling, unknown segments, and scalar mid-path steps.
	 */
	@Nested
	@DisplayName("Given invalid paths")
	inner class InvalidPaths {
		
		@Test
		@DisplayName("blank path and dotted-only paths are rejected by read")
		fun blankPathRejected() {
			val schema = userWithAddressSchema()
			assertThatIllegalArgumentException().isThrownBy { PropertyPath.read(schema, User(null, "x"), "") }.withMessageContaining("must not be blank")
			assertThatIllegalArgumentException().isThrownBy { PropertyPath.read(schema, User(null, "x"), "   ") }.withMessageContaining("must not be blank")
			assertThatIllegalArgumentException().isThrownBy { PropertyPath.read(schema, User(null, "x"), ".") }.withMessageContaining("must not be blank")
			assertThatIllegalArgumentException().isThrownBy { PropertyPath.read(schema, User(null, "x"), "..") }.withMessageContaining("must not be blank")
		}
		
		@Test
		@DisplayName("unknown segment truncates known names after 20 properties")
		fun unknownSegmentTruncatesKnownNames() {
			val properties = (1..21).map { i ->
				PropertySpec(
					declaredName = "p$i",
					externalName = "p$i",
					shape = ScalarShape(ScalarKind.STRING),
					read = { null },
				)
			}
			val schema = ObjectSchema(type = Address::class.java, properties = properties)
			
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, Address("x"), "missing") }
				.withMessageContaining("Unknown property 'missing'")
				.withMessageContaining("p1")
				.withMessageContaining("p20")
				.withMessageContaining("… +1 more")
				.withMessageNotContaining("p21")
		}
		
		@Test
		@DisplayName("paths deeper than MAX_REFERENCE_PATH_DEPTH are rejected")
		fun depthCeiling() {
			val schema = userWithAddressSchema()
			val path = (1..PropertyPath.MAX_REFERENCE_PATH_DEPTH + 1).joinToString(".") { "seg$it" }
			assertThatIllegalArgumentException().isThrownBy { PropertyPath.read(schema, User(null, "x"), path) }.withMessageContaining("max depth").withMessageContaining(PropertyPath.MAX_REFERENCE_PATH_DEPTH.toString())
		}
		
		@Test
		@DisplayName("path at exactly MAX_REFERENCE_PATH_DEPTH is not rejected for depth")
		fun depthAtCeilingNotRejectedForDepth() {
			val schema = userWithAddressSchema()
			val path = (1..PropertyPath.MAX_REFERENCE_PATH_DEPTH).joinToString(".") { "seg$it" }
			assertThat(PropertyPath.split(path)).hasSize(PropertyPath.MAX_REFERENCE_PATH_DEPTH)
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, User(null, "x"), path) }.withMessageContaining("Unknown property")
		}
		
		@Test
		@DisplayName("unknown segment fails with actionable diagnostics including known keys")
		fun unknownSegment() {
			val schema = userWithAddressSchema()
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, User(Address("a"), "b"), "missing") }
				.withMessageContaining("Unknown property 'missing'")
				.withMessageContaining(User::class.java.name)
				.withMessageContaining("address")
				.withMessageContaining("name")
		}
		
		@Test
		@DisplayName("stepping into a scalar property mid-path fails")
		fun cannotStepIntoScalar() {
			val schema = userWithAddressSchema()
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, User(Address("a"), "b"), "name.child") }.withMessageContaining("Cannot step into 'name'").withMessageContaining("ScalarShape")
		}
		
		@Test
		@DisplayName("stepping into IterableShape / MapShape / DynamicShape mid-path fails")
		fun cannotStepIntoNonObjectShapes() {
			data class Bag(
				val items: List<String>,
				val map: Map<String, String>,
				val any: Any?
			)
			
			val schema = ObjectSchema(
				type = Bag::class.java,
				properties = listOf(
					PropertySpec(
						declaredName = "items",
						externalName = "items",
						shape = IterableShape(element = ScalarShape(ScalarKind.STRING)),
						read = { (it as Bag).items },
					),
					PropertySpec(
						declaredName = "map",
						externalName = "map",
						shape = MapShape(
							key = ScalarShape(ScalarKind.STRING),
							value = ScalarShape(ScalarKind.STRING),
						),
						read = { (it as Bag).map },
					),
					PropertySpec(
						declaredName = "any",
						externalName = "any",
						shape = DynamicShape(),
						read = { (it as Bag).any },
					),
				),
			)
			val bag = Bag(items = listOf("x"), map = mapOf("k" to "v"), any = "opaque")
			
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, bag, "items.0") }.withMessageContaining("Cannot step into 'items'").withMessageContaining("IterableShape")
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, bag, "map.k") }.withMessageContaining("Cannot step into 'map'").withMessageContaining("MapShape")
			assertThatIllegalStateException().isThrownBy { PropertyPath.read(schema, bag, "any.field") }.withMessageContaining("Cannot step into 'any'").withMessageContaining("DynamicShape")
		}
	}
}

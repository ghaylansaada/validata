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

package io.ghaylan.validata.intellij.contract

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Locks classpath FQCN contracts used by discovery against library marker packages.
 *
 * Annotation markers are still discovered by string FQCN against the user project classpath;
 * shared schema enums / helpers come from the plugin’s `validata-schema` dependency.
 * 
 * @author Ghaylan Saada
 */
class PropertyRefLibraryFqnsTest {
	
	@Test
	@DisplayName("Constraint marker FQCNs live under io.ghaylan.validata.constraint")
	fun constraintMarkersLiveUnderConstraintPackage() {
		assertThat(PropertyRefLibraryFqns.CONSTRAINT).startsWith("io.ghaylan.validata.constraint.")
		assertThat(PropertyRefLibraryFqns.CONSTRAINT_VALIDATOR).startsWith("io.ghaylan.validata.constraint.")
		assertThat(PropertyRefLibraryFqns.PROPERTY_REF).isEqualTo("io.ghaylan.validata.constraint.PropertyRef")
		assertThat(PropertyRefLibraryFqns.PROPERTY_REF_SCOPE).isEqualTo("io.ghaylan.validata.schema.ref.PropertyRefScope")
		assertThat(PropertyRefLibraryFqns.CONSTRAINT_ARG).isEqualTo("io.ghaylan.validata.constraint.ConstraintArg")
		assertThat(PropertyRefLibraryFqns.CONSTRAINT_ARGS).isEqualTo("io.ghaylan.validata.constraint.ConstraintArgs")
		assertThat(PropertyRefLibraryFqns.REQUIRED_WHEN).isEqualTo("io.ghaylan.validata.constraint.annotation.RequiredWhen")
		assertThat(PropertyRefLibraryFqns.REQUIRED).isEqualTo("io.ghaylan.validata.constraint.annotation.Required")
		assertThat(PropertyRefLibraryFqns.CONSTRAINT_COMPOSITION).isEqualTo("io.ghaylan.validata.constraint.ConstraintComposition")
		assertThat(PropertyRefLibraryFqns.COMPOSITION_MODE).isEqualTo("io.ghaylan.validata.constraint.ConstraintComposition.Mode")
	}
	
	@Test
	@DisplayName("Schema-ref FQCNs live under io.ghaylan.validata.schema.ref")
	fun schemaRefMirrorsLiveUnderSchemaRefPackage() {
		assertThat(PropertyRefLibraryFqns.COMPATIBILITY_KIND).isEqualTo("io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind")
		assertThat(PropertyRefLibraryFqns.CONSTRAINT_ARG_KIND).isEqualTo("io.ghaylan.validata.schema.ref.ConstraintArgKind")
		assertThat(PropertyRefLibraryFqns.CONSTRAINT_ARG_TARGET).isEqualTo("io.ghaylan.validata.schema.ref.ConstraintArgTarget")
		assertThat(PropertyRefLibraryFqns.VALIDATABLE).isEqualTo("io.ghaylan.validata.schema.Validatable")
	}
	
	@Test
	@DisplayName("Library FQCNs never point at the Spring host module")
	fun fqcnsNeverPointAtSpringHostModule() {
		val all = listOf(
			PropertyRefLibraryFqns.CONSTRAINT,
			PropertyRefLibraryFqns.CONSTRAINT_VALIDATOR,
			PropertyRefLibraryFqns.PROPERTY_REF,
			PropertyRefLibraryFqns.PROPERTY_REF_SCOPE,
			PropertyRefLibraryFqns.CONSTRAINT_ARG,
			PropertyRefLibraryFqns.CONSTRAINT_ARGS,
			PropertyRefLibraryFqns.COMPATIBILITY_KIND,
			PropertyRefLibraryFqns.CONSTRAINT_ARG_KIND,
			PropertyRefLibraryFqns.CONSTRAINT_ARG_TARGET,
			PropertyRefLibraryFqns.VALIDATABLE,
			PropertyRefLibraryFqns.REQUIRED_WHEN,
			PropertyRefLibraryFqns.REQUIRED,
			PropertyRefLibraryFqns.CONSTRAINT_COMPOSITION,
			PropertyRefLibraryFqns.COMPOSITION_MODE,
		)
		assertThat(all).noneMatch { it.startsWith("io.ghaylan.validata.web.") }
		assertThat(all).noneMatch { it.contains(".spring.") }
	}
}

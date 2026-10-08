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
package io.ghaylan.validata.processor.fqns

import com.fasterxml.jackson.annotation.JsonProperty
import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintArgs
import io.ghaylan.validata.constraint.PropertyRef
import io.ghaylan.validata.constraint.annotation.RequiredWhen
import io.ghaylan.validata.openapi.presentation.ApiError
import io.ghaylan.validata.schema.Validatable
import io.ghaylan.validata.schema.Validate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestParam

/**
 * Proves [AnnotationAttrs] / [OpenApiPresentationFqns.Attr] names match real annotations
 * on the processor test classpath.
 * 
 * @author Ghaylan Saada
 */
class AnnotationAttrsContractTest {
	
	@Test
	@DisplayName("@Constraint.validatedBy matches AnnotationAttrs.Constraint")
	fun constraintAttrs() {
		assertHasParam(Constraint::class.java, AnnotationAttrs.Constraint.VALIDATED_BY)
	}
	
	@Test
	@DisplayName("@ConstraintArg kinds/target/message match AnnotationAttrs.ConstraintArg")
	fun constraintArgAttrs() {
		assertHasParam(ConstraintArg::class.java, AnnotationAttrs.ConstraintArg.KINDS)
		assertHasParam(ConstraintArg::class.java, AnnotationAttrs.ConstraintArg.TARGET)
		assertHasParam(ConstraintArg::class.java, AnnotationAttrs.ConstraintArg.MESSAGE)
	}
	
	@Test
	@DisplayName("@ConstraintArgs.value matches AnnotationAttrs.ConstraintArgs")
	fun constraintArgsAttrs() {
		assertHasParam(ConstraintArgs::class.java, AnnotationAttrs.ConstraintArgs.VALUE)
	}
	
	@Test
	@DisplayName("@PropertyRef scope/compatibility match AnnotationAttrs.PropertyRef")
	fun propertyRefAttrs() {
		assertHasParam(PropertyRef::class.java, AnnotationAttrs.PropertyRef.SCOPE)
		assertHasParam(PropertyRef::class.java, AnnotationAttrs.PropertyRef.COMPATIBILITY)
	}
	
	@Test
	@DisplayName("@RequiredWhen ref/condition/value/values/mode match AnnotationAttrs")
	fun requiredWhenAttrs() {
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.ConstraintPayload.REF)
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.RequiredWhen.CONDITION)
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.ConstraintPayload.VALUE)
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.ConstraintPayload.VALUES)
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.RequiredWhen.MODE)
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.ConstraintPayload.MESSAGE)
		assertHasParam(RequiredWhen::class.java, AnnotationAttrs.ConstraintPayload.GROUPS)
	}
	
	@Test
	@DisplayName("@Validate groups/oneErrorPerParam/failFast match AnnotationAttrs.Validate")
	fun validateAttrs() {
		assertHasParam(Validate::class.java, AnnotationAttrs.Validate.GROUPS)
		assertHasParam(Validate::class.java, AnnotationAttrs.Validate.ONE_ERROR_PER_PARAM)
		assertHasParam(Validate::class.java, AnnotationAttrs.Validate.FAIL_FAST)
	}
	
	@Test
	@DisplayName("@Validatable discriminator/subtypes and Subtype name/type match AnnotationAttrs")
	fun validatableAttrs() {
		assertHasParam(Validatable::class.java, AnnotationAttrs.Validatable.DISCRIMINATOR)
		assertHasParam(Validatable::class.java, AnnotationAttrs.Validatable.SUBTYPES)
		assertHasParam(Validatable.Subtype::class.java, AnnotationAttrs.ValidatableSubtype.NAME)
		assertHasParam(Validatable.Subtype::class.java, AnnotationAttrs.ValidatableSubtype.TYPE)
	}
	
	@Test
	@DisplayName("@ApiError code/message/catalog match OpenApiPresentationFqns.Attr")
	fun apiErrorAttrs() {
		assertHasParam(ApiError::class.java, OpenApiPresentationFqns.Attr.CODE)
		assertHasParam(ApiError::class.java, OpenApiPresentationFqns.Attr.MESSAGE)
		assertHasParam(ApiError::class.java, OpenApiPresentationFqns.Attr.CATALOG)
		assertThat(OpenApiPresentationFqns.Attr.CODE_INDEX).isEqualTo(0)
		assertThat(OpenApiPresentationFqns.Attr.MESSAGE_INDEX).isEqualTo(1)
		assertThat(OpenApiPresentationFqns.Attr.CATALOG_INDEX).isEqualTo(2)
	}
	
	@Test
	@DisplayName("@JsonProperty.value matches AnnotationAttrs.Jackson")
	fun jacksonAttrs() {
		assertHasParam(JsonProperty::class.java, AnnotationAttrs.Jackson.VALUE)
	}
	
	@Test
	@DisplayName("@RequestParam name/value match AnnotationAttrs.SpringBinding")
	fun springBindingAttrs() {
		assertHasParam(RequestParam::class.java, AnnotationAttrs.SpringBinding.NAME)
		assertHasParam(RequestParam::class.java, AnnotationAttrs.SpringBinding.VALUE)
	}
	
	@Test
	@DisplayName("PresenceSimpleNames match Required / RequiredWhen simple names")
	fun presenceSimpleNames() {
		assertThat(AnnotationAttrs.PresenceSimpleNames.REQUIRED).isEqualTo(io.ghaylan.validata.constraint.annotation.Required::class.simpleName)
		assertThat(AnnotationAttrs.PresenceSimpleNames.REQUIRED_WHEN).isEqualTo(RequiredWhen::class.simpleName)
		assertThat(AnnotationAttrs.PresenceSimpleNames.ALL).containsExactlyInAnyOrder(
			AnnotationAttrs.PresenceSimpleNames.REQUIRED,
			AnnotationAttrs.PresenceSimpleNames.REQUIRED_WHEN,
		)
	}
	
	@Test
	@DisplayName("OpenApiPresentationFqns.API_ERROR matches ApiError FQCN")
	fun apiErrorFqcn() {
		assertThat(OpenApiPresentationFqns.API_ERROR).isEqualTo(ApiError::class.java.name)
	}
	
	private fun assertHasParam(
		annotation: Class<out Annotation>,
		name: String
	) {
		val methods = annotation.declaredMethods.map { it.name }
			.toSet()
		assertThat(methods).withFailMessage { "${annotation.name} lacks method '$name'; has $methods" }
			.contains(name)
	}
}

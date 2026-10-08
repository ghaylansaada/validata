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
package io.ghaylan.validata.support

import io.ghaylan.validata.runtime.PathSegment
import io.ghaylan.validata.runtime.ValidationContextValue
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.shape.ScalarKind
import io.ghaylan.validata.schema.shape.ScalarShape

/**
 * Hand-built sibling / gate containers for PropertyRef and RequiredWhen unit tests.
 * 
 * @author Ghaylan Saada
 */
object SiblingContexts {
	
	data class PasswordPair(
		val password: String?,
		val confirm: String?
	)
	
	data class Ages(
		val minAge: Int?,
		val maxAge: Int?
	)
	
	data class TypedBox(
		val label: String?,
		val count: Int?
	)
	
	data class GateBox(
		val gate: String?,
		val payload: String?
	)
	
	data class IntGateBox(
		val gate: Int?,
		val payload: String?
	)
	
	fun passwordConfirm(
		password: String?,
		confirm: String?
	): TestValidationContext {
		val schema = ObjectSchema(
			type = PasswordPair::class.java,
			properties = listOf(
				PropertySpec(
					"password",
					"password",
					ScalarShape(ScalarKind.STRING),
					{ (it as PasswordPair).password },
				),
				PropertySpec(
					"confirm",
					"confirm",
					ScalarShape(ScalarKind.STRING),
					{ (it as PasswordPair).confirm },
				),
			),
		)
		val instance = PasswordPair(password, confirm)
		return TestValidationContext(
			path = PathSegment.Name(PathSegment.Root, "confirm"),
			fieldName = "confirm",
			containerObject = ValidationContextValue(instance, schema, schema.selfRef),
		)
	}
	
	fun ages(
		minAge: Int?,
		maxAge: Int?,
		field: String = "maxAge"
	): TestValidationContext {
		val schema = ObjectSchema(
			type = Ages::class.java,
			properties = listOf(
				PropertySpec(
					"minAge",
					"minAge",
					ScalarShape(ScalarKind.INTEGRAL),
					{ (it as Ages).minAge },
				),
				PropertySpec(
					"maxAge",
					"maxAge",
					ScalarShape(ScalarKind.INTEGRAL),
					{ (it as Ages).maxAge },
				),
			),
		)
		val instance = Ages(minAge, maxAge)
		return TestValidationContext(
			path = PathSegment.Name(PathSegment.Root, field),
			fieldName = field,
			containerObject = ValidationContextValue(instance, schema, schema.selfRef),
		)
	}
	
	fun typedBox(
		label: String?,
		count: Int?
	): TestValidationContext {
		val schema = ObjectSchema(
			type = TypedBox::class.java,
			properties = listOf(
				PropertySpec(
					"label",
					"label",
					ScalarShape(ScalarKind.STRING),
					{ (it as TypedBox).label },
				),
				PropertySpec(
					"count",
					"count",
					ScalarShape(ScalarKind.INTEGRAL),
					{ (it as TypedBox).count },
				),
			),
		)
		val instance = TypedBox(label, count)
		return TestValidationContext(
			path = PathSegment.Name(PathSegment.Root, "count"),
			fieldName = "count",
			containerObject = ValidationContextValue(instance, schema, schema.selfRef),
		)
	}
	
	fun gate(
		gate: String?,
		payload: String? = null
	): TestValidationContext {
		val schema = ObjectSchema(
			type = GateBox::class.java,
			properties = listOf(
				PropertySpec(
					"gate",
					"gate",
					ScalarShape(ScalarKind.STRING),
					{ (it as GateBox).gate },
				),
				PropertySpec(
					"payload",
					"payload",
					ScalarShape(ScalarKind.STRING),
					{ (it as GateBox).payload },
				),
			),
		)
		val instance = GateBox(gate, payload)
		return TestValidationContext(
			path = PathSegment.Name(PathSegment.Root, "payload"),
			fieldName = "payload",
			containerObject = ValidationContextValue(instance, schema, schema.selfRef),
		)
	}
	
	fun intGate(
		gate: Int?,
		payload: String? = null
	): TestValidationContext {
		val schema = ObjectSchema(
			type = IntGateBox::class.java,
			properties = listOf(
				PropertySpec(
					"gate",
					"gate",
					ScalarShape(ScalarKind.INTEGRAL),
					{ (it as IntGateBox).gate },
				),
				PropertySpec(
					"payload",
					"payload",
					ScalarShape(ScalarKind.STRING),
					{ (it as IntGateBox).payload },
				),
			),
		)
		val instance = IntGateBox(gate, payload)
		return TestValidationContext(
			path = PathSegment.Name(PathSegment.Root, "payload"),
			fieldName = "payload",
			containerObject = ValidationContextValue(instance, schema, schema.selfRef),
		)
	}
}

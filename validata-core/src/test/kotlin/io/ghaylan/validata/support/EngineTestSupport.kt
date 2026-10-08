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

import io.ghaylan.validata.constraint.annotation.Required
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.constraint.composition.CompositionOrRunner
import io.ghaylan.validata.constraint.validator.required.RequiredValidator
import io.ghaylan.validata.engine.*
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.PropertySpec
import io.ghaylan.validata.schema.ValueReader
import io.ghaylan.validata.schema.constraint.CompiledConstraint
import io.ghaylan.validata.schema.request.EndpointSchema
import io.ghaylan.validata.schema.shape.*
import kotlin.reflect.KClass

/**
 * Hand-built schemas and a Spring-free [ValidatorEngine] for `:validata-core` unit tests.
 *
 * Schemas use [ContextAwareConstraintRunner] implementations ([ValidatorBackedRunner] or
 * [CompositionOrRunner]) — the engine hot path requires that contract.
 *
 * The shared [registry] is frozen after construction (product contract). Tests that need to
 * register schemas or validators must create a local [ValidationRegistry].*
 * 
 * @author Ghaylan Saada
 */
object EngineTestSupport {
	
	val registry: ValidationRegistry = ValidationRegistry().also { it.freeze() }
	
	fun engine(limits: ValidationLimits = ValidationLimits()): ValidatorEngine = ValidatorEngine(registry, limits)
	
	/** Fresh unfrozen registry for tests that exercise [ValidationRegistry.register*] / [freeze].	 */
	fun unfrozenRegistry(): ValidationRegistry = ValidationRegistry()
	
	fun requiredConstraint(groups: Set<KClass<*>> = setOf(OnDefault::class)): RequiredConstraint = RequiredConstraint(
		mode = Required.Mode.STRICT,
		message = "",
		groups = groups,
	)
	
	fun requiredCompiled(groups: Set<KClass<*>> = setOf(OnDefault::class)): CompiledConstraint =
		CompiledConstraints.of(RequiredValidator, requiredConstraint(groups), order = 0)
	
	data class TwoFields(
		val left: String?,
		val right: String?
	)
	
	data class NamedItem(val name: String?)
	
	data class Batch(val items: List<NamedItem>?)
	
	data class Settings(val entries: Map<String, String?>?)
	
	data class NestedNode(
		val name: String?,
		val child: NestedNode?
	)
	
	data class TinyBody(val name: String?)
	
	data class StringListBody(val items: List<String?>?)
	
	fun twoFieldsSchema(
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		val constraint = requiredCompiled(groups)
		return ObjectSchema(
			type = TwoFields::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "left",
					externalName = "left",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as TwoFields).left },
					constraints = listOf(constraint),
				),
				PropertySpec(
					declaredName = "right",
					externalName = "right",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as TwoFields).right },
					constraints = listOf(constraint),
				),
			),
		)
	}
	
	fun namedItemSchema(
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		val constraint = requiredCompiled(groups)
		return ObjectSchema(
			type = NamedItem::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "name",
					externalName = "name",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as NamedItem).name },
					constraints = listOf(constraint),
				),
			),
		)
	}
	
	fun batchSchema(
		itemSchema: ObjectSchema = namedItemSchema(),
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		val constraint = requiredCompiled(groups)
		return ObjectSchema(
			type = Batch::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "items",
					externalName = "items",
					shape = IterableShape(element = ObjectRefShape(lazy { itemSchema })),
					read = ValueReader { (it as Batch).items },
					constraints = listOf(constraint),
				),
			),
		)
	}
	
	fun settingsSchema(
		groups: Set<KClass<*>> = setOf(OnDefault::class),
		constrainKeys: Boolean = false,
	): ObjectSchema {
		val constraint = requiredCompiled(groups)
		return ObjectSchema(
			type = Settings::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "entries",
					externalName = "entries",
					shape = MapShape(
						key = ScalarShape(
							kind = ScalarKind.STRING,
							constraints = if (constrainKeys) listOf(constraint) else emptyList(),
						),
						value = ScalarShape(
							kind = ScalarKind.STRING,
							constraints = listOf(constraint),
						),
					),
					read = ValueReader { (it as Settings).entries },
					constraints = listOf(constraint),
				),
			),
		)
	}
	
	data class DynamicHolder(val payload: Any?)
	
	fun dynamicHolderSchema(): ObjectSchema = ObjectSchema(
		type = DynamicHolder::class.java,
		properties = listOf(
			PropertySpec(
				declaredName = "payload",
				externalName = "payload",
				shape = DynamicShape(),
				read = ValueReader { (it as DynamicHolder).payload },
			),
		),
	)
	
	fun nestedNodeSchema(
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		lateinit var schema: ObjectSchema
		val constraint = requiredCompiled(groups)
		schema = ObjectSchema(
			type = NestedNode::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "name",
					externalName = "name",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as NestedNode).name },
					constraints = listOf(constraint),
				),
				PropertySpec(
					declaredName = "child",
					externalName = "child",
					shape = ObjectRefShape(lazy { schema }),
					read = ValueReader { (it as NestedNode).child },
				),
			),
		)
		return schema
	}
	
	fun tinyBodySchema(
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		val constraint = requiredCompiled(groups)
		return ObjectSchema(
			type = TinyBody::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "name",
					externalName = "name",
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as TinyBody).name },
					constraints = listOf(constraint),
				),
			),
		)
	}
	
	fun stringListWithRequiredElementsSchema(
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		val elementRequired = requiredCompiled(groups)
		return ObjectSchema(
			type = StringListBody::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = "items",
					externalName = "items",
					shape = IterableShape(
						element = ScalarShape(
							kind = ScalarKind.STRING,
							constraints = listOf(elementRequired),
						),
					),
					read = ValueReader { (it as StringListBody).items },
				),
			),
		)
	}
	
	fun flatMapSchema(
		propertyName: String,
		groups: Set<KClass<*>> = setOf(OnDefault::class),
	): ObjectSchema {
		val constraint = requiredCompiled(groups)
		return ObjectSchema(
			type = Map::class.java,
			properties = listOf(
				PropertySpec(
					declaredName = propertyName,
					externalName = propertyName,
					shape = ScalarShape(ScalarKind.STRING),
					read = ValueReader { (it as Map<*, *>)[propertyName] },
					constraints = listOf(constraint),
				),
			),
		)
	}
	
	fun bodyRequestSchema(
		body: ObjectSchema,
		failFast: Boolean = false,
		oneErrorPerParam: Boolean = false,
		query: ObjectSchema? = null,
		groups: Set<KClass<*>> = setOf(OnDefault::class),
		id: String = "engine-test",
	): EndpointSchema = EndpointSchema(
		id = id,
		requestBody = body,
		queryParams = query,
		oneErrorPerParam = oneErrorPerParam,
		failFast = failFast,
		groups = groups,
	)
}

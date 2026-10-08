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
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import io.ghaylan.validata.processor.compat.TypeClassification
import io.ghaylan.validata.processor.compat.has
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.model.*
import io.ghaylan.validata.schema.shape.ScalarKind

/**
 * Builds intermediate [ShapeModel] trees from KSP [KSType]s.
 *
 * ## Role in the pipeline
 * [SchemaModelBuilder] and [EndpointModelBuilder] call this for each property / parameter type.
 * The resulting tree mirrors the runtime TypeShape hierarchy and is later rendered by
 * schema / endpoint code writers.
 *
 * ## Decision order (important)
 * 1. Map → [MapShapeModel] (key + value recursively)
 * 2. Iterable → [IterableShapeModel]
 * 3. Array → [IterableShapeModel] (same runtime walk as lists)
 * 4. Scalar → [ScalarShapeModel]
 * 5. Enum class → [ScalarShapeModel] with [ScalarKind.ENUM]
 * 6. User class / interface → cascade check → [ObjectRefShapeModel] or error + [DynamicShapeModel]
 * 7. Anything else → [DynamicShapeModel]
 *
 * @property logger cascade / `@Validatable` diagnostics
 * @property constraints builds type-use constraint lists hanging on shape nodes
 * @property strictCrossModuleCascade when `true`, unmarked cross-module object refs become errors*
 * 
 * @author Ghaylan Saada
 */
internal class ShapeModelBuilder(
	private val logger: KSPLogger,
	private val constraints: ConstraintModelBuilder,
	private val strictCrossModuleCascade: Boolean = false,
) {
	
	/**
	 * Property-shaped entry point used by [SchemaModelBuilder] — forwards owner FQCN / property
	 * name / node into the general [buildShape] overload.
	 *
	 * No side effects.
	 *
	 * @param type resolved property type
	 * @param typeUseAnnotations annotations on the property type reference
	 * @param owner declaring `@Validatable` or endpoint owner class
	 * @param property property declaration under [owner]
	 * @param visiting cycle set shared with schema construction
	 * @param cascadePolicy whether nested object types may cascade
	 * @return [ShapeModel] for [type]	 
	 */
	fun buildShape(
		type: KSType,
		typeUseAnnotations: List<KSAnnotation>,
		owner: KSClassDeclaration,
		property: KSPropertyDeclaration,
		visiting: MutableSet<String>,
		cascadePolicy: CascadePolicy,
	): ShapeModel =
		buildShape(
			type = type,
			typeUseAnnotations = typeUseAnnotations,
			ctx = ShapeBuildContext(
				ownerQualifiedName = owner.qualifiedName?.asString().orEmpty(),
				subjectName = property.simpleName.asString(),
				subjectNode = property,
				visiting = visiting,
				cascadePolicy = cascadePolicy,
			),
		)
	
	/**
	 * Builds the shape for one resolved type (property type, parameter type, or type argument).
	 *
	 * May emit KSP errors or warnings via [logger] during object cascade checks.
	 *
	 * @param type resolved subject type
	 * @param typeUseAnnotations type-use constraint annotations on [type]
	 * @param ctx owner / subject context and cascade policy
	 * @return [ShapeModel] tree for [type]	 
	 */
	fun buildShape(
		type: KSType,
		typeUseAnnotations: List<KSAnnotation>,
		ctx: ShapeBuildContext,
	): ShapeModel {
		val typeConstraints = constraints.resolveConstraintsFromAnnotations(
			ownerQualifiedName = ctx.ownerQualifiedName,
			subjectName = ctx.subjectName,
			subjectNode = ctx.subjectNode,
			annotations = typeUseAnnotations,
			valueType = type,
		)
		val decl = type.declaration
		val qName = decl.qualifiedName?.asString()
		
		return when {
			TypeClassification.isMap(type) -> buildMapShape(type, typeConstraints, ctx)
			TypeClassification.isIterableAssumingNotMap(type) -> buildIterableShape(type, typeConstraints, ctx)
			TypeClassification.isArray(type) -> buildIterableShape(type, typeConstraints, ctx)
			qName != null && TypeClassification.isScalar(qName) -> ScalarShapeModel(
				kind = TypeClassification.scalarKind(qName),
				typeQualifiedName = qName,
				constraints = typeConstraints,
			)
			
			decl is KSClassDeclaration && decl.classKind == ClassKind.ENUM_CLASS -> ScalarShapeModel(
				kind = ScalarKind.ENUM,
				typeQualifiedName = qName,
				constraints = typeConstraints,
			)
			
			decl is KSClassDeclaration && isObjectLike(decl) && !TypeClassification.isPlatformLeaf(qName) -> buildObjectCascadeShape(
				decl, qName, typeConstraints, ctx)
			
			else -> DynamicShapeModel(typeConstraints)
		}
	}
	
	/**
	 * Whether [decl] is modeled as an object-like user type (class, interface, or object).
	 *
	 * No side effects.
	 *
	 * @param decl type declaration candidate
	 * @return `true` for class, interface, or object kinds	 
	 */
	private fun isObjectLike(decl: KSClassDeclaration): Boolean =
		decl.classKind == ClassKind.CLASS || decl.classKind == ClassKind.INTERFACE || decl.classKind == ClassKind.OBJECT
	
	/**
	 * Builds a [MapShapeModel] with recursively built key and value shapes.
	 *
	 * No side effects beyond nested [buildShape] calls.
	 *
	 * @param type resolved map type
	 * @param typeConstraints constraints attached to the map node
	 * @param ctx shared build context
	 * @return map shape, or [DynamicShapeModel] slots when type args are missing	 
	 */
	private fun buildMapShape(
		type: KSType,
		typeConstraints: List<ConstraintModel>,
		ctx: ShapeBuildContext,
	): ShapeModel {
		val args = type.arguments
		val key = args.getOrNull(0)?.let { buildShapeFromArg(it, ctx) }
			?: DynamicShapeModel()
		val value = args.getOrNull(1)?.let { buildShapeFromArg(it, ctx) }
			?: DynamicShapeModel()
		return MapShapeModel(key, value, typeConstraints)
	}
	
	/**
	 * Builds an [IterableShapeModel] for list, set, sequence, or array types.
	 *
	 * No side effects beyond nested [buildShape] calls.
	 *
	 * @param type resolved iterable or array type
	 * @param typeConstraints constraints attached to the iterable node
	 * @param ctx shared build context
	 * @return iterable shape with one element subtree	 
	 */
	private fun buildIterableShape(
		type: KSType,
		typeConstraints: List<ConstraintModel>,
		ctx: ShapeBuildContext,
	): ShapeModel {
		val element = type.arguments.firstOrNull()?.let { buildShapeFromArg(it, ctx) }
			?: DynamicShapeModel()
		return IterableShapeModel(element, typeConstraints)
	}
	
	/**
	 * Applies [CascadeDecision] for a user object-like type and emits the matching shape.
	 *
	 * May emit KSP errors or warnings via [logger].
	 *
	 * @param decl object-like class declaration
	 * @param qName qualified name of [decl], or `null` when absent
	 * @param typeConstraints constraints on the object node
	 * @param ctx shared build context including cascade policy
	 * @return object ref, scalar OTHER, or dynamic fallback shape	 
	 */
	private fun buildObjectCascadeShape(
		decl: KSClassDeclaration,
		qName: String?,
		typeConstraints: List<ConstraintModel>,
		ctx: ShapeBuildContext,
	): ShapeModel {
		if (qName == null) {
			logger.error(
				"'${ctx.subjectName}' on '${ctx.ownerQualifiedName}' has an object-like type without a " + "qualified name — cannot cascade. Annotate a named @Validatable type or mark @NoCascade.",
				ctx.subjectNode,
			)
			return DynamicShapeModel(typeConstraints)
		}
		if (ctx.cascadePolicy == CascadePolicy.NO_CASCADE) {
			return ScalarShapeModel(
				kind = ScalarKind.OTHER,
				typeQualifiedName = qName,
				constraints = typeConstraints,
			)
		}
		val marked = decl.has(ProcessorFqns.VALIDATABLE)
		val sameCompilation = decl.containingFile != null
		return when (CascadeDecision.decide(
			CascadeFacts(
				markedValidatable = marked,
				noCascade = false,
				sameCompilation = sameCompilation,
				strictCrossModuleCascade = strictCrossModuleCascade,
			),
		)) {
			CascadeOutcome.OBJECT_REF -> ObjectRefShapeModel(qName, typeConstraints)
			CascadeOutcome.SCALAR_OTHER -> ScalarShapeModel(
				kind = ScalarKind.OTHER,
				typeQualifiedName = qName,
				constraints = typeConstraints,
			)
			
			CascadeOutcome.ERROR_DYNAMIC -> {
				logger.error(
					"'${ctx.subjectName}' on '${ctx.ownerQualifiedName}' cascades into '$qName' which is not @Validatable. " + "Annotate it with @Validatable or mark the site @NoCascade.",
					ctx.subjectNode,
				)
				DynamicShapeModel(typeConstraints)
			}
			
			CascadeOutcome.WARN_OBJECT_REF -> {
				logger.warn(
					"'${ctx.subjectName}' on '${ctx.ownerQualifiedName}' cascades into '$qName' which is not visibly " + "@Validatable (cross-module). Ensure the declaring module applies validata-processor.",
					ctx.subjectNode,
				)
				ObjectRefShapeModel(qName, typeConstraints)
			}
		}
	}
	
	/**
	 * Builds a shape for one type argument of a map / iterable / array.
	 *
	 * Merges annotations from the type-argument star projection **and** from the nested type
	 * reference so both `List<@Email String>` styles KSP may expose are covered.
	 *
	 * No side effects beyond nested [buildShape] calls.
	 *
	 * @param arg type argument from a map, iterable, or array
	 * @param ctx shared build context
	 * @return shape for the resolved argument type, or [DynamicShapeModel] when unresolved	 
	 */
	private fun buildShapeFromArg(
		arg: KSTypeArgument,
		ctx: ShapeBuildContext,
	): ShapeModel {
		val type = arg.type?.resolve()
			?: return DynamicShapeModel()
		return buildShape(
			type = type,
			typeUseAnnotations = buildList {
				addAll(arg.annotations)
				arg.type?.annotations?.let { addAll(it) }
			},
			ctx = ctx,
		)
	}
}

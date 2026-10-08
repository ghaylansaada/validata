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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.compat.ConstraintValidatorTypeResolver
import io.ghaylan.validata.processor.model.ConstraintMeta
import io.ghaylan.validata.processor.model.ConstraintModel
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming
import io.ghaylan.validata.processor.verify.ConstraintArgVerifier
import io.ghaylan.validata.processor.verify.PropertyRefHostDiscovery

/**
 * Turns property- and type-use annotations into [ConstraintModel]s ready for code generation.
 *
 * Orchestrates [ConstraintMetaResolver], [ConstraintValidatorSelector],
 * [ConstraintMetadataCallRenderer], and [ConstraintCompositionExpander]. Round caches live in
 * [ConstraintRoundSession] (begin/end tied to `SchemaProcessor.process`).
 *
 * ## Ordering
 * Built-in presence annotations are sorted to the front via [ConstraintPresenceOrdering].
 *
 * ## Composition
 * Nested `@Constraint` meta-annotations expand recursively up to [MAX_COMPOSITION_DEPTH].
 * `@ConstraintComposition(OR)` yields one synthetic composition model.
 *
 * @property logger KSP logger used for compile-time diagnostics*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintModelBuilder(
	private val logger: KSPLogger,
	jacksonNaming: JacksonPropertyNaming = JacksonPropertyNaming.IDENTITY,
) {
	
	private var session: ConstraintRoundSession? = null
	private val typeResolver = ConstraintValidatorTypeResolver()
	private val argVerifier = ConstraintArgVerifier(logger, jacksonNaming)
	private val metaResolver = ConstraintMetaResolver()
	private val validatorSelector = ConstraintValidatorSelector(typeResolver)
	private val metadataRenderer = ConstraintMetadataCallRenderer()
	private val compositionExpander = ConstraintCompositionExpander(logger, ::findConstraintMeta)
	
	/**
	 * Starts a round-scoped session and binds verifier caches.
	 *
	 * Mutates internal session state.
	 *
	 * @param resolver current KSP resolver	 
	 */
	fun beginRound(resolver: Resolver) {
		val state = ConstraintRoundSession(resolver)
		session = state
		argVerifier.bindHostCache(state.constraintArgHostCache)
		argVerifier.bindOwnerPropertyTypeCache(state.ownerPropertyTypeCache)
	}
	
	/**
	 * Clears the round session and detaches verifier caches.
	 *
	 * Mutates internal session state.	 
	 */
	fun endRound() {
		session = null
		argVerifier.bindHostCache(null)
		argVerifier.bindOwnerPropertyTypeCache(null)
	}
	
	/**
	 * Collects constraints from a property declaration **and** its getter, merging inherited
	 * annotations for overridees when the subclass did not redeclare the same annotation type.
	 *
	 * May emit KSP errors via [logger] during verification.
	 *
	 * @param owner declaring class
	 * @param property property under [owner]
	 * @param valueType resolved property value type
	 * @return built constraint models in presence-first order	 
	 */
	fun resolveConstraints(
		owner: KSClassDeclaration,
		property: KSPropertyDeclaration,
		valueType: KSType,
	): List<ConstraintModel> {
		val local = buildList {
			addAll(property.annotations)
			property.getter?.annotations?.let { addAll(it) }
		}
		val localTypes = local.mapNotNullTo(HashSet()) { AnnotationFqcn.of(it) }
		val overridee = property.findOverridee()
		val inherited = if (overridee == null) {
			emptyList()
		}
		else {
			buildList {
				addAll(overridee.annotations)
				overridee.getter?.annotations?.let { addAll(it) }
			}.filter { ann ->
				val typeFqcn = AnnotationFqcn.of(ann)
					?: return@filter true
				typeFqcn !in localTypes
			}
		}
		return resolveConstraintsFromAnnotations(
			ownerQualifiedName = owner.qualifiedName?.asString().orEmpty(),
			subjectName = property.simpleName.asString(),
			subjectNode = property,
			annotations = local + inherited,
			valueType = valueType,
			ownerDeclaration = owner,
		)
	}
	
	/**
	 * Collects constraints from an arbitrary annotation list (property, getter, type-use, or param).
	 *
	 * May emit KSP errors via [logger] during expansion and verification.
	 *
	 * @param ownerQualifiedName FQCN of the declaring type or endpoint owner
	 * @param subjectName property or parameter name for diagnostics
	 * @param subjectNode symbol for IDE/build log location
	 * @param annotations constraint annotations to expand
	 * @param valueType subject value type used for validator selection
	 * @param ownerDeclaration optional owner class for property-ref verification
	 * @param siblingParamTypes optional flat-parameter sibling types for cross-param refs
	 * @return built constraint models in presence-first order	 
	 */
	fun resolveConstraintsFromAnnotations(
		ownerQualifiedName: String,
		subjectName: String,
		subjectNode: KSNode?,
		annotations: List<KSAnnotation>,
		valueType: KSType,
		ownerDeclaration: KSClassDeclaration? = null,
		siblingParamTypes: Map<String, KSType>? = null,
	): List<ConstraintModel> {
		val out = mutableListOf<ConstraintModel>()
		var order = 0
		
		fun addConstraint(
			ann: KSAnnotation,
			meta: ConstraintMeta
		): ConstraintModel? {
			val validatorQ = selectValidator(meta.validatorQualifiedNames, valueType)
				?: run {
					logger.error(
						"No validator in @Constraint(validatedBy=…) for '${ann.shortName.asString()}' " + "is compatible with '$subjectName' on '$ownerQualifiedName'.",
						subjectNode,
					)
					return null
				}
			val state = requireSession()
			val metadataDecl = resolveMetadataDeclaration(ann)
			val refs = PropertyRefHostDiscovery.extractRefs(
				ann = ann,
				metadata = metadataDecl,
				hostCache = state.propertyRefHostCache,
			)
			val model = ConstraintModel(
				metadataConstructorCall = metadataRenderer.buildMetadataCall(
					metadataQ = meta.metadataQualifiedName,
					ann = ann,
					metadataDecl = metadataDecl,
					metadataParamSetCache = state.metadataParamSetCache,
				),
				validatorExpression = validatorQ,
				order = order++,
				siblingRefs = refs.siblings,
				elementRefs = refs.elements,
				compatibilityKind = refs.compatibilityKind,
				annotationSimpleName = ann.shortName.asString(),
			)
			argVerifier.verify(
				ann = ann,
				metadata = metadataDecl,
				valueType = valueType,
				subjectNode = subjectNode,
				owner = ownerDeclaration,
				siblingParamTypes = siblingParamTypes,
			)
			return model
		}
		
		fun expand(
			ann: KSAnnotation,
			depth: Int,
			visiting: MutableSet<String>
		) {
			val meta = findConstraintMeta(ann)
			if (meta != null) {
				addConstraint(ann, meta)?.let(out::add)
				return
			}
			if (depth >= MAX_COMPOSITION_DEPTH) return
			val decl = ann.annotationType.resolve().declaration as? KSClassDeclaration
				?: return
			val declFqcn = decl.qualifiedName?.asString()
				?: return
			if (!visiting.add(declFqcn)) return
			try {
				when (compositionExpander.readCompositionMode(decl)) {
					CompositionExpandMode.OR -> compositionExpander.expandOrComposition(
						usageAnn = ann,
						decl = decl,
						addLeaf = ::addConstraint,
						emit = { out += it },
						subjectNode = subjectNode,
					)
					
					CompositionExpandMode.AND -> {
						for (metaAnn in decl.annotations) {
							expand(metaAnn, depth + 1, visiting)
						}
					}
				}
			}
			finally {
				visiting.remove(declFqcn)
			}
		}
		
		for (ann in annotations) {
			expand(ann, depth = 0, visiting = mutableSetOf())
		}
		ConstraintPresenceOrdering.sortInPlace(out)
		return out
	}
	
	/** Property-shaped overload for [ShapeModelBuilder] / [SchemaModelBuilder] call sites.
	 *
	 * No side effects beyond [resolveConstraintsFromAnnotations] delegation.
	 *
	 * @param owner declaring class
	 * @param property property under [owner]
	 * @param annotations type-use or property annotations to expand
	 * @param valueType resolved property value type
	 * @return built constraint models in presence-first order	 
	 */
	fun resolveConstraintsFromAnnotations(
		owner: KSClassDeclaration,
		property: KSPropertyDeclaration,
		annotations: List<KSAnnotation>,
		valueType: KSType,
	): List<ConstraintModel> =
		resolveConstraintsFromAnnotations(
			ownerQualifiedName = owner.qualifiedName?.asString().orEmpty(),
			subjectName = property.simpleName.asString(),
			subjectNode = property,
			annotations = annotations,
			valueType = valueType,
			ownerDeclaration = owner,
		)
	
	/**
	 * Resolves `@Constraint` meta for [ann] using the active round cache.
	 *
	 * No side effects beyond cache population.
	 *
	 * @param ann constraint annotation usage or meta-annotation
	 * @return parsed meta, or `null` when [ann] is not a constraint annotation	 
	 */
	private fun findConstraintMeta(ann: KSAnnotation): ConstraintMeta? =
		metaResolver.resolve(ann, requireSession().constraintMetaCache)
	
	/**
	 * Selects the best-matching validator FQCN for [valueType] from [candidates].
	 *
	 * No side effects beyond round cache population.
	 *
	 * @param candidates validator FQCNs from `@Constraint(validatedBy = …)`
	 * @param valueType subject value type
	 * @return chosen validator FQCN, or `null` when none fit	 
	 */
	private fun selectValidator(
		candidates: List<String>,
		valueType: KSType
	): String? {
		val state = requireSession()
		return validatorSelector.select(
			candidates = candidates,
			valueType = valueType,
			resolver = state.resolver,
			resolvedValidatorCache = state.resolvedValidatorCache,
			typeViewCache = state.typeViewCache,
		)
	}
	
	/**
	 * Loads the generated metadata class declaration for [ann]'s annotation type.
	 *
	 * No side effects beyond round cache population.
	 *
	 * @param ann constraint annotation usage
	 * @return metadata class declaration, or `null` when not resolvable this round	 
	 */
	private fun resolveMetadataDeclaration(ann: KSAnnotation): KSClassDeclaration? {
		val annotationDecl = ann.annotationType.resolve().declaration as? KSClassDeclaration
			?: return null
		val fqcn = MetadataFqcnResolver.conventionFqcn(annotationDecl)
		val state = requireSession()
		return ConstraintRoundSession.cachedNullable(state.metadataDeclCache, fqcn) {
			state.resolver.getClassDeclarationByName(state.resolver.getKSNameFromString(fqcn))
		}
	}
	
	/**
	 * Returns the active [ConstraintRoundSession] or fails fast when the round was not started.
	 *
	 * No side effects.
	 *
	 * @return current round session
	 * @throws IllegalStateException when [beginRound] was not called or [endRound] already ran	 
	 */
	private fun requireSession(): ConstraintRoundSession =
		session
			?: error(
				"Internal error: ConstraintModelBuilder.beginRound(resolver) was not called before " + "constraint resolution (or endRound() already cleared the session).",
			)
	
	companion object {
		
		/**
		 * Maximum nesting depth for composed constraint meta-annotation expansion.
		 */
		const val MAX_COMPOSITION_DEPTH = 4
	}
}

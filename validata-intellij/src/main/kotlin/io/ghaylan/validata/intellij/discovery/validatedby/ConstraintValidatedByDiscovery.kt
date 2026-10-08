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

package io.ghaylan.validata.intellij.discovery.validatedby

import com.intellij.psi.*
import io.ghaylan.validata.intellij.analysis.compat.SubjectTypeViews
import io.ghaylan.validata.intellij.contract.PropertyRefLibraryFqns
import io.ghaylan.validata.intellij.discovery.cache.*
import io.ghaylan.validata.intellij.discovery.propertyref.PropertyRefAttributeDiscovery
import io.ghaylan.validata.intellij.discovery.validatedby.ConstraintValidatedByDiscovery.analyzeKtAnnotationClass
import io.ghaylan.validata.intellij.discovery.validatedby.ConstraintValidatedByDiscovery.starProjectionView
import io.ghaylan.validata.intellij.typing.ValidatorTypeView
import io.ghaylan.validata.schema.types.BuiltinTypeShortNames
import io.ghaylan.validata.schema.types.TypeNames
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

/**
 * Discovers accepted subject types from `@Constraint(validatedBy = […])` by reading each
 * validator’s `ConstraintValidator<V, C>` binding.
 *
 * ## Why it exists
 *
 * Subject-type inspections must know which types a constraint accepts (e.g. `@Size` on
 * `CharSequence` / collections). That contract lives on the library: authors list validator
 * classes in `validatedBy`, and each implements `ConstraintValidator<V, C>`. KSP reads the
 * same binding at compile time; this discovery reproduces it over IntelliJ PSI so the editor
 * stays aligned without an FQCN allowlist of Validata builtins.
 *
 * ## How it fits the plugin
 *
 * Part of the `discovery.validatedby` package. [analyze] is the entry for usage-site
 * annotations; results are sealed [ConstraintValidatedByResult] values, often memoized by
 * [ConstraintDiscoveryCache.validatedBy] when an annotation FQCN is available. Compatibility
 * / ranking layers consume [ConstraintValidatedByOk.valueTypes] as [ValidatorTypeView]s.
 *
 * ## What it is NOT
 *
 * - Not a validator runner and not Bean Validation’s `ConstraintValidator` discovery SPI.
 * - Not responsible for `@PropertyRef` or `@ConstraintArg` hosts.
 * - Not a hard-coded map from annotation simple names to subject types — new consumer
 *   constraints light up when they declare typed validators on the classpath.*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintValidatedByDiscovery {


	/**
	 * Discovers validator value types for [annotation], using the project cache when an FQCN
	 * is available.
	 *
	 * Resolves the annotation declaration via
	 * [PropertyRefAttributeDiscovery.resolveAnnotationDeclaration]. Missing declaration →
	 * [ConstraintValidatedByNotAConstraint]. With an FQCN, delegates to
	 * [ConstraintDiscoveryCache.validatedBy]; otherwise [analyzeUncached].
	 *
	 * @param annotation Usage-site constraint annotation (e.g. `@Size(min = 1)` on a property).
	 * @return Sealed analysis outcome; never throws for unresolved classpath shapes.
	 */
	fun analyze(annotation: KtAnnotationEntry): ConstraintValidatedByResult {
		val declaration = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(annotation)
			?: return ConstraintValidatedByNotAConstraint
		val fqName = PropertyRefAttributeDiscovery.annotationFqName(declaration)
		if (fqName != null) {
			return ConstraintDiscoveryCache.getInstance(annotation.project).validatedBy(fqName) {
				analyzeUncached(declaration)
			}
		}
		return analyzeUncached(declaration)
	}

	/**
	 * Uncached `validatedBy` analysis from a resolved annotation declaration.
	 *
	 * Dispatches on [KtClass] vs [PsiClass]. Unknown element kinds are treated as
	 * [ConstraintValidatedByNotAConstraint].
	 *
	 * @param declaration Annotation type declaration.
	 * @return Analysis outcome for that declaration.
	 */
	fun analyzeUncached(declaration: PsiElement): ConstraintValidatedByResult =
		when (declaration) {
			is KtClass -> analyzeKtAnnotationClass(declaration)
			is PsiClass -> analyzePsiAnnotationClass(declaration)
			else -> ConstraintValidatedByNotAConstraint
		}

	/**
	 * Cached when [declaration] has a resolvable FQCN; otherwise [analyzeUncached].
	 * Prefer this from composition / declaration-site walks.
	 */
	fun analyzeDeclaration(declaration: PsiElement): ConstraintValidatedByResult {
		val fqName = PropertyRefAttributeDiscovery.annotationFqName(declaration)
			?: return analyzeUncached(declaration)
		val project = declaration.project
		return ConstraintDiscoveryCache.getInstance(project).validatedBy(fqName) {
			analyzeUncached(declaration)
		}
	}

	/**
	 * Kotlin-source analysis: require `@Constraint`, resolve `validatedBy` class literals, and
	 * extract each validator’s `V` type argument.
	 *
	 * @param annotationClass Constraint annotation declaration in Kotlin source.
	 * @return [ConstraintValidatedByNotAConstraint] without `@Constraint`;
	 *   [ConstraintValidatedByMissingValidatedBy] when validators are empty or none yield a
	 *   value type; otherwise [ConstraintValidatedByOk].
	 */
	private fun analyzeKtAnnotationClass(annotationClass: KtClass): ConstraintValidatedByResult {
		val constraint = findKtConstraint(annotationClass) ?: return ConstraintValidatedByNotAConstraint
		val validators = resolveKtValidatedByClasses(constraint)
		if (validators.isEmpty()) return ConstraintValidatedByMissingValidatedBy
		val valueTypes = validators.mapNotNull { extractValueType(it) }
		return if (valueTypes.isEmpty()) ConstraintValidatedByMissingValidatedBy else ConstraintValidatedByOk(valueTypes)
	}

	/**
	 * PSI / binary analysis: same contract as [analyzeKtAnnotationClass] using
	 * [PsiClass.getAnnotation] and class-object expressions.
	 *
	 * @param annotationClass Constraint annotation PSI class.
	 * @return Same sealed outcomes as the Kotlin path.
	 */
	private fun analyzePsiAnnotationClass(annotationClass: PsiClass): ConstraintValidatedByResult {
		val constraint = annotationClass.getAnnotation(PropertyRefLibraryFqns.CONSTRAINT)
			?: return ConstraintValidatedByNotAConstraint
		val validators = resolvePsiValidatedByClasses(constraint)
		if (validators.isEmpty()) return ConstraintValidatedByMissingValidatedBy
		val valueTypes = validators.mapNotNull { extractValueTypeFromPsi(it) }
		return if (valueTypes.isEmpty()) ConstraintValidatedByMissingValidatedBy else ConstraintValidatedByOk(valueTypes)
	}

	/**
	 * Finds `@Constraint` on a Kotlin annotation declaration (FQCN or unresolved short name
	 * `"Constraint"`, matching property-ref discovery).
	 *
	 * @param annotationClass Annotation type in source.
	 * @return The meta-annotation entry, or `null` if absent.
	 */
	private fun findKtConstraint(annotationClass: KtClass): KtAnnotationEntry? {
		for (entry in annotationClass.annotationEntries) {
			val resolved = PropertyRefAttributeDiscovery.resolveAnnotationDeclaration(entry)
			val fq = resolved?.let { PropertyRefAttributeDiscovery.annotationFqName(it) }
			if (fq == PropertyRefLibraryFqns.CONSTRAINT) return entry
			if (entry.shortName?.asString() == "Constraint" && fq == null) {
				return entry
			}
		}
		return null
	}

	/**
	 * Resolves class declarations listed in `@Constraint(validatedBy = …)`.
	 *
	 * @param constraint The `@Constraint` annotation entry on the constraint type.
	 * @return Validator class elements (may be empty when the argument is missing or
	 *   literals fail to resolve).
	 */
	private fun resolveKtValidatedByClasses(constraint: KtAnnotationEntry): List<PsiElement> {
		val arg = namedArgument(constraint, "validatedBy") ?: return emptyList()
		val expr = arg.getArgumentExpression() ?: return emptyList()
		return classLiterals(expr).mapNotNull { resolveClassLiteral(it) }
	}

	/**
	 * Resolves `validatedBy` class objects from a PSI `@Constraint` annotation.
	 *
	 * Supports array initializers, a single class-object expression, and nested class-object
	 * children when PSI shapes differ across stub implementations.
	 *
	 * @param constraint PSI `@Constraint` on the annotation class.
	 * @return Resolved validator [PsiClass]es (may be empty).
	 */
	private fun resolvePsiValidatedByClasses(constraint: PsiAnnotation): List<PsiClass> {
		val value = constraint.findAttributeValue("validatedBy") ?: return emptyList()
		val expressions = when (value) {
			is PsiArrayInitializerMemberValue -> value.initializers.toList()
			is PsiClassObjectAccessExpression -> listOf(value)
			else -> value.children.filterIsInstance<PsiClassObjectAccessExpression>()
		}
		return expressions.mapNotNull { expr ->
			val classObject = expr as? PsiClassObjectAccessExpression ?: return@mapNotNull null
			val type = classObject.operand.type as? PsiClassType ?: return@mapNotNull null
			type.resolve()
		}
	}

	/**
	 * Collects class-literal expressions from a `validatedBy` argument expression.
	 *
	 * Handles a single `Foo::class`, a collection literal `[A::class, B::class]`, and loose
	 * child class-literals when the expression shape is unexpected.
	 *
	 * @param expression The `validatedBy` argument expression.
	 * @return Class literals to resolve (may be empty).
	 */
	private fun classLiterals(expression: KtExpression): List<KtClassLiteralExpression> =
		when (expression) {
			is KtClassLiteralExpression -> listOf(expression)
			is KtCollectionLiteralExpression ->
				expression.getInnerExpressions().filterIsInstance<KtClassLiteralExpression>()
			else -> expression.children.filterIsInstance<KtClassLiteralExpression>()
		}

	/**
	 * Resolves a `Foo::class` literal to its class / object declaration.
	 *
	 * Prefers `mainReference`, then same-file declarations, then short-name search — same
	 * resilience pattern as property-ref discovery for light tests and generated sources.
	 *
	 * @param expression Class-literal expression from `validatedBy`.
	 * @return Resolved declaration, or `null` when the receiver cannot be named / found.
	 */
	private fun resolveClassLiteral(expression: KtClassLiteralExpression): PsiElement? {
		val receiver = expression.receiverExpression ?: return null
		receiver.mainReference?.resolve()?.let { return it }
		val name = when (receiver) {
			is KtNameReferenceExpression -> receiver.getReferencedName()
			is KtDotQualifiedExpression ->
				(receiver.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
			else -> null
		} ?: return null
		return expression.containingKtFile.declarations
			.filterIsInstance<KtClassOrObject>()
			.firstOrNull { it.name == name }
			?: PropertyRefAttributeDiscovery.pickBestClass(
				PropertyRefAttributeDiscovery.findClassesByShortName(
					expression.project,
					name,
					expression.resolveScope,
				),
				preferredPackage = expression.containingKtFile.packageFqName.asString().takeIf { it.isNotEmpty() },
			)
	}

	/**
	 * Extracts the `ConstraintValidator` value-type view from a resolved validator declaration.
	 *
	 * @param validator Class / object from `validatedBy` (Kotlin or PSI).
	 * @return [ValidatorTypeView] for `V`, or `null` when the type cannot be read.
	 */
	private fun extractValueType(validator: PsiElement): ValidatorTypeView? =
		when (validator) {
			is KtClassOrObject -> extractValueTypeFromKt(validator)
			is PsiClass -> extractValueTypeFromPsi(validator)
			else -> null
		}

	/**
	 * Walks Kotlin supertypes for `ConstraintValidator<V, C>` (or an intermediate base that
	 * eventually binds it) and converts `V` via [SubjectTypeViews].
	 *
	 * Empty type-argument lists yield `null`. Missing type elements become
	 * [ValidatorTypeView.WILDCARD]. Star-projection-like text such as `Comparable<*>` is
	 * handled by [starProjectionView].
	 *
	 * @param validator Validator class or object in Kotlin source.
	 * @return Value-type view, or `null` when no usable binding is found.
	 */
	private fun extractValueTypeFromKt(validator: KtClassOrObject): ValidatorTypeView? {
		for (entry in validator.superTypeListEntries) {
			val typeRef = when (entry) {
				is KtSuperTypeCallEntry -> entry.typeReference
				is KtSuperTypeEntry -> entry.typeReference
				else -> null
			} ?: continue
			val userType = typeRef.typeElement as? KtUserType ?: continue
			if (isConstraintValidatorType(userType)) {
				val args = userType.typeArgumentsAsTypes
				if (args.isEmpty()) return null
				val vRef = args[0] ?: return ValidatorTypeView.WILDCARD
				val vElem = vRef.typeElement ?: return ValidatorTypeView.WILDCARD
				return SubjectTypeViews.ofTypeElement(vElem)
					?: starProjectionView(vElem.text)
			}
			// Intermediate base (e.g. `FooValidator : AbstractFooValidator()`).
			val baseName = userType.referencedName ?: continue
			val base = validator.containingKtFile.declarations
				.filterIsInstance<KtClassOrObject>()
				.firstOrNull { it.name == baseName }
				?: PropertyRefAttributeDiscovery.pickBestClass(
					PropertyRefAttributeDiscovery.findClassesByShortName(
						validator.project,
						baseName,
						validator.resolveScope,
					),
					preferredPackage = validator.containingKtFile.packageFqName.asString()
						.takeIf { it.isNotEmpty() },
				) as? KtClassOrObject
			if (base != null) {
				extractValueTypeFromKt(base)?.let { return it }
			}
		}
		return null
	}

	/**
	 * Walks PSI supertypes for `ConstraintValidator` and maps the first type parameter to a
	 * [ValidatorTypeView], recursively following intermediate bases.
	 *
	 * Matches by [PropertyRefLibraryFqns.CONSTRAINT_VALIDATOR] or simple name
	 * `ConstraintValidator` when FQCN stubs are incomplete.
	 *
	 * @param validator Validator PSI class.
	 * @return Value-type view, or `null` when unbound.
	 */
	private fun extractValueTypeFromPsi(validator: PsiClass): ValidatorTypeView? {
		for (superType in validator.superTypes) {
			val resolved = superType.resolve() ?: continue
			if (resolved.qualifiedName != PropertyRefLibraryFqns.CONSTRAINT_VALIDATOR &&
				resolved.name != "ConstraintValidator"
			) {
				continue
			}
			val params = superType.parameters
			if (params.isEmpty()) return null
			return psiTypeToView(params[0])
		}
		for (superType in validator.superTypes) {
			val parent = superType.resolve() ?: continue
			extractValueTypeFromPsi(parent)?.let { return it }
		}
		return null
	}

	/**
	 * Whether [userType] names Validata’s `ConstraintValidator` interface.
	 *
	 * @param userType Supertype reference from a validator class.
	 * @return `true` for simple name `ConstraintValidator` or a rendered prefix ending with
	 *   that name / matching the library FQCN.
	 */
	private fun isConstraintValidatorType(userType: KtUserType): Boolean {
		val name = userType.referencedName ?: return false
		if (name == "ConstraintValidator") return true
		val rendered = userType.text.substringBefore('<')
		return rendered.endsWith("ConstraintValidator") ||
			rendered == PropertyRefLibraryFqns.CONSTRAINT_VALIDATOR
	}

	/**
	 * Builds a [ValidatorTypeView] for common open types written with star projections when
	 * [SubjectTypeViews.ofTypeElement] cannot classify the PSI type element.
	 *
	 * Recognizes `Comparable`, `CharSequence`, `Number`, and `Any` (Kotlin FQCNs). Presence of
	 * `<` adds a single [ValidatorTypeView.WILDCARD] type argument.
	 *
	 * @param text Raw type-element text (e.g. `Comparable<*>`).
	 * @return Type view, or `null` for unrecognized short names.
	 */
	private fun starProjectionView(text: String): ValidatorTypeView? {
		val raw = text.substringBefore('<').trim().removeSuffix("?")
		val short = raw.substringAfterLast('.')
		val fq = BuiltinTypeShortNames.ALL[short] ?: return null
		val args = if ('<' in text) listOf(ValidatorTypeView.WILDCARD) else emptyList()
		return SubjectTypeViews.typeView(fq, args)
	}

	/**
	 * Maps a PSI type (typically `ConstraintValidator`’s `V`) to a [ValidatorTypeView].
	 *
	 * Resolves [PsiClassType] to Kotlin-oriented FQCNs for common JDK / kotlin stubs via schema
	 * [BuiltinTypeShortNames] so ranking matches KSP’s view of the same validators.
	 * Unresolved / non-class types become [ValidatorTypeView.WILDCARD].
	 *
	 * @param type PSI type argument.
	 * @return Recursive type view including type arguments.
	 */
	private fun psiTypeToView(type: PsiType): ValidatorTypeView {
		if (type is PsiClassType) {
			val q = type.resolve()?.qualifiedName
				?: type.className?.let { short ->
					BuiltinTypeShortNames.ALL[short]
						?: "${TypeNames.JAVA_LANG_PACKAGE_PREFIX}$short"
				}
				?: return ValidatorTypeView.WILDCARD
			val args = type.parameters.map { psiTypeToView(it) }
			return SubjectTypeViews.typeView(q, args)
		}
		return ValidatorTypeView.WILDCARD
	}

	/**
	 * Finds a named value argument on a Kotlin annotation entry.
	 *
	 * @param entry Annotation whose arguments are searched.
	 * @param name Argument name (typically `validatedBy`).
	 * @return Matching [KtValueArgument], or `null`.
	 */
	private fun namedArgument(entry: KtAnnotationEntry, name: String): KtValueArgument? {
		entry.valueArguments.forEach { arg ->
			val ktArg = arg as? KtValueArgument ?: return@forEach
			if (ktArg.getArgumentName()?.asName?.asString() == name) return ktArg
		}
		return null
	}
}

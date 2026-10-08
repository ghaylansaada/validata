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
package io.ghaylan.validata.processor.compat

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter

/**
 * Resolves the `V` and `C` type arguments of `ConstraintValidator<V, C>` for a validator class.
 *
 * Used by catalog and constraint builders so each `@Constraint(validatedBy = …)` entry knows the
 * accepted value type and metadata type at compile time. The runtime never re-derives these
 * bindings via `kotlin-reflect`.*
 * 
 * @author Ghaylan Saada
 */
internal class ConstraintValidatorTypeResolver {
	
	/**
	 * Walks [validatorDecl]'s type hierarchy until `ConstraintValidator<V, C>` is found.
	 *
	 * Tries the class as a star-projected type first, then each declared supertype — covering both
	 * `object Foo : ConstraintValidator<…>` and intermediate abstract bases.
	 *
	 * Side effects: none.
	 *
	 * @param validatorDecl Class listed in `@Constraint(validatedBy = …)`.
	 * @return Value type `V` and metadata type `C`, or `null` when the hierarchy does not implement
	 *   the validator base type identified by [CONSTRAINT_VALIDATOR_FQN].	 
	 */
	fun resolve(validatorDecl: KSClassDeclaration): ConstraintValidatorResolvedTypes? {
		return findConstraintValidator(validatorDecl.asStarProjectedType())
			?: validatorDecl.superTypes.firstNotNullOfOrNull { findConstraintValidator(it.resolve()) }
	}
	
	/**
	 * Depth-first search of [type] and its supertypes for `ConstraintValidator<V, C>`.
	 *
	 * Side effects: mutates [visited] while walking supertypes.
	 *
	 * @param type Starting type in the validator hierarchy.
	 * @param visited Qualified names already visited (cycle guard).
	 * @return Both type arguments when found and fully resolvable; `null` otherwise.	 
	 */
	private fun findConstraintValidator(
		type: KSType,
		visited: MutableSet<String> = HashSet(),
	): ConstraintValidatorResolvedTypes? {
		val decl = type.declaration as? KSClassDeclaration
			?: return null
		val qName = decl.qualifiedName?.asString()
		if (qName != null && !visited.add(qName)) return null
		if (qName == CONSTRAINT_VALIDATOR_FQN) {
			val args = type.arguments
			if (args.size != 2) return null
			val value = args[0].type?.resolve()
				?: return null
			val metadata = args[1].type?.resolve()
				?: return null
			return ConstraintValidatorResolvedTypes(valueType = value, metadataType = metadata)
		}
		for (superType in decl.superTypes) {
			findConstraintValidator(superType.resolve(), visited)?.let { return it }
		}
		return null
	}
	
	/**
	 * `true` when [type] is still an unresolved type *parameter* (e.g. `T` on a generic validator).
	 *
	 * Such a type cannot become a catalog key or a generated `valueType` literal — the catalog
	 * builder reports a compile error instead.
	 *
	 * Side effects: none.
	 *
	 * @param type Candidate value or metadata type argument.
	 * @return `true` when [type] is still an unresolved type parameter.	 
	 */
	fun isUnresolvedTypeParameter(type: KSType): Boolean =
		type.declaration is KSTypeParameter
	
	companion object {
		
		/**
		 * Fully qualified name of the validator base class in the API module.
		 *
		 * Must match the type used in `@Constraint(validatedBy = …)` hierarchies; this processor
		 * cannot import it (classpath cycle), so the FQCN is matched as a string.
		 */
		const val CONSTRAINT_VALIDATOR_FQN = "io.ghaylan.validata.constraint.ConstraintValidator"
	}
}

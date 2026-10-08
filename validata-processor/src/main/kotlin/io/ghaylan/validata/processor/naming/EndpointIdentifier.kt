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
package io.ghaylan.validata.processor.naming

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import io.ghaylan.validata.processor.compat.TypeClassification
import io.ghaylan.validata.schema.types.KnownTypes
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Builds the endpoint identifier string that must match `Method.getUniqueIdentifier()` byte-for-byte.
 *
 * Format: `pkg.ClassName#methodName(pkg.ParamType1,pkg.ParamType2)`.
 * Parameter types are JVM-erased names as [Class.getTypeName] reports them.
 * Suspend handlers omit the synthetic `Continuation` parameter so compile-time and runtime keys agree.
 * Mirrored by the runtime `getUniqueIdentifier` extension on `java.lang.reflect.Method` in validata-core.*
 * 
 * @author Ghaylan Saada
 */
internal object EndpointIdentifier {

	/**
	 * Computes the unique endpoint id for [function].
	 *
	 * Side effects: none.
	 *
	 * @param function Handler function (must have a class owner).
	 * @return Identifier string matching runtime `getUniqueIdentifier()`.
	 * @throws IllegalStateException When [function] has no class owner.
	 */
	fun of(function: KSFunctionDeclaration): String {
		val owner = function.parentDeclaration as? KSClassDeclaration
			?: error("Endpoint function '${function.simpleName.asString()}' has no class owner")
		val ownerName = jvmBinaryName(owner)
		val methodName = function.simpleName.asString()
		val paramTypeNames = function.parameters.mapNotNull { param ->
			val typeName = jvmTypeName(param.type.resolve())
			// Exact match — same as runtime `Class.name == Continuation` (erased; no type args).
			if (typeName == TypeNames.CONTINUATION_KOTLIN) null else typeName
		}
		return buildString {
			append(ownerName)
			append('#')
			append(methodName)
			append('(')
			paramTypeNames.joinTo(this, ",")
			append(')')
		}
	}

	/**
	 * Renders a KSP type as the JVM erased name [Class.getTypeName] would produce.
	 *
	 * Side effects: none.
	 *
	 * @param type Resolved parameter type.
	 * @return JVM erased type name for the endpoint id.
	 */
	internal fun jvmTypeName(type: KSType): String {
		val decl = type.declaration
		val qName = decl.qualifiedName?.asString() ?: return TypeNames.OBJECT_JAVA

		if (TypeClassification.isArray(type)) {
			// IntArray / LongArray / … → int[] / long[] / … (matches Class.getTypeName).
			KnownTypes.jvmErasedName(qName, notNull = true)?.let { return it }

			// kotlin.Array<T> always erases to a reference-component array (Array<Int> → Integer[]),
			// never a JVM primitive array — even when the element type is a non-null Kotlin primitive.
			val element = type.arguments.firstOrNull()?.type?.resolve()
			return if (element != null) {
				"${jvmArrayComponentName(element)}[]"
			} else {
				"${TypeNames.OBJECT_JAVA}[]"
			}
		}

		val notNull = type.nullability == Nullability.NOT_NULL
		KnownTypes.jvmErasedName(qName, notNull)?.let { return it }

		val classDecl = decl as? KSClassDeclaration
		return if (classDecl != null) jvmBinaryName(classDecl) else qName
	}

	/**
	 * JVM component type name for `kotlin.Array<T>` — always a reference type ([Class.getTypeName]).
	 *
	 * Forces boxed names for Kotlin primitives (`Int` → `java.lang.Integer`) so non-null
	 * `Array<Int>` matches reflection (`Integer[]`), not `int[]`.
	 *
	 * Side effects: none.
	 *
	 * @param element Resolved array element type.
	 * @return JVM reference component name for the endpoint id.
	 */
	private fun jvmArrayComponentName(element: KSType): String {
		val qName = element.declaration.qualifiedName?.asString() ?: return TypeNames.OBJECT_JAVA
		// notNull = false → primitives become boxed java.lang.* names.
		KnownTypes.jvmErasedName(qName, notNull = false)?.let { return it }
		val classDecl = element.declaration as? KSClassDeclaration
		return if (classDecl != null) jvmBinaryName(classDecl) else qName
	}

	/**
	 * JVM binary name for [decl], matching [Class.getName] / [Class.getTypeName] for classes.
	 *
	 * KSP [KSClassDeclaration.qualifiedName] uses Kotlin nested form (`Outer.Inner`);
	 * runtime ids use JVM form (`Outer$Inner`).
	 *
	 * Side effects: none.
	 *
	 * @param decl Class declaration to render.
	 * @return JVM binary name (`pkg.Outer$Inner`).
	 */
	internal fun jvmBinaryName(decl: KSClassDeclaration): String {
		val simpleNames = ArrayList<String>(4)
		var current: KSClassDeclaration? = decl
		while (current != null) {
			simpleNames.add(current.simpleName.asString())
			current = current.parentDeclaration as? KSClassDeclaration
		}
		simpleNames.reverse()
		val nested = simpleNames.joinToString("$")
		val pkg = decl.packageName.asString()
		return if (pkg.isEmpty()) nested else "$pkg.$nested"
	}
}

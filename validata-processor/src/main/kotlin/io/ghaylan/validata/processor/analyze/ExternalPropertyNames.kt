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

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.ghaylan.validata.processor.compat.AnnotationFqcn
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns
import io.ghaylan.validata.processor.naming.JacksonPropertyNaming

/**
 * Resolves the wire / error-path name for a Kotlin property.
 *
 * Order: non-blank `@JsonProperty` on the property, getter, or overridee → else
 * [JacksonPropertyNaming.translate] on the declared name.
 *
 * Shared by [SchemaModelBuilder] and RequiredWhen gate indexing so schema IR and compile-time
 * ref checks stay aligned.
 *
 * @author Ghaylan Saada
 */
internal object ExternalPropertyNames {

	/**
	 * Wire name for [property] under [naming].
	 *
	 * Side effects: none.
	 *
	 * @param property Kotlin property declaration
	 * @param naming KSP naming strategy when `@JsonProperty` is absent
	 * @return external / wire name
	 */
	fun resolve(
		property: KSPropertyDeclaration,
		naming: JacksonPropertyNaming,
	): String {
		val declared = property.simpleName.asString()
		jsonPropertyValue(property)?.takeIf { it.isNotBlank() }?.let { return it }
		return naming.translate(declared)
	}

	/**
	 * Explicit `@JsonProperty` value on [property] / getter / overridee, or `null`.
	 *
	 * Side effects: none.
	 */
	fun jsonPropertyValue(property: KSPropertyDeclaration): String? {
		val sources = buildList {
			addAll(property.annotations)
			property.getter?.annotations?.let { addAll(it) }
			property.findOverridee()?.let { overridee ->
				addAll(overridee.annotations)
				overridee.getter?.annotations?.let { addAll(it) }
			}
		}
		val jsonProp = sources.firstOrNull {
			AnnotationFqcn.isA(it, ProcessorFqns.JSON_PROPERTY)
		} ?: return null
		return jsonProp.arguments
			.firstOrNull {
				it.name?.asString() == AnnotationAttrs.Jackson.VALUE || it.name == null
			}
			?.value as? String
	}
}

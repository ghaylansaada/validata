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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.compat.KotlinStringLiteral
import io.ghaylan.validata.processor.model.EndpointModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * Emits a flat `<Controller>_<method>[_fingerprint]_` constants object for path / query / header
 * wire names on one `@Validate` handler.
 *
 * Values are [EndpointParameterModel.resolvedName] (Spring `name` / `value` / declared).
 * Identifiers are SCREAMING_SNAKE from [EndpointParameterModel.declaredName], same rules as
 * [FieldsObjectCodeWriter]. Body parameters are omitted (use the request DTO `Type_` object).
 *
 * Skipped when the endpoint has no path/query/header parameters.
 *
 * @author Ghaylan Saada
 */
internal object EndpointPathConstantsCodeWriter {

	/**
	 * Whether [model] has at least one flat transport parameter to emit.
	 */
	fun shouldWrite(model: EndpointModel): Boolean =
		model.parameters.any { it.kind.isFlatTransport() }

	/** File / object simple name: `UserController_lookup_1988a7ee_`. */
	fun fileName(model: EndpointModel): String =
		GeneratedNames.endpointPathConstantsFileName(model.functionQualifiedName, model.identifier)

	/** Generated top-level object simple name (matches [fileName]). */
	fun objectName(model: EndpointModel): String =
		GeneratedNames.endpointPathConstantsObjectName(model.functionQualifiedName, model.identifier)

	/**
	 * Renders path-constant source for [model], or `null` when there are no flat params.
	 *
	 * @param packageName package of the generated file
	 * @param model endpoint under emission
	 * @return complete Kotlin source, or `null` when [shouldWrite] is false
	 */
	fun write(
		packageName: String,
		model: EndpointModel,
	): String? {
		val flat = model.parameters.filter { it.kind.isFlatTransport() }
		if (flat.isEmpty()) return null

		val b = KotlinSourceBuilder(packageName)
		val name = objectName(model)
		val controllerFqcn = model.functionQualifiedName.substringBeforeLast('.')
		val methodName = model.functionQualifiedName.substringAfterLast('.')
		val controllerSimple = controllerFqcn.substringAfterLast('.')

		b.addImport(controllerFqcn)
		b.line("/**")
		b.line(" * Generated wire-path constants for [$controllerSimple.$methodName] path / query / header parameters.")
		b.line(" *")
		b.line(" * Values are Spring-effective names (`@PathVariable` / `@RequestParam` / `@RequestHeader`).")
		b.line(" * Use for client-visible error paths. Regenerated whenever the handler signature changes.")
		b.line(" */")
		b.block("object $name") {
			val used = mutableSetOf<String>()
			for (param in flat) {
				val constName = FieldsObjectCodeWriter.uniqueConstName(param.declaredName, used)
				line("/** Wire path to `${param.resolvedName}` (declared `${param.declaredName}`). */")
				line("const val $constName: String = \"${escape(param.resolvedName)}\"")
				line()
			}
		}
		return b.build(model.functionQualifiedName)
	}

	private fun EndpointArgumentKind.isFlatTransport(): Boolean =
		this == EndpointArgumentKind.PATH ||
			this == EndpointArgumentKind.QUERY ||
			this == EndpointArgumentKind.HEADER

	private fun escape(s: String): String =
		KotlinStringLiteral.escape(s)
}

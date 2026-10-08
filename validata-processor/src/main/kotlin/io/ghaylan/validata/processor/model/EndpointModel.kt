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
package io.ghaylan.validata.processor.model

import io.ghaylan.validata.schema.request.EndpointArgumentKind

/**
 * Intermediate model for one `@Validate` handler method.
 *
 * Analysis produces this graph; codegen renders an `EndpointSchema` factory.
 * [NamedShapeLookup] from [flatParameterLookup] lets PropertyReferenceVerifier check
 * cross-field refs against flat parameter names.
 *
 * @property identifier Runtime lookup key — must match `Method.getUniqueIdentifier()`.
 * @property packageName Package of the declaring controller (generated file placement).
 * @property functionQualifiedName Human-readable source coordinate for banners / KDoc.
 * @property sourceFilePath Optional path of the declaring source file (diagnostics only; processors
 *   re-resolve KSFile from the current round's resolver).
 * @property oneErrorPerParam From the effective `@Validate`.
 * @property failFast From the effective `@Validate` (whole-request abort; default false).
 * @property groupsFqcn Fully qualified group marker class names.
 * @property parameters Classified transport-annotated parameters in declaration order.
 * @property argumentLayout Every method parameter in declaration order, including
 *   [EndpointArgumentKind.OTHER] for non-transport slots.*
 * 
 * @author Ghaylan Saada
 */
internal data class EndpointModel(
	val identifier: String,
	val packageName: String,
	val functionQualifiedName: String,
	val sourceFilePath: String?,
	val oneErrorPerParam: Boolean,
	val failFast: Boolean,
	val groupsFqcn: List<String>,
	val parameters: List<EndpointParameterModel>,
	val argumentLayout: List<EndpointArgumentSlotModel> = emptyList(),
) {
	
	/**
	 * Flat-parameter name → shape lookup for sibling refs among query/header/path parameters.
	 *
	 * Accepts both [EndpointParameterModel.declaredName] and [EndpointParameterModel.resolvedName].
	 *
	 * Side effects: none.
	 *
	 * @return Lookup that returns a shape for a flat parameter name, or null when unknown / BODY.	 
	 */
	fun flatParameterLookup(): NamedShapeLookup {
		val byName = HashMap<String, ShapeModel>()
		for (parameter in parameters) {
			if (parameter.kind == EndpointArgumentKind.BODY) continue
			byName[parameter.declaredName] = parameter.shape
			byName[parameter.resolvedName] = parameter.shape
		}
		return NamedShapeLookup { name -> byName[name] }
	}
}

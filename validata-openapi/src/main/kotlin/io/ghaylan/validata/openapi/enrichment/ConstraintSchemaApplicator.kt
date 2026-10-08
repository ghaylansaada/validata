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
package io.ghaylan.validata.openapi.enrichment

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.annotation.RequiredConstraint
import io.ghaylan.validata.openapi.docs.ConstraintDocumentations
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
import io.ghaylan.validata.openapi.mapper.OpenApiConstraintMappers
import io.ghaylan.validata.schema.shape.TypeShape
import io.swagger.v3.oas.models.media.Schema

/**
 * Applies Validata constraint metadata to an OpenAPI schema as **native facets**.
 *
 * Custom constraint payloads are not written here — see [PropertyOpenApiExtensionsWriter]
 * (`x-validata-constraints` array). Non-numeric Min / Max / MultipleOf have no native number
 * facet but still appear in that array.
 *
 * [RequiredConstraint] is expressed via the caller adding the property name to the parent
 * schema’s OpenAPI `required` list (or `Parameter.required` for params) — never a vendor
 * extension.
 *
 * Resolution order:
 * 1. [OpenApiConstraintMapper] SPI that returns `true` (full control — apps / advanced)
 * 2. [ConstraintDocumentations] SPI → [JsonSchemaFacetApplicator] when hints are non-empty
 * 3. Else if a documenter **owns** the metadata → done (empty hints = intentional; no unmapped)
 * 4. Else if **custom** → skip native facets (array entry still written by the extensions writer)
 * 5. Else **standard** with no documenter → [ConstraintExtensionKeys.UNMAPPED]*
 * 
 * @author Ghaylan Saada
 */
internal object ConstraintSchemaApplicator {

	/**
	 * Maps one constraint occurrence onto [schema].
	 *
	 * Mutates [schema] facets and/or [ConstraintExtensionKeys.UNMAPPED] when applicable.
	 *
	 * @param metadata constraint from Validata IR
	 * @param schema target OpenAPI schema
	 * @param shape structural hint for shape-sensitive documenters; may be `null`
	 * @param mappers OpenAPI override SPI; defaults to [OpenApiConstraintMappers.all]
	 * @return `true` when the constraint implies parent OpenAPI `required` / parameter required
	 */
	fun apply(
		metadata: ConstraintMetadata,
		schema: Schema<*>,
		shape: TypeShape?,
		mappers: List<OpenApiConstraintMapper> = OpenApiConstraintMappers.all(),
	): Boolean {
		if (mappers.any { it.apply(metadata, schema, shape) }) {
			return metadata is RequiredConstraint
		}
		val documenter = ConstraintDocumentations.supporting(metadata)
		if (documenter != null) {
			val hints = documenter.hints(metadata, shape)
			return !hints.isEmpty && JsonSchemaFacetApplicator.apply(hints, schema)
			// Owned constraint with no native facet (e.g. temporal Min) — not unmapped.
		}
		// Custom / owned constraints still get x-validata-constraints array entries from the writer.
		if (StandardConstraintClassifier.isCustom(metadata)) {
			return false
		}
		appendUnmapped(schema, metadata::class.java.name)
		return false
	}

	/**
	 * Appends [className] to [ConstraintExtensionKeys.UNMAPPED] as a sorted distinct list.
	 *
	 * Mutates [schema] extensions. First unmapped creates the extension; later names are merged.
	 *
	 * @param schema OpenAPI schema receiving the unmapped list
	 * @param className fully qualified metadata class name
	 */
	private fun appendUnmapped(schema: Schema<*>, className: String) {
		val existing = schema.extensions?.get(ConstraintExtensionKeys.UNMAPPED)
		val names = linkedSetOf<String>()
		when (existing) {
			is Collection<*> -> existing.mapNotNullTo(names) { it?.toString() }
			is String -> names += existing
		}
		names += className
		schema.addExtension(ConstraintExtensionKeys.UNMAPPED, names.sorted())
	}
}

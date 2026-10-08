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
package io.ghaylan.validata.processor.fqns

import io.ghaylan.validata.processor.model.PropertyModel
import io.ghaylan.validata.processor.model.SchemaModel
import io.ghaylan.validata.processor.verify.PropertyRefHostDiscovery
import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
import io.ghaylan.validata.schema.request.spi.RequestSchemaModule
import io.ghaylan.validata.schema.spi.ObjectSchemaModule

/**
 * Fully qualified names the processor matches against during analysis.
 *
 * ## Why this object exists
 * KSP sees annotations as strings / symbols. Comparing by short name (`"Validatable"`) is brittle
 * (collisions, wrong packages). Every annotation / SPI string the processor cares about lives here
 * so a reader can answer “what does this processor recognize?” in one file.
 *
 * ## How values are used
 * | Constant | Used when |
 * |---|---|
 * | [VALIDATABLE] / [NO_CASCADE] | Object schema roots and cascade rules |
 * | [CONSTRAINT] | Detecting validation constraint annotations |
 * | [PROPERTY_REF] | Documented by `Type_` codegen; marks path-valued metadata |
 * | [JSON_PROPERTY] / [JSON_IGNORE] | External names and property omission |
 * | [VALIDATE] / Spring mapping + transport FQCNs | Endpoint schema discovery |
 * | [OBJECT_SCHEMA_MODULE_SPI] | `META-INF/services` for `ObjectSchemaModule` |
 * | [REQUEST_SCHEMA_MODULE_SPI] | `META-INF/services` for `RequestSchemaModule` |
 * | [CONSTRAINT_CATALOG_SPI] | `META-INF/services` for `ConstraintCatalog` (`validata-core`) |
 * | [GENERATED_FALLBACK_PACKAGE] | Aggregator package when types share no prefix |
 *
 * Keep strings exact — they must match types in **`validata-core`** (annotations / SPI),
 * Spring Web (mapping / transport), and **`validata-schema`** (SPI module interfaces).
 * This module only **depends** on `validata-schema` at compile time; other FQCNs exist on the
 * **consumer** classpath (processor tests prove them via `validata-core` / `validata-core`).
 * 
 * @author Ghaylan Saada
 */
internal object ProcessorFqns {
	
	/**
	 * `Validatable` — marks a type as a schema root (and a legal cascade target).
	 *
	 * Without this, the processor will not emit an `ObjectSchema` for the type, and cascading
	 * into it is a compile error unless [NO_CASCADE] is present.
	 */
	const val VALIDATABLE = "io.ghaylan.validata.schema.Validatable"
	
	/**
	 * `NoCascade` — on a property: treat the value as opaque; do not emit an object-ref shape
	 * and do not require the nested type to be `@Validatable`.
	 */
	const val NO_CASCADE = "io.ghaylan.validata.schema.NoCascade"
	
	/**
	 * Meta-annotation `Constraint` on constraint annotations.
	 *
	 * Carries `validatedBy = […]`. Metadata FQCN is implied as
	 * `{annotationPackage}.{AnnotationSimpleName}Constraint`.
	 */
	const val CONSTRAINT = "io.ghaylan.validata.constraint.Constraint"
	
	/**
	 * `PropertyRef` — marks a metadata field whose value is a property path (sibling or
	 * element), not payload data.
	 *
	 * [PropertyRefHostDiscovery] reads these markers to decide which annotation arguments are
	 * path hosts and which [PropertyRefCompatibilityKind] applies. Generated `Type_` constants
	 * remain the preferred authoring form for those args.
	 */
	const val PROPERTY_REF = "io.ghaylan.validata.constraint.PropertyRef"
	
	/**
	 * `ConstraintArg` — marks a metadata field whose value is a constraint argument requiring
	 * tooling checks (`ConstraintArgKind` vocabulary).
	 *
	 * Discovered by `ConstraintArgHostDiscovery` (Phase 2+) the same way [PROPERTY_REF] is discovered.
	 */
	const val CONSTRAINT_ARG = "io.ghaylan.validata.constraint.ConstraintArg"
	
	/**
	 * Container for repeatable [CONSTRAINT_ARG] (`@ConstraintArgs`).
	 */
	const val CONSTRAINT_ARGS = "io.ghaylan.validata.constraint.ConstraintArgs"
	
	/**
	 * `ConstraintComposition` — marks a composed annotation and selects AND/OR expand mode.
	 */
	const val CONSTRAINT_COMPOSITION = "io.ghaylan.validata.constraint.ConstraintComposition"
	
	/**
	 * Nested `ConstraintComposition.Mode` — AND / OR enum for [CONSTRAINT_COMPOSITION].
	 *
	 * JVM binary name uses `$` so [Class.forName] resolves the nested enum (Kotlin FQN uses `.`).
	 */
	const val COMPOSITION_MODE = $$"io.ghaylan.validata.constraint.ConstraintComposition$Mode"
	
	/**
	 * `ConstraintMessage` — role marker: annotation param → generated `ConstraintMetadata.message`.
	 */
	const val CONSTRAINT_MESSAGE = "io.ghaylan.validata.constraint.ConstraintMessage"
	
	/**
	 * `ConstraintGroups` — role marker: annotation param → generated `ConstraintMetadata.groups`.
	 */
	const val CONSTRAINT_GROUPS = "io.ghaylan.validata.constraint.ConstraintGroups"
	
	/**
	 * `ConstraintMetadata` — abstract metadata base / sentinel.
	 */
	const val CONSTRAINT_METADATA = "io.ghaylan.validata.constraint.ConstraintMetadata"
	
	/**
	 * `@RequiredWhen` — conditional presence; gate typing uses [REQUIRED_WHEN] FQCN (not short name).
	 */
	const val REQUIRED_WHEN = "io.ghaylan.validata.constraint.annotation.RequiredWhen"
	
	/**
	 * `ConstraintArgKind` — enum entries parsed from `@ConstraintArg(kinds = …)` arguments.
	 */
	const val CONSTRAINT_ARG_KIND = "io.ghaylan.validata.schema.ref.ConstraintArgKind"
	
	/**
	 * `ConstraintArgTarget` — VALUE / ELEMENT for `@ConstraintArg`.
	 */
	const val CONSTRAINT_ARG_TARGET = "io.ghaylan.validata.schema.ref.ConstraintArgTarget"
	
	/**
	 * Jackson `@JsonProperty` — overrides the wire / error-path name
	 * ([PropertyModel.externalName]).
	 * Matched on the property or its getter (Kotlin use-site targets).
	 */
	const val JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty"
	
	/**
	 * Jackson `@JsonIgnore` — property is omitted from the generated schema entirely
	 * (not validated, not present in [SchemaModel.properties]).
	 */
	const val JSON_IGNORE = "com.fasterxml.jackson.annotation.JsonIgnore"
	
	/**
	 * ServiceLoader resource base name for generated [ObjectSchemaModule] implementations.
	 *
	 * KSP writes `META-INF/services/<this value>` containing the generated class’s FQCN.
	 * Must stay in sync with `ObjectSchemaModule`'s package in `validata-schema`.
	 */
	const val OBJECT_SCHEMA_MODULE_SPI = "io.ghaylan.validata.schema.spi.ObjectSchemaModule"
	
	/**
	 * ServiceLoader resource base name for generated `ConstraintCatalog` implementations.
	 *
	 * That SPI lives in the root Spring library (not `validata-schema`). Must stay in sync with
	 * the interface package there.
	 */
	const val CONSTRAINT_CATALOG_SPI = "io.ghaylan.validata.constraint.spi.ConstraintCatalog"
	
	/**
	 * ServiceLoader resource base name for generated [RequestSchemaModule] implementations.
	 *
	 * KSP writes `META-INF/services/<this value>` containing the generated aggregator’s FQCN.
	 * Must stay in sync with `RequestSchemaModule`'s package in `validata-schema`.
	 */
	const val REQUEST_SCHEMA_MODULE_SPI = "io.ghaylan.validata.schema.request.spi.RequestSchemaModule"
	
	/**
	 * `Validate` — marks a controller class / handler method (endpoint schema root) or a
	 * `@ConfigurationProperties` class (startup validation in the Spring host).
	 * KSP endpoint discovery uses this FQN for FUNCTION and CLASS placements on MVC handlers.
	 */
	const val VALIDATE = "io.ghaylan.validata.schema.Validate"
	
	/**
	 * Spring `@RequestBody` — classifies a handler parameter as the request body section.
	 *
	 * At most one body parameter per method; its type must be `@Validatable` when provable.
	 */
	const val REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody"
	
	/**
	 * Spring `@RequestParam` — classifies a handler parameter as a query parameter.
	 *
	 * Effective name: `name` attribute, then `value`, then the Kotlin parameter name.
	 */
	const val REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam"
	
	/**
	 * Spring `@RequestHeader` — classifies a handler parameter as a request header.
	 *
	 * Effective name precedence matches [REQUEST_PARAM].
	 */
	const val REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader"
	
	/**
	 * Spring `@PathVariable` — classifies a handler parameter as a path variable.
	 *
	 * Effective name precedence matches [REQUEST_PARAM].
	 */
	const val PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable"
	
	/**
	 * Spring mapping annotations that mark a function as an HTTP handler.
	 *
	 * Used when expanding class-level `@Validate` to member functions: only mapped methods
	 * become endpoint candidates.
	 *
	 * Side effects: none (immutable set).
	 */
	val SPRING_MAPPING_ANNOTATIONS: Set<String> = setOf(
		"org.springframework.web.bind.annotation.RequestMapping",
		"org.springframework.web.bind.annotation.GetMapping",
		"org.springframework.web.bind.annotation.PostMapping",
		"org.springframework.web.bind.annotation.PutMapping",
		"org.springframework.web.bind.annotation.DeleteMapping",
		"org.springframework.web.bind.annotation.PatchMapping")
	
	/**
	 * Default validation group when `@Validate(groups=…)` is omitted.
	 *
	 * Must stay aligned with the `OnDefault` validation group marker in **validata-core**.
	 */
	const val ON_DEFAULT = "io.ghaylan.validata.groups.OnDefault"
	
	/**
	 * Package used for SPI aggregators when processed types share no common prefix (e.g. `com.acme`
	 * and `org.other`), or when the package list is empty.
	 *
	 * Library-owned — not a bare `*.generated` sibling that other tools might also claim.
	 */
	const val GENERATED_FALLBACK_PACKAGE = "io.ghaylan.validata.generated"
	
	/**
	 * Every type / annotation FQCN this processor matches by string during analysis.
	 *
	 * Excludes [GENERATED_FALLBACK_PACKAGE] (a package name, not a loadable type).
	 * Covered by `HardcodedFqcnExistenceTest` so renames cannot silently break discovery.
	 *
	 * Side effects: none (immutable list).
	 */
	val ALL_LOADABLE: List<String> = listOf(
		VALIDATABLE,
		NO_CASCADE,
		CONSTRAINT,
		CONSTRAINT_COMPOSITION,
		COMPOSITION_MODE,
		PROPERTY_REF,
		CONSTRAINT_ARG,
		CONSTRAINT_ARGS,
		CONSTRAINT_MESSAGE,
		CONSTRAINT_GROUPS,
		CONSTRAINT_METADATA,
		REQUIRED_WHEN,
		CONSTRAINT_ARG_KIND,
		CONSTRAINT_ARG_TARGET,
		JSON_PROPERTY,
		JSON_IGNORE,
		OBJECT_SCHEMA_MODULE_SPI,
		CONSTRAINT_CATALOG_SPI,
		REQUEST_SCHEMA_MODULE_SPI,
		VALIDATE,
		REQUEST_BODY,
		REQUEST_PARAM,
		REQUEST_HEADER,
		PATH_VARIABLE,
		ON_DEFAULT,
	) + SPRING_MAPPING_ANNOTATIONS.sorted()
}

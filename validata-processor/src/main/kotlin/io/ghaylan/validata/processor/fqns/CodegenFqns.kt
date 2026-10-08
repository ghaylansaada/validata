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

/**
 * Fully qualified names written into generated Kotlin imports / constructor calls.
 *
 * String literals required — processor cannot always `import` target type (classpath cycle with consumer Spring libraries).
 * [ALL_LOADABLE] contract: every entry must resolve via `Class.forName` on consumer / processor-test classpath.
 * Use these constants; do not invent ad-hoc `"io.ghaylan…"` import strings.
 * 
 * @author Ghaylan Saada

 */
internal object CodegenFqns {

	// —— validata-schema IR ——

	/**
	 * `ObjectSchema` — generated schema class for a `@Validatable` root.

	 */
	const val OBJECT_SCHEMA = "io.ghaylan.validata.schema.ObjectSchema"

	/**
	 * `PropertySpec` — generated per-property descriptor inside an [OBJECT_SCHEMA].

	 */
	const val PROPERTY_SPEC = "io.ghaylan.validata.schema.PropertySpec"

	/**
	 * `ValueReader` — generated property accessor used by the runtime engine.

	 */
	const val VALUE_READER = "io.ghaylan.validata.schema.ValueReader"

	/**
	 * `CompiledConstraint` — generated wrapper pairing a validator with its metadata call.

	 */
	const val COMPILED_CONSTRAINT = "io.ghaylan.validata.schema.constraint.CompiledConstraint"

	/**
	 * `DynamicShape` — generated shape for properties whose static type is opaque / `Any`.

	 */
	const val DYNAMIC_SHAPE = "io.ghaylan.validata.schema.shape.DynamicShape"

	/**
	 * `IterableShape` — generated shape describing a `List`/`Set`/array element shape.

	 */
	const val ITERABLE_SHAPE = "io.ghaylan.validata.schema.shape.IterableShape"

	/**
	 * `MapShape` — generated shape describing `Map` key/value shapes.

	 */
	const val MAP_SHAPE = "io.ghaylan.validata.schema.shape.MapShape"

	/**
	 * `ObjectRefShape` — generated shape referencing another `@Validatable` schema.

	 */
	const val OBJECT_REF_SHAPE = "io.ghaylan.validata.schema.shape.ObjectRefShape"

	/**
	 * `ScalarKind` — enum of primitive/scalar shape kinds emitted into generated schemas.

	 */
	const val SCALAR_KIND = "io.ghaylan.validata.schema.shape.ScalarKind"

	/**
	 * `ScalarShape` — generated shape for a scalar-valued property.

	 */
	const val SCALAR_SHAPE = "io.ghaylan.validata.schema.shape.ScalarShape"

	/**
	 * `ObjectSchemaModule` — SPI interface implemented by generated object-schema modules.

	 */
	const val OBJECT_SCHEMA_MODULE = "io.ghaylan.validata.schema.spi.ObjectSchemaModule"

	/**
	 * `EndpointSchema` — generated schema class for a validated handler method.

	 */
	const val ENDPOINT_SCHEMA = "io.ghaylan.validata.schema.request.EndpointSchema"

	/**
	 * `EndpointArgumentSlot` — generated descriptor for one handler parameter.

	 */
	const val ENDPOINT_ARGUMENT_SLOT = "io.ghaylan.validata.schema.request.EndpointArgumentSlot"

	/**
	 * `EndpointArgumentKind` — enum classifying a handler parameter (body / param / header / …).

	 */
	const val ENDPOINT_ARGUMENT_KIND = "io.ghaylan.validata.schema.request.EndpointArgumentKind"

	/**
	 * `RequestSchemaModule` — SPI interface implemented by generated endpoint-schema aggregators.

	 */
	const val REQUEST_SCHEMA_MODULE = "io.ghaylan.validata.schema.request.spi.RequestSchemaModule"

	/**
	 * `SchemaErrorDoc` — docs-only error code baked into generated schemas.

	 */
	const val SCHEMA_ERROR_DOC = "io.ghaylan.validata.schema.docs.SchemaErrorDoc"


	// —— validata-core / validata-core (not on processor main compile classpath) ——

	/**
	 * `CompiledConstraints` — factory for leaf / OR [COMPILED_CONSTRAINT] sites in generated schemas.
	 */
	const val COMPILED_CONSTRAINTS = "io.ghaylan.validata.engine.CompiledConstraints"

	/**
	 * `ValidatorBackedRunner` — generated schema base class that delegates to a validator instance.

	 */
	const val VALIDATOR_BACKED_RUNNER = "io.ghaylan.validata.engine.ValidatorBackedRunner"

	/**
	 * `CompositionConstraint` — hand-written OR composition metadata nested in generated schemas.

	 */
	const val COMPOSITION_CONSTRAINT = "io.ghaylan.validata.constraint.composition.CompositionConstraint"

	/**
	 * `CompositionOrRunner` — OR runner for [COMPOSITION_CONSTRAINT] sites.

	 */
	const val COMPOSITION_OR_RUNNER = "io.ghaylan.validata.constraint.composition.CompositionOrRunner"

	/**
	 * `GeneratedSchemaLookup` — marker implemented by generated schema modules for runtime lookup.

	 */
	const val GENERATED_SCHEMA_LOOKUP = "io.ghaylan.validata.schema.runtime.GeneratedSchemaLookup"

	/**
	 * `ConstraintCatalog` — SPI interface implemented by generated constraint-catalog modules.

	 */
	const val CONSTRAINT_CATALOG = "io.ghaylan.validata.constraint.spi.ConstraintCatalog"

	/**
	 * `ConstraintCatalogEntry` — generated per-annotation entry inside a [CONSTRAINT_CATALOG].

	 */
	const val CONSTRAINT_CATALOG_ENTRY = "io.ghaylan.validata.constraint.spi.ConstraintCatalogEntry"

	/**
	 * `ReflectionUtils` — runtime helper referenced by generated reflective fallbacks.

	 */
	const val REFLECTION_UTILS = "io.ghaylan.validata.internal.ReflectionUtils"

	/**
	 * `ConstraintMetadata` — abstract base class extended by generated `*Constraint` metadata.

	 */
	const val CONSTRAINT_METADATA = "io.ghaylan.validata.constraint.ConstraintMetadata"

	/**
	 * `ConstraintArg` — annotation referenced when generating constraint-argument metadata.

	 */
	const val CONSTRAINT_ARG = "io.ghaylan.validata.constraint.ConstraintArg"

	/**
	 * Container for repeatable [CONSTRAINT_ARG] (`@ConstraintArgs`).

	 */
	const val CONSTRAINT_ARGS = "io.ghaylan.validata.constraint.ConstraintArgs"

	/**
	 * `PropertyRef` — annotation referenced when generating property-reference metadata.

	 */
	const val PROPERTY_REF = "io.ghaylan.validata.constraint.PropertyRef"

	/**
	 * `ConstraintArgKind` — enum referenced by generated `@ConstraintArg(kinds = …)` values.

	 */
	const val CONSTRAINT_ARG_KIND = "io.ghaylan.validata.schema.ref.ConstraintArgKind"

	/**
	 * `ConstraintArgTarget` — enum referenced by generated `@ConstraintArg(target = …)` values.

	 */
	const val CONSTRAINT_ARG_TARGET = "io.ghaylan.validata.schema.ref.ConstraintArgTarget"


	/**
	 * `PropertyRefCompatibilityKind` — enum referenced by generated `@PropertyRef` metadata.

	 */
	const val PROPERTY_REF_COMPATIBILITY_KIND =
		"io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind"

	/**
	 * `PropertyRefScope` — enum referenced by generated `@PropertyRef(scope = …)` metadata.

	 */
	const val PROPERTY_REF_SCOPE = "io.ghaylan.validata.schema.ref.PropertyRefScope"

	/**
	 * Every type FQCN this processor emits into generated source.
	 *
	 * Explicit list (not reflection over this object): missing constant = deliberate omission;
	 * package-only strings excluded.
	 *
	 * Side effects: none (immutable list).

	 */
	val ALL_LOADABLE: List<String> = listOf(
		OBJECT_SCHEMA,
		PROPERTY_SPEC,
		VALUE_READER,
		COMPILED_CONSTRAINT,
		DYNAMIC_SHAPE,
		ITERABLE_SHAPE,
		MAP_SHAPE,
		OBJECT_REF_SHAPE,
		SCALAR_KIND,
		SCALAR_SHAPE,
		OBJECT_SCHEMA_MODULE,
		ENDPOINT_SCHEMA,
		ENDPOINT_ARGUMENT_SLOT,
		ENDPOINT_ARGUMENT_KIND,
		REQUEST_SCHEMA_MODULE,
		SCHEMA_ERROR_DOC,
		COMPILED_CONSTRAINTS,
		VALIDATOR_BACKED_RUNNER,
		COMPOSITION_CONSTRAINT,
		COMPOSITION_OR_RUNNER,
		GENERATED_SCHEMA_LOOKUP,
		CONSTRAINT_CATALOG,
		CONSTRAINT_CATALOG_ENTRY,
		REFLECTION_UTILS,
		CONSTRAINT_METADATA,
		CONSTRAINT_ARG,
		CONSTRAINT_ARGS,
		PROPERTY_REF,
		CONSTRAINT_ARG_KIND,
		CONSTRAINT_ARG_TARGET,
		PROPERTY_REF_COMPATIBILITY_KIND,
		PROPERTY_REF_SCOPE)
}

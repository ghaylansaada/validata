# validata-processor

**Maven coordinate:** `io.github.ghaylansaada:validata-processor`  
**Artifact:** KSP compile-time generator — apply with **`ksp(...)` only**; never put it on the runtime classpath.

| Piece        | Value                                                         |
|--------------|---------------------------------------------------------------|
| Compile deps | `validata-schema` + KSP symbol-processing API                 |
| JPMS name    | `io.ghaylan.validata.processor` (`Automatic-Module-Name`)     |

Version / JDK / Kotlin / KSP pins: [root README](../README.md) (top table). Keep Kotlin and
KSP on an [official pairing](https://github.com/google/ksp/releases) when you upgrade.

---

## Contents

1. [Why this module exists](#why-this-module-exists)
2. [Why it is a separate module](#why-it-is-a-separate-module)
3. [Role in the framework](#role-in-the-framework)
4. [How it works](#how-it-works)
5. [Quick start](#quick-start)
6. [Public surface](#public-surface)
7. [What the processor recognizes](#what-the-processor-recognizes)
8. [What it generates](#what-it-generates)
9. [Verification rules](#verification-rules)
10. [Extension points](#extension-points)
11. [Configuration](#configuration)
12. [Analyze vs verify stage matrix](#analyze-vs-verify-stage-matrix)
13. [Error handling & diagnostics](#error-handling--diagnostics)
14. [Source package layout](#source-package-layout)
15. [Generated package layout](#generated-package-layout)
16. [Testing notes](#testing-notes)
17. [Rules to remember](#rules-to-remember)
18. [What it does not do](#what-it-does-not-do)

App dependency coordinates: [root Installation](../README.md#installation). Framework-wide
config index: [root Configuration](../README.md#configuration).

---

## Why this module exists

Without a compile-time generator, validation schemas are either hand-written (and drift
from real DTOs/handlers) or discovered too late (typos and impossible configs surface at
runtime).

`validata-processor` turns annotations into **generated IR factories and SPI registrations**,
and fails the **build** on bad paths, bad constraint arguments, cascade mistakes, and invalid
polymorphism metadata — so consumers of those schemas agree on one model.

**Analogy:** if `validata-schema` is the floor-plan language, this module is the architect
that draws the plans from your source.

---

## Why it is a separate module

| Isolation goal             | Effect                                                                       |
|----------------------------|------------------------------------------------------------------------------|
| Compile-time only          | Apps never ship KSP / symbol-processing on the runtime classpath             |
| Optional for hand-built IR | Pure IR consumers can depend on `validata-schema` without KSP                |
| Clear producer boundary    | Annotation → IR generation lives here; the engine stays free of KSP          |
| Independent publish        | Library modules and apps apply `ksp` only where they declare annotated types |

Wire it as **`ksp(...)` only** — never `implementation` / `api`.

---

## Role in the framework

```text
You annotate types / handlers / constraints
              │
              ▼
┌─────────────────────────────────────┐
│  validata-processor  ← YOU ARE HERE │
│  analyze → verify → codegen         │
└──────────────────┬──────────────────┘
                   │ emits factories + SPI
                   │ that construct types from
                   ▼
            validata-schema (IR)
                   │
                   ▼
            runtime / host / docs / tooling consumers
```

**This module’s job:** discover annotated symbols, verify authoring rules at compile time,
and emit Kotlin sources + `META-INF/services` registrations.

**Not this module’s job:** executing validators, binding HTTP, publishing OpenAPI documents,
or IDE inspections.

---

## How it works

### Analyze vs verify stages

**Analyze** builds intermediate IR only: shapes, constraints, endpoints, catalogs, and optional
metadata / error-doc models. It does not emit sources. Local annotation mistakes (`@ConstraintArg`, literals, cascade,
polymorphism) fail here.

**Verify** then checks assembled IR for cross-field property paths (object schemas and flat
endpoint params). Failures are KSP diagnostics on the consumer build.

**Emit** runs after verify for ready symbols: Kotlin factories under `<sourcePkg>.ghaylan.validata`
plus SPI aggregator resources under `META-INF/services/…`.

### Pipeline (one compilation unit)

1. **Discover** — `@Validatable` roots, `@Validate` handlers, `@Constraint` annotations
2. **Analyze** — build intermediate models (shapes, constraints, polymorphism, endpoints,
   catalogs, optional metadata, optional error-doc IR)
3. **Verify** — sibling / element `@PropertyRef` paths against the full schema graph
4. **Emit** — Kotlin factories under `<sourcePkg>.ghaylan.validata` plus SPI aggregator
   resources under `META-INF/services/…`

Round-scoped caches (`ProcessorRoundCache`) are **thread-local**.
`ConstraintMetadataProcessor` and `ConstraintCatalogProcessor` call `forceClear()` then
`begin()` at each `process` entry; `SchemaProcessor` calls `begin()` only (it does not
`forceClear` at `process` entry). All three call `end()` in `finally`, and `forceClear`
on `finish` / `onError`, so reused KSP worker threads never inherit a prior round’s maps.

### Three processors (one service file)

Loaded from
`META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
in this order:

| Provider                              | Runs                                                                                                | Emits                                                                                                                              |
|---------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `ConstraintMetadataProcessorProvider` | **Every KSP round** (no single-shot guard)                                                          | Opt-in generated `*Constraint` metadata classes for Option 2 role markers                                                          |
| `SchemaProcessorProvider`             | **Multi-round** — defers unready `@Validatable` roots; after success, further `process` calls no-op | Per-type object schema + Fields as roots become ready; **endpoint** factories + SPI aggregators only when nothing remains deferred |
| `ConstraintCatalogProcessorProvider`  | **Multi-round** — merges new `@Constraint` annotations each round                                   | Per-annotation catalog helpers as FQCNs appear; `ConstraintCatalogModule` + SPI in `finish()`                                      |

`@Validatable` roots that fail `KSNode.validate()` are **returned from `process`** and
revisited in a later round (they are not permanently skipped). Per-type object schema files are
written as each root becomes ready; endpoint factories and `ObjectSchemasModule` /
`RequestSchemasModule` SPI aggregators emit only when the deferred list is empty (so endpoint
property-ref checks see the full schema graph). Permanently broken types still
surface as KSP errors.

**Metadata** (`ConstraintMetadataProcessor`) may run every round so newly emitted metadata
classes become visible to catalog / schema work in the same compilation. It skips FQCNs
already written earlier in the unit and never defers symbols.

Schema roots are tracked by **FQCN only** and re-resolved from the current `Resolver` before
verify/write — KSP symbols are not retained across rounds.

---

## Quick start

```kotlin
@Validatable
data class CreateUserRequest(
    @Email val email: String?,
    @Min("18") val age: Int?,
)
```

Rebuild with KSP enabled. The processor emits:

- `CreateUserRequestSchema` — `object` with `fun build(): ObjectSchema`
- `CreateUserRequest_` — wire-path `const val`s for client-visible error paths (declared Kotlin names remain fine as `@PropertyRef` string literals)
- For `@Validate` handlers with path / query / header params: `UserController_lookup_<fingerprint>_` — flat wire-name `const val`s (Spring `name` / `value` / declared). Body-only handlers skip this file; use the request DTO `Type_` object instead.
- `ObjectSchemasModule` / `RequestSchemasModule` (+ SPI resources) when the unit has schemas / endpoints

---

## Public surface

| Symbol                                                                | Role                                                           |
|-----------------------------------------------------------------------|----------------------------------------------------------------|
| `SchemaProcessorProvider` / `SchemaProcessor`                         | KSP entry → object / endpoint schemas (multi-round)            |
| `ConstraintCatalogProcessorProvider` / `ConstraintCatalogProcessor`   | KSP entry → constraint catalogs (multi-round; SPI in `finish`) |
| `ConstraintMetadataProcessorProvider` / `ConstraintMetadataProcessor` | KSP entry → Option 2 metadata codegen (**every** round)        |
| `ProcessorOptions` (internal; documented for Gradle)                  | KSP option keys (`validata.*`)                                 |

Everything under `analyze`, `verify`, `codegen`, `model`, `compat`, `fqns`, `naming`, plus
`SchemaEmitSupport` / round probes, is **internal** to the jar. Application code must not
depend on those types — consume **generated** sources and SPI registrations only.

---

## What the processor recognizes

Matched by **FQCN** (not short name). Types may live on the **consumer** classpath; this
module’s main compile dependency is `validata-schema` plus the KSP API.

### Schema / cascade

| FQCN / family                            | Role                                                     |
|------------------------------------------|----------------------------------------------------------|
| `io.ghaylan.validata.schema.Validatable` | Object schema root; legal cascade target                 |
| `io.ghaylan.validata.schema.NoCascade`   | Nested property stays opaque; no `@Validatable` required |

### Constraints / metadata / args

| FQCN / family                                                     | Role                                                                                   |
|-------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `io.ghaylan.validata.constraint.Constraint`                       | Meta-annotation on constraint annotations (`validatedBy`)                              |
| `io.ghaylan.validata.constraint.ConstraintComposition`            | AND/OR expand mode for composed (non-`@Constraint`) annotations                        |
| `io.ghaylan.validata.constraint.ConstraintMessage`                | Role marker → generated metadata `message`                                             |
| `io.ghaylan.validata.constraint.ConstraintGroups`                 | Role marker → generated metadata `groups`                                              |
| `io.ghaylan.validata.constraint.PropertyRef`                      | Marks path-valued parameters (sibling / element)                                       |
| `io.ghaylan.validata.constraint.ConstraintArg` / `ConstraintArgs` | Marks argument rules (`ConstraintArgKind` / `ConstraintArgTarget`)                     |
| `io.ghaylan.validata.constraint.annotation.RequiredWhen`          | Conditional presence; compile-time `value`/`values` typed against the **gate** sibling |

Metadata class FQCN is always the convention  
`{annotationPackage}.{AnnotationSimpleName}Constraint`  
(no `@Constraint(metadata = …)` override).

### Endpoints

| FQCN / family                                                                                             | Role                                                                         |
|-----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `io.ghaylan.validata.schema.Validate`                                                                | Handler / class-level endpoint schema root                                   |
| Spring `@RequestBody` / `@RequestParam` / `@RequestHeader` / `@PathVariable`                              | Transport slot classification                                                |
| Spring `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` | When expanding class-level `@Validate`, only mapped members become endpoints |
| `io.ghaylan.validata.groups.OnDefault`                                                                    | Default validation group when `@Validate(groups=…)` is omitted               |

Method-level `@Validate` wins over class-level. Synthetic `$default` bridges are skipped.
At most one `@RequestBody` per handler. **Map request bodies are rejected.**

### Jackson (when on the consumer classpath)

| FQCN                                            | Role                                                  |
|-------------------------------------------------|-------------------------------------------------------|
| `com.fasterxml.jackson.annotation.JsonProperty` | Wire / error-path `externalName` (property or getter) |
| `com.fasterxml.jackson.annotation.JsonIgnore`   | Property omitted from the generated schema            |

### OpenAPI presentation (optional)

When `validata-openapi` (or equivalent) is on the **compile** classpath:

| FQCN                                                  | Role                                                              |
|-------------------------------------------------------|-------------------------------------------------------------------|
| `io.ghaylan.validata.openapi.presentation.ApiError`   | Docs-only error IR on properties (`code` / `message` / `catalog`) |
| `io.ghaylan.validata.model.ConstraintErrorDefinition` | Required interface for enum error catalogs                        |

If those types are absent, no error-doc IR is emitted.

### Shape classification (built-in)

The processor classifies property / parameter types into IR shapes:

- **Map** — Kotlin `Map` / `MutableMap`, `java.util.Map*`, and concrete maps via hierarchy (`HashMap`, …)
- **Iterable** — Kotlin list/set/collection variants, `java.util.List*` / `Set*` / `Collection*`, and concrete
  collections via hierarchy (`ArrayList`, …)
- **Array** — `kotlin.Array` and Kotlin primitive arrays (`IntArray`, …) only — not arbitrary types whose name ends with
  `Array`
- **Scalar** — stdlib/JDK leaves + `java.time.*` + enums
- **Object ref** — user classes/interfaces that cascade (or `@NoCascade` / dynamic)

---

## What it generates

### Object schemas (`@Validatable`)

For each annotated concrete type:

| Artifact         | Form                                                                                                                                 |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Schema factory   | `object <Type>Schema { fun build(): ObjectSchema }`                                                                                  |
| Path constants (`Type_`) | `object <Type>_` — `SCREAMING_SNAKE` **wire**-path `const val`s (nested companions up to `PropertyPath.MAX_REFERENCE_PATH_DEPTH`). For `@PropertyRef`, declared name strings also resolve. |
| Aggregator       | `ObjectSchemasModule` implementing `ObjectSchemaModule`                                                                              |
| SPI resource     | `META-INF/services/io.ghaylan.validata.schema.spi.ObjectSchemaModule`                                                                |

**Polymorphic roots** (sealed interface / abstract / sealed non-class): properties are empty;
`subtypes` carries concrete leaf FQCNs. Rules enforced at compile time:

1. Non-blank `discriminator` must name a **scalar** property on the annotated type
2. Each `@Validatable.Subtype(type = …)` must extend/implement the parent and be `@Validatable`
3. Every **concrete sealed leaf** must also be `@Validatable`
4. `Subtype(name = …)` is a typed literal for the discriminator’s type
5. When both sealed discovery and `subtypes = […]` are present, the concrete type sets must match exactly

`Subtype.name` is authoring / tooling metadata — runtime dispatch uses the instance’s actual type.

**Nested object references** in generated code:

| Situation                                | Emission                                          |
|------------------------------------------|---------------------------------------------------|
| Peer schema in the same compilation unit | `lazy { PeerSchema.build() }` (cycle-safe)        |
| Missing / other-module peer              | `GeneratedSchemaLookup.requireGeneratedSchema(…)` |
| Never                                    | Fake empty `ObjectSchema` for a missing peer      |

### Endpoint schemas (`@Validate`)

For each effective handler:

| Artifact         | Form                                                                                            |
|------------------|-------------------------------------------------------------------------------------------------|
| Factory function | Returns `EndpointSchema` (path / header / query sections, body, groups, flags, argument layout) |
| Path constants (`Controller_function_`) | Flat `object <Owner>_<method>[_fingerprint]_` — `SCREAMING_SNAKE` wire-name `const val`s for path / query / header params only (Spring-effective names). Omitted when the handler has none. Body DTO paths stay on `Type_`. |
| Aggregator       | `RequestSchemasModule` implementing `RequestSchemaModule`                                       |
| SPI resource     | `META-INF/services/io.ghaylan.validata.schema.request.spi.RequestSchemaModule`                  |

**Endpoint id** format (must match runtime reflective keys byte-for-byte):

```text
pkg.ClassName#methodName(pkg.ParamType1,pkg.ParamType2)
```

- Parameter types use JVM erased names (`Class.getTypeName` shape)
- Nested classes use JVM binary names (`Outer$Inner`)
- Suspend handlers omit the synthetic `kotlin.coroutines.Continuation` parameter
- `Array<Int>` erases to `java.lang.Integer[]`; `IntArray` erases to `int[]`

`@Validate` flags (defaults): `oneErrorPerParam = true`, `failFast = false`,
`groups = [OnDefault]` unless overridden.

Flat query/header/path parameters never cascade into DTOs. Body types must be
`@Validatable` when provable (same-module → error; cross-module → warn, or error if
[strict cross-module cascade](#configuration) is on).

**`@RequestBody` shapes**

| Body type                                | Emission                                                                                            |
|------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Single `@Validatable` DTO                | `GeneratedSchemaLookup.requireGeneratedSchema(Dto::class.java)`                                     |
| `List` / array of `@Validatable` element | Wrapper `ObjectSchema` whose sole property is `IterableShape(ObjectRefShape(lazy { … element … }))` |
| `Map<…>`                                 | **Rejected** (KSP error)                                                                            |
| Multiple `@RequestBody` on one handler   | **Rejected** (KSP error)                                                                            |

### Constraint catalogs (`@Constraint`)

For each `@Constraint` annotation that opts in with **both** `@ConstraintMessage` and
`@ConstraintGroups`:

| Artifact       | Form                                                                     |
|----------------|--------------------------------------------------------------------------|
| Entries helper | Top-level function returning `List<ConstraintCatalogEntry>`              |
| Aggregator     | `ConstraintCatalogModule`                                                |
| SPI resource   | `META-INF/services/io.ghaylan.validata.constraint.spi.ConstraintCatalog` |

Also enforced: non-empty `validatedBy`, each validator implements `ConstraintValidator<V, C>`,
value type is concrete, metadata type matches the convention FQCN (or the abstract base),
and the validator is a Kotlin `object` or has a public no-arg constructor. Duplicate
`(metadata, valueType)` bindings in the same unit fail the build.

### Opt-in constraint metadata

When a `@Constraint` annotation declares exactly one `@ConstraintMessage` (`String`) and
exactly one `@ConstraintGroups` (`Array<KClass<*>>`) parameter (**Option 2**),
`ConstraintMetadataProcessor` emits a generated `{Name}Constraint` class (constructor params
mirror annotation args, including forwarded `@ConstraintArg` / `@PropertyRef` markers).

Annotations **without** role markers are skipped by metadata generation (hand-written
metadata path). Catalog generation still **requires** both role markers.

Metadata runs **every KSP round** (after `ProcessorRoundCache.forceClear` + `begin`) so
catalog/schema processors can see newly written metadata in the same compilation. Already
emitted metadata FQCNs are skipped on later rounds.

### Constraints on properties

- Field + getter use-site targets are merged; subclass annotations win over inherited duplicates
- Non-`@Constraint` annotations may compose nested `@Constraint` meta-annotations (AND expansion depth-capped at **4**):
    - absent / `@ConstraintComposition(AND)` → flatten leaves into the property constraint list
    - `@ConstraintComposition(OR)` → one synthetic `CompositionConstraint` + `CompositionOrRunner`
      (≥2 leaf members; **no nested composed members**; presence annotations rejected as OR members)
- Presence annotations (`Required` / `RequiredWhen` by simple name) are sorted ahead of others
- Validator selection ranks `validatedBy` candidates against the subject type (deterministic tie-break by FQCN)
- Type-use annotations on type arguments (e.g. `List<@Email String>`) hang on nested shapes
- Concrete `@Validatable` types with **no readable properties** are skipped (KSP **warning**; no schema file)

---

## Verification rules

Diagnostics are KSP **errors** or **warnings** attached to source symbols.

| Condition                                                                                                                            | Typical severity                                         |
|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| Unknown, self, or nested (multi-segment) sibling `@PropertyRef` path                                                                 | Error                                                    |
| Flat endpoint params: multi-segment sibling path                                                                                     | Error                                                    |
| Element `@PropertyRef` path that is nested or missing on the element DTO                                                             | Error                                                    |
| Scalar compatibility failure for comparison-style refs                                                                               | Error                                                    |
| `@ConstraintArg` kind failure (`NOT_BLANK`, `TYPED_LITERAL`, `REGEX`, `POSITIVE`, …)                                                 | Error                                                    |
| `@RequiredWhen`: `value` / `values` typed against the **gate sibling**; unused under `MISSING` / `PRESENT` / non-matching conditions | Error on bad literal; skipped when unused                |
| `ELEMENT` + `TYPED_LITERAL` typed against collection/array/map **element** (or map value)                                            | Error on mismatch                                        |
| Same-module nest without `@Validatable` / `@NoCascade`                                                                               | Error                                                    |
| Cross-module unmarked nest / unmarked `@RequestBody`                                                                                 | Warn (error if `validata.strictCrossModuleCascade=true`) |
| Bad polymorphism metadata / sealed leaf not `@Validatable` / sealed↔declared mismatch / non-scalar discriminator                     | Error                                                    |
| Duplicate `@Validatable` schema for one FQCN in the unit                                                                             | Error                                                    |
| Unresolvable `@Validatable` in the current round                                                                                     | Deferred to a later KSP round (not skipped)              |
| Empty concrete `@Validatable` (no readable properties)                                                                               | Warning; schema emission skipped                         |
| Shape nesting deeper than `validata.maxShapeNestingDepth`                                                                            | Error                                                    |
| Empty / invalid `@Constraint(validatedBy=…)` / duplicate catalog binding / non-constructible validator                               | Error                                                    |
| Incomplete role markers on a `@Constraint` used for catalog emission                                                                 | Error                                                    |
| Invalid / blank `@ApiError` codes or bad catalog types                                                                               | Error                                                    |
| Multiple transport annotations on one parameter / multiple `@RequestBody` / map body                                                 | Error                                                    |
| OR composition: &lt;2 leaves, presence member, or nested composed member                                                             | Error                                                    |

**Property-ref paths:** sibling refs on object schemas and flat endpoint params are **single-segment only** (same
object / same flat section). Use declared Kotlin names as string literals (`ref = "firstName"`) or generated
`Type_` wire constants (`CreateUserRequest_.FIRST_NAME`); both resolve. `Type_` values match client error paths.
For path / query / header params on `@Validate` handlers, use the flat `Controller_function_` object
(`UserController_lookup_<fp>_.USER_ID`) — values are Spring-effective wire names.

Positional annotation arguments are bound to constructor parameter order for both
`@PropertyRef` path extraction and `@ConstraintArg` checks.

---

## Extension points

| Extension                                | How                                                                                                                                                                  |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Custom constraints                       | Annotate with `@Constraint(validatedBy = …)` **and** both role markers in a module that runs this processor — catalog (+ optional metadata) is emitted automatically |
| Opt-in generated metadata                | `@ConstraintMessage` + `@ConstraintGroups` on annotation parameters                                                                                                  |
| Cross-module cascade strictness          | KSP arg `validata.strictCrossModuleCascade` — see [Configuration](#configuration)                                                                                    |
| Max shape nesting for property-ref walks | KSP arg `validata.maxShapeNestingDepth` — see [Configuration](#configuration)                                                                                        |
| Jackson wire naming when `@JsonProperty` is absent | KSP arg `validata.jackson.naming` (`IDENTITY` / `SNAKE_CASE`) — see [Configuration](#configuration)                                                        |
| Hand-written schemas                     | Implement `ObjectSchemaModule` / `RequestSchemaModule` SPI without this processor                                                                                    |

There are no Spring beans in this module — it is not a runtime component.

---

## Configuration

KSP `environment.options` keys understood by this processor (`ProcessorOptions`). Pass them
from the **consumer** Gradle module that applies `ksp(validata-processor)`:

| KSP arg key                         | Type                                  | Default    | Effect                                                                                                                                                                                                     |
|-------------------------------------|---------------------------------------|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `validata.strictCrossModuleCascade` | boolean string (`"true"` / `"false"`) | `false`    | When `true`, unmarked cascade into a dependency type (no `containingFile`) is a **KSP error** for nested properties **and** unmarked cross-module `@RequestBody` types. When `false`, warn only.           |
| `validata.maxShapeNestingDepth`     | non-negative int string               | `32`       | Caps iterable/map nesting when verifying type-use property refs. Exceeding the depth is a **KSP error** (no `StackOverflowError`). Invalid / negative values fall back to `32` and emit a **KSP warning**. |
| `validata.jackson.naming`           | `IDENTITY` / `SNAKE_CASE`             | `IDENTITY` | Wire / error-path naming when `@JsonProperty` is absent. Align with the app `ObjectMapper` naming strategy. Explicit `@JsonProperty` always wins. Unknown values fall back to `IDENTITY` with a **KSP warning**. |

```kotlin
ksp {
    arg("validata.strictCrossModuleCascade", "true")
    arg("validata.maxShapeNestingDepth", "32")
    arg("validata.jackson.naming", "SNAKE_CASE")
}
```

Same keys are listed in the [root Configuration](../README.md#configuration) for the
framework-wide index. Host runtime limits (`validata.limits.*`) are **not** KSP options —
they belong to the Spring host / engine.

---

## Analyze vs verify stage matrix

| Concern                                                  | Analyze (`analyze.*`)                                    | Verify (`verify.*`)                                              | Notes                                                                |
|----------------------------------------------------------|----------------------------------------------------------|------------------------------------------------------------------|----------------------------------------------------------------------|
| Constraint discovery / validator pick / metadata call    | ✅ when building models                                  | —                                                                | Bad `validatedBy` / missing round session → KSP error during analyze |
| Literal / `@ConstraintArg` / `@RequiredWhen` gate typing | ✅ via `ConstraintArgVerifier` during resolve            | —                                                                | Runs while resolving constraints (before IR write)                   |
| Cascade / `@Validatable` / `@NoCascade`                  | ✅ in shape / endpoint builders (`CascadeDecision`)      | —                                                                | Same-module vs cross-module policy                                   |
| Polymorphism discriminator / subtype sets                | ✅ `PolymorphicSubtypeReconciler` + `DiscriminatorRules` | —                                                                | On every `@Validatable` build                                        |
| Catalog binding / constructibility                       | ✅ `ConstraintCatalogModelBuilder`                       | —                                                                | Duplicate `(metadata, valueType)` fails analyze                      |
| `@ApiError` catalog resolution                           | ✅ parser + `ApiErrorCatalogVerifier`                    | —                                                                | When OpenAPI types are on the classpath                              |
| Sibling / element `@PropertyRef` paths                   | —                                                        | ✅ `PropertyReferenceVerifier` (+ path/element/endpoint helpers) | After schemas (and endpoints) are built; needs full graph            |
| Subject scalar compatibility for comparisons             | —                                                        | ✅ same                                                          | Skipped on nested container type-use sites                           |
| Codegen emission                                         | —                                                        | —                                                                | `codegen.*` via `SchemaEmitSupport` after verify for that unit       |

**Rule of thumb:** analyze builds IR and fails fast on local annotation mistakes; verify walks the assembled graph for
cross-field references that need peer schemas.

---

## Error handling & diagnostics

Messages are written for **inline IDE display**: one clear sentence with context and a fix
hint (no internal ticket IDs).

Severity **error** fails the build. Warnings do not.

If a generated schema is missing at runtime, fix by annotating the type, applying `ksp` on
the **declaring** module, and rebuilding — do not expect reflective recovery.

---

## Source package layout

Internal Kotlin packages under `io.ghaylan.validata.processor` (all `internal` except the
three `*Processor` / `*Provider` entry types):

| Package   | Role                                                                                                                                                    |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| *(root)*  | `SchemaProcessor`, `ConstraintCatalogProcessor`, `ConstraintMetadataProcessor`, providers, `ProcessorOptions`, emit orchestration (`SchemaEmitSupport`) |
| `analyze` | Build IR from KSP symbols (schemas, endpoints, constraints, catalogs, metadata, cascade); also invokes some `verify.*` helpers during resolve (`ConstraintArgVerifier`, `@ApiError` catalog checks) |
| `verify`  | Check helpers + graph-stage `@PropertyRef` walks (`PropertyReferenceVerifier`); arg/literal/`@ApiError` types live here but often run during analyze |
| `codegen` | Render Kotlin sources + SPI aggregators from IR models                                                                                                  |
| `model`   | Intermediate IR data types (stable leaf — no reverse deps on analyze/verify)                                                                            |
| `compat`  | KSP helpers: type FQCN tables, classification, round cache, string literals                                                                             |
| `fqns`    | Annotation / SPI / attribute name constants (FQCN walls)                                                                                                |
| `naming`  | Generated package / file / endpoint-id naming                                                                                                           |

One **top-level** type per source file. Stage packages are the architecture; do not restructure
into feature folders for file-count alone.

---

## Generated package layout

| Kind                                     | Package                                  |
|------------------------------------------|------------------------------------------|
| Per type / handler / annotation artifact | `<sourcePackage>.ghaylan.validata`       |
| SPI aggregators for the compilation unit | `<longestCommonPrefix>.ghaylan.validata` |
| No shared prefix / empty package set     | `io.ghaylan.validata.generated`          |

The `ghaylan.validata` segment is library-owned so output does not collide with other tools’
bare `*.generated` packages.

---

## Testing notes

```bash
./gradlew :validata-processor:test
```

| Kind        | Package                                                    | Purpose                                                                                                                              |
|-------------|------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Unit        | `analyze`, `verify`, `codegen`, `compat`, `naming`, `fqns` | Pure rules (cascade, composition, arg kinds, writers, FQCN contracts) without a full compile                                         |
| Integration | `integration`                                              | KSP compile-testing (`kctfork`) — multi-round, cascade, `@RequiredWhen`, catalogs, endpoints, metadata, typed literals, polymorphism |
| Harness     | `support`                                                  | Shared compile helpers / fixtures / recording logger                                                                                 |

These tests lock down **processor mechanisms** (codegen, diagnostics, cascade, args,
endpoints, catalogs, metadata, multi-round deferral). They do **not** enumerate every
built-in constraint annotation or assert HTTP / runtime outcomes.

When testing **your** module that uses this processor: compile with `ksp` enabled; assert
on generated sources or SPI resources if needed. Do **not** put `validata-processor` on the
test **runtime** classpath as `implementation`.

---

## Rules to remember

1. `ksp(...)` only — never `implementation`.
2. Apply the processor on the **declaring** module for every annotated type / handler / custom constraint.
3. Nested object types must be `@Validatable` (or use `@NoCascade`).
4. Custom `@Constraint` annotations used for catalog emission need **both** role markers and a usable `validatedBy`
   list.
5. Sibling `@PropertyRef` paths are single-segment; declared name strings or `Type_` / `Controller_function_` wire constants both resolve.
6. Endpoint ids must stay aligned with reflective `Class#method(paramTypeName)` keys — including array erasure and
   omitting suspend `Continuation`.
7. `@RequestBody Map` and multiple `@RequestBody` parameters are rejected; collection bodies of `@Validatable` elements
   are supported.
8. After changing aggregator or SPI contracts, rebuild consumers so `META-INF/services` lines regenerate.

---

## What it does not do

| Not handled here                            | Belongs elsewhere                                                            |
|---------------------------------------------|------------------------------------------------------------------------------|
| Validating live values / requests           | Runtime engine / host                                                        |
| Spring MVC binding, HTTP status, error JSON | Spring host module                                                           |
| OpenAPI document publishing                 | OpenAPI companion (this processor only optionally reads `@ApiError` into IR) |
| IDE inspections / PSI                       | IDE plugin (may mirror rules; KSP remains build truth)                       |
| Shipping on the app runtime classpath       | — (compile-time only)                                                        |

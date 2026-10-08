# validata-openapi

**Maven coordinate:** `io.github.ghaylansaada:validata-openapi`  
**Artifact:** optional springdoc / OpenAPI companion — documents Validata rules in `/v3/api-docs`.

Does **not** run validation, throw constraint exceptions, or invent HTTP error envelopes.

| Piece               | Value                                                       |
|---------------------|-------------------------------------------------------------|
| Hard deps           | `validata-core` (API), `kotlin-reflect`                     |
| Provided by the app | Spring Boot Web, springdoc (`compileOnly` — not transitive) |
| JPMS name           | `io.ghaylan.validata.openapi` (`Automatic-Module-Name`)     |

Version / JDK / Kotlin / springdoc pins: [root README](../README.md) (top table).

The jar reads Validata’s compile-time validation IR and overlays it onto the OpenAPI document
springdoc would produce anyway. Native JSON Schema facets appear when they are honest;
everything else lands under `x-validata-*` vendor extensions. HTTP error **envelopes** are
opt-in via your own `ErrorDocPublisher` beans — Validata never forces a response shape.

---

## Contents

1. [Module contract](#module-contract)
2. [Why this module exists](#why-this-module-exists)
3. [Why it is a separate module](#why-it-is-a-separate-module)
4. [Role in the framework](#role-in-the-framework)
5. [How it works](#how-it-works)
6. [Quick start](#quick-start)
7. [What appears in OpenAPI](#what-appears-in-openapi)
8. [Error documentation](#error-documentation)
9. [Extension points](#extension-points)
10. [Auto-configuration](#auto-configuration)
11. [Cache & concurrency contract](#cache--concurrency-contract)
12. [Jakarta Bean Validation coexistence](#jakarta-bean-validation-coexistence)
13. [Native / AOT](#native--aot)
14. [Public API surface](#public-api-surface)
15. [Package layout](#package-layout)
16. [Error handling & diagnostics](#error-handling--diagnostics)
17. [Testing](#testing)
18. [Contributor rules](#contributor-rules)
19. [Rules to remember](#rules-to-remember)
20. [What it does not do](#what-it-does-not-do)

Gradle setup and dependencies: [root README](../README.md#installation).

---

## Module contract

| Responsibility          | Detail                                                                                        |
|-------------------------|-----------------------------------------------------------------------------------------------|
| **Docs overlay only**   | Mutate springdoc `Schema` / `Operation` from Validata IR. Never execute validators.           |
| **Same IR as runtime**  | Use generated `ObjectSchema` / `EndpointSchema` / `*Constraint` metadata — no second mapping. |
| **Native when honest**  | Emit JSON Schema facets only when they match wire values. Otherwise `x-validata-constraints`. |
| **App-owned envelopes** | Zero default `ErrorDocPublisher` beans. Apps publish 400/problem-details if they want them.   |
| **Fail-soft docs**      | Bad catalogs / empty validators must not take down `/v3/api-docs`.                            |
| **No host coupling**    | Depends on `validata-core` (+ schema types via core), not the Spring MVC validation host jar. |
| **App-owned versions**  | Spring Boot and springdoc stay `compileOnly`; the app pins them.                              |

**Invariant:** OpenAPI output must never invent validation semantics the engine does not enforce.

---

## Why this module exists

Swagger / OpenAPI clients need to see the same rules Validata enforces at runtime — lengths,
formats, required fields, custom constraint args, and possible error codes — without running
the validator when `/v3/api-docs` is built.

Without this companion, apps would either:

- hand-maintain OpenAPI `@Schema` annotations that drift from Validata IR, or
- pull springdoc into every Validata consumer, including apps that never expose Swagger

---

## Why it is a separate module

| Isolation goal     | Effect                                                               |
|--------------------|----------------------------------------------------------------------|
| Optional OpenAPI   | Apps that skip Swagger never see springdoc on the classpath          |
| No host coupling   | Depends on `validata-core` only — not the Spring MVC validation host |
| App-owned versions | Spring Boot and springdoc stay `compileOnly`; you pin them           |
| Clear boundary     | Enrichment never executes validators or throws validation exceptions |

```text
App
├── validata              → runtime validation (optional for OpenAPI-only tooling)
└── validata-openapi      → docs overlay (this jar) → validata-core (shared IR)
```

---

## Role in the framework

```text
compile-time IR (ObjectSchema / EndpointSchema / *Constraint metadata)
         │
         ▼
┌──────────────────────────┐
│     validata-openapi     │  reads IR → mutates springdoc Schema / Operation
│  (this module)           │  optional ErrorDocPublisher → response docs
└──────────────────────────┘
         │
         ▼
   /v3/api-docs  (+ Swagger UI)
```

Validation still runs in the host + engine. This module only enriches the OpenAPI document.

### Internal pipeline (for maintainers)

```text
ValidataModelConverter / ValidataOperationCustomizer
        │
        ▼
TypeShapeOpenApiEnricher          → nested list / map shape walk + facets
ConstraintSchemaApplicator        → mapper SPI → ConstraintDocumentation → facets / unmapped
PropertyOpenApiExtensionsWriter   → x-validata-constraints / x-validata-errors
ConstraintErrorCodeWalk           → shared composition + leaf error codes
DeclaredErrorSurfaceFactory       → endpoint surface for ErrorDocPublisher
```

Package dependency direction (no cycles):

```text
springdoc → presentation → enrichment → docs / mapper → validata-core
```

`enrichment` must **not** import `presentation`. Catalog resolution (`SchemaErrorDocResolver`)
lives in `enrichment` so property writers and surface factories share it without a cycle.

---

## How it works

Requires a Spring Boot **web** app, springdoc on the classpath (`OperationCustomizer` /
`RequestService`), and (for parameter enrichment and error-doc publishers) a `ValidationRegistry`
bean — normally from the Validata host. Without a registry, the model converter can still enrich
DTO component schemas from generated object schemas; the operation customizer skips parameters /
publishers.

No `validata.openapi.*` configuration properties exist. Behavior is classpath + beans only.

Validata already compiled validation schemas at build time. This module overlays that IR onto
springdoc’s models. It is a chain of roles, not a second validator:

1. **IR source** — generated object graphs and endpoint schemas (same IR the engine uses).
2. **Constraint resolution** (per constraint occurrence) — see below.
3. **DTO overlay** (`ValidataModelConverter`) — native facets + vendor extensions on
   component schemas (and nested `items` / map value schemas). Applies **all** groups (shared models have no single
   endpoint group context).
4. **Parameter overlay** (`ValidataOperationCustomizer`) — same for path / header / query
   parameters, filtered by the endpoint’s active validation groups via `ConstraintGroupFilter`.
5. **Error surface** — IR collector ∪ `@ApiError` facts → `DeclaredErrorSurface`
   (`DeclaredErrorSurfaceFactory`).
6. **Publishers** (optional) — zero or more `ErrorDocPublisher` beans mutate the operation (response schemas, examples,
   extensions, …). With none registered: constraint enrichment
   only, no forced 400 body.

### Constraint resolution order

1. An `OpenApiConstraintMapper` that returns `true` (full swagger control — **do not** set
   `description`).
2. Else a `ConstraintDocumentation` SPI → apply non-empty native facets via
   `JsonSchemaFacetApplicator`.
3. Else if a documenter **owns** the metadata → done (empty hints = intentional; **not**
   `x-validata-unmapped`). Examples: temporal `@Min`, `@DaysOfWeek`, composition OR,
   `@RelativeToNow`, `@Phone`.
4. Else if **custom** (not in the native-facet standard set) → skip native facets; the
   constraint still appears under `x-validata-constraints`.
5. Else **standard** with no owning documenter → append the metadata class name to
   `x-validata-unmapped` (sorted distinct list).

**Standard set** (can appear on `x-validata-unmapped` if ownership is missing):  
`Required`, `Size`, `Min`, `Max`, `Range`, `MultipleOf`, `Regex`, `Email`, `Url`, `In`, `NotIn`.

First ServiceLoader documenter with `supports == true` wins. Owners are cached by metadata
class after the first resolve (`ConstraintDocumentations`).

### Design trade-offs

| Choice                           | Meaning                                                                           |
|----------------------------------|-----------------------------------------------------------------------------------|
| Fail-soft at docs time           | Bad catalogs / empty validators never take down `/v3/api-docs`                    |
| No forced error envelope         | Response JSON shape stays app-owned                                               |
| Native when honest               | Only map facets that match wire values (no false day-name enums)                  |
| Generation-scoped identity cache | springdoc may convert the same schema twice; re-apply is skipped within one build |
| Cache reset                      | After each OpenAPI build, enrichment / error-code / catalog caches are cleared    |

---

## Quick start

Add the dependencies above. With no extra beans, `/v3/api-docs` already shows:

- Native facets for standard constraints (`minLength`, `format`, `enum`, …)
- `x-validata-constraints` / `x-validata-errors` on properties and parameters

To document HTTP error **responses**, put `@ApiError` on the field or parameter that can fail,
and register a publisher:

```kotlin
enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
    EMAIL_TAKEN("Email already registered");
    override val code: String get() = name
}

data class CreateUserRequest(
    @field:ApiError(code = "EMAIL_TAKEN", catalog = UserErrors::class)
    @field:Required
    val email: String?,
)

@Bean
fun errorDocs(): ErrorDocPublisher = MyProblemDetailsPublisher()
```

`@ApiError` is **docs-only** — it does not change runtime validation. Prefer a blank
`message` so OpenAPI fills it from the catalog entry; a non-blank `message` overrides for
documentation only. Stack several `@ApiError` on the same host (`@Repeatable`).

---

## What appears in OpenAPI

### Vendor extensions (every property / parameter schema level)

| Extension                | Value                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `x-validata-constraints` | **Array** of entries `{ "_constraint": "<Name>", …non-empty args }`. Omits `message` / `groups`. Omits empty args (null, blank string, empty collection/map). `@PropertyRef` args use the sibling/element **wire** name (`@JsonProperty` / naming strategy), not the Kotlin declared name.                                                                              |
| `x-validata-errors`      | Array of `{ "code", "message" }`: property root always includes `VALUE_TYPE_MISMATCH`; plus validators’ `possibleErrorCodes` (and `CONSTRAINT_UNSATISFIABLE` + leaf codes for OR composition); plus `@ApiError` (wins on code collision); plus `VALUE_MISSING` when an active `@Required` is present. Nested `items` / map-value levels list codes for that level only. |
| `x-validata-unmapped`    | Sorted distinct list of **standard** metadata class names with no owning documenter and no mapper (diagnostic).                                                                                                                                                                                                                                                         |

List **element** and map **value** constraints are written onto `items` /
`additionalProperties` schemas (same nesting as native facets). When
`additionalProperties: false`, map-value enrichment is skipped (`OpenApiMapValueSchemas`).

### Native JSON Schema facets

| Constraint                 | Native mapping                                                                                                                                         |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@Required`                | Parent `required` + `nullable: false` (params → `Parameter.required`)                                                                                  |
| `@Size`                    | `minLength`/`maxLength` (string), `minItems`/`maxItems` (list), `minProperties`/`maxProperties` (map). Max facets omitted when max is `Int.MAX_VALUE`. |
| `@Min` / `@Max`            | `minimum` / `maximum` (+ exclusive flags) when the bound parses as a number; temporal / duration bounds → extensions only                              |
| `@Range`                   | `minimum` / `maximum` (+ exclusive flags) when **both** bounds parse as numbers and `negated = false`; otherwise extensions only                     |
| `@MultipleOf`              | `multipleOf` when the factor parses as a number                                                                                                        |
| `@Regex`                   | `pattern`                                                                                                                                              |
| `@Email`                   | `format: email`                                                                                                                                        |
| `@Url`                     | `format: uri`                                                                                                                                          |
| `@In` | `enum` (sorted)                                                                                                                                   |
| `@NotIn` | `not.enum` (sorted)                                                                                                                              |
| `@Coordinate`              | `minimum` / `maximum` (−90…90 or −180…180 by axis)                                                                                                     |
| `@HexColor`                | `pattern` matching the runtime validator: `^#([A-Fa-f0-9]{6}\|[A-Fa-f0-9]{3})$`                                                                        |
| `@NumberSign`              | `minimum` / `maximum` about `0` (`exclusive*` unless `allowZero`)                                                                                      |
| `@NumberParity` (`EVEN`)         | `multipleOf: 2`                                                                                                                                        |
| `@IpAddress`               | `format: ipv4` / `ipv6` when typed; `ANY` → extensions only (owned, empty facets)                                                                      |

**Owned but no native facet** (still in `x-validata-constraints`, never `x-validata-unmapped`):

- **Composition OR** (`CompositionConstraint`) — no honest single `format` / `oneOf` in v1
- `@DaysOfWeek` — restricts day-of-week of a date wire value; **not** an enum of `DayOfWeek` names
- `@DaysOfMonth` / `@Months` — calendar allow-lists; no native integer/`Month` enum facets
- `@RelativeToNow` — no guessed `date` vs `date-time` (IR only classifies coarse `TEMPORAL`)
- Cross-field: `@Compare`, `@RequiredWhen`
- Number customs: `@Digits` (no honest native vocabulary alone)
- String customs: `@Barcode`, `@Base64`, `@FilePath`
- Collection customs: `@Contains`, `@Distinct`
- Misc: `@Phone`, `@Password`, `@Html`, `@CreditCard`, `@FinancialCode`, `@Checksum`, country/currency/language codes,
  `@NotIn`, `@Assert`, …

Built-in documenters live under `docs.builtin` and are registered in
`META-INF/services/…ConstraintDocumentation`. Custom / catch-all owners are split by domain:

| Documenter                            | Owns (examples)                                                                                |
|---------------------------------------|------------------------------------------------------------------------------------------------|
| `NumberCustomConstraintDocumentation` | NumberSign, NumberParity, Coordinate, Digits                                                         |
| `StringCustomConstraintDocumentation` | HexColor, IpAddress, Barcode, Base64, FilePath                                       |
| `MiscCustomConstraintDocumentation`   | RelativeToNow, Phone, Password, Html, FinancialCode, Checksum, codes, Contains, Distinct, Assert, … |

### Merge rules

When several constraints enrich the same schema:

- Bounds (`min*` / `max*` / `minimum` / `maximum`) → **strictest wins**
- `format` / `pattern` / `multipleOf` → keep the **first** non-blank / non-null value
- `enum` → **intersection** (empty intersection falls back to the incoming set)
- Existing `description` is **never** overwritten (belongs to `@Schema` / authors)

### Validation groups

| Surface                              | Behavior                                                                                                                 |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| **Path / header / query parameters** | Facets, `x-validata-constraints`, and `x-validata-errors` respect the endpoint’s active groups (`ConstraintGroupFilter`) |
| **DTO component schemas**            | Union of all declared constraints (shared models have no single endpoint group context)                                  |
| **Endpoint error surface**           | Same group filter as parameters for collector codes                                                                      |

Empty constraint groups or empty active groups → constraint is treated as active.

### Property names

OpenAPI property keys follow Validata `PropertySpec.externalName` (Jackson `@JsonProperty` /
naming strategy). If springdoc keyed a property under the declared name, enrichment renames
it to the wire name when needed. `@PropertyRef` args in `x-validata-constraints` are rewritten
the same way (`OpenApiPropertyRefNames`).

---

## Error documentation

### `@ApiError`

Docs-only annotation on a **field**, **property**, or **value parameter** (also usable as a
meta-annotation). Declares an extra machine code for OpenAPI that validators do not emit (business / uniqueness / …).

| Rule        | Detail                                                                                                                                                                                                                                                          |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Placement   | Body / path / header / query properties (including nested DTO fields)                                                                                                                                                                                           |
| Nested DTOs | Operation error surface deep-walks object / list / map shapes. Paths use wire names with `.` separators; list elements append `[]` (e.g. `items[].sku`); map values use a `*` segment (e.g. `prefs.*.theme`). Recursive types are skipped via an ancestor stack |
| Catalog     | Enum implementing `ConstraintErrorDefinition`; `code` must be a constant name                                                                                                                                                                                   |
| Message     | Blank → catalog message; non-blank → docs override                                                                                                                                                                                                              |
| Runtime     | Ignored by the validation engine                                                                                                                                                                                                                                |

Compile-time tools should validate catalog membership; this module resolves catalogs
fail-soft at docs time (`Class.forName`, cached in `SchemaErrorDocResolver`). Unloadable /
non-enum / unknown-constant catalogs fall back to baked `code` / blank message and warn **once**
per catalog FQCN.

### Endpoint error surface

For each operation with a registry hit, Validata builds a `DeclaredErrorSurface`:

- **detailCodes** — structural codes (`VALUE_TYPE_MISMATCH`, `STRUCTURE_DEPTH_EXCEEDED`),
  constraint `possibleErrorCodes` (group-filtered), `CONSTRAINT_UNSATISFIABLE` plus leaf codes
  for active OR composition, `VALUE_MISSING` when any active `@Required` exists, plus codes
  from `@ApiError`
- **detailErrorDocs** — `{ code, message, path?, location? }` from `@ApiError` on the request
  graph (wire path + `PATH` / `HEADER` / `QUERY` / `BODY`; nested paths use `.` / `[]` / `*`)

Property-level `x-validata-errors` and endpoint `detailCodes` share the same
`ConstraintErrorCodeWalk` for composition parity.

### `ErrorDocPublisher`

```kotlin
fun interface ErrorDocPublisher {
    fun publish(context: ErrorDocPublishContext)
}
```

Register zero or more Spring beans (`@Order` / `Ordered` supported). Each receives:

| Field           | Role                                                      |
|-----------------|-----------------------------------------------------------|
| `surface`       | Immutable `DeclaredErrorSurface` (always present)         |
| `endpointId`    | Validata endpoint id                                      |
| `operation`     | Mutable springdoc `Operation`, or `null` when unavailable |
| `handlerMethod` | Spring MVC handler, or `null` when unavailable            |
| `attributes`    | Forward-compatible bag — **ignore unknown keys**          |

Publishers that need swagger should null-check `operation`. Facts-only unit tests can omit
host handles (`operation` / `handlerMethod` nullable by design).

With **no** publishers, operations get parameter/property enrichment only — no automatic
`400` response schema.

Example sketch (app-owned):

```kotlin
@Bean
fun errorDocs(): ErrorDocPublisher = ErrorDocPublisher { ctx ->
    if (ctx.surface.detailCodes.isEmpty() && ctx.surface.detailErrorDocs.isEmpty()) return@ErrorDocPublisher
    val operation = ctx.operation ?: return@ErrorDocPublisher
    // add ApiResponse("400", …) using ctx.surface.detailCodes / detailErrorDocs
}
```

---

## Extension points

### Constraint documentation SPI (preferred)

Map custom (or built-in) metadata to native facets without touching swagger types directly.

```text
META-INF/services/io.ghaylan.validata.openapi.docs.ConstraintDocumentation
com.example.OddYearsConstraintDocumentation
```

```kotlin
class OddYearsConstraintDocumentation : ConstraintDocumentation {
    override fun supports(metadata: ConstraintMetadata): Boolean =
        metadata is OddYearsConstraint

    override fun hints(metadata: ConstraintMetadata, shape: TypeShape?): ConstraintDocHints =
        ConstraintDocHints.EMPTY // still listed under x-validata-constraints
}
```

First ServiceLoader documenter with `supports == true` wins. Returning `EMPTY` means
“owned, no native facet” — not unmapped.

Also override `possibleErrorCodes` on the validator so detail-code enums stay complete.

### OpenAPI constraint mapper SPI

For arbitrary swagger surgery when facets are not enough:

```text
META-INF/services/io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper
```

Return `true` after mutating the schema to claim the constraint. **Do not** set
`Schema.description`.

### Summary

| Goal                                 | Extension point                               |
|--------------------------------------|-----------------------------------------------|
| Native facets for a constraint       | `ConstraintDocumentation` ServiceLoader       |
| Arbitrary schema edits               | `OpenApiConstraintMapper` ServiceLoader       |
| Extra field error codes              | `@ApiError` on field / parameter              |
| Operation error responses / examples | `ErrorDocPublisher` `@Bean`(s)                |
| Replace Boot wiring                  | `@ConditionalOnMissingBean` (see auto-config) |

---

## Auto-configuration

Registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
`ValidataOpenApiAutoConfiguration` registers (when springdoc is present):

| Bean method / name                    | Type                          | Role                                                                                                                                                 |
|---------------------------------------|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `requestBuilder`                      | `ValidataRequestService`      | Replaces springdoc’s `RequestService` — strips non-Jakarta annotations before Bean Validation → schema mapping. **Name must stay** `requestBuilder`. |
| `validataOperationCustomizer`         | `ValidataOperationCustomizer` | Path/header/query enrichment + runs `ErrorDocPublisher`s when a `ValidationRegistry` is available                                                    |
| `validataModelConverter`              | `ValidataModelConverter`      | Overlays IR onto DTO / component schemas                                                                                                             |
| `validataOpenApiCacheResetCustomizer` | `OpenApiCustomizer`           | Clears enrichment / error-code / catalog caches after each document build                                                                            |

All are `@ConditionalOnMissingBean` (by type or bean name as noted in source). There are **no** default
`ErrorDocPublisher` beans.

If you replace or disable this auto-config, you must still clear caches after each document
build or risk stale identity marks / unbounded retention. Prefer keeping
`validataOpenApiCacheResetCustomizer` (or copy its three clears into your own
`OpenApiCustomizer` **inside this module** / a fork). Apps outside the jar can call the
public collectors:

```kotlin
EndpointErrorCodeCollector.clear(clearWarnings = false)
SchemaErrorDocResolver.clear(clearWarnings = false)
// OpenApiEnrichmentCache is internal — cleared only by the autoconfig OpenApiCustomizer
```

Do not skip the enrichment-cache clear when shipping a custom wiring.

---

## Cache & concurrency contract

| Cache                        | Scope                                            | Cleared by autoconfig?                      |
|------------------------------|--------------------------------------------------|---------------------------------------------|
| `OpenApiEnrichmentCache`     | Generation + identity marks (schema / operation) | Yes (`clear` bumps generation)              |
| `EndpointErrorCodeCollector` | Endpoint id + groups → codes                     | Yes (warn-once set kept)                    |
| `SchemaErrorDocResolver`     | Catalog FQCN + code → entry                      | Yes (warn-once set kept)                    |
| `ConstraintDocumentations`   | SPI list + per-metadata-class owner              | No (process lifetime; `resetForTests` only) |
| `OpenApiConstraintMappers`   | SPI list                                         | No (process lifetime; `resetForTests` only) |
| Empty-validator warn set     | One warn per validator class                     | Kept across builds (intentional)            |

**Concurrent `/v3/api-docs` generation is not supported.** Serialize OpenAPI builds. Within one
build, identity marks prevent double enrichment; after the build, caches clear so the next
regeneration re-enriches.

---

## Jakarta Bean Validation coexistence

springdoc matches Bean Validation annotations by **simple name**, then casts to
`jakarta.validation.*`. Validata’s `@Size` / `@Min` / `@Max` (and peers) collide and would
throw `ClassCastException`.

`ValidataRequestService` filters the annotation list to Jakarta Validation /
Hibernate Validator packages only before that path runs (`JakartaValidationAnnotationFilter`).
Validata rules are documented from IR instead.

---

## Native / AOT

`ValidataOpenApiRuntimeHints` registers:

- ServiceLoader resource patterns for `ConstraintDocumentation` and `OpenApiConstraintMapper`
- Reflection for those SPIs, `ErrorDocPublisher`, `@ApiError`, `ConstraintMetadata`, and
  built-in `*Constraint` metadata classes (needed to serialize `x-validata-constraints`)

Every AOT-registered built-in metadata type must have an owning `ConstraintDocumentation`
(enforced by unit test).

---

## Public API surface

Stable types apps and extensions typically use:

| Package         | Types                                                                                                                                                                        |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `…presentation` | `@ApiError`, `ErrorDocPublisher`, `ErrorDocPublishContext`, `DeclaredErrorSurface`, `DeclaredErrorSurfaceFactory`, `PublishedErrorDoc`                                       |
| `…docs`         | `ConstraintDocumentation`, `ConstraintDocHints`, `JsonSchemaFacet`, `ConstraintDocumentations`                                                                               |
| `…mapper`       | `OpenApiConstraintMapper`, `OpenApiConstraintMappers`                                                                                                                        |
| `…enrichment`   | `ConstraintExtensionKeys`, `ConstraintGroupFilter`, `SchemaErrorDocResolver`, `ObjectSchemaOpenApiEnricher`, `EndpointErrorCodeCollector`, `PropertyOpenApiExtensionsWriter` |
| `…config`       | `ValidataOpenApiAutoConfiguration` (Boot entry)                                                                                                                              |
| `…aot`          | `ValidataOpenApiRuntimeHints`                                                                                                                                                |

**Prefer** the SPIs and presentation API unless you are replacing auto-config beans.

Marked `internal` (module-private): enrichment helpers such as `TypeShapeOpenApiEnricher`,
`ConstraintSchemaApplicator`, `ConstraintErrorCodeWalk`, `OpenApiMapValueSchemas`,
`OpenApiPropertyRefNames`, `OpenApiEnrichmentCache`, `NumericBound`, BV filter, etc.

`EndpointErrorCodeCollector.isActive` is **deprecated** — use `ConstraintGroupFilter.isActive`.

Springdoc adapter types (`ValidataOperationCustomizer`, `ValidataModelConverter`,
`ValidataRequestService`) are public for `@ConditionalOnMissingBean` replacement, not for
casual app use.

---

## Package layout

```text
io.ghaylan.validata.openapi
├── config/         Spring Boot auto-configuration
├── springdoc/      RequestService, ModelConverter, OperationCustomizer, BV filter
├── enrichment/     Schema overlay, facets, extensions, error-code walk, caches
├── mapper/         OpenApiConstraintMapper SPI + ServiceLoader registry
├── docs/           ConstraintDocumentation SPI, facets, hints, NumericBound
│   └── builtin/    First-party documenters (standard + Number/String/Misc customs)
├── presentation/   @ApiError, error surface, ErrorDocPublisher
└── aot/            GraalVM / AOT hints
```

Architecture guard: `ModuleBoundaryTest` fails if `enrichment` depends on `presentation`.

---

## Error handling & diagnostics

OpenAPI generation **does not throw** on a bad `@ApiError` catalog or incomplete validator
metadata. Prefer compile-time checks for catalog mistakes.

| Situation                                                    | OpenAPI result                             | Log                              |
|--------------------------------------------------------------|--------------------------------------------|----------------------------------|
| Validator with empty `possibleErrorCodes`                    | Those field codes omitted from the surface | Warn once per validator class    |
| Standard constraint, no documenter, no mapper                | Class name on `x-validata-unmapped`        | Silent (extension is the signal) |
| Unloadable / non-enum / unknown `@ApiError` catalog constant | Falls back to IR `code` / blank message    | Warn once per catalog FQCN       |
| `additionalProperties: false`                                | Map-value enrichment skipped               | Silent                           |

Check the document; do not fail the app.

---

## Testing

- Hit `/v3/api-docs` (MockMvc or WebTestClient) and assert native facets plus
  `x-validata-constraints` / `x-validata-errors`.
- Do **not** expect operation-level error schemas unless an `ErrorDocPublisher` bean is
  registered in the test slice.
- Colliding Jakarta `@Size` and Validata `@Size` on the same type must not throw while
  generating docs (BV filter path).
- Unit tests that re-run enrichment in-process may call test-only `resetForTests()` helpers
  on documenter / mapper / cache / collector / catalog registries; production clears caches
  via the OpenAPI customizer after each build.
- Facts-only publisher tests may pass `operation = null` / `handlerMethod = null`.
- Module unit suite: `./gradlew :validata-openapi:test`. Coverage expectations are tracked in
  `UNIT-TEST-AUDIT-REPORT.md`. Samples may add an `OpenApiDocsIT` for springdoc wiring smoke.

---

## Contributor rules

When changing this module, keep the contract above. Practical checklist:

1. **New core constraint** → add / extend a `ConstraintDocumentation` (or own it in
   Number / String / Misc), register in `META-INF/services`, update AOT hints if needed.
   The “every builtin metadata owned” test must stay green.
2. **Native facets** only when wire-honest; otherwise return `ConstraintDocHints.EMPTY`.
3. **Never** set `Schema.description` from mappers or documenters.
4. **Composition / groups** → use `ConstraintErrorCodeWalk` and `ConstraintGroupFilter`; do not
   fork a second walk that disagrees with property vs endpoint collectors.
5. **Nested shapes** → use `TypeShapeOpenApiEnricher` / shared writers; do not re-implement
   list/map enrichment in springdoc adapters.
6. **Package edges** → `enrichment` must not import `presentation`. Prefer FQCN in KDoc over
   imports that create cycles.
7. **Caches** → process-lifetime warn-once sets are intentional; identity / endpoint caches
   must clear after each OpenAPI build.
8. **Tests** → kill the mutant (branch / boundary / empty / inactive groups / SPI registration),
   not just the happy path.
9. **Docs overlay only** — do not change Validata runtime validation semantics from this jar.

---

## Rules to remember

1. This module documents; it does not validate.
2. Add springdoc yourself — it is not a transitive dependency.
3. Prefer native facets only when they match wire values; otherwise use
   `x-validata-constraints`.
4. Never overwrite `Schema.description` from mappers or documenters.
5. Register an `ErrorDocPublisher` only when you want documented error responses.
6. Parameter docs follow endpoint groups; shared DTO schemas are the union of constraints.
7. Ignore unknown `attributes` on publish context / surface (forward compatible).
8. Serialize OpenAPI document builds; concurrent generation is unsupported.

---

## What it does not do

- Does not run validators or throw `ConstraintViolationException`
- Does not pin or transitively ship Spring Boot or springdoc
- Does not depend on the Spring MVC validation host jar
- Does not ship a default error-envelope publisher
- Does not invent OpenAPI `description` text for constraints
- Does not replace author-owned `@Schema` / springdoc annotations — it overlays Validata IR
  beside them
- Does not support concurrent `/v3/api-docs` generation (caches are process-scoped per build)
- Does not change Validata runtime validation or constraint semantics

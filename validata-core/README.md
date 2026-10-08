# validata-core

**Maven coordinate:** `io.github.ghaylansaada:validata-core`  
**Artifact:** Spring-free validation runtime — built-in constraints, custom-constraint API, engine, registry.

| Piece     | Value                                                                                            |
|-----------|--------------------------------------------------------------------------------------------------|
| Hard deps | `validata-schema` (API), `kotlin-reflect`                                                        |
| Optional  | Jsoup (`@Html`), libphonenumber (`@Phone`) — compileOnly, not transitive                         |
| Built-ins | **38** annotations (`@Min`/`@Max`/`@Range` fan out per subject family) |
| JPMS name | `io.ghaylan.validata.core` (`Automatic-Module-Name`)                                             |

Version / JDK / Kotlin pins: [root README](../README.md) (top table).

This jar is the **inspector**: every built-in constraint annotation and validator, the
`ConstraintValidator` SPI for custom rules, and `ValidatorEngine` / `ValidationRegistry` that
walk pre-compiled schemas. It does **not** bind Spring MVC or generate schemas.

---

## Contents

1. [Why this module exists](#why-this-module-exists)
2. [Why it is a separate module](#why-it-is-a-separate-module)
3. [Role in the framework](#role-in-the-framework)
4. [How it works](#how-it-works)
5. [Engine API](#engine-api)
6. [Walk semantics](#walk-semantics)
7. [Built-in constraints](#built-in-constraints)
8. [Custom constraints](#custom-constraints)
9. [Markers and groups](#markers-and-groups)
10. [Package layout](#package-layout)
11. [Public API surface](#public-api-surface)
12. [Configuration](#configuration)
13. [Error handling & diagnostics](#error-handling--diagnostics)
14. [Testing notes](#testing-notes)
15. [Rules to remember](#rules-to-remember)
16. [What it does not do](#what-it-does-not-do)

App dependency coordinates and KSP wiring: [root Installation](../README.md#installation).
Host YAML + KSP args index: [root Configuration](../README.md#configuration).

---

## Why this module exists

Validata needs one Spring-free place that defines *how* values are checked:

- Authors write `@Email`, `@Required`, custom `@Constraint`s
- `ValidatorEngine` walks `ObjectSchema` / `EndpointSchema` graphs (IR from `validata-schema`)
- Hosts decide *when* to call the engine and how to map errors

Without this jar, every sample, benchmark, or non-Spring host would need Boot just to compile
an annotation. Same rule semantics everywhere.

---

## Why it is a separate module

| Isolation goal             | Effect                                                    |
|----------------------------|-----------------------------------------------------------|
| No Spring / servlet types  | Processor tests, samples, and non-Spring hosts stay light |
| Shared validators + engine | One implementation for Boot and custom hosts              |
| Independent publish        | Tooling can depend on runtime types without the web stack |
| Clear boundary             | Root `checkModuleBoundaries` forbids Spring imports here  |

Spring apps get this jar **transitively** via `validata`. Depend on `validata-core`
**directly** for non-Spring hosts, custom runners, library authoring, or annotations + engine
only.

---

## Role in the framework

```text
annotate types / endpoints
         │
         ▼
   schema producers          emit ObjectSchema / EndpointSchema + *Constraint metadata
         │
         ▼
┌─────────────────────┐
│   validata-core     │  annotations · validators · ValidatorEngine · registry
│   ← YOU ARE HERE    │
└─────────┬───────────┘
          │
          ▼
   host / callers            decide when to validate and how to present ConstraintError
```

**This module’s job:** declare constraints, run them against live values, emit stable
`ConstraintError` / `ConstraintErrorCode` results, enforce fail-closed limits.

**Not this module’s job:** generating schemas, defining IR types, HTTP binding, OpenAPI, or IDE PSI.

---

## How it works

1. **Compile time.** You annotate DTOs and handlers. A schema producer emits
   `ObjectSchema` / `EndpointSchema` factories plus `*Constraint` metadata and catalog SPI
   entries. This module supplies the annotation and validator *types* those factories
   reference.
2. **Startup.** The **host** builds a validator map from constraint catalogs (`GeneratedConstraintCatalogs` /
   `ServiceLoader`) and registers it with
   `ValidationRegistry.registerValidators(…)`, plus endpoint schemas via
   `registerStaticSchemas(…)`, then calls **`freeze()`**. Object schemas resolve only through
   `GeneratedSchemas` / `ObjectSchemaModule` SPI — not through the constraint catalog.
   Unknown `@Validatable` classes fail loudly (`SchemaNotFoundException`) with a setup
   checklist. Unknown endpoint ids on `validateRequest(id, …)` throw `IllegalStateException`.
3. **Call time.** `ValidatorEngine.validate(…, ValidationOptions(…))` or `validateRequest(…)`
   walks the schema against live values: properties, list elements (including `null` slots by
   index), map keys/values, nested objects. Each compiled constraint calls
   `ConstraintValidator.runValidation`, which routes `null` → `validateNull` and non-null →
   `validate(value: Value, …)`. Failures become `ConstraintError`s with a stable
   `ConstraintErrorCode` and a short default message (override per annotation when needed).
4. **Limits.** Depth / width ceilings (`ValidationLimits`) fail closed —
   `STRUCTURE_DEPTH_EXCEEDED` / `COLLECTION_TOO_LARGE` rather than silently skipping invalid data.
5. **Regex safety.** `@Regex` rejects inputs longer than `RegexValidator.MAX_INPUT_LENGTH`
   (10_000) before `Pattern.matcher`, so user patterns cannot hang a thread on megabyte
   payloads.

**Constraint triple:** annotation (what you write) → generated `*Constraint` metadata (arguments) →
`ConstraintValidator` (the check).

**Null contract (framework guarantee):** `validate(value: Value, …)` never receives `null`
(`Value : Any`). `runValidation` sends `null` to `validateNull` (default = skip / valid).
Only presence rules (`@Required` / `@RequiredWhen`) override `validateNull`. Empty / blank
strings are non-null and still go through `validate`.

```text
runValidation(rawValue, metadata, context)
        │
        ├─ rawValue == null  →  validateNull(metadata, context)   // default: skip
        └─ rawValue != null  →  validate(typedValue, metadata, context)
                                        │
                                        └─ null (ok)  or  ConstraintError (fail)
```

---

## Engine API

`ValidatorEngine` is stateless and safe to share as a singleton.

- **Standalone** walks take `ValidationOptions` (flags + groups).
- **Request** walks read `oneErrorPerParam` / `failFast` / `groups` from the compiled
  `EndpointSchema` (populated from `@Validate` by KSP) — they do **not** take
  `ValidationOptions`.

```kotlin
val registry = ValidationRegistry()
registry.registerValidators(builtFromCatalog)
registry.registerStaticSchemas(endpointSchemas)
registry.freeze()   // idempotent; further register* throws IllegalStateException

val engine = ValidatorEngine(registry, ValidationLimits())

// Standalone DTO (schema resolved via GeneratedSchemas / SPI):
val errors = engine.validate(
    params = userRequest,
    options = ValidationOptions(
        oneErrorPerParam = true,
        failFast = false,
        groups = arrayOf(OnDefault::class),
    ),
)

// HTTP-shaped request against a pre-registered endpoint schema:
val requestErrors = engine.validateRequest(
    id = method.getUniqueIdentifier(),  // MethodUniqueIdentifiers
    body = body,
    params = queryMap,
    headers = headerMap,
    pathVariables = pathMap,
)
```

| Method / type                                               | Purpose                                                                                                              |
|-------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `validate(params, options = ValidationOptions())`           | Walk one object / DTO schema using [options]                                                                         |
| `validateRequest(id, body, params, headers, pathVariables)` | Lookup `EndpointSchema` by id; flags come from the schema                                                            |
| `validateRequest(schema, …)`                                | Same walk with an already-resolved `EndpointSchema`                                                                  |
| `ValidationOptions`                                         | Standalone only: `oneErrorPerParam` (default `true`), `failFast` (default `false`), `groups` (default `[OnDefault]`) |
| `ValidationLimits`                                          | Per-engine ceilings — see [Configuration](#configuration)                                                            |
| Empty returned list                                         | Valid                                                                                                                |
| Non-empty list                                              | Deduplicated, ordered by natural path then error-code name                                                           |

`ValidationRegistry` APIs: `registerValidators`, `registerStaticSchemas`, `freeze()`,
`isFrozen()`, plus lookup helpers. After `freeze()`, registration throws
`IllegalStateException`. Shared test fixtures freeze once; use a fresh unfrozen registry
when registering in tests. `MethodUniqueIdentifiers.getUniqueIdentifier()` builds the
endpoint id that must match compile-time request-schema keys.

---

## Walk semantics

| Behavior                       | Detail                                                                                                                                                                                                                  |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Null policy**                | Framework-wide: `runValidation` routes `null` → `validateNull` (default skip). Only `@Required` / `@RequiredWhen` override `validateNull`. Format / bound / comparison `validate(…)` methods receive a non-null `Value` |
| **Blank vs null**              | Empty / blank `CharSequence` is non-null → goes to `validate`. Most format validators treat blank as a format failure; **`@Url` and `@Html` also skip blank** (presence stays `@Required`)                              |
| **Null body**                  | When an endpoint declares a request body schema, a `null` body still walks that schema so property-level `@Required` (and similar) can fire                                                                             |
| **Null list / array elements** | Indices are preserved; the engine visits each slot. Element constraints hit `validateNull` / skip; container rules (e.g. `@Size`) see the full structure; `@Distinct` is element type-use and sees siblings via array context |
| **`oneErrorPerParam`**         | At most one violation **per field path** (O(1) failed-path set). Other paths keep collecting unless `failFast`                                                                                                          |
| **`failFast`**                 | Stop after the first violation anywhere (remaining params / sections may be skipped). Opt-in; default `false`                                                                                                           |
| **Groups**                     | Only constraints whose `groups` intersect the active set run. Defaults to `OnDefault`                                                                                                                                   |
| **Cascade**                    | Nested `@Validatable` graphs are walked unless the property is `@NoCascade` (property constraints still run; nested fields do not)                                                                                      |
| **Polymorphism**               | Dispatch by runtime class via `Validatable` sealed subclasses and/or `subtypes`                                                                                                                                         |
| **Limits**                     | `maxDepth` → `STRUCTURE_DEPTH_EXCEEDED`; over-wide container → `COLLECTION_TOO_LARGE`; `maxErrors` stops collecting once the request already failed                                                                     |
| **Path stamping**              | Validators return path-free errors with a precise `message`; `runValidation` stamps `path` and finalizes `message` (annotation → validator → code default)                                                              |

Standalone: pass flags via `ValidationOptions`. Request walks: `@Validate` is compiled into the
`EndpointSchema` (`oneErrorPerParam`, `failFast`, `groups`) — hosts do not re-pass those options
to `validateRequest`.

---

## Built-in constraints

All **38** built-in annotations ship in this jar. Package:
`io.ghaylan.validata.constraint.annotation`.

### Shared parameters (every annotation)

Every constraint accepts these two parameters. They are omitted from the per-constraint tables
below only to avoid repeating them 38 times — they are always available.

| Parameter | Type               | Default              | Role                                                                 |
|-----------|--------------------|----------------------|----------------------------------------------------------------------|
| `message` | `String`           | `""`                 | Non-blank overrides the validator’s precise message for that failure |
| `groups`  | `Array<KClass<*>>` | `[OnDefault::class]` | Only runs when active groups intersect this set                      |

Blank `message` → the validator’s precise failure text (then the `ConstraintErrorCode` default
as last resort). Groups such as `OnCreate` / `OnUpdate` let one DTO serve several endpoints.

**Null policy:** `null` never reaches `validate`. Default `validateNull` skips. Presence is
`@Required` / `@RequiredWhen` only (they override `validateNull`). Absence is not a format
error — pair format constraints with presence when needed.

**Placement:** where you put the annotation decides the subject, not a flag on the constraint:

```kotlin
@field:Size(min = 1)                     // the list itself
val tags: List<@Size(min = 3) String>?   // each element's length

val links: Map<@Size(min = 1) String, @Required AddressDto?>?
// value path: links[homepage] · key path: links.keys[homepage]
```

**Sibling refs** (`@Compare`, `@RequiredWhen`, …) are **one segment only**: write the field
name as a string (`@Compare(ref = "password", operation = Compare.Operation.EQ)`). Nested
paths such as `"address.city"` are rejected at compile time / in the IDE.

Nested enums stay nested: `Required.Mode`, `Compare.Operation`, `RelativeToNow.Relation`,
`Coordinate.Axis`, `NumberSign.Sign`, `NumberParity.Value`, `Contains.Mode`, `Url.Type`,
`FinancialCode.Type`, `Checksum.Algorithm`, `IpAddress.Type`, `Barcode.Type`,
`ConstraintComposition.Mode`. `RequiredWhen.Condition` is nested under `@RequiredWhen`
(same package as the annotation).

---

### Presence

#### `@Required`

Unconditional presence. Default `mode` is deep emptiness (`STRICT`).

| Parameter | Type            | Default | Meaning                  |
|-----------|-----------------|---------|--------------------------|
| `mode`    | `Required.Mode` | `STRICT`  | How “missing” is decided |

`Required.Mode`:

| Value   | Treated as missing when                                                                               |
|---------|-------------------------------------------------------------------------------------------------------|
| `NULL`  | Reference is `null` only (blank strings / empty containers pass)                                      |
| `EMPTY` | `null` or structurally empty (`length == 0` for text; empty containers). Whitespace-only strings pass |
| `STRICT`  | `null` or every nested collection/map/array/string is empty                                           |

Repeatable. Failures: `VALUE_MISSING`, `TEXT_BLANK`, `VALUE_EMPTY`.

```kotlin
@field:Required
val email: String?
```

Prefer **nullable** properties so Jackson absence survives as `null` and `@Required` can fire.

#### `@RequiredWhen`

Presence gated on a **sibling**. Repeatable: several gates on one field combine with **OR**
(required if **any** gate matches).

| Parameter   | Type                      | Default      | Meaning                                                                                  |
|-------------|---------------------------|--------------|------------------------------------------------------------------------------------------|
| `ref`       | `String` (`@PropertyRef`) | *(required)* | Sibling name (single segment, not blank)                                                 |
| `condition` | `RequiredWhen.Condition`  | *(required)* | How the sibling is interpreted as a gate                                                 |
| `value`     | `String`                  | `""`         | Literal for `EQ` / `NE` / `GT` / `LT` / `GTE` / `LTE`. Typed against the **gate** type  |
| `values`    | `Array<String>`           | `[]`         | Literals for `IN` / `NIN`. Ignored otherwise                                             |
| `mode`      | `Required.Mode`           | `STRICT`       | Presence strictness for `MISSING` / `PRESENT` and for the annotated value when enforcing |

`RequiredWhen.Condition` (nested on `@RequiredWhen`):

| Value        | Gate matches when                                           |
|--------------|-------------------------------------------------------------|
| `MISSING`    | Sibling is missing per `mode`                               |
| `PRESENT`    | Sibling is present per `mode`                               |
| `EQ`         | Sibling’s `toString()` equals `value`. `null` never equals  |
| `NE`         | Sibling is non-null and `toString()` differs from `value`   |
| `IN`         | Sibling’s `toString()` is in `values`. `null` never matches |
| `NIN`        | Sibling is non-null and `toString()` is not in `values`     |
| `GT` / `GTE` / `LT` / `LTE` | Sibling compares against `value` (numeric when both parse as decimals, else lexicographic). `null` never matches |

Failures: `VALUE_MISSING`, `TEXT_BLANK`, `VALUE_EMPTY`.

```kotlin
@RequiredWhen(ref = "email", condition = RequiredWhen.Condition.MISSING)
val phone: String?

@RequiredWhen(ref = "contactType", condition = RequiredWhen.Condition.EQ, value = "PHONE")
val phone: String?

@RequiredWhen(ref = "status", condition = RequiredWhen.Condition.IN, values = ["OPEN", "PENDING"])
val assignee: String?
```

---

### Bounds

#### `@Min`

Lower bound: `value >= bound` (or `>` when `inclusive = false`). Subject type selects the
validator and the literal syntax for `value`. Unsupported subject types fail at compile time /
in the IDE — there is no catch-all `Any` validator.

| Parameter   | Type      | Default      | Meaning                                              |
|-------------|-----------|--------------|------------------------------------------------------|
| `value`     | `String`  | *(required)* | Bound literal (not blank; typed against the subject) |
| `inclusive` | `Boolean` | `true`       | `true` → `>=`; `false` → strict `>`                  |

Literal syntax by subject:

| Subject                                             | `value` example             |
|-----------------------------------------------------|-----------------------------|
| `Number` (`Int`, `Long`, `Double`, `BigDecimal`, …) | `"18"`, `"1_000"`           |
| ISO temporals (`LocalDate`, `Instant`, …), `Year`   | type-specific ISO-8601      |
| `Duration`                                          | `"PT1H30M"`                 |
| `Period`                                            | `"P1Y2M"`                   |
| `YearMonth`                                         | `"uuuu-MM"`                 |
| `MonthDay`                                          | `"--MM-dd"`                 |
| `Month`                                             | `"JANUARY"` or `"1"`…`"12"` |

Failures: `NUMBER_TOO_SMALL`, `TEMPORAL_TOO_EARLY`, `TEMPORAL_DURATION_TOO_SHORT`, `VALUE_PARSING_FAILED`.

```kotlin
@field:Min("18")
val age: Int

@field:Min("PT30M", inclusive = false)
val sessionTimeout: Duration
```

#### `@Max`

Upper bound: `value <= bound` (or `<` when `inclusive = false`). Same subject / literal table
as `@Min`.

| Parameter   | Type      | Default      | Meaning                             |
|-------------|-----------|--------------|-------------------------------------|
| `value`     | `String`  | *(required)* | Bound literal                       |
| `inclusive` | `Boolean` | `true`       | `true` → `<=`; `false` → strict `<` |

Failures: `NUMBER_TOO_LARGE`, `TEMPORAL_TOO_LATE`, `TEMPORAL_DURATION_TOO_LONG`, `VALUE_PARSING_FAILED`.

```kotlin
@field:Max("120")
val age: Int

@field:Max("P2Y")
val subscriptionLength: Period
```

#### `@Range`

Inclusive interval: `from <= value <= to` by default. Use `fromInclusive` / `toInclusive` for
strict `>` / `<` on either end. When `negated = true`, the value must lie **outside** the
interval. Same subject / literal table as `@Min` / `@Max`. Lower bound is checked first;
failures reuse Min/Max error codes (or `NUMBER_OUT_OF_RANGE` / `TEMPORAL_OUT_OF_RANGE` when negated
and inside).

| Parameter       | Type      | Default      | Meaning                             |
|-----------------|-----------|--------------|-------------------------------------|
| `from`          | `String`  | *(required)* | Lower bound literal                 |
| `to`            | `String`  | *(required)* | Upper bound literal                 |
| `fromInclusive` | `Boolean` | `true`       | `true` → `>=`; `false` → strict `>` |
| `toInclusive`   | `Boolean` | `true`       | `true` → `<=`; `false` → strict `<` |
| `negated`       | `Boolean` | `false`      | When `true`, value must be outside `[from, to]` |

Failures: `NUMBER_TOO_SMALL` / `NUMBER_TOO_LARGE`, `TEMPORAL_TOO_EARLY` / `TEMPORAL_TOO_LATE`,
duration short/long codes, `NUMBER_OUT_OF_RANGE` / `TEMPORAL_OUT_OF_RANGE` (negated + inside),
`VALUE_PARSING_FAILED`.

```kotlin
@field:Range(from = "18", to = "120")
val age: Int

@field:Range(from = "2020-01-01", to = "2030-12-31")
val effectiveDate: LocalDate

@field:Range(from = "PT30M", to = "PT8H", fromInclusive = false)
val sessionTimeout: Duration

@field:Range(from = "0", to = "17", negated = true)
val adultAge: Int
```

#### `@MultipleOf`

`Number` is an exact multiple of `factor`, computed in `BigDecimal` (not floating point).
Repeatable. Factor `"0"` or negative **disables** the check (every value passes).

| Parameter | Type     | Default      | Meaning                                                           |
|-----------|----------|--------------|-------------------------------------------------------------------|
| `factor`  | `String` | *(required)* | Decimal divisor (`"5"`, `"0.25"`, `"1_000"`). Underscores allowed |

Failures: `NUMBER_NOT_MULTIPLE`, `VALUE_PARSING_FAILED`.

```kotlin
@field:MultipleOf("0.05")
val price: BigDecimal
```

#### `@Size`

Inclusive length / element-count range. Applies only to the **direct** annotated type:
`CharSequence` length, `Collection` / `Map` size, JVM arrays. A `LocalDate` (etc.) is rejected
at compile time.

| Parameter | Type  | Default         | Meaning                    |
|-----------|-------|-----------------|----------------------------|
| `min`     | `Int` | `0`             | Inclusive minimum (`>= 0`) |
| `max`     | `Int` | `Int.MAX_VALUE` | Inclusive maximum (`>= 0`) |

Failures: `TEXT_TOO_SHORT`, `TEXT_TOO_LONG`, `COLLECTION_TOO_SMALL`, `COLLECTION_TOO_LARGE`,
`OBJECT_TOO_SMALL`, `OBJECT_TOO_LARGE`.

```kotlin
@field:Size(min = 8, max = 64)
val password: String

@field:Size(min = 1, max = 10)
val tags: List<String>
```

#### `@Coordinate`

Geographic coordinate on `Double`. [axis] selects latitude (`-90.0..90.0`) or longitude
(`-180.0..180.0`), both inclusive. Non-finite values (`NaN`, ±Infinity) fail the same range check.

| Parameter | Type              | Default      | Meaning                  |
|-----------|-------------------|--------------|--------------------------|
| `axis`    | `Coordinate.Axis` | *(required)* | `LATITUDE` or `LONGITUDE` |

Failure: `NUMBER_OUT_OF_RANGE`.

```kotlin
@field:Coordinate(axis = Coordinate.Axis.LATITUDE)
val lat: Double
```

#### `@NumberSign`

Sign checks on `Number` via exact `BigDecimal` arithmetic.

| Parameter   | Type              | Default | Meaning                       |
|-------------|-------------------|---------|-------------------------------|
| `sign`      | `NumberSign.Sign` | *(required)* | `POSITIVE` or `NEGATIVE` |
| `allowZero` | `Boolean`         | `false` | When `true`, zero is accepted |

Failures: `NUMBER_NOT_POSITIVE` / `NUMBER_NOT_NEGATIVE`, `NUMBER_ZERO_NOT_ALLOWED`,
`VALUE_PARSING_FAILED`.

```kotlin
@field:NumberSign(sign = NumberSign.Sign.POSITIVE)
val quantity: Int

@field:NumberSign(sign = NumberSign.Sign.NEGATIVE, allowZero = true)
val adjustment: BigDecimal
```

#### `@NumberParity`

Integral even/odd on `Number`. Fractional values fail with `NUMBER_NOT_INTEGER`.

| Parameter | Type           | Default      | Meaning          |
|-----------|----------------|--------------|------------------|
| `value`   | `NumberParity.Value` | *(required)* | `EVEN` or `ODD` |

Failures: `NUMBER_NOT_EVEN` / `NUMBER_NOT_ODD`, `NUMBER_NOT_INTEGER`, `VALUE_PARSING_FAILED`.

```kotlin
@field:NumberParity(value = NumberParity.Value.EVEN)
val pairCount: Int
```

#### `@Digits`

Caps integer and fractional digit counts (Bean Validation semantics on `BigDecimal`
precision/scale).

| Parameter  | Type  | Default           | Meaning                               |
|------------|-------|-------------------|---------------------------------------|
| `integer`  | `Int` | *(required, ≥ 0)* | Max digits left of the decimal point  |
| `fraction` | `Int` | *(required, ≥ 0)* | Max digits right of the decimal point |

Failures: `NUMBER_PRECISION_EXCEEDED`, `NUMBER_SCALE_EXCEEDED`, `VALUE_PARSING_FAILED`.

```kotlin
@field:Digits(integer = 5, fraction = 2)
val amount: BigDecimal
```

---

### Comparison

#### `@Compare`

Cross-field ordering or equality against sibling [ref]. [ref] is `@PropertyRef` (`SIBLING`,
`COMPARABLE_FAMILY`), single-segment, not blank. Repeatable.

| Parameter   | Type                | Default      | Meaning                                                                 |
|-------------|---------------------|--------------|-------------------------------------------------------------------------|
| `ref`       | `String`            | *(required)* | Sibling property name                                                   |
| `operation` | `Compare.Operation` | *(required)* | `GT`, `GTE`, `LT`, `LTE`, `EQ`, `NE` |

Both sides must share a comparable family (numeric↔numeric, temporal↔temporal, or the same
scalar kind for text-like leaves). `null` on the annotated value skips via
`validateNull`. `EQ` treats a `null` sibling as non-matching; `NE` passes when the
sibling is `null`; ordering operations skip when the sibling is missing/`null`.

Failures depend on [operation] (`COMPARISON_NOT_EQUAL`, `COMPARISON_EQUAL`,
`COMPARISON_NOT_ORDERABLE`, …).

```kotlin
@field:Compare(ref = "password", operation = Compare.Operation.EQ)
val confirmPassword: String

@field:Compare(ref = "minQuantity", operation = Compare.Operation.GT)
val quantity: Int
```

#### `@In`

Closed allow-list. Compares `toString()` of scalar subjects. Non-scalars skip. Each [values]
entry is compile-time checked against the subject type. For a deny-list, use [`@NotIn`](#notin).

| Parameter | Type | Default | Meaning |
|-----------|------|---------|---------|
| `values` | `Array<String>` | *(required, non-empty)* | Allowed forms |

Failure: `VALUE_NOT_ALLOWED`.

```kotlin
@field:In(values = ["OPEN", "PENDING", "CLOSED"])
val status: String
```

#### `@NotIn`

Closed deny-list. Same scalar / `toString()` / compile-time literal rules as [`@In`](#in).

| Parameter | Type | Default | Meaning |
|-----------|------|---------|---------|
| `values` | `Array<String>` | *(required, non-empty)* | Forbidden forms |

Failure: `VALUE_NOT_ALLOWED`.

```kotlin
@field:NotIn(values = ["admin", "root"])
val username: String
```

---

### Collections

#### `@Contains`

Membership over **collection or array** subjects (not plain strings). Each element’s
`toString()` is compared to configured literals.

| Parameter | Type            | Default | Meaning                                              |
|-----------|-----------------|---------|------------------------------------------------------|
| `values`  | `Array<String>` | *(required, non-empty)* | Literals (typed against element type)          |
| `mode`    | `Contains.Mode` | `ANY`   | `ANY`, `ALL`, or `NONE`                              |

Failures: `COLLECTION_ITEM_MISSING` (`ANY` / `ALL`), `VALUE_NOT_ALLOWED` (`NONE`).

```kotlin
@field:Contains(values = ["DRAFT"], mode = Contains.Mode.ANY)
val statuses: List<String>
```

#### `@Distinct`

Elements in a list / set / array must be unique — whole elements, or keyed by fields on each
element. **Placement:** type-use on the **element** only (`List<@Distinct T>`), never on the list
property. Failures use indexed element paths (e.g. `contacts[1]`). Scalar elements compare by
value. Collections with fewer than 2 elements always pass. Each `by` entry is a **non-blank,
single-segment** field on the **element** type (`@PropertyRef(scope = PropertyRefScope.ELEMENT)`).
Repeatable. `null` elements use default `validateNull` (skip).

There is **no** `mode` parameter. Strategy is driven only by `by`:

| `by`              | Rule                                                                                       |
|-------------------|--------------------------------------------------------------------------------------------|
| `[]` (default)    | Whole-element uniqueness (value equality for scalars; equals for objects)                  |
| one or more names | Uniqueness of the **tuple** of those fields (a single name is a length-1 tuple)            |

Failure: `COLLECTION_DUPLICATE` (`ConstraintError.metadata` holds the failing `DistinctConstraint`).
Messages are item-scoped (path carries the index): `Item is duplicated in the collection.` or
`Item is duplicated in the collection by fields: email.` when `by` is set.

```kotlin
val contacts: List<@Distinct(by = ["email"]) ContactDto>

val lines: List<@Distinct(by = ["sku", "warehouse"]) LineItem>

val tags: List<@Distinct String>
```

---

### Booleans

#### `@Assert`

Require a [Boolean] to equal [value] (`true` or `false`).

| Parameter | Type      | Default      | Meaning           |
|-----------|-----------|--------------|-------------------|
| `value`   | `Boolean` | *(required)* | Required boolean |

`null` never reaches `validate` (default `validateNull` skip). Failure: `VALUE_NOT_ALLOWED`.

```kotlin
@field:Assert(value = true)
val termsAccepted: Boolean

@field:Assert(value = false)
val isDraft: Boolean
```

---

### Strings and formats

#### `@Email`

Syntax-only `local-part@domain.tld`. Does **not** check MX / deliverability. Empty string **fails** the syntax check.
`null` is handled by default `validateNull` (skip) — pair with
`@Required` for presence. No extra parameters.

Failure: `VALUE_FORMAT_INVALID`.

```kotlin
@field:Email
val contactEmail: String
```

#### `@Url`

URL / `java.net.URI` length, syntax, and optional policy filters. Applies to `CharSequence`
and `URI`. Checks stop at the first failure:
`maxLength` → URI syntax (`CharSequence` only) → host (only for `WEBSITE`) → protocols → ports → query params →
extensions. **Blank** `CharSequence` is skipped inside `validate` (unlike most format validators). `null`
is still the default `validateNull` skip. Presence remains `@Required`.

All `allowed*` arrays default to `["*"]` (no restriction). An explicit `"*"` entry disables
that filter; otherwise only listed values pass.

| Parameter           | Type            | Default | Meaning                                                                                                                        |
|---------------------|-----------------|---------|--------------------------------------------------------------------------------------------------------------------------------|
| `type`              | `Url.Type`      | `ANY`   | Resource category; media types enforce a built-in extension set unless `allowedExtensions` overrides                           |
| `maxLength`         | `Int`           | `2048`  | Max raw URL string length                                                                                                      |
| `allowedPorts`      | `Array<String>` | `["*"]` | Permitted ports (`"443"`); `"*"` allows any, including no explicit port                                                        |
| `allowedParams`     | `Array<String>` | `["*"]` | Permitted query **keys**. Only checked when a query string is present. An **empty** array (not `["*"]`) then rejects any query |
| `allowedProtocols`  | `Array<String>` | `["*"]` | Permitted schemes, case-insensitive (`"https"`)                                                                                |
| `allowedExtensions` | `Array<String>` | `["*"]` | Permitted lowercase path extensions **without** the dot (`"png"`). `"*"` disables `type`’s built-in media set                  |

`Url.Type`:

| Value      | Host required        | Default extensions (when `allowedExtensions` is `["*"]`)                       |
|------------|----------------------|--------------------------------------------------------------------------------|
| `ANY`      | no                   | none (no extension check)                                                      |
| `WEBSITE`  | yes (non-blank host) | none                                                                           |
| `DOCUMENT` | no                   | `pdf`, `zip`, `rar`, `tar`, `exe`, `doc`, `docx`, `ppt`, `pptx`, `xls`, `xlsx` |
| `IMAGE`    | no                   | `jpg`, `jpeg`, `png`, `gif`, `webp`, `svg`, `bmp`, `tiff`, `ico`, `avif`       |
| `VIDEO`    | no                   | `mp4`, `avi`, `mov`, `mkv`, `flv`, `wmv`, `webm`, `mpeg`                       |
| `AUDIO`    | no                   | `mp3`, `wav`, `ogg`, `flac`, `aac`, `wma`, `m4a`, `opus`                       |

Failures: `TEXT_TOO_LONG`, `VALUE_FORMAT_INVALID`, `VALUE_INVALID`, `VALUE_NOT_ALLOWED`.

```kotlin
@field:Url(type = Url.Type.IMAGE, allowedProtocols = ["https"])
val avatarUrl: String

@field:Url(type = Url.Type.WEBSITE, allowedProtocols = ["https"])
val callback: URI
```

#### `@Regex`

Entire `CharSequence` must match `pattern` (full match, not a substring). Inputs longer than
`RegexValidator.MAX_INPUT_LENGTH` (10_000) fail with `TEXT_PATTERN_MISMATCH` **before**
`Pattern.matcher`. `name` is opaque metadata on the failing `RegexConstraint` (exposed via
`ConstraintError.metadata`) so clients can distinguish which named pattern failed without seeing
the regex.

| Parameter | Type     | Default      | Meaning                                                |
|-----------|----------|--------------|--------------------------------------------------------|
| `pattern` | `String` | *(required)* | Java `Pattern` (compile-time / IDE checked)            |
| `name`    | `String` | *(required)* | Human identifier for the format (e.g. `"US_ZIP_CODE"`) |

Failure: `TEXT_PATTERN_MISMATCH`.

```kotlin
@field:Regex(pattern = "\\d{5}(-\\d{4})?", name = "US_ZIP_CODE")
val zipCode: String
```

#### `@Password`

Configurable password policy. Checks run in order and **stop at the first failure** (one error
per value): `minLength` → `maxLength` → uppercase → lowercase → digit → special →
`noSequentialChars` → `noRepetitivePatterns`.
The raw password is **never** copied into `message` or `metadata`.

| Parameter              | Type      | Default                      | Meaning                                                                                                                                 |
|------------------------|-----------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `minLength`            | `Int`     | `6`                          | Inclusive minimum length                                                                                                                |
| `maxLength`            | `Int`     | `64`                         | Inclusive maximum length                                                                                                                |
| `requireUppercase`     | `Boolean` | `false`                      | At least one uppercase letter                                                                                                           |
| `requireLowercase`     | `Boolean` | `false`                      | At least one lowercase letter                                                                                                           |
| `requireDigit`         | `Boolean` | `false`                      | At least one digit                                                                                                                      |
| `requireSpecialChar`   | `Boolean` | `false`                      | At least one character from `allowedSpecialChars`                                                                                       |
| `allowedSpecialChars`  | `String`  | `!@#$%^&*()-_=+[{]};:,<.>/?` | Alphabet counted for `requireSpecialChar`                                                                                               |
| `noSequentialChars`    | `Boolean` | `false`                      | Reject ascending/descending letter or digit runs of length ≥ 3 (case-insensitive for letters), e.g. `"abc"`, `"321"`, `"AbCd"`           |
| `noRepetitivePatterns` | `Boolean` | `false`                      | Reject the same character three or more times in a row (e.g. `"aaaa"`) or consecutive repeating blocks of length ≥ 2 (e.g. `"abcabc"`) |

Failures: `TEXT_TOO_SHORT`, `TEXT_TOO_LONG`, `TEXT_PATTERN_MISMATCH`
(`ConstraintError.metadata` holds the failing `PasswordConstraint`).

```kotlin
@field:Password(
    minLength = 10,
    requireUppercase = true,
    requireDigit = true,
    requireSpecialChar = true,
    noSequentialChars = true,
    noRepetitivePatterns = true,
)
val newPassword: String
```

#### `@Phone`

International phone number via [libphonenumber](https://github.com/google/libphonenumber). **Requires**
`com.googlecode.libphonenumber:libphonenumber` on the runtime classpath (not
transitive). Value must be **digits only** — no `+`, spaces, dashes, or parentheses — and must
include the full international dial prefix (e.g. `"14155552671"`). Formatted input fails
immediately.

| Parameter          | Type                     | Default | Meaning                                                                                                                            |
|--------------------|--------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------|
| `allowedTypes`     | `Array<PhoneNumberType>` | `[]`    | libphonenumber `PhoneNumberType` values (`MOBILE`, `FIXED_LINE`, `FIXED_LINE_OR_MOBILE`, `TOLL_FREE`, `VOIP`, …). Empty = any type |
| `allowedCountries` | `Array<String>`          | `[]`    | ISO 3166-1 alpha-2 codes derived from the dial prefix. Empty = any country                                                         |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_NOT_ALLOWED`.

```kotlin
@field:Phone(
    allowedTypes = [PhoneNumberType.MOBILE],
    allowedCountries = ["US", "CA"],
)
val contactNumber: String
```

#### `@Html`

Fail (do not silently strip) when markup is outside an allow-list. **Requires**
`org.jsoup:jsoup` on the runtime classpath (not transitive). Blank is skipped inside
`validate`; `null` uses default `validateNull`.
Checks stop at the first problem: unknown tag → disallowed attribute → disallowed
protocol → sanitization would alter content. Markup is parsed as a body fragment.

Each `allowed*` array defaults to `["*"]` (no restriction for that dimension). Use an
explicit list to tighten the policy.

| Parameter          | Type            | Default | Meaning                                                                                                                                                                       |
|--------------------|-----------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `allowedTags`      | `Array<String>` | `["*"]` | Permitted element names; `"*"` allows any tag                                                                                                                                 |
| `allowedAttrs`     | `Array<String>` | `["*"]` | Permitted `"tag:attribute"` pairs (e.g. `"a:href"`); `"*"` allows any. Entries without `:` are ignored when not `"*"`                                                         |
| `allowedProtocols` | `Array<String>` | `["*"]` | Permitted `"tag:attribute:scheme1,scheme2"` triples (e.g. `"a:href:https"`); `"*"` allows any scheme. Incomplete entries are ignored                                            |

Failures: `VALUE_NOT_ALLOWED`, `VALUE_SANITIZATION_MISMATCH`.

```kotlin
@field:Html(allowedTags = ["p", "a", "strong"], allowedAttrs = ["a:href"])
val bio: String
```

#### `@HexColor`

CSS hex color: `#` + exactly 3 or 6 hex digits (`"#0af"`, `"#00AAFF"`). Letters may be mixed
case. 4- and 8-digit alpha forms (`#RGBA`, `#RRGGBBAA`) are **not** accepted. No extra
parameters.

Failure: `VALUE_FORMAT_INVALID`.

#### `@FinancialCode`

Financial identifier of the given `type`. Whitespace is stripped and the value is uppercased
before checks. When `countries` is non-empty, the embedded ISO country code must be one of
those values (compared uppercase). Empty `countries` accepts any country (IBAN still requires
a registry entry).

| Parameter  | Type             | Default | Meaning                                              |
|------------|------------------|---------|------------------------------------------------------|
| `type`     | `FinancialCode.Type` | *(required)* | Identifier family                              |
| `countries`| `Array<String>`  | `[]`    | Optional ISO 3166-1 alpha-2 filter. Empty = any country |

`FinancialCode.Type`:

| Value  | Meaning                                                                  |
|--------|--------------------------------------------------------------------------|
| `IBAN` | ISO 13616 IBAN: known country, fixed country length, MOD-97-10 checksum  |
| `ISIN` | ISO 6166 ISIN: 12 characters with Luhn check digit                       |
| `BIC`  | BIC/SWIFT: 8 or 11 characters                                            |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_NOT_ALLOWED`, `VALUE_CHECKSUM_INVALID`.

```kotlin
@field:FinancialCode(type = FinancialCode.Type.IBAN, countries = ["DE", "FR"])
val accountIban: String
```

#### `@Checksum`

Check-digit / checksum over a `CharSequence` slice `[startIndex, endIndex)` (`endIndex = -1`
means through the end). The check digit sits at `checkDigitIndex` relative to that slice
(`-1` = last character). When `ignoreNonDigits` is `true`, non-digit characters are stripped
before most algorithms; for `MOD97_10`, letters are kept and mapped `A`–`Z` → 10–35
(ISO 7064), and only other non-alphanumeric characters are skipped.

| Parameter         | Type                 | Default      | Meaning                                                              |
|-------------------|----------------------|--------------|----------------------------------------------------------------------|
| `algorithm`       | `Checksum.Algorithm` | *(required)* | Check-digit algorithm                                                |
| `checkDigitIndex` | `Int`                | `-1`         | Index of the check digit within the slice; `-1` = last               |
| `startIndex`      | `Int`                | `0`          | Inclusive start of the validated slice                               |
| `endIndex`        | `Int`                | `-1`         | Exclusive end of the slice; `-1` = end of the value                  |
| `ignoreNonDigits` | `Boolean`            | `false`      | Strip non-digits (or non-alphanumeric for MOD-97-10) first           |

`Checksum.Algorithm`:

| Value      | Meaning                                                              |
|------------|----------------------------------------------------------------------|
| `LUHN`     | Classic Luhn (Mod-10); check digit included, sum ≡ 0 (mod 10)        |
| `MOD10`    | Alias of `LUHN` (payment-card style)                                 |
| `MOD11`    | Weighted Mod-11 (ISBN-10 style); check digit may be `X` for 10       |
| `VERHOEFF` | Verhoeff dihedral-group check digit                                  |
| `DAMM`     | Damm quasigroup check digit                                          |
| `MOD97_10` | ISO 7064 MOD 97-10 over alphanumeric characters                      |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_CHECKSUM_INVALID`.

```kotlin
@field:Checksum(algorithm = Checksum.Algorithm.LUHN, ignoreNonDigits = true)
val accountNumber: String
```

#### `@CreditCard`

Payment card PAN: digits, spaces, and hyphens allowed as separators; 13–19 digits after
stripping; Luhn (Mod-10) checksum. Does **not** check expiry, network, or that the card is
active. No extra parameters.

Failures: `VALUE_FORMAT_INVALID`, `VALUE_CHECKSUM_INVALID`.

#### `@IsoCountry`

ISO 3166-1 **alpha-2**, exact **uppercase** (`"US"` passes, `"us"` fails). No extra parameters.

Failure: `VALUE_NOT_ALLOWED`.

#### `@IsoCurrency`

ISO 4217 three-letter code, exact **uppercase** (`"USD"` passes, `"usd"` fails). No extra
parameters.

Failure: `VALUE_NOT_ALLOWED`.

#### `@IsoLanguage`

Language tag `xx` or `xx-YY` (`"en"`, `"en-US"`). Exact and case-sensitive: language
lowercase, region uppercase. Extended BCP 47 (scripts, variants) is rejected. No extra
parameters.

Failure: `VALUE_FORMAT_INVALID`.

#### `@Barcode`

Validates a barcode / publishing identifier. Spaces and hyphens are ignored as separators.

| Parameter | Type           | Default      | Meaning                                 |
|-----------|----------------|--------------|-----------------------------------------|
| `type`    | `Barcode.Type` | *(required)* | `EAN`, `UPC`, `GTIN`, `ISBN`, or `ISSN` |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_CHECKSUM_INVALID`.

```kotlin
@field:Barcode(type = Barcode.Type.ISBN)
val isbn: String
```

#### `@IpAddress`

DNS-free IPv4 / IPv6 literal check. `null` → default `validateNull` (skip).

| Parameter | Type             | Default | Meaning              |
|-----------|------------------|---------|----------------------|
| `type`    | `IpAddress.Type` | `ANY`   | `V4`, `V6`, or `ANY` |

Failure: `VALUE_FORMAT_INVALID`.

```kotlin
@field:IpAddress(type = IpAddress.Type.V4)
val gateway: String
```

#### `@Base64`

Decode check for `CharSequence`. `null` → default `validateNull` (skip).

| Parameter        | Type      | Default | Meaning                                           |
|------------------|-----------|---------|---------------------------------------------------|
| `urlSafe`        | `Boolean` | `false` | When `true`, accept URL-safe alphabet (`-` / `_`) |
| `requirePadding` | `Boolean` | `true`  | When `true`, require `=` padding where applicable |

Failure: `VALUE_FORMAT_INVALID`.

#### `@FilePath`

Safe **relative** file path. Always rejects null bytes, blank values, `..` segments,
Windows-forbidden characters (`< > : " | ? *`) in each segment, and absolute paths
(Unix `/…`, UNC `\\…`, Windows drive prefixes `C:…`). Path separators `/` and `\` are
allowed for relative multi-segment paths. By default the last segment must carry a file
extension; set `requireExtension = false` for directories / extensionless names.
`null` → default `validateNull` (skip).

| Parameter          | Type      | Default | Meaning                                                      |
|--------------------|-----------|---------|--------------------------------------------------------------|
| `requireExtension` | `Boolean` | `true`  | Require a non-empty extension on the last path segment       |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_NOT_ALLOWED`.

```kotlin
@field:FilePath
val relativePath: String

@field:FilePath(requireExtension = false)
val targetDirectory: String
```

---

### Temporal

#### `@RelativeToNow`

`Temporal` relative to “now” in the **same concrete temporal type**. Supported “now” mappings
include ISO locals/offsets/zones, `Instant`, `Year`, and chronology-backed types. Optional
[within] caps how far past/future the value may lie (`Int.MAX_VALUE` = unbounded).

| Parameter  | Type                     | Default          | Meaning                                                                 |
|------------|--------------------------|------------------|-------------------------------------------------------------------------|
| `relation` | `RelativeToNow.Relation` | *(required)*     | `LT`, `LTE`, `GT`, `GTE`, or `EQ`                                       |
| `within`   | `Int`                    | `Int.MAX_VALUE`  | Max offset from now in [unit] steps; `Int.MAX_VALUE` disables the bound |
| `unit`     | `ChronoUnit`             | `DAYS`           | Unit for [within]                                                       |

Failures include `TEMPORAL_NOT_IN_PAST`, `TEMPORAL_NOT_IN_FUTURE`,
`COMPARISON_UNSATISFIED_NOT_EQUAL` (`EQ`), `VALUE_UNSUPPORTED` (incompatible window unit),
`TEMPORAL_TOO_EARLY`, `TEMPORAL_TOO_LATE` (subset depends on [relation]).

```kotlin
@field:RelativeToNow(relation = RelativeToNow.Relation.LT, within = 30, unit = ChronoUnit.DAYS)
val eventOccurredAt: LocalDate

@field:RelativeToNow(relation = RelativeToNow.Relation.GTE, within = 90, unit = ChronoUnit.DAYS)
val expiresAt: LocalDate
```

#### `@DaysOfWeek`

Calendar day-of-week of a date-aware temporal must be one of `days` (or must **not** be when
`negated = true`). Time-only temporals (`LocalTime`) **skip** rather than fail.

| Parameter | Type               | Default                 | Meaning        |
|-----------|--------------------|-------------------------|----------------|
| `days`    | `Array<DayOfWeek>` | *(required, non-empty)* | Allow-list (or deny-list when negated) |
| `negated` | `Boolean`          | `false`                 | When `true`, day must not be in `days` |

Failure: `TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED`.

```kotlin
@field:DaysOfWeek([DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY])
val deliveryDate: LocalDate
```

#### `@DaysOfMonth`

Calendar day-of-month (1–31) of a date-aware temporal must be one of `days` (or must **not** be
when `negated = true`). Time-only temporals **skip**.

| Parameter | Type       | Default                 | Meaning                                                          |
|-----------|------------|-------------------------|------------------------------------------------------------------|
| `days`    | `IntArray` | *(required, non-empty)* | Allow-list (or deny-list when negated; stored as `Set<Int>` in metadata) |
| `negated` | `Boolean`  | `false`                 | When `true`, day must not be in `days` |

Failure: `TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED`.

```kotlin
@field:DaysOfMonth([1, 15])
val billingDate: LocalDate
```

#### `@Months`

Calendar month of a date-aware temporal must be one of `months` (or must **not** be when
`negated = true`). Time-only temporals **skip**.

| Parameter | Type           | Default                 | Meaning          |
|-----------|----------------|-------------------------|------------------|
| `months`  | `Array<Month>` | *(required, non-empty)* | Allow-list (or deny-list when negated) |
| `negated` | `Boolean`      | `false`                 | When `true`, month must not be in `months` |

Failure: `TEMPORAL_MONTH_NOT_ALLOWED`.

```kotlin
@field:Months([Month.JUNE, Month.JULY, Month.AUGUST])
val seasonStart: LocalDate
```

---

## Custom constraints

A constraint is always **annotation + validator**. Metadata (`{Ann}Constraint`) is **generated
by KSP** from role markers — do not hand-write it.

```text
You write                  KSP emits                 Engine runs
─────────                  ─────────                 ──────────
@OddYears                  OddYearsConstraint        OddYearsValidator.runValidation(…)
@Constraint(validatedBy)   ConstraintCatalog SPI         ├─ null  → validateNull (default skip)
                                                         └─ value → validate(value: Value, …)
```

`ConstraintValidator<Value : Any, Constraint : ConstraintMetadata>` — `Value` is the accepted **non-null** subject type
(a nullable type argument is rejected at compile time). Wrong
placements fail while typing (IntelliJ plugin) and at KSP compile. Apply
`ksp(validata-processor)` on the **module that declares the annotation** so the catalog SPI
entry is emitted.

Working end-to-end examples live in the framework’s sample module (outside this jar).

### Null policy for authors

| Hook                                | When              | What you do                                                                          |
|-------------------------------------|-------------------|--------------------------------------------------------------------------------------|
| `validate(value: Value, …)`         | Non-null subject  | Implement the rule. **Do not** declare `Value?` or null-check                        |
| `validateNull(constraint, context)` | Subject is `null` | Default skips. Override **only** for presence-on-null                                |
| `runValidation(value: Any?, …)`     | Engine entry      | Groups → route null/non-null → stamp path/message. Do not call this from custom code |

Empty / blank / deep-empty checks belong in `validate` — those values are non-null.

### 1. Annotation

Every `@Constraint` annotation must declare:

- `@Constraint(validatedBy = [YourValidator::class])` — **required**, non-empty (`validatedBy` is an array — bounds
  constraints register several validators)
- exactly one `@ConstraintMessage` parameter (`String`)
- exactly one `@ConstraintGroups` parameter (`Array<KClass<*>>`)
- `@Retention(RUNTIME)` and targets that include at least `FIELD` / `VALUE_PARAMETER` / `TYPE`
  (type-use is what enables `List<@YourAnn String>`)

```kotlin
@MustBeDocumented
@Constraint(validatedBy = [OddYearsValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class OddYears(
    @ConstraintMessage
    val message: String = "",
    @ConstraintGroups
    val groups: Array<KClass<*>> = [OnDefault::class],
)
```

Keep the **annotation parameter name** identical to the generated metadata property name so
discovery stays aligned.

### Constraint composition (AND / OR)

Stack leaf `@Constraint` meta-annotations on a **non-`@Constraint`** outer type. Do **not**
put `@Constraint` on the outer annotation.

| Mode              | Marker                                   | Runtime                                                                |
|-------------------|------------------------------------------|------------------------------------------------------------------------|
| **AND** (default) | absent, or `@ConstraintComposition(AND)` | Flatten leaves into the property’s constraint list                     |
| **OR**            | `@ConstraintComposition(OR)`             | One synthetic composition site; passes when **any** active leaf passes |

```kotlin
@Email
@Phone
@ConstraintComposition(ConstraintComposition.Mode.OR)
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class EmailOrPhone(
    @ConstraintMessage val message: String = "",
    @ConstraintGroups val groups: Array<KClass<*>> = [OnDefault::class],
)
```

**OR rules:** ≥2 leaf `@Constraint`s; no nested composed members; presence (`@Required` / `@RequiredWhen`) stays on the
usage site, not as OR members. Failure code is
`CONSTRAINT_UNSATISFIABLE`. If **no** leaf is active under the current groups, the composition
is **skipped** (returns valid). Runtime uses hand-written `CompositionConstraint` metadata —
not KSP-generated and **not** in the constraint catalog. See
`@ConstraintComposition` / `ConstraintComposition.Mode` in this module.

### 2. Validator

Prefer a Kotlin `object` so the catalog can share one instance. Implement `validate` only for
value rules — `runValidation` (groups, null routing, path, message) is the engine’s job.

```kotlin
enum class OddYearsError(override val message: String) : ConstraintErrorDefinition {
    YEAR_NOT_ODD("Must be an odd integer — even and fractional values are not accepted.");

    override val code: String get() = name
}

object OddYearsValidator : ConstraintValidator<Number, OddYearsConstraint>() {

    override fun possibleErrorCodes(constraint: OddYearsConstraint): Set<ConstraintErrorDefinition> =
        setOf(OddYearsError.YEAR_NOT_ODD)

    override fun validate(
        value: Number,                       // non-null — null never reaches here
        constraint: OddYearsConstraint,
        context: ValidationContext,
    ): ConstraintError<*>? {
        if (value.toLong() % 2L != 0L) return null
        return ConstraintError(
            code = OddYearsError.YEAR_NOT_ODD,
            message = OddYearsError.YEAR_NOT_ODD.message,
        )
    }
}
```

Presence-on-null example (override `validateNull`; empty/blank still go through `validate`):

```kotlin
override fun validateNull(
    constraint: RequiredConstraint,
    context: ValidationContext,
): ConstraintError<*>? {
    // return ConstraintError(…) or null when null is acceptable for this mode
}
```

Contract:

| Rule                                                           | Why                                                                                 |
|----------------------------------------------------------------|-------------------------------------------------------------------------------------|
| `validate` takes non-null `Value`                              | `Value : Any`; null is routed to `validateNull`                                     |
| Return `null` on success                                       | Engine materializes path only on failure                                            |
| Return path-free `ConstraintError` with precise `message`      | Enum defaults are abstract; clients need rule-specific text                         |
| Override `validateNull` only for presence-on-null              | Default already skips null                                                          |
| Override `possibleErrorCodes(constraint)`                        | OpenAPI / docs tooling lists 400 codes for that config (empty set = undocumented)   |
| Do not retain `ValidationContext`                              | Cursor is valid only for this call                                                  |
| Never put secrets in `message` / `metadata`                  | Password-style fields must not echo the raw value                                   |
| `requiresArrayContext = true` only if you read `context.array` | Needed for sibling-element uniqueness; a false negative silently skips those checks |
| Prefer `getPropertyValue(name, context)` for siblings          | Resolves declared / `@JsonProperty` names; throws if the path cannot be resolved    |

You may use a built-in `ConstraintErrorCode` or your own enum that implements
`ConstraintErrorDefinition` (must also be an `enum` — `ConstraintError<CodeT>` requires both).

```kotlin
enum class ShopError(override val message: String) : ConstraintErrorDefinition {
    ODD_YEAR_REQUIRED("Year must be odd");

    override val code: String get() = name
}
```

### 3. Parameters (`@ConstraintArg`)

Mark payload parameters so KSP and the IDE know how to check them — **no allowlist of
annotation names**. Stack several `@ConstraintArg` on one parameter; each pairs `kinds` with a
`target`.

| Kind            | Rule                                                                                 |
|-----------------|--------------------------------------------------------------------------------------|
| `NOT_BLANK`     | Reject blank strings (use `target = ELEMENT` for array elements)                     |
| `NON_EMPTY`     | Reject empty collections / blank scalars                                             |
| `TYPED_LITERAL` | String must parse as the annotated **subject** type (number, temporal, enum name, …) |
| `NON_NEGATIVE`  | Numeric arg `>= 0`                                                                   |
| `POSITIVE`      | Numeric arg `> 0`                                                                    |
| `REGEX`         | String must compile as `java.util.regex.Pattern`                                     |

`target` is `VALUE` (default — the argument as a whole) or `ELEMENT` (each array element).

Example with a typed numeric floor:

```kotlin
@Constraint(validatedBy = [SampleFloorValidator::class])
annotation class SampleFloor(
    @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
    val value: String,
    @ConstraintMessage val message: String = "",
    @ConstraintGroups val groups: Array<KClass<*>> = [OnDefault::class],
)

object SampleFloorValidator : ConstraintValidator<Number, SampleFloorConstraint>() {
    override fun possibleErrorCodes(constraint: SampleFloorConstraint) = setOf(
        ConstraintErrorCode.VALUE_PARSING_FAILED,
        ConstraintErrorCode.NUMBER_TOO_SMALL,
    )

    override fun validate(
        value: Number,
        constraint: SampleFloorConstraint,
        context: ValidationContext,
    ): ConstraintError<*>? {
        val min = constraint.value.toConstraintNumber()
            ?: return ConstraintError(
                code = ConstraintErrorCode.VALUE_PARSING_FAILED,
                message = "Cannot parse floor literal '${constraint.value}' as a number.",
            )
        val actual = value.toBigDecimalOrNull()
            ?: return ConstraintError(
                code = ConstraintErrorCode.VALUE_PARSING_FAILED,
                message = "Cannot convert $value to a decimal for the floor check.",
            )
        if (actual >= min) return null
        return ConstraintError(
            code = ConstraintErrorCode.NUMBER_TOO_SMALL,
            message = "Must be at least $min (got $actual).",
            metadata = constraint,
        )
    }
}
```

Array arguments often need two markers (collection non-empty **and** each element typed):

```kotlin
@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
@ConstraintArg(
    ConstraintArgKind.NOT_BLANK,
    ConstraintArgKind.TYPED_LITERAL,
    target = ConstraintArgTarget.ELEMENT,
)
val values: Array<String>
```

`ConstraintArgKind` / `ConstraintArgTarget` live in **`validata-schema`**; the
`@ConstraintArg` annotation itself lives here.

### 4. Sibling / element paths (`@PropertyRef`)

Use when a parameter **names another property** rather than carrying data.

```kotlin
annotation class Compare(
    @PropertyRef(compatibility = PropertyRefCompatibilityKind.COMPARABLE_FAMILY)
    @ConstraintArg(ConstraintArgKind.NOT_BLANK)
    val ref: String,
    val operation: Operation,
    @ConstraintMessage val message: String = "",
    @ConstraintGroups val groups: Array<KClass<*>> = [OnDefault::class],
)
```

| `PropertyRefScope`  | Resolve root                     | Example                                        |
|---------------------|----------------------------------|------------------------------------------------|
| `SIBLING` (default) | Declaring class / sibling params | `@Compare(ref = "password", …)`                |
| `ELEMENT`           | Collection element type          | `List<@Distinct(by = ["email"]) UserDto>` |

`PropertyRefScope` and `PropertyRefCompatibilityKind` live in **`validata-schema`**
(`io.ghaylan.validata.schema.ref`). Paths are **single-segment** only (`"password"` OK;
`"address.city"` rejected).

| `PropertyRefCompatibilityKind` | Use when                                                                      |
|--------------------------------|-------------------------------------------------------------------------------|
| `NONE`                         | Existence only (`@RequiredWhen`, `@Distinct.by`)                              |
| `COMPARABLE_FAMILY`            | Cross-field compare (`@Compare`) — both numeric, both temporal, or same orderable kind |
| `SAME_SCALAR_KIND`             | Same scalar kind on both sides (custom constraints)                           |

A parameter may carry both `@PropertyRef` and `@ConstraintArg`. With `NOT_BLANK`, blank is an
error; without it, blank means “no reference”.

Read siblings at runtime via the inherited **`getPropertyValue(name, context)`** helper (preferred — resolves declared /
wire names and fails loudly on unknown paths). Raw access:
`ValidationContext.containerObject`. For element-uniqueness, set `requiresArrayContext = true`
and read `context.array`.

### 5. Enable KSP and rebuild

Apply `ksp(validata-processor)` on the module that **declares** the annotation (see
[root Installation](../README.md#installation)). Rebuild. Do **not** edit
`build/generated/…/*Constraint.kt`. KSP emits:

- `{AnnotationSimpleName}Constraint` in the same package as the annotation
- a `ConstraintCatalog` ServiceLoader entry under
  `META-INF/services/io.ghaylan.validata.constraint.spi.ConstraintCatalog`

If the catalog is missing, the annotation compiles but never runs.

### 6. Hand-written catalog (tests / third-party jars without KSP)

Generated catalogs cover built-ins and app annotations. Implement `ConstraintCatalog` only in
tests or when you cannot run the processor:

```kotlin
class TestCatalog : ConstraintCatalog {
    override fun entries(): List<ConstraintCatalogEntry> = listOf(
        ConstraintCatalogEntry(
            annotationType = OddYears::class.java,
            metadataType = OddYearsConstraint::class.java,
            valueType = /* TypeInfo for Number */,
            validatorType = OddYearsValidator::class.java,
            defaultInstanceFactory = { OddYearsValidator },
        ),
    )
}
```

Register under `META-INF/services/io.ghaylan.validata.constraint.spi.ConstraintCatalog`.
Prefer `GeneratedConstraintCatalogs.all` at runtime. For hand-written schemas in tests, use
`compileConstraints(mapOf(metadata to validator))` to build `CompiledConstraint` lists.

### 7. Override a built-in (or custom) validator

Built-in validators are Kotlin `object`s. Hosts that use a DI container can register a bean of
the **same validator type** — looked up by `ConstraintCatalogEntry.validatorType`, with
`defaultInstanceFactory` as fallback. Outside DI, wire your instance into the runner / catalog
yourself.

### 8. Docs mapping (optional)

If a documentation module maps metadata to OpenAPI (or similar), implement its
`ConstraintDocumentation` SPI so custom metadata is not flagged unmapped. That SPI lives
outside this jar.

### Checklist

1. Write `@Constraint(validatedBy = […])` with `@ConstraintMessage` / `@ConstraintGroups`.
2. Mark payload params with `@ConstraintArg` and/or `@PropertyRef` as needed.
3. Implement `ConstraintValidator<Value : Any, YourAnnConstraint>` (metadata type is generated).
4. Implement `validate(value: Value, …)` — **non-null**; override `validateNull` only for presence.
5. Override `possibleErrorCodes(constraint)`.
6. Enable `ksp("…:validata-processor")` on that module.
7. Rebuild — do not edit generated `*Constraint.kt`.
8. Use the annotation on a `@Validatable` type (or handler param) and run a test that expects
   the error code.

### Do not

- Hand-write `{Ann}Constraint` data classes (except engine-internal composition metadata)
- Declare `validate(value: Value?, …)` or null-check inside `validate`
- Flatten nested enums (`Url.Type`, `RequiredWhen.Condition` stay nested)
- Put passwords / tokens in error `message` or `metadata`
- Scan annotations at request time — schemas are compiled ahead of time
- Forget KSP on the module that *declares* the custom annotation (the consumer of the
  annotation is not enough if the annotation lives in another jar)

---

## Markers and groups

| Item                                | Role                                                                                                                                           |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `@Validatable`                      | Type marker for schema generation / lookup. Params: `discriminator: String = ""`, `subtypes: Array<Validatable.Subtype> = []`                  |
| `Validatable.Subtype`               | `name` (discriminator literal) + `type` (concrete `@Validatable` class)                                                                        |
| `@NoCascade`                        | Property opt-out: the property’s own constraints still run; nested fields are not walked                                                       |
| `@Validate`                         | Handler / config marker (Spring-free). KSP copies `oneErrorPerParam`, `failFast`, `groups` onto the `EndpointSchema` used by `validateRequest` |
| `OnDefault`, `OnCreate`, `OnUpdate` | Built-in group tokens; custom `KClass` tokens work too                                                                                         |
| `ValidationRegistry`                | `registerValidators` / `registerStaticSchemas` / `freeze` / lookups (`engine.ValidationRegistry`)                                              |

**Polymorphism:** for sealed / abstract / interface roots, declare concrete children with
`discriminator` + `Validatable.Subtype` entries. The discriminator property must be a **scalar**; each subtype pairs one
discriminator value (`name`) with one concrete class (`type`). When both sealed subclasses and `subtypes` are present,
their type sets must match (compile-time error). The engine still dispatches by actual runtime class.

---

## Package layout

```text
io.ghaylan.validata
├── constraint/                 # authoring API + built-ins
│   ├── annotation/             # 38 built-in annotations (+ generated *Constraint)
│   ├── composition/            # CompositionConstraint, CompositionOrRunner (engine-facing)
│   ├── spi/                    # ConstraintCatalog, GeneratedConstraintCatalogs
│   ├── support/                # Shared helpers (numbers, temporals, …)
│   └── validator/              # nested by family: string/email/, bound/duration/, …
├── engine/                     # ValidatorEngine, ValidationRegistry, Options, Limits,
│   │                           # CompileConstraints, CompiledConstraintExecutor, runners
│   ├── walk/                   # Standalone + request orchestration (internal)
│   ├── support/                # Deduper, limit guards (internal)
│   └── fastpath/               # Array-context fast path (internal)
├── exception/                  # ConstraintViolationException
├── ext/                        # MethodUniqueIdentifiers
├── groups/                     # OnDefault, OnCreate, OnUpdate
├── model/                      # ConstraintError, ConstraintErrorCode,
│                               # ErrorLocation, ConstraintErrorDefinition, …
├── runtime/                    # ValidationContext (validator-facing)
├── schema/                     # @Validatable, @NoCascade, @Validate
│   └── runtime/                # SchemaNotFoundException
└── internal/                   # TypeInfo, ReflectionUtils, StructureClassifier, SubjectTypes, …
                                # (toolchain SPI — prefer not calling from app code)
```

Also at `constraint/` root: `@ConstraintComposition` (with nested `ConstraintComposition.Mode`), and the shared
`@Constraint` / `@ConstraintArg` / `@PropertyRef` / `@ConstraintMessage` / `@ConstraintGroups`
authoring API. Extension helpers live under `constraint/ext/` (`NumberExt`, `TemporalExt`, …).

---

## Public API surface

SemVer coverage for these types (and the rest of the framework): [root Stability & SemVer](../README.md#stability--semver).
This module is **0.x** until the project tags **1.0.0**.

| Area          | Types                                                                                                                                                                                                  |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Constraints   | `@Constraint`, `ConstraintValidator` (`validate` / `validateNull` / `runValidation`), `@ConstraintArg` / `@PropertyRef` / `@ConstraintMessage` / `@ConstraintGroups`, **built-in `@…` annotations** |
| Composition   | `@ConstraintComposition` (`ConstraintComposition.Mode`)                                                                                                                                     |
| Catalog       | `ConstraintCatalog`, `ConstraintCatalogEntry`, `GeneratedConstraintCatalogs`                                                                                                                           |
| Engine        | `ValidatorEngine`, `ValidationOptions`, `ValidationLimits`, `ValidationRegistry`, `compileConstraints`, `ValidatorBackedRunner`                                                                        |
| Context       | `runtime.ValidationContext` — see table below                                                                                                                                                          |
| Errors        | `ConstraintError` (optional `metadata` payload), `ConstraintErrorCode`, `ConstraintErrorDefinition`, `RequiredWhen.Condition`, `ErrorLocation`                                                         |
| Markers       | `@Validatable`, `@NoCascade`, `@Validate`, `OnDefault` / `OnCreate` / `OnUpdate`                                                                                                                       |
| Lookup        | `SchemaNotFoundException` (missing `@Validatable` / object schema)                                                                                                                                     |
| Aggregation   | `ConstraintViolationException`                                                                                                                                                                         |
| Endpoint keys | `MethodUniqueIdentifiers`                                                                                                                                                                              |

### `ValidationContext` (validators)

| Member                          | Role                                                     |
|---------------------------------|----------------------------------------------------------|
| `fieldPath` / `path`            | Wire path (materialize `fieldPath` only on failure)      |
| `containerObject`               | Parent object for sibling reads                          |
| `array`                         | Sibling-element list when `requiresArrayContext`         |
| `elementIndex`                  | Index in an iterable, or `NO_ELEMENT_INDEX`              |
| `clock`                         | Fixed for the run (`@RelativeToNow`; inject in tests) |
| `getOrComputeAttribute`         | Per-run memoization bag                                  |
| `groups` / `skipGroupChecks`    | Active groups / engine fast-path                         |
| `oneErrorPerParam` / `failFast` | Walk flags for the current run                           |

Do **not** retain a context across calls. `engine.walk.*`, `engine.support.*`,
`engine.fastpath.*`, and `ValidationCursor` are implementation detail.

---

## Configuration

`ValidationLimits` (constructor defaults; the Spring host binds the same ceilings from
`validata.limits.*` — see [root Configuration](../README.md#configuration)):

| Field                     | Default  | Meaning                                                                                      |
|---------------------------|----------|----------------------------------------------------------------------------------------------|
| `maxDepth`                | `32`     | Nesting ceiling; exceeding records `STRUCTURE_DEPTH_EXCEEDED` (fail closed, not truncated)   |
| `maxElementsPerContainer` | `10_000` | Max entries walked in one collection / array / map; exceeding records `COLLECTION_TOO_LARGE` |
| `maxErrors`               | `200`    | Stop collecting after this many violations (request already failed)                          |

A limit that is tighter than `@Size(max = …)` wins. Raise a **specific** limit for bulk
endpoints rather than all three.

`RegexValidator.MAX_INPUT_LENGTH` (10_000) caps `@Regex` subject length before matching.

Compile-time KSP options (`validata.jackson.naming`, cascade, shape depth) are **not**
engine constructor fields — see
[validata-processor Configuration](../validata-processor/README.md#configuration).

---

## Error handling & diagnostics

| Surface                                   | When                                                           | Guidance                                                                                                                                                                              |
|-------------------------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ConstraintError` + `ConstraintErrorCode` | Constraint / limit failure                                     | `path`, `location`, `code`, precise `message`, optional `metadata` (failing `*Constraint` metadata). Resolution: annotation `message` → validator message → code default            |
| `ConstraintViolationException`            | Aggregated failures (hosts may wrap)                           | Path-ordered `errors` for clients; empty stack (`writableStackTrace=false`)                                                                                                           |
| `SchemaNotFoundException`                 | Missing generated **object** schema for a `@Validatable` class | Actionable checklist (KSP, `@Validatable`, classpath)                                                                                                                                 |
| `IllegalStateException`                   | Unknown endpoint id on `validateRequest(id, …)`                | Register the static `EndpointSchema` / fix the method unique id                                                                                                                       |
| `ConstraintError.metadata`                | Extra failure payload                                          | Usually the generated `*Constraint` instance that failed; engine limit errors leave it `null` and put bounds in `message` — never secrets                                             |

`metadata` often holds a generated `*Constraint` (`ConstraintMetadata`). That type is **not**
guaranteed Jackson-serializable (for example `groups: Set<KClass<*>>`). Hosts must map errors to
their own wire DTO — omit `metadata`, or project public args to a `Map` — before JSON responses.
Do not rely on library-side `@JsonIgnore`.

Built-in codes live in `ConstraintErrorCode` (presence, bounds, formats, text patterns,
structure limits, …). Apps may define their own `ConstraintErrorDefinition` enums for custom
validators. `VALUE_TYPE_MISMATCH` and `PROPERTY_UNKNOWN` are reserved codes in the catalog for
host / binding layers.

Password validators never put the raw password in `message` or `metadata`.

---

## Testing notes

```bash
./gradlew :validata-core:test --parallel
```

Current suite: **~879** JUnit tests, all green (validators + engine + SPI + internals).

Tests mirror production packages:

| Layer           | Where                          | What it locks                                                                                  |
|-----------------|--------------------------------|------------------------------------------------------------------------------------------------|
| L1 — Wiring     | `constraint/spi/`              | Built-in annotation → catalog → validator                                                      |
| L2 — Validators | `constraint/validator/**`      | Null via `validateNull`, pass, fail+code, edges (NaN, whitespace, regex length, range constraint) |
| L3 — Engine     | `engine/`                      | Walk, `ValidationOptions`, cascade, DynamicShape, polymorphism, paths, groups, limits, freeze  |
| L4 — Internals  | `internal/`, `engine/support/` | ReflectionUtils, StructureClassifier, LimitGuards, Deduper                                     |
| L5 — Golden     | `golden/`                      | Multi-constraint request walks (when present)                                                  |

Shared harness: `support/ValidatorTestSupport.kt`, `SiblingContexts.kt`, `EngineTestSupport.kt`.
`EngineTestSupport.registry` is **frozen** — use `unfrozenRegistry()` when registering schemas
in a test. Use `compileConstraints(…)` for hand-written schema constraint lists.

### New `*Validator` checklist

Every new validator ships a matching `*ValidatorTest` covering at minimum:

1. Null behavior via `runValidation` / harness (`validateNull` default skip, or presence override)
2. Happy path on a non-null value
3. Boundary / inclusive-exclusive where applicable
4. Failure with exact `ConstraintErrorCode` (prefer asserting `ConstraintError.metadata` is the failing metadata)
5. Blank / whitespace / unparseable literal bounds when the constraint parses strings
6. Non-finite `Double`/`Float` when the subject is numeric

Tips:

- Optional Jsoup / libphonenumber are required only when exercising `@Html` / `@Phone`.
- Prefer real schemas and validators over mocks on critical paths.
- Construct schemas and register them on an **unfrozen** `ValidationRegistry`, then freeze before
  `ValidatorEngine` walks when you need production-like lookup.

**Not covered by design:** PIT mutation CI (optional).

---

## Rules to remember

1. No Spring in this module.
2. `validate(value: Value, …)` is **non-null** — null is `validateNull` (default skip).
3. Presence is not implied — pair format constraints with `@Required` / `@RequiredWhen`.
4. Optional Html/Phone jars are not transitive — add them when you use those constraints.
5. Public FQCNs (and nested enum names) are contracts for KSP and the IntelliJ plugin.
6. Nested enums stay nested (`Url.Type`, `RequiredWhen.Condition`, …).
7. Custom constraints need KSP on the module that **declares** the annotation.
8. `@Distinct` uniqueness is whole-element or field-tuple — there is no separate `mode`.
9. Empty email strings fail `@Email`; `@Url` / `@Html` also skip blank.
10. Standalone walks use `ValidationOptions`; request walks use flags on `EndpointSchema`.
11. Freeze `ValidationRegistry` before serving; further `register*` throws.
12. `@Password` never puts the raw password (or its length) in `message` / `metadata`.

---

## What it does not do

| Not handled here                                    | Belongs elsewhere                             |
|-----------------------------------------------------|-----------------------------------------------|
| Binding HTTP / Spring MVC                           | Spring host module                            |
| Generating schemas from source                      | KSP processor module                          |
| Defining `ObjectSchema` / `EndpointSchema` IR types | `validata-schema`                             |
| Scanning annotations at request time                | not done — schemas are compiled ahead of time |
| MessageSource / i18n, HTTP status, error JSON shape | host module                                   |
| OpenAPI / IDE PSI                                   | documentation / IDE modules                   |

**Rule of thumb:** if it *binds HTTP* or *auto-configures Boot*, it isn’t this module.

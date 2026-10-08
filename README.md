# Validata

Compile-time validation for Kotlin and Spring Boot.

You annotate DTOs and handlers. **KSP** turns those annotations into schemas at compile
time. At runtime a **Spring-free engine** walks the schema against live values. Spring Boot
binds HTTP and calls the engine. Optional OpenAPI and the IntelliJ plugin read the **same**
schemas — editor, build, docs, and production share one model.

```text
annotate  →  KSP emits schemas  →  engine checks values  →  your app maps the errors
```

This README is the **framework guide**: architecture, install, authoring, every built-in
constraint, configuration, and how the modules fit together. Each published jar also has its
own README for module-specific detail — start here, then open the module you care about
([Artifacts](#artifacts)).

Validata is **not** a Jakarta Bean Validation provider. `jakarta.validation` annotations are
not executed.

| Pin                    | Value                                                                            |
|------------------------|----------------------------------------------------------------------------------|
| Version                | **1.0.0** (`gradle.properties` / `libs.versions.toml`)                           |
| Maven group            | `io.github.ghaylansaada`                                                         |
| JDK (libraries)        | 21                                                                               |
| Kotlin                 | 2.4.20                                                                           |
| KSP                    | 2.3.12 (KSP2 — **not** the same major as Kotlin)                                 |
| Spring Boot            | 4.1.1 (compileOnly — your app pins the runtime)                                  |
| springdoc              | 3.1.1 (verified; compileOnly)                                                    |
| IntelliJ IDEA          | 2025.3+ (`sinceBuild` 253; built/tested against **2026.2.3**)                    |
| Plugin JDK             | 21 (compatible with the minimum supported IDE)                                   |
| Platform Gradle Plugin | 2.19.0                                                                           |
| Compatibility          | Supported surface **frozen** — see [Stability and SemVer](#stability-and-semver) |
| Changelog              | [CHANGELOG.md](CHANGELOG.md)                                                     |

Framework module READMEs do **not** repeat these pins — change them here (and in Gradle)
only. `validata-samples` / `validata-benchmarks` may keep their own run notes.

---

## Contents

1. [What it is](#what-it-is)
2. [How it is different](#how-it-is-different)
3. [How it works](#how-it-works)
4. [Installation](#installation)
5. [Quick start](#quick-start)
6. [Authoring](#authoring)
7. [Built-in constraints](#built-in-constraints)
8. [Custom constraints](#custom-constraints)
9. [Errors](#errors)
10. [Configuration](#configuration)
11. [OpenAPI](#openapi)
12. [IntelliJ plugin](#intellij-plugin)
13. [Without Spring](#without-spring)
14. [Performance](#performance)
15. [Sample app](#sample-app)
16. [Artifacts](#artifacts)
17. [Stability and SemVer](#stability-and-semver)
18. [Limitations](#limitations)
19. [Rules](#rules)
20. [License and Attribution](#license-and-attribution)

---

## What it is

Validata is a validation **pipeline** whose model is built at compile time, not discovered by
scanning annotations on every request.

| Layer                            | Role                                                                                                                           |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| **You**                          | Mark types with `@Validatable`, handlers with `@Validate`, optionally write custom `@Constraint`s                              |
| **KSP (`validata-processor`)**   | Verifies paths, literals, cascade, and polymorphism; emits `ObjectSchema` / `EndpointSchema` factories and constraint catalogs |
| **IR (`validata-schema`)**       | Shared blueprint types — nothing executes a check here                                                                         |
| **Engine (`validata-core`)**     | 38 built-in constraints, custom-constraint API, `ValidatorEngine` — **no Spring**                                              |
| **Host (`validata`)**            | Spring Boot WebMVC integration, optional `@ConfigurationProperties` validation, binding-failure translation                    |
| **OpenAPI (`validata-openapi`)** | Optional springdoc overlay from the same IR (depends on core, **not** the host)                                                |
| **IntelliJ plugin**              | Optional editor DX — not on the app classpath                                                                                  |

**Spring apps** depend on `validata` + `ksp(validata-processor)`. The processor never ships at
runtime. Missing schemas **fail closed** — there is no reflective fallback that “just works”
without KSP.

---

## How it is different

Validata’s model is **known at compile time**. KSP fails the build on unknown property
references, impossible constraint arguments, unmarked same-module nests, and bad
polymorphism metadata. The hot path is a walk over those generated graphs — not reflection
over annotations on each request.

| Topic                   | Validata                                                                                                                                                    |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| When the model is built | Compile time (KSP). Rebuild after changing annotations.                                                                                                     |
| Opt-in                  | `@Validate` on the handler (or class) **and** `@Validatable` on walked types                                                                                |
| Engine vs web           | Engine has no Spring. The host only binds HTTP and calls the engine.                                                                                        |
| Error JSON              | Engine and translated Spring / Jackson binding failures throw `ConstraintViolationException`. **You** map it — no `@ControllerAdvice` ships in the library. |
| OpenAPI                 | Optional companion reads the **same** IR. Docs cannot invent a second mapping.                                                                              |
| Custom constraints      | Annotation + validator. KSP generates `{Ann}Constraint` metadata and the catalog SPI.                                                                       |
| Editor                  | Optional plugin treats constraint strings as code (complete, color, red errors, rename).                                                                    |
| Payload ceilings        | `ValidationLimits` fail closed (`STRUCTURE_DEPTH_EXCEEDED`), not silent truncation.                                                                         |
| `@Regex`                | Inputs longer than 10 000 characters fail **before** `Pattern.matcher`.                                                                                     |
| Jakarta BV              | Not used. Not a Hibernate Validator adapter.                                                                                                                |

**What stays familiar:** field annotations, groups, fail-fast, cross-field comparisons,
element constraints (`List<@Email String>`).

**What you must do:** annotate, apply KSP on **every module that declares** those types,
rebuild. A handler with `@Validate` and no generated `EndpointSchema` fails at startup or
first request.

In-memory JMH comparison with Hibernate Validator (same fixtures): [Performance](#performance).

---

## How it works

```text
@Validatable DTO / @Validate handler / @Constraint
                    │
                    ▼
         validata-processor (KSP)
         analyze → verify → emit factories + SPI
                    │
                    ▼
              validata-schema (IR)
                    │
                    ▼
              validata-core (engine)
                    │
          ┌─────────┼──────────┐
          ▼         ▼          ▼
       validata   openapi   IntelliJ
       (Boot)     (docs)    (editor)
```

Think of four moments in the lifecycle:

1. **Compile.** KSP emits `<Type>Schema.build()`, request DTO wire-path objects
   (`CreateUserRequest_`), endpoint factories, flat path/query/header constants
   (`UserController_lookup_<fp>_` when present), and ServiceLoader aggregators under
   `<sourcePackage>.ghaylan.validata`.
2. **Startup.** The Boot host creates one registry and one engine, loads catalogs, and
   indexes `@Validate` handlers → generated `EndpointSchema`s.
3. **First call** to a handler. The host resolves a plan once (which HTTP sections to pack)
   and caches it for the context lifetime.
4. **Every request.** Spring binds body, query, header, and path. The host packs only the
   sections that plan needs and calls `ValidatorEngine` **once**. Violations →
   `ConstraintViolationException`. Success → the controller method. Selected Spring / Jackson
   binding failures on `@Validate` handlers are rewritten into the same exception so one
   `@ExceptionHandler` covers both. No per-request annotation scanning after the first plan
   resolve.

---

## Installation

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("com.google.devtools.ksp") version "<version>"
    id("org.springframework.boot") version "<version>"
}

dependencies {
    implementation("io.github.ghaylansaada:validata:<version>")
    ksp("io.github.ghaylansaada:validata-processor:<version>")

    // Required — Validata does not pin or re-export Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Optional: only needed if you use @Html or @Phone
    implementation("org.jsoup:jsoup:<version>")
    implementation("com.googlecode.libphonenumber:libphonenumber:<version>")

    // Optional: OpenAPI / Swagger documentation of validation rules
    implementation("io.github.ghaylansaada:validata-openapi:<version>")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:<version>")
}
```

**Checklist**

1. Add `implementation("…:validata:<version>")` and `ksp("…:validata-processor:<version>")`.
2. Apply `ksp(validata-processor)` on **every module that declares** `@Validatable`,
   `@Validate`, or a custom `@Constraint` — not only the Boot application module.
3. Keep Kotlin and KSP on an [official pairing](https://github.com/google/ksp/releases).
   KSP 2.x version numbers are **not** the Kotlin version (`2.3.x` with Kotlin `2.4.x` is
   expected).
4. Pin Spring Boot (and springdoc, if used) yourself. Validata compiles against them as
   `compileOnly` and does **not** publish Spring or springdoc as transitive dependencies.
   `validata-openapi` is verified against the springdoc version in the table above.
5. Add jsoup / libphonenumber only if you use `@Html` / `@Phone`.

---

## Quick start

Three pieces: a `@Validatable` DTO, a `@Validate` handler, and an `@ExceptionHandler` for
`ConstraintViolationException`. Then rebuild so KSP runs.

### 1. DTO + handler

```kotlin
@Validatable
data class CreateUserRequest(
    @field:Required
    @field:Size(min = 2, max = 40)
    val name: String?,

    @field:Required
    @field:Min("18")
    val age: Int?,
)

@RestController
class UserController {

    @Validate
    @PostMapping("/api/users")
    fun create(@RequestBody body: CreateUserRequest): String =
        "created ${body.name}"
}
```

Prefer **nullable** properties so JSON absence is `null` and `@Required` can fire.

Path, query, and header parameters use the same annotations:

```kotlin
@Validate
@GetMapping("/api/users/{userId}")
fun lookup(
    @PathVariable("userId") @Required @Size(min = 3, max = 32) userId: String?,
    @RequestParam("q") @Required @Size(min = 2, max = 64) q: String?,
    @RequestHeader("X-Tenant") @Required @Size(min = 2, max = 64) tenant: String?,
): String = "user=$userId"
```

### 2. Map errors in your app

Validata does not register `@ControllerAdvice`. The JSON shape is yours. One handler covers
engine violations **and** translated Spring / Jackson binding failures on `@Validate`
handlers:

```kotlin
@RestControllerAdvice
class ApiErrors {
	
	@ExceptionHandler(ConstraintViolationException::class)
	fun onValidation(
        ex: ConstraintViolationException
	): ResponseEntity<Map<String, Any?>> {
		val errors = ex.errors.map {
			mapOf(
				"path" to it.path,
				"location" to it.location?.name,
				"code" to it.code?.code,
				"message" to it.message)
		}
		
		return ResponseEntity.badRequest().body(
			mapOf("errors" to errors),
		)
	}
}
```

### 3. Rebuild and run

Rebuild so KSP regenerates schemas (`./gradlew build` or your IDE’s rebuild). Invalid input
fails **after** Spring binds arguments and **before** the handler body. Valid input reaches
the method.

### Configuration properties

The same `@Validate` on `@ConfigurationProperties` runs after the bean is bound. Invalid
YAML / env fails context refresh — the app never serves traffic.

```kotlin
@Validate
@Validatable
@ConfigurationProperties(prefix = "app.mail")
data class MailProperties(
    @field:Required
    @field:Size(min = 1)
    val host: String? = null,
)
```

---

## Authoring

### Markers

These four annotations are the whole authoring surface most apps need:

| Annotation     | Target                                                                    | Role                                                                                                                |
|----------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `@Validatable` | Type                                                                      | KSP emits an `ObjectSchema`. Nested types that are walked must be annotated too, or mark the property `@NoCascade`. |
| `@Validate`    | Controller method or class (method wins); `@ConfigurationProperties` type | Opt this handler / config bean into validation.                                                                     |
| `@NoCascade`   | Property                                                                  | The property’s **own** constraints still run; nested fields are not walked.                                         |
| `@Constraint`  | Your annotation type                                                      | Declares a custom rule (`validatedBy = [YourValidator::class]`).                                                    |

`@Validate` parameters:

| Parameter          | Type               | Default              | Meaning                                                                                |
|--------------------|--------------------|----------------------|----------------------------------------------------------------------------------------|
| `oneErrorPerParam` | `Boolean`          | `true`               | Stop after the first violation **on each** parameter                                   |
| `failFast`         | `Boolean`          | `false`              | Stop after the first violation **anywhere** (remaining params/sections may be skipped) |
| `groups`           | `Array<KClass<*>>` | `[OnDefault::class]` | Only constraints whose groups intersect this set run                                   |

`failFast` and `oneErrorPerParam` are independent. Collect-all vs fail-fast is this
annotation — not a host switch.

### Placement is the subject

Where you put the annotation decides what is checked — not a flag on the constraint.
Annotate the property for the collection itself; use a type-use annotation on the element
type for each item:

```kotlin
@field:Size(min = 1)                     // the list itself
val tags: List<@Size(min = 3) String>?   // each element's length

val links: Map<@Size(min = 1) String, @Required AddressDto?>?
// value path: links[homepage] · key path: links.keys[homepage]
```

### Null policy

Framework-wide null contract:

- `null` never reaches `validate(…)`. The engine routes it to `validateNull` (default = **skip**).
- Only presence rules (`@Required`, `@RequiredWhen`) override `validateNull`.
- Empty / blank strings are **non-null** and still go through `validate`.
- Absence is not a format error — pair `@Email`, `@Url`, `@Regex`, … with `@Required` when the
  field must be present.

### Shared constraint parameters

Every built-in (and well-formed custom) constraint has:

| Parameter | Type               | Default              | Role                                                        |
|-----------|--------------------|----------------------|-------------------------------------------------------------|
| `message` | `String`           | `""`                 | Non-blank overrides the error-code default for that failure |
| `groups`  | `Array<KClass<*>>` | `[OnDefault::class]` | Runs only when `@Validate(groups = …)` intersects this set  |

Built-in group tokens: `OnDefault`, `OnCreate`, `OnUpdate`. Custom `KClass` tokens work too.

```kotlin
@field:Required(groups = [OnCreate::class])
val password: String?

@Validate(groups = [OnCreate::class])
@PostMapping
fun create(@RequestBody body: UserDto) { /* … */ }

@Validate(groups = [OnUpdate::class])
@PutMapping("/{id}")
fun update(@RequestBody body: UserDto) { /* … */ }
```

### Sibling references

`@Compare`, `@RequiredWhen`, `@Distinct(by = …)` name **another property** (or, for
`@Compare`, a sibling named in `ref`). Paths are **one segment only**:
`"password"` is valid; `"address.city"` is a KSP error and a red underline in the plugin.

Prefer declared Kotlin names as string literals for `@PropertyRef` / `@Compare`
(`ref = "firstName"`), or generated `Type_` wire constants when you want the JSON / error-path
spelling (`CreateUserRequest_.FIRST_NAME`). Both resolve. The IntelliJ plugin
autocompletes, resolves (Ctrl+Click), recolors resolved names, and red-underlines typos;
KSP verifies both forms at compile time.

For `@Validate` path / query / header parameters, KSP also emits a flat constants object
named after the handler (`UserController_lookup_<fingerprint>_`) with Spring-effective wire
names (`USER_ID`, `Q`, `TENANT`). Body fields stay on the request DTO `Type_` object
(`CreateUserRequest_`). Body-only handlers skip the endpoint constants file.

```kotlin
@field:Compare(ref = "password", operation = Compare.Operation.EQ)
val confirmPassword: String?

// Client-visible paths for lookup errors (generated):
// UserController_lookup_….USER_ID  → "userId"
// UserController_lookup_….Q        → "q"
// UserController_lookup_….TENANT   → "X-Tenant"
// CreateUserRequest_.EMAIL         → wire spelling for the JSON body
```

### Polymorphism

For sealed / abstract / interface roots, declare a **scalar** discriminator and
`Validatable.Subtype` entries. Runtime dispatch uses the bound instance’s **actual class**;
`name` is authoring and tooling metadata, not a dispatch key.

```kotlin
@Validatable(
    discriminator = "kind",
    subtypes = [
        Validatable.Subtype(name = "EMAIL", type = EmailContact::class),
        Validatable.Subtype(name = "PHONE", type = PhoneContact::class),
    ],
)
interface Contact {
    val kind: ContactKind
}
```

KSP / IDE require:

1. Non-blank `discriminator` names a **scalar** property on the annotated type
2. Each `Subtype.type` extends/implements the parent and is itself `@Validatable`
3. `Subtype.name` is a typed literal of the discriminator’s type
4. When both sealed subclasses and `subtypes = […]` are present, the concrete type sets must match

Jackson (or any other serializer) is **not** read for subtype discovery — declare them here.

Same-module nested types must be `@Validatable` or `@NoCascade`. Unmarked cascade is a
build error. Cross-module unmarked cascade warns by default; see
[`validata.strictCrossModuleCascade`](#ksp-arguments).

### Jackson names

`@JsonProperty` / `@JsonIgnore` set **external** names (JSON / error paths).
`@PropertyRef` strings always use the **declared** Kotlin name.

---

## Built-in constraints

Package: `io.ghaylan.validata.constraint.annotation`. **38** annotations.

Each section below documents parameters, defaults, subject types, failure codes, and a short
example. `message` and `groups` are omitted from the parameter tables — they are always
present on every constraint ([Shared constraint parameters](#shared-constraint-parameters)).

Nested enums stay nested: `Required.Mode`, `Compare.Operation`, `RelativeToNow.Relation`,
`Coordinate.Axis`, `NumberSign.Sign`, `NumberParity.Value`, `Contains.Mode`, `Url.Type`,
`FinancialCode.Type`, `Checksum.Algorithm`, `IpAddress.Type`, `Barcode.Type`,
`ConstraintComposition.Mode`. `RequiredWhen.Condition` is nested under `@RequiredWhen`
(same package as the annotation).

Unsupported subject types fail at **compile time** and in the IDE. There is no catch-all
`Any` validator.

| Group                                       | Constraints                                                                                                                                                                                                            |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Presence](#presence)                       | `@Required`, `@RequiredWhen`                                                                                                                                                                                           |
| [Bounds](#bounds)                           | `@Min`, `@Max`, `@Range`, `@MultipleOf`, `@Size`, `@Coordinate`, `@NumberSign`, `@NumberParity`, `@Digits`                                                                                                             |
| [Comparison](#comparison)                   | `@Compare`, `@In`, `@NotIn`                                                                                                                                                                                            |
| [Collections](#collections)                 | `@Distinct`, `@Contains`                                                                                                                                                                                               |
| [Booleans](#booleans)                       | `@Assert`                                                                                                                                                                                                              |
| [Strings and formats](#strings-and-formats) | `@Email`, `@Url`, `@Regex`, `@Password`, `@Phone`, `@Html`, `@HexColor`, `@FinancialCode`, `@Checksum`, `@CreditCard`, `@IsoCountry`, `@IsoCurrency`, `@IsoLanguage`, `@Barcode`, `@IpAddress`, `@Base64`, `@FilePath` |
| [Temporal](#temporal)                       | `@RelativeToNow`, `@DaysOfWeek`, `@DaysOfMonth`, `@Months`                                                                                                                                                             |

---

### Presence

#### `@Required`

Unconditional presence. Default `mode` is deep emptiness (`STRICT`).

| Parameter | Type            | Default  | Meaning         |
|-----------|-----------------|----------|-----------------|
| `mode`    | `Required.Mode` | `STRICT` | See table below |

`Required.Mode`:

| Value    | Treated as missing when                                                                                   |
|----------|-----------------------------------------------------------------------------------------------------------|
| `NULL`   | Reference is `null` only (blank strings / empty containers **pass**)                                      |
| `EMPTY`  | `null` or structurally empty (`length == 0` for text; empty containers). Whitespace-only strings **pass** |
| `STRICT` | `null` or every nested collection/map/array/string is empty                                               |

Repeatable. Failures: `VALUE_MISSING`, `TEXT_BLANK`, `VALUE_EMPTY`.

```kotlin
@field:Required
val email: String?
```

#### `@RequiredWhen`

Presence gated on a **sibling**. Repeatable: several gates on one field combine with **OR**
(required if **any** gate matches).

| Parameter   | Type                      | Default      | Meaning                                                                                                                            |
|-------------|---------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------|
| `ref`       | `String` (`@PropertyRef`) | *(required)* | Sibling name (single segment, not blank)                                                                                           |
| `condition` | `RequiredWhen.Condition`  | *(required)* | How the sibling is interpreted as a gate                                                                                           |
| `value`     | `String`                  | `""`         | Literal for `EQ` / `NE` / `GT` / `LT` / `GTE` / `LTE`. Ignored otherwise. Typed against the **gate** type, not the annotated field |
| `values`    | `Array<String>`           | `[]`         | Literals for `IN` / `NIN`. Ignored otherwise                                                                                       |
| `mode`      | `Required.Mode`           | `STRICT`     | Presence strictness for `MISSING` / `PRESENT`, and for the annotated value when enforcing                                          |

`RequiredWhen.Condition` (nested on `@RequiredWhen`):

| Value                       | Gate matches when                                                                                                          |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `MISSING`                   | Sibling is missing per `mode`                                                                                              |
| `PRESENT`                   | Sibling is present per `mode`                                                                                              |
| `EQ`                        | Sibling’s `toString()` equals `value`. `null` never equals                                                                 |
| `NE`                        | Sibling is non-null and `toString()` differs from `value`                                                                  |
| `IN`                        | Sibling’s `toString()` is in `values`. `null` never matches                                                                |
| `NIN`                       | Sibling is non-null and `toString()` is not in `values`                                                                    |
| `GT` / `GTE` / `LT` / `LTE` | Sibling compares greater/less than `value` (numeric when both parse as decimals, else lexicographic). `null` never matches |

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

Lower bound: `value >= bound`, or `>` when `inclusive = false`. Subject type selects the
validator and the literal syntax for `value`.

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

`null` is skipped. Failures: `NUMBER_TOO_SMALL`, `TEMPORAL_TOO_EARLY`, `TEMPORAL_DURATION_TOO_SHORT`,
`VALUE_PARSING_FAILED`.

```kotlin
@field:Min("18")
val age: Int?

@field:Min("PT30M", inclusive = false)
val sessionTimeout: Duration?
```

#### `@Max`

Upper bound: `value <= bound`, or `<` when `inclusive = false`. Same subject / literal table
as `@Min`.

| Parameter   | Type      | Default      | Meaning                             |
|-------------|-----------|--------------|-------------------------------------|
| `value`     | `String`  | *(required)* | Bound literal                       |
| `inclusive` | `Boolean` | `true`       | `true` → `<=`; `false` → strict `<` |

`null` is skipped. Failures: `NUMBER_TOO_LARGE`, `TEMPORAL_TOO_LATE`, `TEMPORAL_DURATION_TOO_LONG`,
`VALUE_PARSING_FAILED`.

```kotlin
@field:Max("120")
val age: Int?

@field:Max("P2Y")
val subscriptionLength: Period?
```

#### `@Range`

Inclusive interval: `from <= value <= to` by default. Use `fromInclusive` / `toInclusive` for
strict `>` / `<` on either end. When `negated = true`, the value must lie **outside** the
interval. Same subject / literal table as `@Min` / `@Max`. Lower bound is checked first;
failures reuse Min/Max error codes (or `NUMBER_OUT_OF_RANGE` / `TEMPORAL_OUT_OF_RANGE` when negated
and inside).

| Parameter       | Type      | Default      | Meaning                                         |
|-----------------|-----------|--------------|-------------------------------------------------|
| `from`          | `String`  | *(required)* | Lower bound literal                             |
| `to`            | `String`  | *(required)* | Upper bound literal                             |
| `fromInclusive` | `Boolean` | `true`       | `true` → `>=`; `false` → strict `>`             |
| `toInclusive`   | `Boolean` | `true`       | `true` → `<=`; `false` → strict `<`             |
| `negated`       | `Boolean` | `false`      | When `true`, value must be outside `[from, to]` |

`null` is skipped. Failures: `NUMBER_TOO_SMALL` / `NUMBER_TOO_LARGE`, `TEMPORAL_TOO_EARLY` /
`TEMPORAL_TOO_LATE`, duration short/long codes, `NUMBER_OUT_OF_RANGE` / `TEMPORAL_OUT_OF_RANGE`
(negated + inside), `VALUE_PARSING_FAILED`.

```kotlin
@field:Range(from = "18", to = "120")
val age: Int?

@field:Range(from = "2020-01-01", to = "2030-12-31")
val effectiveDate: LocalDate?

@field:Range(from = "PT30M", to = "PT8H", fromInclusive = false)
val sessionTimeout: Duration?

@field:Range(from = "0", to = "17", negated = true)
val adultAge: Int?
```

#### `@MultipleOf`

`Number` is an exact multiple of `factor`, computed in `BigDecimal` (not floating point).
Repeatable. Factor `"0"` or negative **disables** the check (every value passes).

| Parameter | Type     | Default      | Meaning                                                           |
|-----------|----------|--------------|-------------------------------------------------------------------|
| `factor`  | `String` | *(required)* | Decimal divisor (`"5"`, `"0.25"`, `"1_000"`). Underscores allowed |

`null` is skipped. Failures: `NUMBER_NOT_MULTIPLE`, `VALUE_PARSING_FAILED`.

```kotlin
@field:MultipleOf("0.05")
val price: BigDecimal?
```

#### `@Size`

Inclusive length / element-count range. Applies only to the **direct** annotated type:
`CharSequence` length, `Collection` / `Map` size, JVM arrays. A `LocalDate` (etc.) is rejected
at compile time.

| Parameter | Type  | Default         | Meaning                    |
|-----------|-------|-----------------|----------------------------|
| `min`     | `Int` | `0`             | Inclusive minimum (`>= 0`) |
| `max`     | `Int` | `Int.MAX_VALUE` | Inclusive maximum (`>= 0`) |

`null` is skipped. Failures: `TEXT_TOO_SHORT`, `TEXT_TOO_LONG`, `COLLECTION_TOO_SMALL`,
`COLLECTION_TOO_LARGE`, `OBJECT_TOO_SMALL`, `OBJECT_TOO_LARGE`.

```kotlin
@field:Size(min = 8, max = 64)
val password: String?

@field:Size(min = 1, max = 10)
val tags: List<String>?
```

#### `@Coordinate`

Geographic coordinate on `Double`. Select the axis — latitude `-90.0..90.0` or longitude
`-180.0..180.0` (both inclusive). `null` is skipped.

| Parameter | Type              | Default      | Meaning                   |
|-----------|-------------------|--------------|---------------------------|
| `axis`    | `Coordinate.Axis` | *(required)* | `LATITUDE` or `LONGITUDE` |

Failures: `NUMBER_OUT_OF_RANGE`. Non-finite values (`NaN`, ±Infinity) fail the same range check.

```kotlin
@field:Coordinate(axis = Coordinate.Axis.LATITUDE)
val lat: Double?

@field:Coordinate(axis = Coordinate.Axis.LONGITUDE)
val lng: Double?
```

#### `@NumberSign`

Sign checks on `Number` via exact `BigDecimal` arithmetic (`NumberSign.Sign.POSITIVE` or
`NEGATIVE`). `allowZero = true` accepts zero (Bean Validation PositiveOrZero /
NegativeOrZero).

| Parameter   | Type              | Default      | Meaning                       |
|-------------|-------------------|--------------|-------------------------------|
| `sign`      | `NumberSign.Sign` | *(required)* | Required sign                 |
| `allowZero` | `Boolean`         | `false`      | When `true`, zero is accepted |

`null` is skipped. Failures: `NUMBER_NOT_POSITIVE` / `NUMBER_NOT_NEGATIVE`,
`NUMBER_ZERO_NOT_ALLOWED`, `VALUE_PARSING_FAILED`.

```kotlin
@field:NumberSign(sign = NumberSign.Sign.POSITIVE)
val quantity: Int?

@field:NumberSign(sign = NumberSign.Sign.NEGATIVE, allowZero = true)
val adjustment: BigDecimal?
```

#### `@NumberParity`

Integral even/odd on `Number`. Fractional values fail with `NUMBER_NOT_INTEGER`.

| Parameter | Type                 | Default      | Meaning         |
|-----------|----------------------|--------------|-----------------|
| `value`   | `NumberParity.Value` | *(required)* | `EVEN` or `ODD` |

`null` is skipped. Failures: `NUMBER_NOT_EVEN` / `NUMBER_NOT_ODD`, `NUMBER_NOT_INTEGER`,
`VALUE_PARSING_FAILED`.

```kotlin
@field:NumberParity(value = NumberParity.Value.EVEN)
val pairCount: Int?
```

#### `@Digits`

Caps integer and fractional digit counts (Bean Validation semantics on `BigDecimal`
precision/scale).

| Parameter  | Type  | Default           | Meaning                               |
|------------|-------|-------------------|---------------------------------------|
| `integer`  | `Int` | *(required, ≥ 0)* | Max digits left of the decimal point  |
| `fraction` | `Int` | *(required, ≥ 0)* | Max digits right of the decimal point |

`null` is skipped. Failures: `NUMBER_PRECISION_EXCEEDED`, `NUMBER_SCALE_EXCEEDED`,
`VALUE_PARSING_FAILED`.

```kotlin
@field:Digits(integer = 5, fraction = 2)
val amount: BigDecimal?
```

---

### Comparison

#### `@Compare`

Cross-field ordering or equality against a sibling `ref`. `ref` is `@PropertyRef`
(`SIBLING`, `COMPARABLE_FAMILY`), single-segment, not blank. Repeatable.

| Parameter   | Type                | Default      | Meaning                              |
|-------------|---------------------|--------------|--------------------------------------|
| `ref`       | `String`            | *(required)* | Sibling property name                |
| `operation` | `Compare.Operation` | *(required)* | `GT`, `GTE`, `LT`, `LTE`, `EQ`, `NE` |

Both sides must share a comparable family (numeric↔numeric, temporal↔temporal, or the same
scalar kind for `String` / `Char` / enum / `UUID`). Ordering on `String` uses lexicographic
`Comparable` order.

`null` on the annotated value is skipped. For `EQ`, a `null` sibling is not a match. For
`NE`, a `null` sibling passes. For ordering operations, a `null` sibling → skip.

Failures depend on `operation` (for example `COMPARISON_NOT_EQUAL`, `COMPARISON_EQUAL`,
`COMPARISON_NOT_ORDERABLE`, `COMPARISON_GREATER`, `COMPARISON_LESS`, …).

```kotlin
@field:Compare(ref = "password", operation = Compare.Operation.EQ)
val confirmPassword: String?

@field:Compare(ref = "minAge", operation = Compare.Operation.GT)
val maxAge: Int?

@field:Compare(ref = "endDate", operation = Compare.Operation.LT)
val startDate: LocalDate?
```

#### `@In`

Closed allow-list. Compares `toString()` of scalar subjects (`CharSequence`, `Number`, enums,
temporals, …). Non-scalars are skipped, not failed. Each `values` entry is compile-time checked
against the subject type. For a deny-list, use [`@NotIn`](#notin).

| Parameter | Type            | Default                 | Meaning              |
|-----------|-----------------|-------------------------|----------------------|
| `values`  | `Array<String>` | *(required, non-empty)* | Allowed string forms |

`null` is skipped. Failure: `VALUE_NOT_ALLOWED`.

```kotlin
@field:In(values = ["OPEN", "PENDING", "CLOSED"])
val status: String?
```

#### `@NotIn`

Closed deny-list. Same scalar / `toString()` / compile-time literal rules as [`@In`](#in).

| Parameter | Type            | Default                 | Meaning                |
|-----------|-----------------|-------------------------|------------------------|
| `values`  | `Array<String>` | *(required, non-empty)* | Forbidden string forms |

`null` is skipped. Failure: `VALUE_NOT_ALLOWED`.

```kotlin
@field:NotIn(values = ["admin", "root", "system"])
val username: String?
```

---

### Collections

#### `@Contains`

Membership over **collection or array** subjects (not plain strings). Each element’s
`toString()` is compared to configured literals. String scalars are rejected at compile time —
use `@Regex` or `@In` / `@NotIn` instead.

| Parameter | Type            | Default                 | Meaning                                                                                |
|-----------|-----------------|-------------------------|----------------------------------------------------------------------------------------|
| `values`  | `Array<String>` | *(required, non-empty)* | Literal needles (typed against element type)                                           |
| `mode`    | `Contains.Mode` | `ANY`                   | `ANY` (at least one match), `ALL` (every element matches), `NONE` (no element matches) |

`null` collection is skipped. Failures: `COLLECTION_ITEM_MISSING` (`ANY` / `ALL`),
`VALUE_NOT_ALLOWED` (`NONE`).

```kotlin
@field:Contains(values = ["DRAFT", "REVIEW"], mode = Contains.Mode.ANY)
val statuses: List<String>?
```

#### `@Distinct`

Elements in a list / set / array must be unique — whole elements, or keyed by fields on each
element. **Placement:** type-use on the **element** only (`List<@Distinct T>`), never on the list
property. Failures are reported on indexed element paths (e.g. `contacts[1]`). Scalar elements
compared by value. Collections with fewer than 2 elements always pass. Each `by` entry is a
**non-blank, single-segment** field on the **element** type
(`@PropertyRef(scope = PropertyRefScope.ELEMENT)`). Repeatable. `null` elements are skipped.

There is **no** `mode` parameter. Strategy is driven only by `by`:

| `by`              | Rule                                        |
|-------------------|---------------------------------------------|
| `[]` (default)    | Whole-element uniqueness                    |
| one or more names | Uniqueness of the **tuple** of those fields |

Failure: `COLLECTION_DUPLICATE` — message describes the **item** (path already has the index):
`Item is duplicated in the collection.` or `… by fields: email.` when `by` is set.

```kotlin
val contacts: List<@Distinct(by = ["email"]) ContactDto>?

val lines: List<@Distinct(by = ["sku", "warehouse"]) LineItem>?

val tags: List<@Distinct String>?
```

---

### Booleans

#### `@Assert`

Require a `Boolean` to equal `value` (`true` or `false`). Useful for TOS checkboxes and
“must remain false” draft flags.

| Parameter | Type      | Default      | Meaning          |
|-----------|-----------|--------------|------------------|
| `value`   | `Boolean` | *(required)* | Required boolean |

`null` is skipped. Failure: `VALUE_NOT_ALLOWED`.

```kotlin
@field:Assert(value = true)
val termsAccepted: Boolean?

@field:Assert(value = false)
val isDraft: Boolean?
```

---

### Strings and formats

#### `@Email`

Syntax-only `local-part@domain.tld`. Does **not** check MX / deliverability. Empty string
**fails** the syntax check — pair with `@Required` for presence when needed. No extra
parameters. `null` is skipped via `validateNull`.

Failure: `VALUE_FORMAT_INVALID`.

```kotlin
@field:Email
val contactEmail: String?
```

#### `@Url`

URL / `java.net.URI` length, syntax, and optional policy filters. Applies to `CharSequence`
and `URI`. Checks stop at the first failure: `maxLength` → URI syntax (`CharSequence` only) →
host (only for `WEBSITE`) → protocols → ports → query params → extensions. **Blank**
`CharSequence` is skipped inside `validate` (unlike most format validators). `null` uses
default `validateNull`. Presence remains `@Required`.

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
val avatarUrl: String?

@field:Url(type = Url.Type.WEBSITE, allowedProtocols = ["https"])
val callback: URI?
```

#### `@Regex`

Entire `CharSequence` must match `pattern` (full match, not a substring). Inputs longer than
`RegexValidator.MAX_INPUT_LENGTH` (**10 000**) fail with `TEXT_PATTERN_MISMATCH` **before**
`Pattern.matcher`. `name` is opaque metadata on the failing `RegexConstraint` (exposed via
`ConstraintError.metadata`) so clients can distinguish which named pattern failed without seeing
the regex.

| Parameter | Type     | Default      | Meaning                                                |
|-----------|----------|--------------|--------------------------------------------------------|
| `pattern` | `String` | *(required)* | Java `Pattern` (compile-time / IDE checked)            |
| `name`    | `String` | *(required)* | Human identifier for the format (e.g. `"US_ZIP_CODE"`) |

`null` is skipped. Failure: `TEXT_PATTERN_MISMATCH`.

```kotlin
@field:Regex(pattern = "\\d{5}(-\\d{4})?", name = "US_ZIP_CODE")
val zipCode: String?
```

#### `@Password`

Configurable password policy. Checks run in order and **stop at the first failure** (one error
per value): `minLength` → `maxLength` → uppercase → lowercase → digit → special →
`noSequentialChars` → `noRepetitivePatterns`.
The raw password is **never** copied into `message` or `metadata`.

| Parameter              | Type      | Default                      | Meaning                                                                                                                                |
|------------------------|-----------|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `minLength`            | `Int`     | `6`                          | Inclusive minimum length                                                                                                               |
| `maxLength`            | `Int`     | `64`                         | Inclusive maximum length                                                                                                               |
| `requireUppercase`     | `Boolean` | `false`                      | At least one uppercase letter                                                                                                          |
| `requireLowercase`     | `Boolean` | `false`                      | At least one lowercase letter                                                                                                          |
| `requireDigit`         | `Boolean` | `false`                      | At least one digit                                                                                                                     |
| `requireSpecialChar`   | `Boolean` | `false`                      | At least one character from `allowedSpecialChars`                                                                                      |
| `allowedSpecialChars`  | `String`  | `!@#$%^&*()-_=+[{]};:,<.>/?` | Alphabet counted for `requireSpecialChar`                                                                                              |
| `noSequentialChars`    | `Boolean` | `false`                      | Reject ascending/descending letter or digit runs of length ≥ 3 (case-insensitive for letters), e.g. `"abc"`, `"321"`, `"AbCd"`         |
| `noRepetitivePatterns` | `Boolean` | `false`                      | Reject the same character three or more times in a row (e.g. `"aaaa"`) or consecutive repeating blocks of length ≥ 2 (e.g. `"abcabc"`) |

`null` is skipped. Failures: `TEXT_TOO_SHORT`, `TEXT_TOO_LONG`, `TEXT_PATTERN_MISMATCH` (`ConstraintError.metadata` holds the failing `PasswordConstraint`).

```kotlin
@field:Password(
    minLength = 10,
    requireUppercase = true,
    requireDigit = true,
    requireSpecialChar = true,
    noSequentialChars = true,
    noRepetitivePatterns = true,
)
val newPassword: String?
```

#### `@Phone`

International phone number via [libphonenumber](https://github.com/google/libphonenumber).
**Requires** `com.googlecode.libphonenumber:libphonenumber` on the runtime classpath (not
transitive from `validata-core`). Value must be **digits only** — no `+`, spaces, dashes, or
parentheses — and must include the full international dial prefix (e.g. `"14155552671"`).
Formatted input fails immediately.

| Parameter          | Type                     | Default | Meaning                                                                                                                                                                                                                                                                          |
|--------------------|--------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `allowedTypes`     | `Array<PhoneNumberType>` | `[]`    | libphonenumber [PhoneNumberType](https://javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/latest/com/google/i18n/phonenumbers/PhoneNumberUtil.PhoneNumberType.html) values (`MOBILE`, `FIXED_LINE`, `FIXED_LINE_OR_MOBILE`, `TOLL_FREE`, `VOIP`, …). Empty = any type |
| `allowedCountries` | `Array<String>`          | `[]`    | ISO 3166-1 alpha-2 codes derived from the dial prefix. Empty = any country                                                                                                                                                                                                       |

`null` is skipped. Failures: `VALUE_FORMAT_INVALID`, `VALUE_NOT_ALLOWED`.

```kotlin
@field:Phone(
    allowedTypes = [PhoneNumberType.MOBILE],
    allowedCountries = ["US", "CA"],
)
val contactNumber: String?
```

#### `@Html`

Fail (do not silently strip) when markup is outside an allow-list. **Requires**
`org.jsoup:jsoup` on the runtime classpath (not transitive). Blank is skipped inside
`validate`; `null` uses default `validateNull`. Checks stop at the first problem: unknown
tag → disallowed attribute → disallowed protocol → sanitization would alter content.

Each `allowed*` array defaults to `["*"]` (no restriction for that dimension). Use an
explicit list to tighten the policy.

| Parameter          | Type            | Default | Meaning                                                                                                                              |
|--------------------|-----------------|---------|--------------------------------------------------------------------------------------------------------------------------------------|
| `allowedTags`      | `Array<String>` | `["*"]` | Permitted element names; `"*"` allows any tag                                                                                        |
| `allowedAttrs`     | `Array<String>` | `["*"]` | Permitted `"tag:attribute"` pairs (e.g. `"a:href"`); `"*"` allows any. Entries without `:` are ignored when not `"*"`                |
| `allowedProtocols` | `Array<String>` | `["*"]` | Permitted `"tag:attribute:scheme1,scheme2"` triples (e.g. `"a:href:https"`); `"*"` allows any scheme. Incomplete entries are ignored |

Failures: `VALUE_NOT_ALLOWED`, `VALUE_SANITIZATION_MISMATCH`.

```kotlin
@field:Html(allowedTags = ["p", "a", "strong"], allowedAttrs = ["a:href"])
val bio: String?
```

#### `@HexColor`

CSS hex color: `#` + exactly 3 or 6 hex digits (`"#0af"`, `"#00AAFF"`). Letters may be mixed
case. 4- and 8-digit alpha forms (`#RGBA`, `#RRGGBBAA`) are **not** accepted. No extra
parameters. `null` is skipped.

Failure: `VALUE_FORMAT_INVALID`.

#### `@FinancialCode`

Financial identifier of the given `type`. Whitespace is stripped and the value is uppercased
before checks. When `countries` is non-empty, the embedded ISO country code must be one of
those values (compared uppercase). Empty `countries` accepts any country (IBAN still requires
a registry entry). `null` is skipped.

| Parameter   | Type                 | Default      | Meaning                                                 |
|-------------|----------------------|--------------|---------------------------------------------------------|
| `type`      | `FinancialCode.Type` | *(required)* | Identifier family                                       |
| `countries` | `Array<String>`      | `[]`         | Optional ISO 3166-1 alpha-2 filter. Empty = any country |

`FinancialCode.Type`:

| Value  | Meaning                                                                 |
|--------|-------------------------------------------------------------------------|
| `IBAN` | ISO 13616 IBAN: known country, fixed country length, MOD-97-10 checksum |
| `ISIN` | ISO 6166 ISIN: 12 characters with Luhn check digit                      |
| `BIC`  | BIC/SWIFT: 8 or 11 characters                                           |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_NOT_ALLOWED`, `VALUE_CHECKSUM_INVALID`.

```kotlin
@field:FinancialCode(type = FinancialCode.Type.IBAN, countries = ["DE", "FR"])
val accountIban: String?
```

#### `@Checksum`

Check-digit / checksum over a `CharSequence` slice `[startIndex, endIndex)` (`endIndex = -1`
means through the end). The check digit sits at `checkDigitIndex` relative to that slice
(`-1` = last character). When `ignoreNonDigits` is `true`, non-digit characters are stripped
before most algorithms; for `MOD97_10`, letters are kept and mapped `A`–`Z` → 10–35
(ISO 7064), and only other non-alphanumeric characters are skipped. `null` is skipped.

| Parameter         | Type                 | Default      | Meaning                                                    |
|-------------------|----------------------|--------------|------------------------------------------------------------|
| `algorithm`       | `Checksum.Algorithm` | *(required)* | Check-digit algorithm                                      |
| `checkDigitIndex` | `Int`                | `-1`         | Index of the check digit within the slice; `-1` = last     |
| `startIndex`      | `Int`                | `0`          | Inclusive start of the validated slice                     |
| `endIndex`        | `Int`                | `-1`         | Exclusive end of the slice; `-1` = end of the value        |
| `ignoreNonDigits` | `Boolean`            | `false`      | Strip non-digits (or non-alphanumeric for MOD-97-10) first |

`Checksum.Algorithm`:

| Value      | Meaning                                                        |
|------------|----------------------------------------------------------------|
| `LUHN`     | Classic Luhn (Mod-10); check digit included, sum ≡ 0 (mod 10)  |
| `MOD10`    | Alias of `LUHN` (payment-card style)                           |
| `MOD11`    | Weighted Mod-11 (ISBN-10 style); check digit may be `X` for 10 |
| `VERHOEFF` | Verhoeff dihedral-group check digit                            |
| `DAMM`     | Damm quasigroup check digit                                    |
| `MOD97_10` | ISO 7064 MOD 97-10 over alphanumeric characters                |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_CHECKSUM_INVALID`.

```kotlin
@field:Checksum(algorithm = Checksum.Algorithm.LUHN, ignoreNonDigits = true)
val accountNumber: String?
```

#### `@CreditCard`

Payment card PAN: digits, spaces, and hyphens allowed as separators; 13–19 digits after
stripping; Luhn (Mod-10) checksum. Does **not** check expiry, network, or that the card is
active. No extra parameters. `null` is skipped.

Failures: `VALUE_FORMAT_INVALID`, `VALUE_CHECKSUM_INVALID`.

#### `@IsoCountry`

ISO 3166-1 **alpha-2**, exact **uppercase** (`"US"` passes, `"us"` fails). No extra
parameters. `null` is skipped.

Failure: `VALUE_NOT_ALLOWED`.

#### `@IsoCurrency`

ISO 4217 three-letter code, exact **uppercase** (`"USD"` passes, `"usd"` fails). No extra
parameters. `null` is skipped.

Failure: `VALUE_NOT_ALLOWED`.

#### `@IsoLanguage`

Language tag `xx` or `xx-YY` (`"en"`, `"en-US"`). Exact and case-sensitive: language
lowercase, region uppercase. Extended BCP 47 (scripts, variants) is rejected. No extra
parameters. `null` is skipped.

Failure: `VALUE_FORMAT_INVALID`.

#### `@Barcode`

Validates a barcode / publishing identifier. Spaces and hyphens are ignored as separators.

| Parameter | Type           | Default      | Meaning                                 |
|-----------|----------------|--------------|-----------------------------------------|
| `type`    | `Barcode.Type` | *(required)* | `EAN`, `UPC`, `GTIN`, `ISBN`, or `ISSN` |

`null` is skipped. Failures: `VALUE_FORMAT_INVALID`, `VALUE_CHECKSUM_INVALID`.

```kotlin
@field:Barcode(type = Barcode.Type.ISBN)
val isbn: String?
```

#### `@IpAddress`

DNS-free IPv4 / IPv6 literal check.

| Parameter | Type             | Default | Meaning              |
|-----------|------------------|---------|----------------------|
| `type`    | `IpAddress.Type` | `ANY`   | `V4`, `V6`, or `ANY` |

`null` is skipped. Failure: `VALUE_FORMAT_INVALID`.

```kotlin
@field:IpAddress(type = IpAddress.Type.V4)
val gateway: String?
```

#### `@Base64`

| Parameter        | Type      | Default | Meaning                   |
|------------------|-----------|---------|---------------------------|
| `urlSafe`        | `Boolean` | `false` | Use the URL-safe alphabet |
| `requirePadding` | `Boolean` | `true`  | Require `=` padding       |

`null` is skipped. Failure: `VALUE_FORMAT_INVALID`.

```kotlin
@field:Base64
val payload: String?
```

#### `@FilePath`

Safe **relative** file path. Always rejects null bytes, blank values, `..` segments,
Windows-forbidden characters (`< > : " | ? *`) in each segment, and absolute paths
(Unix `/…`, UNC `\\…`, Windows drive prefixes `C:…`). Path separators `/` and `\` are
allowed for relative multi-segment paths. By default the last segment must also carry a
file extension; set `requireExtension = false` for directories / extensionless names.
`null` is skipped.

| Parameter          | Type      | Default | Meaning                                                |
|--------------------|-----------|---------|--------------------------------------------------------|
| `requireExtension` | `Boolean` | `true`  | Require a non-empty extension on the last path segment |

Failures: `VALUE_FORMAT_INVALID`, `VALUE_NOT_ALLOWED`.

```kotlin
@field:FilePath
val relativePath: String?

@field:FilePath(requireExtension = false)
val targetDirectory: String?
```

---

### Temporal

#### `@RelativeToNow`

`Temporal` (`LocalDate`, `Instant`, `ZonedDateTime`, …) relative to “now” in the same temporal
type. Optional `within` window caps how far past/future the value may lie
(`Int.MAX_VALUE` = unbounded on that side).

| Parameter  | Type                     | Default         | Meaning                                                                                                             |
|------------|--------------------------|-----------------|---------------------------------------------------------------------------------------------------------------------|
| `relation` | `RelativeToNow.Relation` | *(required)*    | `LT`, `LTE`, `GT`, `GTE`, or `EQ`                                                                                   |
| `within`   | `Int`                    | `Int.MAX_VALUE` | Max offset from now in `unit` steps. Finite windows use `1 until Int.MAX_VALUE`; `Int.MAX_VALUE` disables the bound |
| `unit`     | `ChronoUnit`             | `DAYS`          | Unit for `within`; must be supported by the annotated temporal type                                                 |

`null` is skipped. Failures include `TEMPORAL_NOT_IN_PAST`, `TEMPORAL_NOT_IN_FUTURE`,
`COMPARISON_UNSATISFIED_NOT_EQUAL` (`EQ`), `VALUE_UNSUPPORTED` (incompatible window unit),
`TEMPORAL_TOO_EARLY`, `TEMPORAL_TOO_LATE` (subset depends on `relation`).

```kotlin
@field:RelativeToNow(relation = RelativeToNow.Relation.LT, within = 30, unit = ChronoUnit.DAYS)
val eventOccurredAt: LocalDate?

@field:RelativeToNow(relation = RelativeToNow.Relation.GTE, within = 90, unit = ChronoUnit.DAYS)
val expiresAt: LocalDate?
```

#### `@DaysOfWeek`

Calendar day-of-week of a date-aware temporal must be one of `days` (or must **not** be when
`negated = true`). Time-only temporals (`LocalTime`) **skip** rather than fail.

| Parameter | Type               | Default                 | Meaning                                |
|-----------|--------------------|-------------------------|----------------------------------------|
| `days`    | `Array<DayOfWeek>` | *(required, non-empty)* | Allow-list (or deny-list when negated) |
| `negated` | `Boolean`          | `false`                 | When `true`, day must not be in `days` |

`null` is skipped. Failure: `TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED`.

```kotlin
@field:DaysOfWeek([DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY])
val deliveryDate: LocalDate?
```

#### `@DaysOfMonth`

Calendar day-of-month (1–31) of a date-aware temporal must be one of `days` (or must **not** be
when `negated = true`). Time-only temporals **skip**.

| Parameter | Type       | Default                 | Meaning                                                                  |
|-----------|------------|-------------------------|--------------------------------------------------------------------------|
| `days`    | `IntArray` | *(required, non-empty)* | Allow-list (or deny-list when negated; stored as `Set<Int>` in metadata) |
| `negated` | `Boolean`  | `false`                 | When `true`, day must not be in `days`                                   |

`null` is skipped. Failure: `TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED`.

```kotlin
@field:DaysOfMonth([1, 15])
val billingDate: LocalDate?
```

#### `@Months`

Calendar month of a date-aware temporal must be one of `months` (or must **not** be when
`negated = true`). Time-only temporals **skip**.

| Parameter | Type           | Default                 | Meaning                                    |
|-----------|----------------|-------------------------|--------------------------------------------|
| `months`  | `Array<Month>` | *(required, non-empty)* | Allow-list (or deny-list when negated)     |
| `negated` | `Boolean`      | `false`                 | When `true`, month must not be in `months` |

`null` is skipped. Failure: `TEMPORAL_MONTH_NOT_ALLOWED`.

```kotlin
@field:Months([Month.JUNE, Month.JULY, Month.AUGUST])
val seasonStart: LocalDate?
```

---

## Custom constraints

A constraint is always **annotation + validator**. Metadata (`{Ann}Constraint`) is
**generated by KSP** from role markers — do not hand-write it.

```text
You write                  KSP emits                 Engine runs
─────────                  ─────────                 ──────────
@OddYears                  OddYearsConstraint        OddYearsValidator.runValidation(…)
@Constraint(validatedBy)   ConstraintCatalog SPI         ├─ null  → validateNull (default skip)
                                                         └─ value → validate(value: Value, …)
```

Apply `ksp(validata-processor)` on the **module that declares the annotation**. If the catalog
is missing, the annotation compiles but never runs. The module that *uses* the annotation is
not enough when the annotation lives in another jar.

Working originals: `OddYears` and `SampleFloor` in [`validata-samples`](validata-samples).

### 1. Annotation

Every `@Constraint` annotation must declare:

- `@Constraint(validatedBy = [YourValidator::class])` — **required**, non-empty
- exactly one `@ConstraintMessage` parameter (`String`)
- exactly one `@ConstraintGroups` parameter (`Array<KClass<*>>`)
- `@Retention(RUNTIME)` and targets that include at least `FIELD` / `VALUE_PARAMETER` / `TYPE`
  (type-use is what enables `List<@YourAnn String>`)

Keep the **annotation parameter name** identical to the generated metadata property name.

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

`validatedBy` may list several validators covering different runtime types. Empty
`validatedBy` fails KSP and the IDE.

### Constraint composition (AND / OR)

Stack leaf `@Constraint` meta-annotations on a **non-`@Constraint`** outer type to compose
them. Do **not** put `@Constraint` on the outer annotation.

| Mode              | Marker                                   | Runtime                                                                |
|-------------------|------------------------------------------|------------------------------------------------------------------------|
| **AND** (default) | absent, or `@ConstraintComposition(AND)` | Flatten leaves into the property’s constraint list (conjunctive)       |
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

@field:Required
@field:EmailOrPhone
val contact: String?
```

**OR rules (v1):** at least two leaf `@Constraint`s; no nested composed members; `@Required` /
`@RequiredWhen` cannot be OR members (put presence on the usage site). Failure uses
`CONSTRAINT_UNSATISFIABLE` (optional outer `message`). If **no** leaf is active under the
current groups, the composition is **skipped**. Runtime uses hand-written
`CompositionConstraint` metadata — not KSP-generated and **not** in the constraint catalog.
OpenAPI does **not** emit dishonest native facets (e.g. `format: email`); the site appears
under `x-validata-constraints` as `Composition` with `composition=OR` and `members`. Nested
OR / OAS `oneOf` are out of scope.

Working sample: `EmailOrPhone` on `AllConstraintsRequest.contact` in [`validata-samples`](validata-samples).

### 2. Role markers on parameters

KSP and the IntelliJ plugin discover rules from these markers — **not** from an allowlist of
annotation names. Custom constraints light up in the IDE automatically.

#### `@ConstraintMessage` / `@ConstraintGroups`

Role markers for generated metadata. Not argument checks. Exactly one of each.

#### `@ConstraintArg`

Marks a **payload** argument that tooling must check. Repeatable: stack several on one
parameter; each pairs `kinds` with a `target`. Authors write `@ConstraintArg`, never the
synthetic holder `@ConstraintArgs`.

| Parameter | Type                  | Default      | Meaning                                                                |
|-----------|-----------------------|--------------|------------------------------------------------------------------------|
| `kinds`   | `ConstraintArgKind…`  | *(required)* | One or more rules                                                      |
| `target`  | `ConstraintArgTarget` | `VALUE`      | `VALUE` = the argument as a whole; `ELEMENT` = each array/list element |
| `message` | `String`              | `""`         | Optional diagnostic prefix; empty uses the kind’s default              |

`ConstraintArgKind`:

| Kind            | Rule                                                                                                                                     |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `NOT_BLANK`     | Reject blank strings (use `target = ELEMENT` for array elements)                                                                         |
| `NON_EMPTY`     | Reject empty collections / blank scalars                                                                                                 |
| `TYPED_LITERAL` | String must parse as the annotated **subject** type (number, temporal, enum **name**, …). No-op beyond `NOT_BLANK` for `String` subjects |
| `NON_NEGATIVE`  | Numeric arg `>= 0`                                                                                                                       |
| `POSITIVE`      | Numeric arg `> 0`                                                                                                                        |
| `REGEX`         | String must compile as `java.util.regex.Pattern`                                                                                         |

`ConstraintArgKind` / `ConstraintArgTarget` live in **`validata-schema`**; `@ConstraintArg`
itself lives in **`validata-core`**.

```kotlin
@Constraint(validatedBy = [SampleFloorValidator::class])
annotation class SampleFloor(
    @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
    val value: String,
    @ConstraintMessage val message: String = "",
    @ConstraintGroups val groups: Array<KClass<*>> = [OnDefault::class],
)
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

#### `@PropertyRef`

Marks a parameter that **names another property** rather than carrying data.

| Parameter       | Type                           | Default   | Meaning                                                    |
|-----------------|--------------------------------|-----------|------------------------------------------------------------|
| `scope`         | `PropertyRefScope`             | `SIBLING` | Where the name resolves                                    |
| `compatibility` | `PropertyRefCompatibilityKind` | `NONE`    | Scalar check between annotated subject and referenced leaf |

| `PropertyRefScope` | Resolve root                             | Example                                   |
|--------------------|------------------------------------------|-------------------------------------------|
| `SIBLING`          | Declaring class / sibling handler params | `@Compare(ref = "password", …)`           |
| `ELEMENT`          | Collection element type                  | `List<@Distinct(by = ["email"]) UserDto>` |

| `PropertyRefCompatibilityKind` | Use when                                                                                                       |
|--------------------------------|----------------------------------------------------------------------------------------------------------------|
| `NONE`                         | Existence only (`@RequiredWhen`, `@Distinct.by`)                                                               |
| `COMPARABLE_FAMILY`            | Cross-field compare (`@Compare`) — both numeric, both temporal, or same orderable kind (STRING/CHAR/ENUM/UUID) |
| `SAME_SCALAR_KIND`             | Same scalar kind on both sides (custom constraints)                                                            |

`PropertyRefScope` and `PropertyRefCompatibilityKind` live in **`validata-schema`**
(`io.ghaylan.validata.schema.ref`). A parameter may carry both `@PropertyRef` and
`@ConstraintArg`. With `NOT_BLANK`, blank is an error; without it, blank means “no reference”.

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

Prefer the inherited **`getPropertyValue(name, context)`** helper for siblings (resolves
declared / `@JsonProperty` names). Raw access: `ValidationContext.containerObject`. For
element-uniqueness, set `requiresArrayContext = true` and read `context.array`.

### 3. Validator

Prefer a Kotlin `object` so the catalog can share one instance. Implement `validate` only —
`runValidation` (groups, null routing, path, message) is the engine’s job.

`Value` in `ConstraintValidator<Value : Any, C>` is the **accepted non-null subject type**.
A nullable type argument is rejected at compile time. Wrong placements fail while typing
(IntelliJ plugin) and at KSP compile. `C` is the **generated** `{Ann}Constraint`.

| Hook                                | When              | What you do                                                                     |
|-------------------------------------|-------------------|---------------------------------------------------------------------------------|
| `validate(value: Value, …)`         | Non-null subject  | Implement the rule. **Do not** declare `Value?` or null-check                   |
| `validateNull(constraint, context)` | Subject is `null` | Default skips. Override **only** for presence-on-null                           |
| `runValidation(value: Any?, …)`     | Engine entry      | Groups → route null/non-null → stamp path/message. Do not call from custom code |

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

Validator contract:

| Rule                                                           | Why                                                                                |
|----------------------------------------------------------------|------------------------------------------------------------------------------------|
| `validate` takes non-null `Value`                              | `Value : Any`; null is routed to `validateNull`                                    |
| Return `null` on success                                       | Engine materializes path only on failure                                           |
| Override `validateNull` only for presence-on-null              | Default already skips null                                                         |
| Override `possibleErrorCodes(constraint)`                      | OpenAPI / docs list 400 codes for that annotation config. Empty set = undocumented |
| Do not retain `ValidationContext`                              | Cursor is valid only for this `validate` call                                      |
| Never put secrets in `message` / `metadata`                    | Password-style fields must not echo the raw value                                  |
| `requiresArrayContext = true` only if you read `context.array` | A false negative silently skips those checks                                       |
| Prefer `getPropertyValue(name, context)` for siblings          | Resolves declared / wire names; throws if unresolved                               |

You may use a built-in `ConstraintErrorCode` or your own enum that implements
`ConstraintErrorDefinition`. `ConstraintError<CodeT>` requires both: an `enum` **and**
`ConstraintErrorDefinition`.

Typed-literal example (`SampleFloor` — inclusive numeric floor, same idea as `@Min`):

```kotlin
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

### 4. Rebuild

Do **not** edit `build/generated/…/*Constraint.kt`. KSP emits:

- `{AnnotationSimpleName}Constraint` in the same package as the annotation
- a `ConstraintCatalog` ServiceLoader entry under
  `META-INF/services/io.ghaylan.validata.constraint.spi.ConstraintCatalog`

### 5. Override a validator

Built-in validators are Kotlin `object`s. In Spring, register a `@Bean` / `@Component` of the
**same validator type** — the host looks beans up by catalog `validatorType` and falls back to
the default factory. Outside Spring, wire your instance into the runner / catalog yourself.

### 6. OpenAPI (optional)

If you publish `validata-openapi`, implement `ConstraintDocumentation` so custom metadata is
not listed under `x-validata-unmapped`. See [OpenAPI](#custom-constraints-in-openapi).

### 7. Hand-written catalog (tests / jars without KSP)

Prefer generated catalogs. Implement `ConstraintCatalog` only in tests or when you cannot run
the processor, and register under
`META-INF/services/io.ghaylan.validata.constraint.spi.ConstraintCatalog`.

### Checklist

1. Write `@Constraint(validatedBy = […])` with `@ConstraintMessage` / `@ConstraintGroups`.
2. Mark payload params with `@ConstraintArg` and/or `@PropertyRef`.
3. Implement `ConstraintValidator<Value : Any, YourAnnConstraint>` (metadata type is generated).
4. Implement `validate(value: Value, …)` — **non-null**; override `validateNull` only for presence.
5. Override `possibleErrorCodes(constraint)`; empty/blank checks belong in `validate` (those values are non-null).
6. Enable `ksp("…:validata-processor")` on **that** module.
7. Rebuild — do not edit generated `*Constraint.kt`.
8. Use the annotation on a `@Validatable` type (or handler param) and assert the error code.

### Do not

- Hand-write `{Ann}Constraint` data classes (except engine-internal composition metadata)
- Declare `validate(value: Value?, …)` or null-check inside `validate`
- Flatten nested enums (`Url.Type`, `RequiredWhen.Condition` stay nested)
- Put passwords / tokens in error `message` or `metadata`
- Scan annotations at request time — schemas are compiled ahead of time
- Forget KSP on the module that *declares* the custom annotation

---

## Errors

When validation fails, the engine (or the Spring host’s binding translation) throws
`ConstraintViolationException` with a list of `ConstraintError`s. Your app maps that to HTTP —
see [Quick start](#2-map-errors-in-your-app).

### `ConstraintError`

One field-attributed failure. Clients should key off `code`, not `message` text.

| Field      | Meaning                                                                                                                                        |
|------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `path`     | Dot/bracket locator (`user.address[0].city`, `X-Tenant`, `userId`)                                                                             |
| `location` | Request section when HTTP-scoped (`EndpointArgumentKind`): `BODY` / `QUERY` / `HEADER` / `PATH`                                                |
| `code`     | Machine-readable enum (`ConstraintErrorCode` or yours)                                                                                         |
| `message`  | Human text. Resolution: annotation `message` → validator message → code default                                                                |
| `metadata` | Failing constraint metadata (typically a generated `*Constraint`), or opaque extras. **Never secrets**. Engine limit errors leave this `null`. |

Validators typically return path-free errors with a precise `message`. The engine stamps
`path` (and often `location`) from the cursor and finalizes `message`.

`metadata` often holds a generated `*Constraint` (`ConstraintMetadata`). That type is **not**
guaranteed Jackson-serializable (for example `groups: Set<KClass<*>>`). Hosts must map errors to
their own wire DTO — omit `metadata`, or project public args to a `Map` — before JSON responses.
Do not rely on library-side `@JsonIgnore`.

### Exceptions

| Type                               | When                                                                                         | What you do                                                                                                   |
|------------------------------------|----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `ConstraintViolationException`     | Engine violation **or** translated Spring / Jackson binding failure on a `@Validate` handler | Map in `@RestControllerAdvice` (typically 400). `errors` is the payload; stack traces are suppressed          |
| `ConfigurationValidationException` | Invalid `@Validate` `@ConfigurationProperties`                                               | Context refresh fails. Boot FailureAnalyzer prints a readable report (`beanName`, `propertyPrefix`, `errors`) |
| `SchemaNotFoundException`          | Annotated type has no generated `ObjectSchema`                                               | Apply `ksp` on the declaring module and rebuild                                                               |
| `IllegalStateException`            | Annotated handler has no `EndpointSchema`, or targeted config bean but engine missing        | Fail closed — processor on that module, or check auto-config is not excluded                                  |

### Exception translation (Spring host)

The host installs `ValidataExceptionResolver` (servlet web) so selected Spring / Jackson
failures on Validata-marked handlers become `ConstraintViolationException` — one
`@ExceptionHandler` covers engine checks and binding failures. Unmarked handlers keep
Spring’s original exception. See [`validata/README.md`](validata/README.md#exception-translation)
for the full mapping table (`VALUE_MISSING`, `VALUE_TYPE_MISMATCH`, `PROPERTY_UNKNOWN`, …).

### Built-in codes

Complete catalog of `ConstraintErrorCode` in `validata-core` — **70** codes. Names and default
messages are part of the [frozen public surface](#stability-and-semver); contract tests lock them.
Prefer `code` over `message` text in clients. Domain detail belongs in `metadata`, not in new code
names. Presence validators emit `VALUE_MISSING` / `TEXT_BLANK` / `VALUE_EMPTY`; host binding
translation also uses `VALUE_MISSING` for omitted required query/path/header params.

#### Presence / nullability

| Code            | Default message          |
|-----------------|--------------------------|
| `VALUE_MISSING` | Value must not be null.  |
| `VALUE_EMPTY`   | Value must not be empty. |

#### Mapping / input resolution

| Code                   | Default message                    |
|------------------------|------------------------------------|
| `VALUE_TYPE_MISMATCH`  | Value has an unexpected data type. |
| `VALUE_PARSING_FAILED` | Value cannot be parsed.            |
| `VALUE_FORMAT_INVALID` | Value has an invalid format.       |
| `VALUE_INVALID`        | Value is invalid.                  |
| `VALUE_UNSUPPORTED`    | Value is not supported.            |
| `VALUE_NOT_ALLOWED`    | Value is not allowed.              |
| `PROPERTY_UNKNOWN`     | Property is not recognized.        |

#### Numbers

| Code                        | Default message                                          |
|-----------------------------|----------------------------------------------------------|
| `NUMBER_TOO_SMALL`          | Number is smaller than the permitted minimum.            |
| `NUMBER_TOO_LARGE`          | Number is larger than the permitted maximum.             |
| `NUMBER_NOT_POSITIVE`       | Number must be greater than zero.                        |
| `NUMBER_NOT_NEGATIVE`       | Number must not be negative.                             |
| `NUMBER_ZERO_NOT_ALLOWED`   | Number must not be zero.                                 |
| `NUMBER_NOT_INTEGER`        | Number must be an integer.                               |
| `NUMBER_NOT_FINITE`         | Number must be finite.                                   |
| `NUMBER_NOT_MULTIPLE`       | Number must be an exact multiple of the required factor. |
| `NUMBER_NOT_EVEN`           | Number must be even.                                     |
| `NUMBER_NOT_ODD`            | Number must be odd.                                      |
| `NUMBER_OUT_OF_RANGE`       | Number is outside the valid range.                       |
| `NUMBER_PRECISION_EXCEEDED` | Number exceeds the permitted precision.                  |
| `NUMBER_SCALE_EXCEEDED`     | Number exceeds the permitted scale.                      |
| `NUMBER_OVERFLOW`           | Number exceeds the representable range.                  |
| `NUMBER_UNDERFLOW`          | Number falls below the representable range.              |

#### Text / strings

| Code                    | Default message                                    |
|-------------------------|----------------------------------------------------|
| `TEXT_BLANK`            | Text must not be blank.                            |
| `TEXT_TOO_SHORT`        | Text is shorter than the permitted minimum length. |
| `TEXT_TOO_LONG`         | Text exceeds the permitted maximum length.         |
| `TEXT_PATTERN_MISMATCH` | Text does not match the required pattern.          |

#### Temporals

| Code                                | Default message                                  |
|-------------------------------------|--------------------------------------------------|
| `TEMPORAL_NOT_IN_PAST`              | Temporal value must be in the past.              |
| `TEMPORAL_NOT_IN_FUTURE`            | Temporal value must be in the future.            |
| `TEMPORAL_TOO_EARLY`                | Temporal value is earlier than permitted.        |
| `TEMPORAL_TOO_LATE`                 | Temporal value is later than permitted.          |
| `TEMPORAL_OUT_OF_RANGE`             | Temporal value is outside the valid range.       |
| `TEMPORAL_DAY_OF_MONTH_NOT_ALLOWED` | Day of the month is not allowed.                 |
| `TEMPORAL_DAY_OF_WEEK_NOT_ALLOWED`  | Day of the week is not allowed.                  |
| `TEMPORAL_MONTH_NOT_ALLOWED`        | Month is not allowed.                            |
| `TEMPORAL_QUARTER_NOT_ALLOWED`      | Calendar quarter is not allowed.                 |
| `TEMPORAL_YEAR_NOT_ALLOWED`         | Year is not allowed.                             |
| `TEMPORAL_HOUR_NOT_ALLOWED`         | Hour is not allowed.                             |
| `TEMPORAL_MINUTE_NOT_ALLOWED`       | Minute is not allowed.                           |
| `TEMPORAL_SECOND_NOT_ALLOWED`       | Second is not allowed.                           |
| `TEMPORAL_TIMEZONE_MISSING`         | Timezone information is missing.                 |
| `TEMPORAL_TIMEZONE_NOT_ALLOWED`     | Timezone is not allowed.                         |
| `TEMPORAL_OFFSET_NOT_ALLOWED`       | UTC offset is not allowed.                       |
| `TEMPORAL_DURATION_TOO_SHORT`       | Temporal duration is shorter than permitted.     |
| `TEMPORAL_DURATION_TOO_LONG`        | Temporal duration exceeds the permitted maximum. |

#### Collection codes

| Code                         | Default message                                                    |
|------------------------------|--------------------------------------------------------------------|
| `COLLECTION_TOO_SMALL`       | Collection contains fewer items than permitted.                    |
| `COLLECTION_TOO_LARGE`       | Collection contains more items than permitted.                     |
| `COLLECTION_DUPLICATE`       | Item is duplicated in the collection.                              |
| `COLLECTION_ITEM_MISSING`    | Required collection item is missing.                               |
| `COLLECTION_SUBSET_MISMATCH` | Collection is not a subset of the permitted values.                |
| `COLLECTION_OVERLAP`         | Collections contain overlapping items where they must be disjoint. |

#### Objects / structure

| Code                       | Default message                                  |
|----------------------------|--------------------------------------------------|
| `OBJECT_TOO_SMALL`         | Object contains fewer properties than permitted. |
| `OBJECT_TOO_LARGE`         | Object contains more properties than permitted.  |
| `STRUCTURE_DEPTH_EXCEEDED` | Structure exceeds the permitted nesting depth.   |

#### Relationships / dependencies

| Code                              | Default message                            |
|-----------------------------------|--------------------------------------------|
| `RELATIONSHIP_REFERENCE_MISMATCH` | Value does not match its referenced value. |
| `RELATIONSHIP_REFERENCE_MISSING`  | Required dependent value is missing.       |
| `RELATIONSHIP_REFERENCE_INVALID`  | Dependent value is invalid.                |

#### Comparison codes

| Code                                           | Default message                                             |
|------------------------------------------------|-------------------------------------------------------------|
| `COMPARISON_UNSATISFIED_EQUAL`                 | Value must not be equal to the reference value.             |
| `COMPARISON_UNSATISFIED_NOT_EQUAL`             | Value must be equal to the reference value.                 |
| `COMPARISON_UNSATISFIED_LESS_THAN`             | Value must be greater than or equal to the reference value. |
| `COMPARISON_UNSATISFIED_LESS_THAN_OR_EQUAL`    | Value must be greater than the reference value.             |
| `COMPARISON_UNSATISFIED_GREATER_THAN`          | Value must be less than or equal to the reference value.    |
| `COMPARISON_UNSATISFIED_GREATER_THAN_OR_EQUAL` | Value must be less than the reference value.                |
| `COMPARISON_NOT_ORDERABLE`                     | Value cannot be compared with the reference value.          |

#### Integrity / transformation

| Code                          | Default message                          |
|-------------------------------|------------------------------------------|
| `VALUE_CHECKSUM_INVALID`      | Value failed checksum verification.      |
| `VALUE_SIGNATURE_INVALID`     | Value failed signature verification.     |
| `VALUE_SANITIZATION_MISMATCH` | Value does not match its sanitized form. |

#### Composition / structural conflicts

| Code                       | Default message                             |
|----------------------------|---------------------------------------------|
| `CONSTRAINT_UNSATISFIABLE` | Validation constraints cannot be satisfied. |

---

## Configuration

Two places to tune behavior:

1. **Runtime** — `validata.limits.*` in `application.yml` (engine ceilings).
2. **Compile time** — KSP `arg(…)` keys on modules that run the processor.

Module READMEs that own a knob also keep the same tables (processor KSP args, host YAML
limits, core `ValidationLimits`).

### Engine / host limits (`application.yml`)

Prefix: `validata.limits`. Bound at startup onto `ValidationLimits`. Non-positive values fail
when the limits bean is created.

| Property                                     | Type  | Default | Effect                                      |
|----------------------------------------------|-------|---------|---------------------------------------------|
| `validata.limits.max-depth`                  | `Int` | `32`    | Max nesting the engine will walk            |
| `validata.limits.max-elements-per-container` | `Int` | `10000` | Max list/map/array size before fail-closed  |
| `validata.limits.max-errors`                 | `Int` | `200`   | Cap on collected errors per validation call |

```yaml
validata:
  limits:
    max-depth: 16
    max-elements-per-container: 5000
    max-errors: 50
```

Exceeding a ceiling records `STRUCTURE_DEPTH_EXCEEDED` (or the matching width/error stop) —
fail closed, not silently truncated nested invalid data. A limit tighter than `@Size(max = …)`
wins. Raise a **specific** limit for bulk endpoints rather than all three.

IDE metadata for these keys ships in `META-INF/spring-configuration-metadata.json`
(validata host). Constructor field names on `ValidationLimits`: `maxDepth`,
`maxElementsPerContainer`, `maxErrors` — see [validata-core Configuration](validata-core/README.md#configuration).

`RegexValidator.MAX_INPUT_LENGTH` (10_000) caps `@Regex` subject length before matching
(not a YAML key).

There is **no** `validata.openapi.*` properties namespace. OpenAPI behavior is classpath + beans.

### KSP arguments

Pass these via Gradle `ksp { arg(…) }` on **every module** that applies
`ksp(validata-processor)`. They are compile-time options, not Spring properties. Full table also in
[validata-processor Configuration](validata-processor/README.md#configuration).

| KSP arg key                         | Type                      | Default    | Effect                                                                                                                                                                                                                                     |
|-------------------------------------|---------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `validata.strictCrossModuleCascade` | `"true"` / `"false"`      | `false`    | When `true`, unmarked cascade into a dependency type (no `containingFile`) is a **KSP error** for nested properties **and** unmarked cross-module `@RequestBody` types. When `false`, warn only — the declaring module must still run KSP. |
| `validata.maxShapeNestingDepth`     | non-negative int string   | `32`       | Caps iterable/map nesting when verifying type-use property refs. Exceeding is a **KSP error** (no `StackOverflowError`). Invalid / negative values fall back to `32` with a **KSP warning**.                                               |
| `validata.jackson.naming`           | `IDENTITY` / `SNAKE_CASE` | `IDENTITY` | Wire / error-path naming when `@JsonProperty` is absent. Align with the app `ObjectMapper` naming strategy. Explicit `@JsonProperty` always wins. Unknown values fall back to `IDENTITY` with a **KSP warning**.                           |

```kotlin
ksp {
    arg("validata.strictCrossModuleCascade", "true")
    arg("validata.maxShapeNestingDepth", "32")
    arg("validata.jackson.naming", "SNAKE_CASE")
}
```

Example: if Jackson uses `PropertyNamingStrategies.SNAKE_CASE`, set
`validata.jackson.naming` to `SNAKE_CASE` so generated wire / error paths match JSON keys
when properties have no `@JsonProperty`.

### Host extension points

All replaceable via `@ConditionalOnMissingBean` (or a Spring bean of the validator type):

| Extension                     | How                                                                                |
|-------------------------------|------------------------------------------------------------------------------------|
| Registry / engine / limits    | Provide your own `@Bean` of the same type                                          |
| Plan cache                    | Replace the web-only cache bean                                                    |
| Config post-processor         | Replace the `BeanPostProcessor` if you need different startup policy               |
| Constraint validators         | A `@Bean` of the concrete `ConstraintValidator` type wins over the catalog factory |
| Disable MVC substitution      | `spring.autoconfigure.exclude` for `…config.WebMvcValidationAutoConfiguration`     |
| Disable exception translation | `spring.autoconfigure.exclude` for `…config.ValidataWebMvcConfiguration`           |

Do not subclass the validating invocable for product code — replace beans or exclude
auto-config instead.

Validation runs on the servlet request thread (CPU walk). There is no interrupt/cooperative
cancel in the host call; tighten `validata.limits` if hostile payloads are a concern.
Plan-cache and related memo maps are process-lifetime and keyed by stable handler methods /
classes (static KSP schemas).

---

## OpenAPI

Optional companion `validata-openapi`. It does **not** run validation. It reads the compiled
IR and overlays it onto the document springdoc would have produced.

Add the dependency (see [Installation](#installation)). With no extra beans, `/v3/api-docs`
already shows:

- **Native JSON Schema facets** when honest: `@Required`, `@Size`, numeric `@Min`/`@Max`/`@Range`,
  `@MultipleOf`, `@Regex`, `@Email`, `@Url`, `@In`, `@Coordinate`,
  `@HexColor`, `@NumberSign`, `@NumberParity`, typed `@IpAddress` (`minLength`, `format`, `enum`, …)
- **Owned but extensions-only** (never `x-validata-unmapped`): temporal bounds, `@DaysOfWeek` /
  `@DaysOfMonth` / `@Months`, `@RelativeToNow`, `@Compare`, composition OR,
  `@Phone` / `@Password` / `@Html`, and other customs without an honest native facet
- `x-validata-constraints` — **array** of every Validata constraint (`{ "_constraint": "Size", … }`),
  args without `message` / `groups`; empty args omitted; `@PropertyRef` args use the **wire** name
- `x-validata-errors` — `{ code, message }`: always includes `VALUE_TYPE_MISMATCH` at the property
  root; plus validators’ `possibleErrorCodes` (and `CONSTRAINT_UNSATISFIABLE` + leaf codes for OR);
  plus `@ApiError` (wins on code collision); plus `VALUE_MISSING` when an active `@Required` is present
- `x-validata-unmapped` — sorted distinct **standard** metadata class names with no owning documenter

Overlapping facets on the same param merge **strictest-wins** for bounds; `format`/`pattern` /
`multipleOf` keep the first value; `enum` is intersected. Existing `description` is never
overwritten. Parameter docs respect the endpoint’s active groups; shared DTO component schemas
are the union of all constraints.

OpenAPI generation **does not throw** on a bad catalog. KSP and the IntelliJ plugin own those
hard failures. Docs time warns once and continues. Concurrent `/v3/api-docs` generation is
**not** supported — serialize OpenAPI builds.

`@ApiError` is published from body / path / header / query properties, including **nested** DTO
fields (wire paths with `.` / `[]` / `*`).

### Documenting HTTP errors (opt-in)

Validata does **not** own your error envelope. `@ApiError` is **docs-only**. Runtime
still throws your exceptions (or `ConstraintViolationException`). Register a publisher if you
want documented error responses; with **none**, operations get constraint enrichment only.

#### `@ApiError` (field / parameter)

Docs-only codes **not** emitted by validators (manual / business checks). `catalog` must be an
`enum class` implementing `ConstraintErrorDefinition`.

| Parameter | Type                                    | Default                 | Meaning                                                  |
|-----------|-----------------------------------------|-------------------------|----------------------------------------------------------|
| `code`    | `String`                                | *(required, not blank)* | Enum constant name (selector)                            |
| `message` | `String`                                | `""`                    | Optional OpenAPI override; blank → catalog entry message |
| `catalog` | `KClass<out ConstraintErrorDefinition>` | *(required)*            | Enum catalog; KSP + IDE enforce membership               |

Repeatable via Kotlin `@Repeatable` — stack several `@ApiError`; there is no container annotation.

```kotlin
enum class UserErrors(override val message: String) : ConstraintErrorDefinition {
    EMAIL_TAKEN("Email already registered");
    override val code: String get() = name
}

@field:ApiError(code = "EMAIL_TAKEN", catalog = UserErrors::class)
@field:Required
val email: String?

@Bean
fun errorDocs(): ErrorDocPublisher = MyProblemDetailsPublisher()
```

Register your own publisher(s). Unknown `attributes` on the publish context must be ignored
(forward compatible).

### Custom constraints in OpenAPI

```text
META-INF/services/io.ghaylan.validata.openapi.docs.ConstraintDocumentation
com.example.OddYearsConstraintDocumentation
```

```kotlin
class OddYearsConstraintDocumentation : ConstraintDocumentation {
    override fun supports(metadata: ConstraintMetadata): Boolean =
        metadata is OddYearsConstraint

    override fun hints(metadata: ConstraintMetadata, shape: TypeShape?): ConstraintDocHints =
        ConstraintDocHints.EMPTY
}
```

Returning `EMPTY` still publishes an `OddYears` entry under `x-validata-constraints` and avoids
`x-validata-unmapped`.
Also override `possibleErrorCodes` on the validator.

OR composition sites (`CompositionConstraint`) are owned by the built-in
`CompositionConstraintDocumentation`: empty native facets, published as
`{ "_constraint": "Composition", "composition": "OR", "members": […] }` under
`x-validata-constraints` (no OAS `oneOf`, no dishonest `format: email`).

For arbitrary swagger schema edits, implement
`META-INF/services/io.ghaylan.validata.openapi.mapper.OpenApiConstraintMapper`. **Do not**
set `description`.

Standard constraints with **no owning documenter** are listed in `x-validata-unmapped`
(sorted distinct metadata class names). Non-numeric `@Min` / `@Max` / `@MultipleOf` are owned
and still appear under `x-validata-constraints` (without native number facets).

---

## IntelliJ plugin

**Plugin id:** `io.ghaylan.validata`. IDE / Platform / plugin JDK pins: see the version table
at the top of this README. Bundled Java + Kotlin plugins required.

This is an **IDE plugin**, not an `implementation(...)` dependency. Your app builds and runs
identically without it. KSP remains the source of truth for CI and for editors other than
IntelliJ. The plugin mirrors the same rules **as you type**.

Without it, constraint arguments are opaque strings. With it they behave like code:
autocomplete, Ctrl+Click, rename, typed colors, and red underlines.

### Property references (`@PropertyRef` strings)

Applies to `@Compare(ref = "password", …)`, `@RequiredWhen(ref = "email")`,
`@Distinct(by = ["email"])`, and any custom parameter marked `@PropertyRef`.

| Feature                        | Behavior                                                                                       |
|--------------------------------|------------------------------------------------------------------------------------------------|
| Autocomplete                   | Sibling (or element) names as you type inside `"…"`, letter-by-letter — no Ctrl+Space required |
| Ctrl+Click / Go to Declaration | Jumps to the named property                                                                    |
| Rename                         | Renaming a DTO field updates matching path strings                                             |
| Field color                    | A **resolved** path uses the IDE instance-field color (not plain string green)                 |
| Unresolved                     | Typo `"passw"` → red “Cannot resolve property…”                                                |
| Self-reference                 | `@Compare(ref = "maxAge", …)` on `maxAge` itself → error underline                             |
| Nested path                    | `"address.city"` → error (single segment only, same as KSP)                                    |

Lookup scope:

| Annotated on      | Example                                           | Resolves against                       |
|-------------------|---------------------------------------------------|----------------------------------------|
| DTO field         | `@Compare(ref = "password", …)` on `confirmation` | Other properties of that class         |
| Handler parameter | `@Compare(ref = "min", …)` on a query param       | Other value-parameters of that method  |
| Collection        | `List<@Distinct(by = ["email"]) UserDto>`         | Properties of `UserDto` (element type) |

```kotlin
@Compare(ref = "password", operation = Compare.Operation.EQ)  // complete + Ctrl+Click + rename + field color
@Compare(ref = "address.city", operation = Compare.Operation.EQ)  // red — nested paths are not supported
@Compare(ref = User_.PASSWORD, operation = Compare.Operation.EQ)  // Fields constant — ordinary Kotlin ref; plugin does not intercept
```

### Type compatibility

Two distinct red-error checks.

**1. Can this constraint apply to this field at all?**

| Example                                                      | Result                                                        |
|--------------------------------------------------------------|---------------------------------------------------------------|
| `@Compare(…, operation = GREATER)` on incompatible ref types | Error — scalar kind mismatch (e.g. String field vs Int ref)   |
| `@Email` on an `Int`                                         | Error — no validator accepts `Int`                            |
| `@Email` on a `String`                                       | OK                                                            |
| `@Constraint(validatedBy = [])`                              | Error — must declare real `ConstraintValidator<V, C>` classes |
| `List<@Email String>`                                        | Subject is the element type (`String`), not the list          |
| Composed `@EmailOrPhone` on a field                          | **Every** leaf must accept the subject (AND and OR alike)     |

**2. Can this field be compared to the referenced field?**
Same matrix as KSP (`COMPARABLE_FAMILY` on `@Compare.ref`):

| Example                                                                        | Result                    |
|--------------------------------------------------------------------------------|---------------------------|
| `@Compare(ref = "age", operation = EQUAL)` on `String`, `age: Int`             | Error — String ↔ Int      |
| `@Compare(ref = "min", operation = GREATER)` on `Int`, `min: Int`              | OK                        |
| `@Compare(ref = "min", operation = GREATER)` on `Int`, `min: BigInteger`       | OK — both numeric         |
| `@Compare(ref = "start", operation = GREATER)` on `LocalDate`, `start: String` | Error — temporal ↔ string |

### Typed literals and enums (`@ConstraintArg`)

Strings/numbers that are **values**, not property names: `@Min("18")`, `@In(["ADMIN"])`,
`@Validatable.Subtype(name = "PHONE")`.

| Feature                               | Behavior                                                                                                 |
|---------------------------------------|----------------------------------------------------------------------------------------------------------|
| Enum autocomplete                     | Constant names as you type                                                                               |
| Enum Ctrl+Click                       | `"PHONE"` → `ContactType.PHONE`                                                                          |
| Number / temporal / enum / ref colors | Valid `"18"` IDE number, `"2020-01-01"` amber, `"PHONE"` IDE constant, property paths IDE instance-field |
| Invalid literal                       | Bad number, bad date, unknown enum name, disallowed blank → red                                          |
| Empty / illegal args                  | `@In([])`, blank elements, `@Size(min = -1)` → errors (KSP parity)                                       |
| RegExp coloring                       | `@Regex(pattern = "…")` injects RegExp syntax highlighting                                               |

What a literal is typed against:

| Annotation                             | Literal args       | Typed against                                                      |
|----------------------------------------|--------------------|--------------------------------------------------------------------|
| `@Min` / `@Max` / `@In` / `@NotIn` / … | `value` / `values` | The annotated field/parameter’s type                               |
| `@RequiredWhen`                        | `value` / `values` | The **gate** sibling named by `property` — not the annotated field |
| `@Validatable.Subtype`                 | `name`             | The discriminator property’s scalar type                           |

`@RequiredWhen` example — annotated field is `String`, but `value` is an enum of the gate:

```kotlin
enum class ContactType { EMAIL, PHONE }

@field:Required
@field:In(values = ["EMAIL", "PHONE"])
val contactType: ContactType?,

@field:RequiredWhen(
    ref = "contactType",
    condition = RequiredWhen.Condition.EQ,
    value = "PHONE",            // typed as ContactType: complete / Ctrl+Click / red on "SMS"
)
val phoneBackup: String?,
```

`condition = MISSING` / `PRESENT` skip the `value`/`values` check (those conditions do not
use them). `IN` / `NIN` use the same gate typing. Ordering conditions (`GT` / `LT` / `GTE` / `LTE`)
also type-check `value` against the gate.

### `@ConstraintComposition` (declaration site)

| Check (OR mode)                                       | Result                            |
|-------------------------------------------------------|-----------------------------------|
| Fewer than 2 leaf `@Constraint` members               | Error on `@ConstraintComposition` |
| Presence leaf (`Required` / `RequiredWhen`) inside OR | Error                             |
| Nested composed annotation inside OR (v1)             | Error                             |

### Polymorphism (`@Validatable`)

Editor twin of KSP’s subtype reconciler:

| Feature                 | Behavior                                                                                   |
|-------------------------|--------------------------------------------------------------------------------------------|
| Discriminator exists    | Non-blank `discriminator = "…"` must name a property on the annotated type — missing → red |
| Discriminator is scalar | Named property that isn’t string/number/temporal/enum → underline                          |
| Subtype assignability   | `Subtype(type = X::class)` must extend/implement the parent → underline                    |
| Typed `name`            | `name = "…"` checked as a typed literal against the discriminator                          |
| Enum DX on `name`       | When the discriminator is an enum: color, complete, Ctrl+Click                             |

### OpenAPI annotations

Requires `validata-openapi` on the **module classpath**. Same enum DX as `@In` / `@NotIn`.

| Annotation                                         | What the plugin does                                                                                                |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `@ApiError(code = "…", catalog = SomeEnum::class)` | Enum must implement `ConstraintErrorDefinition`; complete, Ctrl+Click, enum color on `code`. Unknown constant → red |

```kotlin
@field:ApiError(
    code = "NAME_INVALID",          // enum color + Ctrl+Click + complete
    catalog = SampleApiErrors::class,
)
val name: String?
```

### Custom constraints in the IDE

No annotation allowlist. The plugin reads `@Constraint`, `@PropertyRef`, and `@ConstraintArg`
from the project classpath. Mark a metadata parameter with those and strings get
resolve/complete/highlight for free.

If `@Constraint` is present but `{Name}Constraint` cannot be found (KSP has not run), a weak
warning is shown.

### Quick reference

| You type                                                              | IDE response                            |
|-----------------------------------------------------------------------|-----------------------------------------|
| `@Compare(ref = "` + letters                                          | Property autocomplete (siblings)        |
| `@In(["` on an enum field                                             | Enum constant autocomplete              |
| `@RequiredWhen(…, value = "` on a gate enum                           | Enum autocomplete for the **gate** type |
| Typo in a property path                                               | Red “Cannot resolve property”           |
| `@Compare(ref = "age", …)` on `String` while `age` is `Int`           | Red incompatible types                  |
| Valid `@Compare(ref = "password", …)`                                 | Field color + Ctrl+Click + rename       |
| Valid `@Min("18")`                                                    | Number-blue literal                     |
| Valid `@Min("2020-01-01")`                                            | Amber temporal literal                  |
| `@Validatable.Subtype(name = "PHONE")` on an enum discriminator       | Enum-colored literal + Ctrl+Click       |
| `@ConstraintComposition(OR)` with 1 leaf / presence / nested composed | Declaration-site error                  |
| `@Regex(pattern = "…")`                                               | RegExp syntax highlighting              |
| `@ApiError(code = "` with a `ConstraintErrorDefinition` enum catalog  | Enum complete / color / Ctrl+Click      |

### Install

From this repository:

```bash
./gradlew :validata-schema:publishToMavenLocal
./gradlew -p validata-intellij runIde
./gradlew -p validata-intellij buildPlugin
```

Install the ZIP under `validata-intellij/build/distributions/` via
**Settings → Plugins → ⚙️ → Install Plugin from Disk…**, then restart.

---

## Without Spring

You can use the engine outside Spring Boot. Depend on `validata-core` +
`ksp(validata-processor)` (skip the `validata` host jar):

```kotlin
implementation("io.github.ghaylansaada:validata-core:<version>")
ksp("io.github.ghaylansaada:validata-processor:<version>")
```

Load generated schemas via `GeneratedSchemas.get(MyDto::class.java)` /
`GeneratedRequestSchemas.get(endpointId)`, construct `ValidatorEngine` with a
`ValidationRegistry` and `ValidationLimits`, and call `validate`.

Populate the registry then call **`registry.freeze()`** before sharing it across threads
(or with a long-lived engine). Further `register*` calls throw after freeze. The Spring Boot
host does this automatically.

`validata-openapi` also depends on core only — never on the Spring MVC host.

---

## Performance

Steady-state in-memory benchmark of Validata vs Hibernate Validator using deterministic
fixture parity and violation-parity correctness gates. Full tables, charts, alloc / CPU,
and raw scores: [validata-benchmarks/README.md](validata-benchmarks/README.md).

### Method

|             |                                                                                         |
|-------------|-----------------------------------------------------------------------------------------|
| Scope       | Engine `validate` only — no HTTP / Tomcat / Jackson                                     |
| Fixtures    | Same logical payloads on both engines; factory/registry init outside measured loop      |
| Small       | Heterogeneous 5-field DTO (`name`, `age`, `email`, `birthDate`, `tags`)                 |
| Scalability | 10–1000 fields via six rotating 5-field constraint slices (~30 fair twins)              |
| Sizes       | **5 / 10 / 25 / 50 / 100 / 250 / 500 / 1000** leaf fields                               |
| Invalidity  | **0%** Valid · **25%** Quarter · **50%** Half · **100%** All (deterministic)            |
| Cells       | **64** (8 sizes × 4 invalidity levels × 2 engines)                                      |
| Gate        | Outcome + violation count + property paths must match                                   |
| JMH         | 10 forks · 5×2 s warmup · 10×2 s measure · `-Xms2g -Xmx2g` · GC + process CPU profilers |
| Host        | JDK **21.0.9** · Linux amd64 · 12 CPUs · run `2026-10-08` (precise)                     |
| Report      | Neutral ratios only (no winners / rankings / aggregate scores)                          |

Re-run:

```bash
./gradlew :validata-benchmarks:jmh :validata-benchmarks:benchmarkReport -PjmhPrecise
```

Numbers below are **one machine, one precise run**. Prefer same-run ratios over absolute
ops/s across hosts. Ratio = Validata ÷ Hibernate Validator:

- throughput ratio **> 1** → higher Validata throughput
- allocation ratio **< 1** → lower Validata allocation
- CPU ratio **< 1** → lower Validata CPU time

### Throughput (ops/s)

#### Valid (0% invalid)

| Size             | Validata | Hibernate Validator | Ratio |
|------------------|---------:|--------------------:|------:|
| Small (5)        |  735 473 |             814 638 | 0.90× |
| Medium (10)      |  687 923 |             654 712 | 1.05× |
| Large (25)       |  199 918 |             148 270 | 1.35× |
| XLarge (50)      |  107 979 |              68 513 | 1.58× |
| Very Large (100) |   53 209 |              38 128 | 1.40× |
| Extreme (250)    |   21 584 |              15 027 | 1.44× |
| Stress (500)     |   10 866 |               7 491 | 1.45× |
| Maximum (1000)   |    4 880 |               3 653 | 1.34× |

#### Quarter Invalid (25%)

| Size             | Validata | Hibernate Validator | Ratio |
|------------------|---------:|--------------------:|------:|
| Small (5)        |  710 239 |             760 613 | 0.93× |
| Medium (10)      |  505 594 |             341 081 | 1.48× |
| Large (25)       |  142 904 |             112 225 | 1.27× |
| XLarge (50)      |   70 605 |              33 075 | 2.13× |
| Very Large (100) |   28 111 |              22 503 | 1.25× |
| Extreme (250)    |   11 055 |               6 911 | 1.60× |
| Stress (500)     |    5 363 |               4 429 | 1.21× |
| Maximum (1000)   |    2 482 |               2 183 | 1.14× |

#### Half Invalid (50%)

| Size             | Validata | Hibernate Validator | Ratio |
|------------------|---------:|--------------------:|------:|
| Small (5)        |  793 580 |           1 490 883 | 0.53× |
| Medium (10)      |  334 946 |             277 096 | 1.21× |
| Large (25)       |   79 678 |              73 859 | 1.08× |
| XLarge (50)      |   43 752 |              38 973 | 1.12× |
| Very Large (100) |   19 746 |              17 278 | 1.14× |
| Extreme (250)    |    7 758 |               6 763 | 1.15× |
| Stress (500)     |    3 879 |               3 399 | 1.14× |
| Maximum (1000)   |    1 585 |               1 669 | 0.95× |

#### All Invalid (100%)

| Size             | Validata | Hibernate Validator | Ratio |
|------------------|---------:|--------------------:|------:|
| Small (5)        |  613 249 |             615 823 | 1.00× |
| Medium (10)      |  168 404 |             167 614 | 1.00× |
| Large (25)       |   50 235 |              35 267 | 1.42× |
| XLarge (50)      |   28 246 |              16 620 | 1.70× |
| Very Large (100) |   12 773 |               7 817 | 1.63× |
| Extreme (250)    |    4 561 |               2 997 | 1.52× |
| Stress (500)     |    2 487 |               1 470 | 1.69× |
| Maximum (1000)   |    1 140 |                 725 | 1.57× |

### Allocation (B/op, valid path)

| Size             | Validata | Hibernate Validator | Ratio |
|------------------|---------:|--------------------:|------:|
| Small (5)        |      938 |               2 030 | 0.46× |
| Medium (10)      |    1 086 |               2 819 | 0.39× |
| Large (25)       |    2 672 |               8 098 | 0.33× |
| XLarge (50)      |    4 146 |              14 566 | 0.28× |
| Very Large (100) |    7 027 |              28 335 | 0.25× |
| Extreme (250)    |   16 780 |              70 765 | 0.24× |
| Stress (500)     |   33 412 |             141 982 | 0.24× |
| Maximum (1000)   |   68 761 |             288 347 | 0.24× |

GC normalized allocation (`·gc.alloc.rate.norm`). Invalid-path alloc, CPU, and heap tables are
in the [benchmarks README](validata-benchmarks/README.md). Heap after iteration is diagnostic
only — not a retention proof.

### Constraint mapping

| Hibernate Validator / Jakarta          | Validata                                                            | Equivalence                                                                              |
|----------------------------------------|---------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `@NotBlank` / `@NotNull` / `@NotEmpty` | `@Required` / `@Required(NULL)` / `@Required(EMPTY)`                | fixture-domain for `@NotBlank`↔`@Required`; `@Required(NULL)` is null-only; others exact |
| `@Min` / `@Max`                        | `@Min` / `@Max`                                                     | exact                                                                                    |
| `@DecimalMin` / `@DecimalMax`          | `@Min` / `@Max` on `BigDecimal`                                     | fixture-domain                                                                           |
| `@Digits`                              | `@Digits`                                                           | exact                                                                                    |
| `@Positive` / `@PositiveOrZero`        | `@NumberSign(POSITIVE)` / `@NumberSign(POSITIVE, allowZero = true)` | exact                                                                                    |
| `@Negative` / `@NegativeOrZero`        | `@NumberSign(NEGATIVE)` / `@NumberSign(NEGATIVE, allowZero = true)` | exact                                                                                    |
| `@Size`                                | `@Size`                                                             | exact                                                                                    |
| `@Pattern`                             | `@Regex`                                                            | exact (ASCII fixtures)                                                                   |
| `@Email`                               | `@Email`                                                            | exact                                                                                    |
| `@Past` / `@PastOrPresent`             | `@RelativeToNow(LT)` / `@RelativeToNow(LTE)`                        | exact                                                                                    |
| `@Future` / `@FutureOrPresent`         | `@RelativeToNow(GT)` / `@RelativeToNow(GTE)`                        | exact                                                                                    |
| `@AssertTrue` / `@AssertFalse`         | `@Assert(true)` / `@Assert(false)`                                  | exact                                                                                    |
| `@URL`                                 | `@Url`                                                              | closest available                                                                        |
| `@CreditCardNumber`                    | `@CreditCard`                                                       | fixture-domain (Luhn)                                                                    |
| `@UUID`                                | *(no built-in — use `@Regex` or custom)*                            | fixture-domain dropped from core                                                         |
| `@EAN` / `@ISBN`                       | `@Barcode(EAN)` / `@Barcode(ISBN)`                                  | fixture-domain                                                                           |
| `@IpAddress`                           | `@IpAddress`                                                        | exact                                                                                    |
| `@DurationMin` / `@DurationMax`        | `@Min` / `@Max` on `Duration`                                       | exact for fixture bounds                                                                 |

`@Required` on strings vs `@NotBlank` differs on whitespace. Fixtures use non-blank valid
strings and `""` invalid strings. Dates are fixed calendar values (never `now()`). ASCII only.

---

## Sample app

[`validata-samples`](validata-samples) is a real Boot app (not a published artifact). It
composes the host, engine, KSP, optional OpenAPI, app-owned error JSON, and a GraalVM surface.

```bash
./gradlew :validata-samples:bootRun
./gradlew :validata-samples:test
```

| Path                                       | What it shows                                                                      |
|--------------------------------------------|------------------------------------------------------------------------------------|
| `POST /api/users`                          | Body: `@Required`, `@Size`, custom `OddYears` / `SampleFloor`, `@Compare(GREATER)` |
| `GET /api/users/{userId}?q=…` + `X-Tenant` | Path + query + header share the same schema IR as bodies                           |
| `POST /api/all-constraints`                | Every built-in constraint plus OddYears                                            |
| `GET /v3/api-docs`                         | Native facets, `x-validata-constraints`, documented 400/404 examples               |

Copy `OddYears` / `SampleFloor` from that module.

---

## Artifacts

Published libraries and tooling around the same compile-time → runtime pipeline:

| Module                                               | Coordinate / id                             | One-line role                                                           |
|------------------------------------------------------|---------------------------------------------|-------------------------------------------------------------------------|
| [validata-schema](validata-schema/README.md)         | `io.github.ghaylansaada:validata-schema`    | Shared IR types — blueprints only, zero runtime deps                    |
| [validata-processor](validata-processor/README.md)   | `io.github.ghaylansaada:validata-processor` | KSP generator — **`ksp(...)` only**, never runtime                      |
| [validata-core](validata-core/README.md)             | `io.github.ghaylansaada:validata-core`      | 38 built-ins, validators, `ValidatorEngine` — no Spring                 |
| [validata](validata/README.md)                       | `io.github.ghaylansaada:validata`           | Boot WebMVC host + config-property validation + binding translation     |
| [validata-openapi](validata-openapi/README.md)       | `io.github.ghaylansaada:validata-openapi`   | Optional springdoc overlay from the same IR (depends on core, not host) |
| [validata-intellij](validata-intellij/README.md)     | Plugin `io.ghaylan.validata`                | Optional IDE plugin ZIP — not on the app classpath                      |
| [validata-samples](validata-samples/README.md)       | *(unpublished)*                             | Runnable acceptance app — copy patterns, not a dependency               |
| [validata-benchmarks](validata-benchmarks/README.md) | *(unpublished)*                             | In-memory JMH vs Hibernate Validator with parity gates                  |

Spring stays in your app starters. The engine, schemas, and processor never import it. OpenAPI never
depends on the host. The IntelliJ plugin never ships inside your application.

---

## Stability and SemVer

Validata follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
**Version 1.0.0 establishes the first stable compatibility line under this policy.** The
supported public surface below is frozen: no renames, removals, or silent semantic shifts
without a SemVer bump and a [CHANGELOG](CHANGELOG.md) entry
([Keep a Changelog](https://keepachangelog.com/)).

Breaking changes to that surface require a **major** version. Additive APIs are usually a
**minor**; bug fixes that do not change the contract are usually a **patch**.

Contract tests under `validata-core` / `validata-processor` (`…contract` packages) lock names,
default messages, enum entries, KSP option keys, and limit defaults so drift fails CI.

### Version rules

| Kind      | When                                     | Example                                                                                  |
|-----------|------------------------------------------|------------------------------------------------------------------------------------------|
| **Major** | Breaking change to the supported surface | Rename `@IsoCountry`, remove an error code, change `@Required` STRICT vs EMPTY semantics |
| **Minor** | Backward-compatible addition             | New annotation, new error code, new KSP option                                           |
| **Patch** | Backward-compatible fix                  | Validator rejected valid input; docs/typo that does not change contract                  |

### Supported public surface (frozen)

| Area                   | Supported contract                                                                                                                                                                                                                                                                   | Why it matters                                                                                       |
|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| **Annotations**        | Built-in `@Constraint` annotations, `@Validate` / `@Validatable` / `@NoCascade`, `@ConstraintComposition`, parameter names / defaults / nested enums (`Required.Mode`, `Url.Type`, …)                                                                                                | Apps compile against these. Renaming or changing defaults is a break.                                |
| **Engine / host APIs** | `ValidatorEngine.validate` / `validateRequest`, `ValidationOptions` (defaults: `oneErrorPerParam=true`, `failFast=false`), `ValidationLimits` defaults (`32` / `10000` / `200`) and `validata.limits.*` keys, `ValidationRegistry`, `ConstraintViolationException`, Boot auto-config | Library and Spring apps call these. Signature or fail-semantics changes break them.                  |
| **KSP options**        | `validata.jackson.naming` (`IDENTITY` / `SNAKE_CASE`), `validata.strictCrossModuleCascade`, `validata.maxShapeNestingDepth`                                                                                                                                                          | Build scripts depend on keys and allowed values.                                                     |
| **Error codes**        | Full [Built-in codes](#built-in-codes) catalog (`ConstraintErrorCode` name + default `message`; `code == name`)                                                                                                                                                                      | Clients branch on `code`; OpenAPI lists codes. Renaming or removing a code breaks handlers and docs. |

Module READMEs list the concrete types under each jar’s public API section
(e.g. [validata-core Public API surface](validata-core/README.md#public-api-surface)).

### What freeze does *not* mean

- No new features — additive APIs are fine (usually a minor bump).
- No bug fixes — correcting “accepted invalid input” is usually a patch.
- Every private type is frozen — only the **supported** surface above.

Internals (`engine.walk.*`, processor `analyze.*`, and any package marked non-API / `internal`)
may change without a SemVer bump.

### Out of scope (not a SemVer break if absent)

- WebFlux / reactive host (WebMVC only today)
- Jakarta Bean Validation provider compatibility
- Marketplace packaging guarantees for the IntelliJ plugin beyond the documented `sinceBuild`

---

## Limitations

Honest boundaries so you can decide if Validata fits before you adopt it:

| Area             | Limitation                                                                                                                                                                                                                                                                                              |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Language         | **Kotlin only.** Annotations, KSP processing, and the engine target Kotlin source. There is no Java annotation / APT surface.                                                                                                                                                                           |
| Minimum versions | Libraries require **JDK 21** and **Kotlin 2.4.x** with a matching **KSP 2.x** pairing. The Spring host is built against **Spring Boot 4.x** (compileOnly — you pin the runtime). The IntelliJ plugin requires **IDEA 2025.3+** (`sinceBuild` 253). Exact pins: version table at the top of this README. |
| Bean Validation  | Not a Jakarta BV provider. `jakarta.validation` / Hibernate Validator annotations are **not** executed. Migrate annotations deliberately — or run both stacks only if you understand the overlap.                                                                                                       |
| HTTP stack       | Spring **WebMVC** (servlet) host only. WebFlux / reactive is out of scope for v1.                                                                                                                                                                                                                       |
| Code generation  | KSP is **required** on every module that declares `@Validatable`, `@Validate`, or a custom `@Constraint`. Missing schemas fail closed; there is no reflective fallback.                                                                                                                                 |
| Sibling paths    | `@Compare` / `@RequiredWhen` / `@Distinct(by = …)` accept **one segment** only (`"password"` is valid; `"address.city"` is not).                                                                                                                                                                        |
| Error envelope   | The library throws `ConstraintViolationException`. It does **not** ship `@ControllerAdvice` or a fixed JSON shape — your app owns the HTTP mapping.                                                                                                                                                     |
| Optional deps    | `@Html` needs jsoup; `@Phone` needs libphonenumber. Neither is a transitive dependency of `validata-core`.                                                                                                                                                                                              |
| Polymorphism     | Subtypes must be declared on `@Validatable` — Jackson annotations are **not** read for discovery.                                                                                                                                                                                                       |
| OpenAPI          | Docs overlay is optional and concurrent `/v3/api-docs` generation is **not** supported. OR composition does not emit OAS `oneOf`.                                                                                                                                                                       |
| IntelliJ plugin  | Optional DX. CI and non-IntelliJ editors rely on KSP alone. Marketplace packaging beyond `sinceBuild` is not a SemVer guarantee.                                                                                                                                                                        |
| Engine thread    | Validation runs on the calling thread (servlet request thread in the host). There is no cooperative cancel — tighten `validata.limits` for hostile payloads.                                                                                                                                            |

See also [Out of scope](#out-of-scope-not-a-semver-break-if-absent) under [Stability and SemVer](#stability-and-semver).

---

## Rules

1. `ksp(validata-processor)` on every declaring module. No reflective recovery.
2. Presence is not implied. `null` goes to `validateNull` (default skip) — pair format constraints with `@Required`.
3. Prefer nullable DTO properties so JSON absence is `null`.
4. Sibling paths are one segment. Declared name strings or `Type_` / `Controller_function_` wire constants both work (plugin + KSP).
5. Nested types in the same module must be `@Validatable` or `@NoCascade`.
6. Error JSON is yours. Map `ConstraintViolationException` (engine + translated binding failures).
7. `@Html` / `@Phone` need extra jars (not transitive).
8. Never `implementation` the processor.
9. Custom constraints need KSP on the module that **declares** the annotation.
10. Do not put secrets in constraint `message` or error `metadata`.
11. KSP is CI truth. The IntelliJ plugin is optional DX that mirrors it.
12. Validata is not Bean Validation. `jakarta.validation` `@Valid` is not executed.
13. Empty email strings fail `@Email`; `@Url` / `@Html` also skip blank.
14. Custom `validate(value: Value, …)` is **non-null** — do not null-check inside `validate`.

## License and Attribution

This project is licensed under the **Apache License 2.0** — see the [LICENSE](LICENSE) file
for the full terms.

### Original Author

* **Author & Lead Architect:** [Ghaylan Saada](https://github.com/ghaylansaada)
* **Official Repository:** [github.com/ghaylansaada/validata](https://github.com/ghaylansaada/validata)

### Apache 2.0 Requirements

Redistribution and use are governed by Apache License 2.0. In particular, redistributions
and derivative works must preserve the required copyright and license notices, including the
[`NOTICE`](NOTICE) file where applicable (`Copyright 2026 Ghaylan Saada`), and must include
prominent notices on files you modify stating that changes were made. See [LICENSE](LICENSE)
for the complete conditions.

### Trademark

Apache 2.0 does **not** grant trademark rights. The name **Validata**, project logos, and the
author’s name may not be used to promote or endorse derivative products without prior written
permission.
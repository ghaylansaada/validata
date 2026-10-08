# validata-intellij

**Marketplace / plugin id:** `io.ghaylan.validata`  
**Artifact:** IntelliJ IDEA plugin ZIP — editor DX for Validata annotation strings; **not** an app dependency.

| Piece                        | Value                                                                                                  |
|------------------------------|--------------------------------------------------------------------------------------------------------|
| Bundled plugins              | Java (`com.intellij.java`), Kotlin (`org.jetbrains.kotlin`) — required                                 |
| Compile dependency           | `validata-schema` only (shared path / scalar / arg contracts)                                          |
| Library markers at edit time | Discovered from the **open project** classpath (`validata-core`, custom constraints, optional openapi) |
| Packaging                    | Plugin ZIP under `build/distributions/`                                                                |

Version / IDEA / Platform / plugin JDK pins: [root README](../README.md) (top table). Keep
`validata-intellij/gradle.properties` aligned with that table.

This module ships an **optional IDE plugin**. Applications build and run the same with or
without it. It does **not** validate values at runtime and does **not** fail the build.

KSP (`validata-processor`) remains the **source of truth** for CI. This plugin is a best-effort **editor mirror** of the
same authoring contracts.

---

## Contents

1. [Why this module exists](#why-this-module-exists)
2. [Why it is a separate module](#why-it-is-a-separate-module)
3. [Role in the framework](#role-in-the-framework)
4. [How it works](#how-it-works)
5. [Features](#features)
6. [Highlighting and colors](#highlighting-and-colors)
7. [Scopes and typing rules](#scopes-and-typing-rules)
8. [Discovery and custom constraints](#discovery-and-custom-constraints)
9. [What authors should know](#what-authors-should-know)
10. [Quick reference](#quick-reference)
11. [Build, run, and test](#build-run-and-test)
12. [Package layout](#package-layout)
13. [Contributor guidelines](#contributor-guidelines)
14. [Extension points](#extension-points)
15. [Rules to remember](#rules-to-remember)
16. [What it does not do](#what-it-does-not-do)

Application Gradle setup (Validata library + KSP): [root README](../README.md#installation).

---

## Why this module exists

Validata authors put **meaning** in string and number arguments that the stock IDE treats as
plain literals:

```kotlin
@Compare(ref = "password", operation = Compare.Operation.EQ)
@RequiredWhen(ref = "contactType", condition = RequiredWhen.Condition.EQ, value = "PHONE")
List<@Distinct(by = ["email"]) ContactDto>
@Min("18")
@In(["ADMIN"])
@Regex(pattern = "[a-z]+")
@ConstraintComposition(Mode.OR)   // on a composed annotation declaration
```

Without this plugin:

| You do                                         | IDE                       | Later                                         |
|------------------------------------------------|---------------------------|-----------------------------------------------|
| Typo `@Compare(ref = "passw", …)`              | silence                   | compile-time / CI failure (or a shipped typo) |
| `@Compare(ref = "age", …)` on incompatible types | silence                 | compile-time / CI failure                     |
| Illegal `@ConstraintComposition(OR)` authoring | silence                   | KSP failure                                   |
| Rename `password` → `secret`                   | string stays `"password"` | broken cross-field rule                       |
| Type inside `"…"`                              | no property / enum popup  | slow authoring                                |

`validata-intellij` is the **editor mirror** of those authoring rules: autocomplete, navigation,
rename, errors, and colors as you type — so mistakes show up in the open editor, not only in CI.

---

## Why it is a separate module

| Isolation goal           | Effect                                                                                            |
|--------------------------|---------------------------------------------------------------------------------------------------|
| Optional install         | Apps never depend on IntelliJ Platform APIs                                                       |
| Composite `includeBuild` | Avoids KSP / Platform Gradle classpath clashes with the root build                                |
| Schema contracts only    | Compiles against `validata-schema`; discovers library markers from the **user** classpath by FQCN |
| No runtime coupling      | Plugin ZIP is never on the application classpath                                                  |

Wire it by **installing the plugin**, never as `implementation(...)` in an app module.

---

## Role in the framework

```text
You annotate DTOs / handlers / constraints in the IDE
              │
              ▼
┌─────────────────────────────────────┐
│  validata-intellij  ← YOU ARE HERE  │
│  PSI analysis + editor features     │
│  (optional; mirrors authoring rules)│
└──────────────────┬──────────────────┘
                   │ uses shared contracts from
                   ▼
            validata-schema
                   │
                   │ compile-time / runtime layers remain separate
                   ▼
            processors, runtime, hosts, docs tooling
```

**This module’s job:** make Validata string / numeric annotation arguments navigable, completable,
checked, and colored inside IntelliJ IDEA — and flag illegal composition / polymorphism /
presentation authoring that KSP also rejects.

**Not this module’s job:** generating schemas, running validators, binding HTTP, or publishing
OpenAPI. Those stay in their own modules; this plugin only improves the authoring experience.

---

## How it works

1. You write Validata annotations in Kotlin.
2. The plugin discovers which arguments are property paths (`@PropertyRef`), typed literals /
   other arg rules (`@ConstraintArg` / `@ConstraintArgs`), composition roots (`@ConstraintComposition`), polymorphism
   (`@Validatable`), and presentation catalogs (`@ApiError`) by reading markers on the **open project’s** classpath —
   builtins and custom
   constraints alike (**no hardcoded allowlist** of `@Compare`, `@In` / `@NotIn`, …).
3. Results that are expensive to recompute are memoized in `ConstraintDiscoveryCache`
   (per-project; invalidated on PSI stamp; sticky for `io.ghaylan.validata.*` library FQCNs).
4. It attaches PSI references, completion, annotators, and (for regex) language injection.
5. Shared spelling / depth / scalar-compatibility / arg-kind vocabulary comes from the bundled
   `validata-schema` dependency inside the plugin ZIP.

Editor checks are **best-effort mirrors** of compile-time authoring rules. CI / the processor
remain authoritative for shipping code.

---

## Features

### Property-path references (`@PropertyRef`)

Strings that name another field or parameter, e.g. `@Compare(ref = "password", …)`,
`@RequiredWhen(ref = "…")`, `@Distinct(by = ["…"])`.

| Feature                        | Behavior                                                                                      |
|--------------------------------|-----------------------------------------------------------------------------------------------|
| Autocomplete                   | Sibling or element property names inside `"…"`, letter-by-letter (autopopup while typing)     |
| Ctrl+Click / Go to Declaration | Jumps to the named member                                                                     |
| Rename                         | Renaming a DTO field / parameter updates matching path strings                                |
| Field-like color               | Resolved paths use the IDE **instance-field** color (not plain string green)                  |
| Unresolved                     | Typo → **red text** + error                                                                   |
| Self-reference                 | Sibling path naming the annotated member itself → error (underline; name stays field-colored) |
| Nested paths                   | `"address.city"` → error (only single-segment sibling / element names)                        |
| Empty `""`                     | Soft reference so completion works while the string is still empty                            |

```kotlin
data class RegisterRequest(
    val password: String,
    @Compare(ref = "password", operation = Compare.Operation.EQ)   // complete + Ctrl+Click + rename + field color
    val confirmation: String,
)
```

### Subject-type and cross-field type checks

| Check                        | Example                                               | Result                                                                  |
|------------------------------|-------------------------------------------------------|-------------------------------------------------------------------------|
| Constraint vs annotated type | `@Compare(…, GREATER)` with incompatible ref types    | Error on path / annotation                                              |
| Validator fit                | `@Email` on `Int`                                     | Error — no `validatedBy` `V` accepts `Int`                              |
| Empty `validatedBy`          | `@Constraint(validatedBy = [])`                       | Error — must list real `ConstraintValidator` classes                    |
| Type-use on collections      | `List<@Email String>`                                 | Subject is the **element** type                                         |
| Composed annotation subject  | `@EmailOrPhone` on a field                            | **Every** leaf must have a `V` that fits the subject (AND and OR alike) |
| Path leaf vs annotated type  | `@Compare(ref = "age", …)` on `String` while `age: Int` | Error (incompatible scalar kinds)                                     |
| Numeric compare              | `@Compare(ref = "min", operation = GREATER)` on `Int` with `min: BigInteger` | OK (both numeric)                              |

Resolved names keep field color; type-mismatch / self-ref / nested-path use **error underline**
only. Full red text is reserved for **unresolved** names and invalid literals.

### Typed literals and `@ConstraintArg` rules

Arguments that are **values**, not property names — `@Min("18")`, `@In(["ADMIN"])`,
`@Size(min = -1)`, `@Password(minLength = -1)`, `@Url(maxLength = -1)`,
`@Regex(pattern = "…")`, etc.

Markers may be a single `@ConstraintArg`, repeatable copies, or the `@ConstraintArgs` container (including
`target = ELEMENT` for collection argument elements).

| Feature                        | Behavior                                                                                  |
|--------------------------------|-------------------------------------------------------------------------------------------|
| Enum autocomplete / Ctrl+Click | `"ADMIN"` → `Role.ADMIN` when the subject type is an enum (incl. `java.time.Month` names) |
| Number / temporal colors       | Valid numeric / temporal strings recolored (see [Highlighting](#highlighting-and-colors)) |
| `NOT_BLANK`                    | Blank required strings → error                                                            |
| `TYPED_LITERAL`                | Bad number / date / unknown enum constant → error                                         |
| `NON_EMPTY`                    | Empty collection args (`@In([])`) → error                                           |
| `NON_NEGATIVE` / `POSITIVE`    | Integer or string-form bounds out of range → error                                        |
| `REGEX`                        | Invalid Java regex syntax → error; valid patterns get RegExp language injection           |

**What type is a literal checked against?**

| Site                                              | Literal args                    | Typed against                                     |
|---------------------------------------------------|---------------------------------|---------------------------------------------------|
| Most constraints (`@Min`, `@In` / `@NotIn`, …) | `value` / `values` / bounds / … | Annotated field / parameter type                  |
| `@RequiredWhen`                                   | `value` / `values`              | The **gate** sibling named by `property`          |
| `@Validatable.Subtype`                            | `name`                          | Discriminator property type on the annotated root |

```kotlin
@field:RequiredWhen(
    ref = "contactType",
    condition = RequiredWhen.Condition.EQ,
    value = "PHONE",           // typed as ContactType, not as String
)
val phoneBackup: String?
```

`condition = MISSING` / `PRESENT` skip `value` / `values` checks. `EQ` / `NE` / `GT` / `LT` /
`GTE` / `LTE` use `value` only; `IN` / `NIN` use `values` only (inactive parameters are not flagged).

### `@ConstraintComposition` (declaration site)

Composed annotations (nested leaf `@Constraint`s **without** an outer `@Constraint`) are
discovered like KSP. Optional `@ConstraintComposition(AND|OR)` controls expand mode.

| Check (OR mode)                                       | Result                            |
|-------------------------------------------------------|-----------------------------------|
| Fewer than 2 leaf `@Constraint` members               | Error on `@ConstraintComposition` |
| Presence leaf (`Required` / `RequiredWhen`) inside OR | Error                             |
| Nested composed annotation inside OR (v1)             | Error                             |
| Valid OR with ≥2 leaf constraints                     | No composition authoring error    |

AND / unmarked composition flattens leaves for subject-type checks. OR changes **runtime expand**
and declaration-site authoring rules; subject-type fit still requires **every** leaf validator
to accept the annotated subject (same as KSP’s use-site check).

```kotlin
@Email
@Phone
@ConstraintComposition(Mode.OR)
annotation class EmailOrPhone
```

### `@Validatable` polymorphism

| Feature                  | Behavior                                                                          |
|--------------------------|-----------------------------------------------------------------------------------|
| Discriminator exists     | Non-blank `discriminator = "…"` must name a property on the annotated type        |
| Discriminator is scalar  | Non-scalar discriminator → underline error                                        |
| Discriminator navigation | `discriminator = "kind"` → property reference (complete / Ctrl+Click)             |
| Subtype assignability    | `Subtype(type = X::class)` must extend / implement the parent; same-type rejected |
| Subtype `name`           | Typed literal against the discriminator (colors + enum DX when applicable)        |

```kotlin
@Validatable(
	discriminator = "kind",
	subtypes = [Validatable.Subtype(name = "A", type = Alpha::class)],
)
interface Root {
	val kind: Kind
}
```

### `@ApiError` catalog DX

When OpenAPI presentation annotations are on the module classpath:

| Feature              | Behavior                                                                                   |
|----------------------|--------------------------------------------------------------------------------------------|
| Enum `code`          | Autocomplete, Ctrl+Click, constant color                                                   |
| Blank / unknown code | Red unresolved error                                                                       |
| Bad catalog          | Missing catalog, non-enum, or catalog not implementing `ConstraintErrorDefinition` → error |

Absent presentation types → these checks are no-ops; other Validata IDE features keep working.

### Authoring helpers

| Feature                  | Behavior                                                                                               |
|--------------------------|--------------------------------------------------------------------------------------------------------|
| No annotation allowlist  | Custom `@Constraint`s light up when parameters carry `@PropertyRef` / `@ConstraintArg`                 |
| Missing metadata warning | `@Constraint` on a Kotlin annotation whose `{Name}Constraint` class cannot be found → **weak warning** |
| Fields constants ignored | `@Compare(ref = User_.PASSWORD, …)` is a normal Kotlin reference — plugin does not intercept it   |
| Autopopup in strings     | Completion confidence + typed handler allow letter-by-letter popup inside Validata hosts               |

---

## Highlighting and colors

| Key / look                            | Used for                                                                                         |
|---------------------------------------|--------------------------------------------------------------------------------------------------|
| IDE instance-field color              | Resolved property-path segments                                                                  |
| IDE number color                      | Valid numeric typed strings (`@Min("18")`)                                                       |
| Amber (`GHAYLAN_CONSTRAINT_TEMPORAL`) | Valid non-enum temporal strings (`"2020-01-01"`, `"P1Y"`, …)                                     |
| IDE constant color                    | Enum / catalog name strings (`"ADMIN"`, `@ApiError` codes, `Month` names)                        |
| Red unresolved                        | Missing property, unknown enum constant, invalid typed literal, blank / empty arg errors         |
| Error underline only                  | Self-ref, nested path, type mismatch on an **already resolved** name; some `@Validatable` issues |
| RegExp injection                      | Valid `@ConstraintArg(REGEX)` pattern strings (multi-token regex coloring)                       |

Default and Darcula schemes ship amber temporal defaults via `colorSchemes/GhaylanValidation*.xml`.
Number / enum / property-ref keys inherit the active IDE theme.

---

## Scopes and typing rules

**Where property names resolve:**

| Annotated on                     | Example                                        | Resolves against                     |
|----------------------------------|------------------------------------------------|--------------------------------------|
| Class / data-class property      | `@Compare(ref = "password", …)` on `confirmation` | Other properties of that class    |
| Constructor value-parameter      | Same, on a primary-constructor param           | Containing class properties / params |
| Handler / method value-parameter | `@Compare(ref = "min", …)` on a query param    | Peer value-parameters of that method |
| Collection (element scope)       | `List<@Distinct(by = ["email"]) UserDto>` | Properties of the **element** type   |

Only **single-segment** paths are supported. Nested `"a.b"` paths are rejected in the editor (same policy as
compile-time authoring). Soft empty segments (trailing `.` / `""`) exist for
completion only.

---

## Discovery and custom constraints

The plugin does **not** hard-code builtin annotation FQCNs for feature enablement. It discovers:

| Marker                               | Enables                                                   |
|--------------------------------------|-----------------------------------------------------------|
| `@Constraint` / `validatedBy`        | Accepted subject types (`ConstraintValidatedByDiscovery`) |
| `@PropertyRef`                       | Path hosts (scope + compatibility)                        |
| `@ConstraintArg` / `@ConstraintArgs` | Literal / numeric / regex / emptiness rules               |
| `@ConstraintComposition`             | AND / OR expand mode on composed annotation declarations  |
| `@Validatable`                       | Polymorphism hosts                                        |
| `@ApiError` (when present)           | Catalog / code hosts                                      |

Markers may live on the annotation declaration or on the conventional generated
`{AnnotationName}Constraint` metadata class. Custom constraints get the same IDE behavior once
those markers are on the classpath.

FQCN string constants used for discovery live in `contract.PropertyRefLibraryFqns` /
`OpenApiPresentationFqns` — update those when library packages move; do **not** reintroduce
per-constraint allowlists in editor code.

### Discovery cache

`discovery.cache.ConstraintDiscoveryCache` (project service):

- Keys: annotation FQCN → `@PropertyRef` hosts, `@ConstraintArg` hosts, `validatedBy` result
- Single-flight via `ConcurrentHashMap.compute`
- Clears **user** FQCNs when the PSI modification stamp advances
- Sticky-keeps `io.ghaylan.validata.*` library FQCNs across stamp clears (jars do not change with local edits)

### Package DAG

```
contract, path, typing     (leaves)
    ↑
model                      (data only)
    ↑
discovery.cache            (ConstraintDiscoveryCache + Result + entries)
    ↑
discovery / discovery.{propertyref,constraintarg,validatedby,composition}
    (+ PsiClassLookup)
    ↑
analysis.{compat,literal,validatable,apierror}
    ↑
scope, resolve
    ↑
editor.{annotator,reference,completion,inject,color}
```

`model` / `path` / `contract` / `typing` / `editor.color` must not import `editor.annotator`,
`editor.reference`, or `discovery.*` (enforced by `architecture/PackageBoundaryTest`).
`analysis` must not import `editor.*`.

```kotlin
@Compare(ref = "password", operation = Compare.Operation.EQ)  // string path — plugin handles it
@Compare(ref = User_.PASSWORD, operation = Compare.Operation.EQ)  // Fields constant — ordinary Kotlin ref, ignored here
@Compare(ref = "address.city", operation = Compare.Operation.EQ)  // nested path — editor error
```

---

## What authors should know

1. Install the plugin in the IDE; do not add this module to your app’s Gradle dependencies.
2. Keep library annotations (and, for `@ApiError`, presentation types) on the **module** classpath
   so discovery can resolve markers.
3. Prefer string paths when you want IDE help; Fields constants already navigate as Kotlin code.
4. Editor diagnostics are a productivity layer — keep compile-time generation / verification in CI.
5. Incomplete / unresolved projects may skip some checks until indexes and classpath settle.

---

## Quick reference

| You type / place                                                      | IDE response                                   |
|-----------------------------------------------------------------------|------------------------------------------------|
| `@Compare(ref = "` + letters                                          | Property autocomplete (siblings)               |
| `@In(["` on an enum field                                        | Enum constant autocomplete                     |
| `@RequiredWhen(…, value = "` with enum gate                           | Enum autocomplete for the **gate** type        |
| Typo in a property path                                               | Red “Cannot resolve property…”                 |
| `@Compare(ref = "age", …)` on `String` while `age` is `Int`           | Red incompatible-types (underline on the path) |
| Valid `@Compare(ref = "password", …)`                                 | Field color + Ctrl+Click + rename              |
| Valid `@Min("18")`                                                    | Number color                                   |
| `@Size(min = -1)`                                                     | NON_NEGATIVE error                             |
| `@Validatable.Subtype(name = "PHONE")` (enum discriminator)           | Constant color + Ctrl+Click                    |
| `@ConstraintComposition(OR)` with 1 leaf / presence / nested composed | Declaration-site error                         |
| `@Regex(pattern = "…")`                                               | RegExp syntax highlighting                     |
| `@ApiError(code = "…", catalog = Errors::class)`                      | Enum DX when catalog is valid                  |

---

## Build, run, and test

### Install the plugin ZIP

1. Build the plugin (commands below), or download a released ZIP.
2. In IntelliJ IDEA: **Settings → Plugins → ⚙️ → Install Plugin from Disk…**
3. Choose `validata-intellij/build/distributions/validata-intellij-<version>.zip`
4. Restart the IDE

**Requirements in the open project:** Kotlin sources with Validata annotations (markers must
resolve on the **module** classpath). For `@ApiError` catalog DX, OpenAPI presentation types must
be on that module’s classpath. The plugin is **not** a Gradle dependency of your app — library
setup is in the [root README](../README.md#installation).

This folder is a **Gradle composite** (`includeBuild("validata-intellij")` from the repo root).
It resolves `validata-schema` from **mavenLocal** (included builds cannot wait on a sibling
publish in the same Gradle invocation).

| File                                     | Role                                                      |
|------------------------------------------|-----------------------------------------------------------|
| `gradle.properties`                      | Coordinates, IDEA / Platform / Kotlin pins                |
| `settings.gradle.kts`                    | Repositories (`mavenLocal` first), parent version catalog |
| `build.gradle.kts`                       | Platform dependencies, plugin packaging, sandbox tweaks   |
| `src/main/resources/META-INF/plugin.xml` | Extension registrations                                   |

```bash
# From the repo root — publish schema, then work in this composite:
./gradlew :validata-schema:publishToMavenLocal
./gradlew -p validata-intellij build
./gradlew -p validata-intellij test
./gradlew -p validata-intellij runIde          # sandbox IDE
./gradlew -p validata-intellij buildPlugin     # ZIP under build/distributions/
```

**Tests:**

- Pure JUnit 5 unit tests for schema-aligned logic (`ConstraintLiteralFormats`, marker parsers, …)
- IntelliJ platform light tests (`ValidataLightPlatformTestCase`) for discovery, resolve,
  completion, annotators, injectors
- Shared stubs in `src/test/kotlin/.../support/` (`PropertyRefLightFixtures`, `ApiErrorLightFixtures`)
- Architecture: `PackageBoundaryTest` (DAG)
- They do **not** execute runtime validators and do **not** replace a real ZIP install smoke

Headless platform tests disable the Ultimate licensing plugin in the test sandbox (unified IDEA
2026.2+); that is a test-harness detail only.

---

## Package layout

Internal packages follow a strict DAG (nothing points upward against the diagram above):

| Package           | Responsibility                                                                              |
|-------------------|---------------------------------------------------------------------------------------------|
| `contract`        | FQCN strings for classpath discovery (`PropertyRefLibraryFqns`, OpenAPI presentation FQCNs) |
| `path`            | Segment split + IDE text ranges on top of schema `PropertyPath`                             |
| `typing`          | `ValidatorTypeView` and related type views shared by discovery / analysis                   |
| `model`           | Host DTOs (`PropertyRef` / `ConstraintArg` metadata) — no reverse imports                   |
| `discovery.cache` | `ConstraintDiscoveryCache` + cached result / entry types                                    |
| `discovery`       | `PsiClassLookup`; subpackages for property-ref, constraint-arg, validatedBy, composition    |
| `analysis`        | Scalar kinds, validator fit, literals, `@Validatable`, `@ApiError`                          |
| `scope`           | Sibling / flat-params / element-root resolution                                             |
| `resolve`         | `PsiReference` implementations (paths + enum literals)                                      |
| `editor`          | Annotators, completion, references, injectors, colors — wired from `plugin.xml`             |

Suggested reading order for contributors:

1. `editor/reference/PropertyRefReferenceContributor` + `plugin.xml`
2. `discovery/propertyref/PropertyRefAttributeDiscovery` + `PropertyRefAnnotationMatcher`
3. `discovery/constraintarg/ConstraintArgAttributeDiscovery` + `ConstraintArgMarkerParser`
4. `discovery/composition/ConstraintCompositionDiscovery`
5. `discovery/cache/ConstraintDiscoveryCache`
6. `path/PropertyPathSegments` → `scope/PropertyRefOwnerResolver`
7. `analysis/compat/ValidatorTypeCompatibility` → `analysis/literal/ConstraintLiteralFormats`
8. Annotators under `editor/annotator/` (thin EP adapters over analysis / discovery)

---

## Contributor guidelines

Treat this README as the contract for changing the module.

1. **KSP parity first.** Editor rules must stay aligned with `validata-processor` (paths, literals,
   composition OR rules, Validatable reconciler, ApiError catalog). When processor wording or
   matrices change, update the twin analysis here and add a light test that fails without the fix.
2. **No per-constraint allowlists.** Discovery is marker-driven. New builtins (e.g. `@In` / `@NotIn`) light
   up when `@ConstraintArg` / `@PropertyRef` appear on the classpath — do not special-case names
   in annotators.
3. **Respect the package DAG.** Leaves (`contract`, `path`, `typing`, `model`, `editor.color`)
   must not depend on `discovery` or `editor` EP packages. Prefer extracting shared types over
   KDoc-only reverse imports. `PackageBoundaryTest` must stay green.
4. **Keep annotators thin.** Parsing and classification live in `discovery` / `analysis`;
   `editor.annotator` paints findings and wires severity / colors.
5. **Cache carefully.** Prefer `ConstraintDiscoveryCache` for FQCN-keyed discovery. Do not invent
   unbounded PSI maps. Library sticky prefix is `io.ghaylan.validata.` — extend only with evidence.
6. **Compile dependency is `validata-schema` only.** Do not add `validata-core` as a compile dep;
   markers come from the user’s project. Short-name / file-walk fallbacks go through
   `PsiClassLookup` (capped / cancellable).
7. **Tests.** Pure logic → JUnit 5 without platform. PSI behavior → `ValidataLightPlatformTestCase`
    + fixtures. Every bug fix needs a regression that fails without the fix.
8. **Do not publish** Marketplace / Maven from casual work unless explicitly requested. Build the
   ZIP locally with `buildPlugin`.
9. **Versions.** Keep IDEA / Platform / Kotlin pins in this composite’s `gradle.properties`
   aligned with the [root README](../README.md) version table; `PluginConstants.PLUGIN_ID`
   must match `build.gradle.kts` / patched `plugin.xml`.

---

## Extension points

Registered in `META-INF/plugin.xml` (id / version / description / vendor patched from
`build.gradle.kts`):

| Extension                                          | Role                                                                                           |
|----------------------------------------------------|------------------------------------------------------------------------------------------------|
| `psi.referenceContributor`                         | Property paths, enum literals, `@Validatable` strings, `@ApiError` codes                       |
| `annotator` ×7                                     | Paths, subject types, **composition**, missing metadata, literals, `@Validatable`, `@ApiError` |
| `completion.contributor` + `completion.confidence` | Basic completion + autopopup inside Validata string hosts                                      |
| `typedHandler`                                     | Schedules autopopup while typing in those hosts                                                |
| `multiHostInjector`                                | RegExp language into `@ConstraintArg(REGEX)` strings                                           |
| `additionalTextAttributes`                         | Default + Darcula temporal color defaults                                                      |

Depends on: `com.intellij.modules.platform`, `com.intellij.java`, `org.jetbrains.kotlin`.

---

## Rules to remember

1. This is a **plugin ZIP**, not an `implementation(...)` dependency of your app.
2. Compile-time generation / verification remains authoritative; the plugin mirrors rules in the IDE.
3. Shared contracts come from **`validata-schema`** (bundled in the ZIP); annotation markers are
   discovered by FQCN on the **user** project classpath.
4. String paths get IDE help; Fields constants do not (they are already Kotlin references).
5. Only single-segment sibling / element paths are supported — nested `a.b` is an editor error.
6. Custom constraints light up when authors mark parameters with `@PropertyRef` / `@ConstraintArg`.
7. Composed annotations follow `@ConstraintComposition` AND/OR rules (OR authoring: ≥2 leaves,
   no presence, no nested composed in v1). Subject-type fit still requires every leaf to accept
   the subject.
8. Keep IDEA / Platform pins in this composite’s `gradle.properties` aligned with the
   [root README](../README.md) version table.

---

## What it does not do

| Not handled here                               | Belongs elsewhere                                     |
|------------------------------------------------|-------------------------------------------------------|
| Runtime validation (“is this email valid?”)    | Runtime / host modules                                |
| Failing the build on bad paths or bad args     | Compile-time processor                                |
| Spring MVC / HTTP status / error JSON          | Spring host module                                    |
| Walking generated `ObjectSchema` / endpoint IR | Runtime + schema consumers                            |
| Compiling against `validata-core`              | Discovers markers from the **user** classpath instead |
| Bean Validation (`jakarta.validation`)         | Not used                                              |
| Marketplace publishing automation              | Manual ZIP install (or your release pipeline)         |
| Nested property paths (`"address.city"`)       | Rejected by design today                              |

**Rule of thumb:** if it runs validation or ships inside your application jar, it is not this
module. This is editor-only, opt-in DX.

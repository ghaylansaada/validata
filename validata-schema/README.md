# validata-schema

**Maven coordinate:** `io.github.ghaylansaada:validata-schema`  
**Artifact:** shared validation IR (intermediate representation) — blueprints only, no Spring.

| Piece        | Value                                                                  |
|--------------|------------------------------------------------------------------------|
| Runtime deps | **none** (zero production dependencies; stdlib only — no `kotlin-reflect`) |
| JPMS name    | `io.ghaylan.validata.schema` (`Automatic-Module-Name`)                 |

Version / JDK / Kotlin pins: [root README](../README.md) (top table).

This jar defines the data structures every Validata layer agrees on: object graphs,
endpoint layouts, constraint handles, path/scalar contracts, and SPI loaders. It does **not** execute validators, scan
annotations, or talk to Spring.

---

## Contents

1. [Why this module exists](#why-this-module-exists)
2. [Why it is a separate module](#why-it-is-a-separate-module)
3. [Role in the framework](#role-in-the-framework)
4. [How it works](#how-it-works)
5. [Quick start](#quick-start)
6. [Public API surface](#public-api-surface)
7. [Package layout](#package-layout)
8. [Extension points](#extension-points)
9. [Error handling & diagnostics](#error-handling--diagnostics)
10. [Testing notes](#testing-notes)
11. [Rules to remember](#rules-to-remember)
12. [What it does not do](#what-it-does-not-do)

Gradle setup and configuration: [root README](../README.md#installation).

---

## Why this module exists

Validata is a compile-time → runtime pipeline. Every stage must describe the *same*
validated type or endpoint:

- producers emit schemas (normally KSP)
- consumers walk those schemas (engine, HTTP host, docs, IDE)

Without one shared model, each layer invents its own format. Those formats drift —
“looks fine in the IDE, fails at runtime.” `validata-schema` is that shared floor plan:
structures that describe **what** to check. Other modules decide **how** and **when**.

---

## Why it is a separate module

| Isolation goal                      | Effect                                                                                     |
|-------------------------------------|--------------------------------------------------------------------------------------------|
| No Spring / Jackson / servlet types | Producers and the engine can depend on IR without the web stack                            |
| No `kotlin-reflect`                 | Closed-world bytecode readers (`ValueReader`) stay cheap                                   |
| Zero runtime dependencies           | Tiny jar for KSP, IDE tooling, and non-Spring hosts                                        |
| Independent publish                 | Tooling can version against schema alone                                                   |
| Clear boundary                      | This module’s `build.gradle.kts` has no Spring / Jackson / servlet deps — keep it that way |

Apps that use the Spring host normally get this jar **transitively**. Depend on
`validata-schema` **directly** only when you build or read IR outside that path (custom tooling, tests, non-Spring
hosts, hand-written schemas).

---

## Role in the framework

```text
annotate types / endpoints
         │
         ▼
   schema producers          emit ObjectSchema / EndpointSchema
         │                   (using types from this module)
         ▼
┌─────────────────────┐
│  validata-schema    │  shared blueprints + path/ref helpers + SPI
│  ← YOU ARE HERE     │
└─────────┬───────────┘
          │
          ▼
   schema consumers          walk / bind / document / mirror the same IR
```

**This module’s job:** types, indexes, path helpers, scalar-compatibility vocabulary,
and classpath merge of SPI contributions.

**Not this module’s job:** generating schemas from source, running constraints, HTTP
binding, OpenAPI publishing, or IDE PSI.

---

## How it works

### Object graphs

1. A producer implements `ObjectSchemaModule` and registers it under
   `META-INF/services/io.ghaylan.validata.schema.spi.ObjectSchemaModule`.
2. `GeneratedSchemas` discovers every module via `ServiceLoader`, merges contributions,
   and **fails loudly** on duplicate `Class` keys (both contributors named).
3. Consumers call `GeneratedSchemas.get(MyDto::class.java)` or `GeneratedSchemas.all()`.
4. The classpath snapshot is cached for the classloader lifetime until
   `GeneratedSchemas.resetForTests()` (test isolation only).

### Endpoint graphs

Same pattern with `RequestSchemaModule` → `GeneratedRequestSchemas.get(endpointId)` /
`all()`. Endpoint ids match `Method.getUniqueIdentifier()`-style coordinates (`pkg.Class#method(pkg.Type1,pkg.Type2)`).

### Constraint handles

Nothing here *executes* a check. A `CompiledConstraint` pairs a `ConstraintConfig` with a
`ConstraintRunner`. Concrete configs and validators live in consuming modules; this jar
only defines the IR slots they fill.

### Field vs type-use constraints

| Where                      | Typical content                                                   |
|----------------------------|-------------------------------------------------------------------|
| `PropertySpec.constraints` | Property-level rules (`@Required`, `@Size` on the field)          |
| `TypeShape.constraints`    | Type-use rules (`List<@Email String>`, map key/value annotations) |

Element constraints on collections/maps live on the nested shape (`IterableShape.element`,
`MapShape.key` / `MapShape.value`), not on the property’s constraint list.

---

## Quick start

Hand-built schema (tests / learning — production code normally gets this from a generator).
The `ValueReader` cast matches KSP output (`{ (it as Owner).field }`):

```kotlin
data class User(val email: String?)

val schema = ObjectSchema(
    type = User::class.java,
    properties = listOf(
        PropertySpec(
            declaredName = "email",
            externalName = "email",
            shape = ScalarShape(ScalarKind.STRING),
            read = { (it as User).email },
        ),
    ),
)

val value = schema.byDeclaredName.getValue("email").read.read(User("a@b.c"))
```

Self-referencing object (lazy `ObjectRefShape` — cycle in the schema graph, not infinite
build-time expansion):

```kotlin
lateinit var nodeSchema: ObjectSchema
nodeSchema = ObjectSchema(
    type = Node::class.java,
    properties = listOf(
        PropertySpec(
            declaredName = "child",
            externalName = "child",
            shape = ObjectRefShape(lazy { nodeSchema }),
            read = { (it as Node).child },
        ),
    ),
)
// Equivalent publication-safe handle: nodeSchema.selfRef
```

Classpath lookup after SPI registration:

```kotlin
GeneratedSchemas.get(User::class.java)
GeneratedSchemas.all()

GeneratedRequestSchemas.get(endpointId)
GeneratedRequestSchemas.all()
```

---

## Public API surface

### Object IR

| Type                                             | Role                                                                                             |
|--------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `ObjectSchema`                                   | One DTO/class: `type`, frozen `properties` (declaration order), frozen `subtypes` (polymorphism) |
| `ObjectSchema.byDeclaredName` / `byExternalName` | Lazy 1:1 indexes (uniqueness enforced at construction)                                           |
| `ObjectSchema.selfRef`                           | Lazy `ObjectRefShape` pointing at this schema (publication-safe)                                 |
| `ObjectSchema.toString`                          | Compact diagnostic: `ObjectSchema(fqcn, props=N, subtypes=M)`                                    |
| `PropertySpec`                                   | One property: names, shape, `ValueReader`, property constraints, optional `errorDocs`            |
| `ValueReader`                                    | `fun interface` — `read(container: Any): Any?` (closed-world cast + access; not generic)         |

`declaredName` is the source / `@PropertyRef` spelling. `externalName` is the wire /
error-path spelling (e.g. `@JsonProperty`). They may differ.

Construction rejects duplicate `declaredName` or `externalName` values with
`IllegalStateException`. Caller lists/maps passed into the constructor are **copied and
frozen** — later mutation of those collections does not change the schema or its indexes.

`ObjectSchema` is intentionally **non-generic**: SPI maps, nested `ObjectRefShape` graphs,
and subtype dispatch are heterogeneous. Typed readers would erase at the engine boundary
anyway; KSP emits `{ (it as Owner).field }` and hand-written IR should match that shape.

### Type shapes

`TypeShape` is a sealed interface. Every variant carries `constraints` (type-use).

| Shape                                     | Meaning                                                                                                            |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `ScalarShape(kind)`                       | Leaf: string, number, temporal, enum, …                                                                            |
| `ObjectRefShape(ref: Lazy<ObjectSchema>)` | Nested object; `Lazy` enables self-reference (FQN under `shape/`)                                                  |
| `IterableShape(element)`                  | List / set / collection / array                                                                                    |
| `MapShape(key, value)`                    | Map with independent key and value shapes                                                                          |
| `DynamicShape`                            | Runtime-only structure (`Any`, unannotated interface); consumers resolve via `GeneratedSchemas` or treat as opaque |

### `ScalarKind`

Coarse leaf family for cross-field checks and tooling (not the concrete Java type):

| Kind       | Typical leaves                     |
|------------|------------------------------------|
| `BOOLEAN`  | `Boolean` / `boolean`              |
| `CHAR`     | `Char` / `char`                    |
| `STRING`   | `String` / CharSequence-like       |
| `INTEGRAL` | `Int`, `Long`, `Short`, `Byte`, …  |
| `DECIMAL`  | `Float`, `Double`, `BigDecimal`, … |
| `TEMPORAL` | `java.time.*`, `Date`, `Calendar`  |
| `ENUM`     | enum constants                     |
| `UUID`     | `java.util.UUID`                   |
| `OTHER`    | opaque / unclassified leaf         |

### Shared type contracts (`types/`)

Single source of truth for FQCN catalogs and pure classification / literal rules used by
KSP, IntelliJ, and the runtime. Hosts extract an FQCN from `KSType` / PSI / `Class`, then call:

| Type / API | Role |
|------------|------|
| `TypeNames` | FQCN / package-prefix constants |
| `TypeTables` | Lookup sets/maps (numeric, temporal hosts, leaf packages, …) |
| `KnownTypes` | Canonicalize, family predicates, JVM erased names |
| `ScalarKinds` | FQCN → `ScalarKind`, scalar / platform-leaf checks |
| `ConstraintLiteralRules` | Number / temporal / typed-literal parse checks |
| `TypedLiteralResult` | Shared verdict (hosts format diagnostics) |
| `BuiltinTypeShortNames` | Short-name → FQCN fallbacks for unresolved PSI |

Do **not** re-list leaf FQCNs in processor / IntelliJ / core — extend these catalogs instead.

### Constraint IR (handles only)

| Type                 | Role                                                                                                    |
|----------------------|---------------------------------------------------------------------------------------------------------|
| `CompiledConstraint` | `metadata` + `runner` + declaration `order` (producers sort `@Required` first)                          |
| `ConstraintConfig`   | Interface: `message`, `groups: Set<KClass<*>>` — concrete types live elsewhere                          |
| `ConstraintRunner`   | `fun interface` — `run(value, config) → ConstraintFailure?`                                             |
| `ConstraintFailure`  | `code` / `message` / `constraint` — **no path / location** (engine stamps those on wire `ConstraintError`) |

Groups on `ConstraintConfig` use `KClass` from kotlin-stdlib only (no `kotlin-reflect` dependency).

### Request IR

| Type                   | Role                                                                   |
|------------------------|------------------------------------------------------------------------|
| `EndpointSchema`       | One `@Validate` endpoint: transport sections + flags + argument layout |
| `EndpointArgumentSlot` | Positional handler-parameter slot (`kind` + optional `name`)           |
| `EndpointArgumentKind` | `BODY` / `QUERY` / `HEADER` / `PATH` / `OTHER`                         |

`EndpointSchema` fields:

| Field                                       | Meaning                                                                                |
|---------------------------------------------|----------------------------------------------------------------------------------------|
| `id`                                        | Endpoint coordinate (`Method.getUniqueIdentifier()`-style)                             |
| `pathVariables` / `headers` / `queryParams` | Section `ObjectSchema`s, or `null` when empty                                          |
| `requestBody`                               | Body `ObjectSchema`, or `null` when absent                                             |
| `oneErrorPerParam`                          | When `true`, stop after the first failure on a single param (default `true`)           |
| `failFast`                                  | When `true`, stop after the first failure anywhere in the request (default `false`)    |
| `groups`                                    | Active validation groups from `@Validate` (required; no default)                       |
| `argumentLayout`                            | Positional map of handler parameters → transport roles (`OTHER` keeps indices aligned) |

Constant names on `EndpointArgumentKind` mirror common HTTP annotations by *name only* —
this module does not import Spring types.

### SPI

| Type                      | Role                                                                               |
|---------------------------|------------------------------------------------------------------------------------|
| `ObjectSchemaModule`      | One compilation unit’s `Map<Class<*>, ObjectSchema>`                               |
| `GeneratedSchemas`        | ServiceLoader merge + classloader-lifetime cache (`get` / `all` / `resetForTests`) |
| `RequestSchemaModule`     | One compilation unit’s `Map<String, EndpointSchema>`                               |
| `GeneratedRequestSchemas` | Same pattern for endpoint ids                                                      |

Merged maps are **unmodifiable**. Duplicate keys throw `IllegalStateException` naming both
contributors (shared wording via internal `SchemaSpiDiagnostics`).

Internals (not public API): `ClasspathSchemaCache` (shared lock/reset semantics),
`ServiceLoaderMerge` (merge + `mapCapacity`), `SchemaSpiDiagnostics` (duplicate-message text).

`resetForTests()` is **public on purpose** — dependent modules’ tests call it for isolation.
Production code never resets the cache.

### Path helpers (`PropertyPath`)

Lives in the root package (`io.ghaylan.validata.schema`), not under `schema.ref`.

| Member                                  | Role                                                                          |
|-----------------------------------------|-------------------------------------------------------------------------------|
| `PropertyPath.split`                    | Dotted path → non-blank trimmed segments (empty when blank / dots-only)       |
| `PropertyPath.findProperty`             | Lookup by `declaredName`, else `externalName` (**declared wins** on conflict) |
| `PropertyPath.read`                     | Walk `ObjectRefShape` segments; call `ValueReader` per step                   |
| `PropertyPath.MAX_REFERENCE_PATH_DEPTH` | `6` — max segment count for cross-field refs                                  |

`PropertyPath.read` behavior:

- `null` root instance → `null` without walking
- Blank / empty segment list → `IllegalArgumentException`
- Depth &gt; `MAX_REFERENCE_PATH_DEPTH` → `IllegalArgumentException`
- Unknown segment → `IllegalStateException` (lists up to **20** declared names, then `… +N more`)
- Mid-path non-`ObjectRefShape` → `IllegalStateException`

Not a JSONPath engine and does not validate values.

### Shared tooling contracts (`schema.ref`)

| Type                             | Role                                                                                                    |
|----------------------------------|---------------------------------------------------------------------------------------------------------|
| `PropertyRefScalarCompatibility` | Pure scalar matrix + diagnostic messages (string and `ScalarKind` overloads)                            |
| `PropertyRefCompatibilityKind`   | `NONE` / `SAME_SCALAR_KIND` / `COMPARABLE_FAMILY`                                                       |
| `PropertyRefScope`               | Where a `@PropertyRef` path resolves: `SIBLING` / `ELEMENT`                                             |
| `ConstraintArgKind`              | Metadata-argument rules: `NOT_BLANK`, `NON_EMPTY`, `TYPED_LITERAL`, `NON_NEGATIVE`, `POSITIVE`, `REGEX` |
| `ConstraintArgTarget`            | Where an arg rule applies: `VALUE` / `ELEMENT`                                                          |

`PropertyRefScalarCompatibility` rules (interpreted in this module):

| Kind                | Pairwise (`isCompatible`)        | Subject gate (`isSubjectCompatible`) |
|---------------------|----------------------------------|--------------------------------------|
| `NONE`              | Always true                      | Always true                          |
| `COMPARABLE_FAMILY` | Both numeric, both `TEMPORAL`, or same orderable kind (`STRING`/`CHAR`/`ENUM`/`UUID`) | Subject is orderable (not `BOOLEAN`/`OTHER`) |
| `SAME_SCALAR_KIND`  | Identical kinds, or both numeric | Subject ≠ `OTHER`                    |

Prefer the `ScalarKind` overloads when both sides already have IR kinds; string overloads take
`ScalarKind.name` values (`"INTEGRAL"`, …). Mismatch helpers return `""` for `NONE`.

These enums are a **discovery contract** for KSP and IDE tooling: entry names should stay
stable across releases. Only `PropertyRefCompatibilityKind` is interpreted here (via
`PropertyRefScalarCompatibility`). `PropertyRefScope`, `ConstraintArgKind`, and
`ConstraintArgTarget` are vocabulary only — annotation metadata that *declares* which rule
applies lives in consuming modules (processor / IDE / core).

### Docs-only IR

| Type             | Role                                                                                    |
|------------------|-----------------------------------------------------------------------------------------|
| `SchemaErrorDoc` | Property-level error documentation (`code`, optional `message`, optional `catalogFqcn`) |

Carried on `PropertySpec.errorDocs`. Ignored by validation walks — documentation consumers
only. Blank `message` with a `catalogFqcn` means “resolve text from the catalog at docs time.”

---

## Package layout

```text
io.ghaylan.validata.schema
├── ObjectSchema, PropertySpec, PropertyPath, ValueReader
├── constraint/     CompiledConstraint, ConstraintConfig, ConstraintRunner, ConstraintFailure
├── shape/          TypeShape + ScalarKind + concrete shapes (incl. ObjectRefShape)
├── types/          Shared FQCN catalogs + ScalarKinds + ConstraintLiteralRules (tooling/runtime SSOT)
├── request/        EndpointSchema, EndpointArgumentSlot, EndpointArgumentKind
│   └── spi/        RequestSchemaModule, GeneratedRequestSchemas
├── spi/            ObjectSchemaModule, GeneratedSchemas
│                   (+ internal: ClasspathSchemaCache, ServiceLoaderMerge, SchemaSpiDiagnostics)
├── ref/            PropertyRef* + ConstraintArg* tooling contracts
└── docs/           SchemaErrorDoc
```

---

## Extension points

### 1. Contribute object schemas (SPI)

```kotlin
class ThirdPartySchemas : ObjectSchemaModule {
    override fun schemas(): Map<Class<*>, ObjectSchema> =
        mapOf(ExternalDto::class.java to externalSchema)
}
```

Register:

```text
META-INF/services/io.ghaylan.validata.schema.spi.ObjectSchemaModule
→ fully.qualified.ThirdPartySchemas
```

Duplicate keys across modules throw at first `GeneratedSchemas.all()` / `get()`.

### 2. Contribute endpoint schemas (SPI)

Implement `RequestSchemaModule` and register under
`META-INF/services/io.ghaylan.validata.schema.request.spi.RequestSchemaModule`.

### 3. Hand-built graphs

Construct `ObjectSchema` / `EndpointSchema` / shapes / `CompiledConstraint` directly in
tests or custom hosts. Prefer a compile-time generator for production application types.

There are **no Spring beans to override** — this module is not auto-configured. It exposes no
Spring Boot properties or `application.yml` keys; limits and HTTP policy live in consuming modules
(see [root README Configuration](../README.md#configuration)).

---

## Error handling and diagnostics

| Surface                                             | When                                                    | Guidance                                                                        |
|-----------------------------------------------------|---------------------------------------------------------|---------------------------------------------------------------------------------|
| `IllegalStateException` from `ObjectSchema` init    | Duplicate `declaredName` or `externalName`              | Emit unique names from the producer / `ObjectSchemaModule`                      |
| `IllegalStateException` from SPI merge              | Two modules claim the same `Class` or endpoint id       | Remove the duplicate registration (message names both owners)                   |
| `IllegalArgumentException` from `PropertyPath.read` | Blank path or depth &gt; `MAX_REFERENCE_PATH_DEPTH` (6) | Shorten the path                                                                |
| `IllegalStateException` from `PropertyPath.read`    | Unknown segment or non-object mid-path                  | Fix the name; unknown-segment diagnostics list ≤20 declared names then truncate |
| `PropertyRefScalarCompatibility` messages           | Tooling inline diagnostics                              | Short “what’s wrong + how to fix” one-liners                                    |
| `SchemaErrorDoc`                                    | Documentation text only                                 | Not a runtime validation failure                                                |

Diagnostic strings from the path/scalar helpers are intentionally short enough for inline
IDE display.

---

## Testing notes

```bash
./gradlew :validata-schema:test
```

What these tests protect:

- Path grammar, depth ceiling, declared-over-external lookup, known-name truncation (`PropertyPath`)
- Scalar compatibility matrix + `ScalarKind` overloads (`PropertyRefScalarCompatibility`)
- Shared type catalogs + literal rules (`TypeNames` / `KnownTypes` / `ScalarKinds` / `ConstraintLiteralRules`)
- SPI merge fail-loud and unmodifiable caches (`GeneratedSchemas`, `GeneratedRequestSchemas`)
- Frozen `ObjectSchema` collections, indexes, `selfRef`, `toString`
- IR construction / retention (shapes, docs fields, endpoint slots, constraint handles)
- Module boundary: main sources must not import Spring / Jackson / servlet packages

What they do **not** answer: “is this email valid?” — that is engine/validator territory.
They also do **not** snapshot `schema.ref` enum entry-name lists — rename stability is enforced
by processor / IDE consumers and release review.

Tips when testing code that depends on this module:

- Call `GeneratedSchemas.resetForTests()` / `GeneratedRequestSchemas.resetForTests()` between
  classpath manipulations in the same JVM (public API for dependents’ tests).
- Register test SPI modules under `src/test/resources/META-INF/services/…`.
- Hand-build small `ObjectSchema` graphs with `ValueReader` casts matching KSP output;
  duplicate property names fail at construction.

---

## Rules to remember

1. This jar is types + shared path/ref helpers — not the engine.
2. `declaredName` (source / `@PropertyRef`) can differ from `externalName` (JSON / error path);
   path lookup prefers declared when both match.
3. Field constraints ≠ type-use / element constraints (`List<@Email String>`).
4. `ObjectRefShape` uses `Lazy` so self-referencing models form a cycle, not an infinite tree.
   The `schema` ↔ `shape` package edge (`ObjectSchema` ↔ `ObjectRefShape`) is intentional IR —
   keep the `ObjectRefShape` FQN under `schema.shape` stable for KSP/`CodegenFqns`.
5. `ObjectSchema` freezes `properties` / `subtypes` at construction — mutating caller collections
   afterward does not change the schema or its indexes.
6. Duplicate schema keys and duplicate property names fail loudly — no silent overwrite.
7. `schema.ref` enum **names** are a tooling discovery contract — keep them stable across releases (this module does not
   unit-test the name list; processor / IDE consumers are the real check).
8. `ValueReader` takes `Any` on purpose — closed-world cast in the lambda; do not genericize
   `ObjectSchema` for ergonomics alone.
9. Never add Spring / Jackson / servlet types or dependencies here.

---

## What it does not do

| Not handled here                       | Belongs elsewhere                            |
|----------------------------------------|----------------------------------------------|
| Checking if a value is valid           | validation engine / validators               |
| Scanning annotations at runtime        | nowhere — schemas are compiled ahead of time |
| Generating schemas from your source    | compile-time processor                       |
| Spring MVC / HTTP status / error JSON  | Spring host module                           |
| OpenAPI document enrichment            | OpenAPI companion                            |
| Bean Validation (`jakarta.validation`) | not used                                     |

**Rule of thumb:** if it *executes* a check or *talks to Spring*, it isn’t this module.

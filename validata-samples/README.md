# validata-samples

Runnable Spring Boot **acceptance app** for the Validata stack. It is a normal Gradle module (`:validata-samples`), not
a nested `samples/` demo and **not a published Maven artifact**.

Clone it, run it, copy from it. It is the place that proves the host, the engine, KSP, and
optional OpenAPI actually compose on one classpath.

**Maven coordinate:** none. Root `publishToMavenLocal` omits this module by design.

---

## Contents

1. [Why this module exists](#why-this-module-exists)
2. [Why it is a separate module](#why-it-is-a-separate-module)
3. [How it works](#how-it-works)
4. [How other modules depend on it](#how-other-modules-depend-on-it)
5. [Quick start](#quick-start)
6. [Full usage](#full-usage)
7. [Extension points](#extension-points)
8. [Sample properties](#sample-properties)
9. [Error handling](#error-handling)
10. [Testing notes](#testing-notes)

Gradle setup for Validata in a real app: [root README](../README.md#installation).

---

## Why this module exists

A published library must not ship a Boot application, Graal native-image config, or a
kitchen-sink DTO. Newcomers still need one place that shows, end to end:

- `@Validate` on JSON bodies and on query / header / path parameters
- `@Validate` on `@ConfigurationProperties` (startup fail-closed)
- every built-in constraint plus Option-2 custom constraints discovered closed-world
- app-owned HTTP error JSON
- springdoc `/v3/api-docs` when OpenAPI is on the classpath
- the same assertions as a GraalVM contract when native-image is available

Without this module, those proofs would leak into `:validata` / `:validata-core`, or they
would live as throwaway gists that drift from the real APIs.

---

## Why it is a separate module

| Isolation goal       | Effect                                                                  |
|----------------------|-------------------------------------------------------------------------|
| Unpublished demo     | Library jars stay free of Boot apps, springdoc, and sample DTOs         |
| Realistic classpath  | Jsoup and libphonenumber are present so `@Html` / `@Phone` actually run |
| Graal / AOT          | Native processing runs against a real `@SpringBootApplication`          |
| Copy-paste reference | Feature packages match how a production Boot app is laid out            |

---

## How it works

This module is a composition harness, not an engine. Three paths share the same compile-time
schemas:

1. **Request path.** Spring binds the HTTP arguments. Validata then checks them against the
   generated schema and throws if anything fails. The sample maps that exception into its own
   JSON envelope — Validata does not own the wire shape.
2. **Startup path.** The same `@Validate` marker on configuration properties runs after the
   bean is bound. Invalid config fails context refresh instead of serving traffic.
3. **Docs path.** With the OpenAPI companion on the classpath, `/v3/api-docs` overlays native
   JSON Schema facets, custom constraint payloads, and documented business errors. Runtime
   validation does not consult Swagger.

Custom constraints are annotation + validator only. KSP emits metadata and a catalog SPI at
compile time (closed-world). Optional documenters teach OpenAPI how to describe those custom
rules so they are not listed as unmapped.

---

## How other modules depend on it

They do **not**. No Gradle project imports types from this module.

The inversion is the point: the sample depends **outward** on `:validata` (auto-config,
`@Validate`, startup failures), `:validata-openapi` (presentation annotations, publishers,
constraint documenters), and `ksp(:validata-processor)`.

`:validata-core` and the **root** README copy the OddYears / SampleFloor authoring snippets
from here. Those copies are documentation, not compile-time consumers. The IntelliJ plugin
and processor tests do not compile against this app.

---

## Quick start

There is no Maven coordinate — clone the repo and run from the root:

```bash
# JVM tests (always)
./gradlew :validata-samples:test

# Run the app
./gradlew :validata-samples:bootRun
```

Then `POST /api/users` and `GET /v3/api-docs` against the local server.

Create-user (quick-start API):

```http
POST /api/users
Content-Type: application/json

{"first_name":"Ada","minAge":5,"maxAge":30}
```

Lookup (path + query + header share the same schema IR as bodies):

```http
GET /api/users/ada?q=hi
X-Tenant: acme
```

KSP also emits wire-path constants you can import from `<dtoPackage>.ghaylan.validata`:

- `CreateUserRequest_` — JSON body field paths (`FIRST_NAME`, …)
- `UserController_lookup_<fingerprint>_` — flat path / query / header names (`USER_ID`, `Q`, `TENANT`)

Kitchen-sink (every built-in constraint + OddYears):

```http
POST /api/all-constraints
```

---

## Full usage

Package layout (copy the slice you need):

| Package                       | Role                                                                                              |
|-------------------------------|---------------------------------------------------------------------------------------------------|
| `io.ghaylan.validata.samples` | `@SpringBootApplication` + `main`                                                                 |
| `…samples.user`               | Quick-start API: `UserController`, `CreateUserRequest` (`first_name` wire name)                   |
| `…samples.matrix`             | Kitchen-sink DTO + `AllConstraintsController`                                                     |
| `…samples.metadata`         | Option-2 `OddYears` / `SampleFloor` + composed `EmailOrPhone` + OpenAPI `ConstraintDocumentation` |
| `…samples.error`              | Envelope, catalog, `@RestControllerAdvice`                                                        |
| `…samples.config`             | `sample.app.*` properties + sample-owned `ErrorDocPublisher` `@Bean`                              |

HTTP paths that must stay stable: `/api/users`, `/api/users/{userId}`, `/api/all-constraints`.

Annotation names **`OddYears`**, **`SampleFloor`**, and composed **`EmailOrPhone`**
(`@ConstraintComposition(OR)` on `AllConstraintsRequest.contact`) are the identifiers other
READMEs point at.

---

## Extension points

| What you copy               | How it is discovered                                                                                |
|-----------------------------|-----------------------------------------------------------------------------------------------------|
| `@Constraint` + validator   | KSP emits `{Ann}Constraint` + catalog SPI                                                           |
| `ConstraintDocumentation`   | Hand-written SPI under `META-INF/services/io.ghaylan.validata.openapi.docs.ConstraintDocumentation` |
| `ErrorDocPublisher` `@Bean` | Opt-in; Validata does not ship an envelope publisher                                                |
| `@ApiError`                 | Baked into schema IR / OpenAPI `x-validata-errors` and examples                                     |

Prefer a `ConstraintDocumentation` for generated metadata over an OpenAPI-only mapper.
See [validata-openapi/README.md](../validata-openapi/README.md).

---

## Sample properties

Prefix `sample.app.*`:

| Key                    | Type   | Default                           | Constraints                         |
|------------------------|--------|-----------------------------------|-------------------------------------|
| `sample.app.tenant-id` | String | `acme` (`application.properties`) | `@Required`, `@Size(min=2, max=32)` |

Constructor-bound (`val`). A value shorter than 2 characters fails context refresh.

This module exposes no `validata.*` properties of its own; host limits come from `:validata`.

---

## Error handling

| Failure                            | HTTP                                       | Body                                                                                       |
|------------------------------------|--------------------------------------------|--------------------------------------------------------------------------------------------|
| `ConstraintViolationException`     | 400                                        | Envelope with `errors` (path + constraint code). Message is the exception’s fixed summary. |
| `SampleBusinessException`          | Catalog status (e.g. 404 `USER_NOT_FOUND`) | Same envelope, empty `errors`                                                              |
| `ConfigurationValidationException` | —                                          | Startup only — the context does not refresh                                                |

Validata throws; the sample maps. That split is the product contract.

---

## Testing notes

This module **is** the acceptance suite. It is not a test-jar of fixtures for other modules.

```bash
# JVM (always) — includes NativeSurfaceIT
./gradlew :validata-samples:test

# Spring AOT processing (HotSpot; no native-image binary required)
./gradlew :validata-samples:processAot

# Full GraalVM native tests (requires a GraalVM JDK with `native-image` on PATH /
# GRAALVM_HOME; HotSpot alone fails at :nativeTestCompile with missing javaLauncher)
./gradlew :validata-samples:nativeTest
```

| Suite                               | What it locks                                                                                                                                        |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ValidationSamplesIT`               | Quick-start body, OddYears, cross-field refs, flat params, business 404                                                                              |
| `AllConstraintsIT`                  | Full violation table over MockMvc (acceptance for every built-in + OddYears + EmailOrPhone)                                                          |
| `AllConstraintsEngineTest`          | Short smoke: registry + `ValidatorEngine.validate` without MVC (not a second golden matrix)                                                          |
| `OpenApiDocsIT`                     | `first_name`, Size `minLength`, OddYears / SampleFloor `x-validata-constraints`, `@Compare` EQUAL → `confirm_secret`, contact OR composition, sample 400 docs |
| `SampleAppPropertiesValidationTest` | Invalid `tenant-id` fails startup (`ApplicationContextRunner`, not `@SpringBootTest`)                                                                |
| `NativeSurfaceIT`                   | Three-test Graal subset (config bound, reject, accept) — do not expand it                                                                            |

Both `NativeSurfaceIT` and the config-properties runner re-run under `:nativeTest` when Graal
is available.

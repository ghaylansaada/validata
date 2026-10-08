# validata

**Maven coordinate:** `io.github.ghaylansaada:validata`  
**Artifact:** Spring Boot WebMVC host — binds HTTP / config beans and calls the Spring-free engine.

| Piece     | Value                                                                  |
|-----------|------------------------------------------------------------------------|
| Hard deps | `validata-core` (API)                                                  |
| Provided  | Spring Boot Web, Jackson, servlet API (`compileOnly` — not transitive) |
| JPMS name | `io.ghaylan.validata` (`Automatic-Module-Name`)                        |

Version / JDK / Spring Boot pins: [root README](../README.md) (top table).

This is the jar Spring apps usually depend on. It re-exports `validata-core` as `api`. The host
checks `@Validate` handlers **after** Spring binds arguments and **before** the method runs,
loads KSP endpoint schemas at startup, optionally validates `@ConfigurationProperties`, and
rewrites selected Spring / Jackson binding failures into `ConstraintViolationException`.

**Analogy:** schema is the floor plan, core is the inspector, this module is the **front desk**.

It is the **only** library module allowed to import Spring (`ModuleBoundaryTest`). Host package
direction (`config` → `web` / `exception` / `bootstrap` / `aot`) is enforced by
`HostPackageBoundaryTest`.

---

## Contents

1. [Why this module exists](#why-this-module-exists)
2. [Why it is a separate module](#why-it-is-a-separate-module)
3. [Role in the framework](#role-in-the-framework)
4. [How it works](#how-it-works)
5. [Package layout](#package-layout)
6. [Quick start](#quick-start)
7. [Extension points](#extension-points)
8. [Configuration](#configuration)
9. [Error handling](#error-handling)
10. [Testing notes](#testing-notes)
11. [Rules to remember](#rules-to-remember)
12. [What it does not do](#what-it-does-not-do)

App dependency coordinates: [root Installation](../README.md#installation). KSP args
(`SNAKE_CASE`, cascade, …): [processor Configuration](../validata-processor/README.md#configuration)
and [root Configuration](../README.md#configuration).

---

## Why this module exists

So the engine can stay Spring-free. Without this host, every Boot app would discover
`@Validate` handlers, load endpoint schemas, replace handler invocation, rewrite binding
failures, and map config-property failures itself. `validata` does that wiring once.

---

## Why it is a separate module

| Isolation goal         | Effect                                                                                                                     |
|------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Spring only here       | `validata-core` stays usable in tests, OpenAPI, and non-Boot callers                                                       |
| One published host jar | Apps depend on a single coordinate for Boot + engine types                                                                 |
| Enforceable boundary   | Sibling `ModuleBoundaryTest`s keep Spring out of core/schema; `HostPackageBoundaryTest` keeps the host package DAG acyclic |

---

## Role in the framework

```text
You annotate DTOs / controllers
              │
              ▼
   validata-processor (KSP) → validata-schema (IR) → validata-core (engine)
              │
              ▼
   validata   ← YOU ARE HERE
   binds the HTTP request / config bean and returns errors
```

**This module’s job:** Spring Boot auto-config, request packing, plan cache, binding-failure
translation, `@ConfigurationProperties` validation.

**Not this module’s job:** constraint annotations, IR types, KSP codegen, OpenAPI overlay, IDE PSI.

Samples and out-of-repo apps depend on this jar. `validata-openapi` depends on **core only**.

---

## How it works

1. **Boot refresh.** `ValidationConfig` creates the shared `ValidationRegistry` and `ValidatorEngine`, warms the
   constraint catalog, and registers validators. On context refresh it indexes `@Validate` handlers → generated
   `EndpointSchema`s and freezes the registry. Servlet web apps also get `ValidatedEndpointPlanCache`,
   `WebMvcValidationAutoConfiguration` (validating invocable), and `ValidataWebMvcConfiguration` (exception resolver).
   Those two MVC auto-configs are `@ConditionalOnWebApplication` (servlet) so config-only / non-web apps can use
   `@ConfigurationProperties` validation without MVC beans.
2. **First request to a handler.** `ValidatedEndpointPlanCache` resolves a plan once: `Skip` (no `@Validate`, or zero
   parameters) or `Active` (pre-resolved schema, section flags, and a precomputed `schemaWhenBodyAbsent` for optional
   `@RequestBody`). Cached for the context lifetime.
3. **Every request.** Spring still binds body, query, header, and path arguments. The host packs only the transport
   sections the plan needs (`RequestArgumentAssembler`), calls `ValidatorEngine.validateRequest` once with the cached
   schema (or the absent-body variant when the body is null), then either throws `ConstraintViolationException` or
   invokes the controller method.
4. **Binding / Jackson failures.** When Spring fails before the engine runs and the handler is Validata-marked,
   `ValidataExceptionResolver` (highest precedence) asks `ValidataExceptionTranslator` to map the failure into
   `ConstraintViolationException`, then re-dispatches through Boot’s `ExceptionHandlerExceptionResolver`. Your
   `@ExceptionHandler` sees one exception type.
5. **Config beans (optional).** The same `@Validate` marker on a `@ConfigurationProperties` type is checked in
   `ConfigurationValidationPostProcessor` after initialization. Missing engine for a targeted bean fails closed; invalid
   values fail context refresh (`ConfigurationValidationException` + FailureAnalyzer).
6. **Collect-all vs fail-fast** is an annotation policy on `@Validate`, not a host switch. The host always asks the
   engine once per annotated invocation; the engine decides how many errors to keep.

There is no per-request annotation scanning after the first plan resolve, and no reflective schema rebuild on the hot
path.

---

## Package layout

| Package      | Role                                                                                                                       |
|--------------|----------------------------------------------------------------------------------------------------------------------------|
| `…config`    | Boot auto-configuration (composition root): registry, engine, limits, plan cache, config post-processor, MVC registrations |
| `…bootstrap` | Startup: validator catalog, `@Validate` handler discovery, endpoint schema index, registry initializer                     |
| `…web`       | Request path: plan cache, argument assembler, validating invocable (`internal`)                                            |
| `…exception` | Binding/Jackson → `ConstraintViolationException`, Boot FailureAnalyzer for config failures, location mapping               |
| `…aot`       | GraalVM / AOT runtime hints                                                                                                |

---

## Quick start

For DTO + `@Validate` handler basics, KSP wiring, and a minimal `@ExceptionHandler`, see the
[root README quick start](../README.md#quick-start).

### Optional: validate `@ConfigurationProperties` at startup

```kotlin
@Validate
@ConfigurationProperties(prefix = "app.mail")
@Validatable
data class MailProperties(
	@field:Required
	var host: String? = null,
)
```

### Try the sample

```bash
./gradlew :validata-samples:bootRun
```

### App-owned HTTP errors

This module throws `ConstraintViolationException` (from `validata-core`) for both engine violations and translated
Spring / Jackson binding failures. It does **not** register a `@ControllerAdvice` — the JSON shape is yours:

```kotlin
@RestControllerAdvice
class ApiErrors {
	@ExceptionHandler(ConstraintViolationException::class)
	fun onValidation(ex: ConstraintViolationException): ResponseEntity<Map<String, Any?>> = ResponseEntity.badRequest()
		.body(
			mapOf(
				"errors" to ex.errors.map {
					mapOf(
						"path" to it.path,
						"location" to it.location?.name,
						"code" to it.code.code,
						"message" to it.message,
					)
				},
			),
		)
}
```

One handler covers engine checks and binding failures on Validata-marked endpoints
(see [Error handling](#error-handling)).

---

## Extension points

Boot auto-configuration entry points (also in
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`):

- `io.ghaylan.validata.config.ValidationConfig`
- `io.ghaylan.validata.config.WebMvcValidationAutoConfiguration` (servlet web)
- `io.ghaylan.validata.config.ValidataWebMvcConfiguration` (servlet web)

All of these are replaceable via `@ConditionalOnMissingBean` (or a Spring bean of the validator type):

| Extension                     | How                                                                                                               |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Registry / engine / limits    | Provide your own `@Bean` of the same type                                                                         |
| Plan cache                    | Replace the web-only cache bean when you need different lifecycle semantics                                       |
| Config post-processor         | Replace the BeanPostProcessor bean if you need different startup policy                                           |
| Constraint validators         | Register a Spring `@Bean` of the concrete `ConstraintValidator` type — it wins over the catalog’s default factory |
| Disable MVC substitution      | Exclude `…config.WebMvcValidationAutoConfiguration` via `spring.autoconfigure.exclude`                            |
| Disable exception translation | Exclude `…config.ValidataWebMvcConfiguration` via `spring.autoconfigure.exclude`                                  |

Do **not** subclass the validating invocable for product code — it is `internal` to this module. Replace beans or
exclude auto-config instead.

---

## Configuration

Prefix: `validata.limits`. Bound at startup onto engine ceilings. Non-positive values fail when
the limits bean is created.

| Property                                     | Type  | Default | Effect                                      |
|----------------------------------------------|-------|---------|---------------------------------------------|
| `validata.limits.max-depth`                  | `Int` | `32`    | Max nesting depth the engine will walk      |
| `validata.limits.max-elements-per-container` | `Int` | `10000` | Max list/map/array size before fail-closed  |
| `validata.limits.max-errors`                 | `Int` | `200`   | Cap on collected errors per validation call |

```yaml
validata:
  limits:
    max-depth: 16
    max-elements-per-container: 5000
    max-errors: 50
```

IDE metadata for these keys ships in `META-INF/spring-configuration-metadata.json`.

Validation runs on the servlet request thread (CPU walk). There is no interrupt/cooperative cancel
in the host call; tighten limits if hostile payloads are a concern. Plan-cache and related memo
maps are process-lifetime and keyed by stable handler methods / classes (static KSP schemas).

Compile-time KSP options (`validata.jackson.naming` = `SNAKE_CASE`, cascade strictness, shape
depth) are **not** Spring properties — see
[validata-processor Configuration](../validata-processor/README.md#configuration).

Same limits table: [root Configuration](../README.md#configuration).

---

## Error handling

| Failure                                  | When                                                                                             | What you see                                                                                     |
|------------------------------------------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `ConstraintViolationException`           | Engine violation **or** translated Spring / Jackson binding failure on a Validata-marked handler | App maps it to JSON / status (see Quick start)                                                   |
| `ConfigurationValidationException`       | Invalid `@Validate` `@ConfigurationProperties`                                                   | Context refresh fails; Boot FailureAnalyzer prints a readable report                             |
| `IllegalStateException` (missing schema) | Annotated handler has no KSP `EndpointSchema`                                                    | Startup (schema index) or first request (plan cache) — apply `validata-processor` to that module |
| `IllegalStateException` (no engine)      | Targeted config bean but engine bean missing                                                     | Fail closed — check auto-config is not excluded                                                  |

### Exception translation

`ValidataWebMvcConfiguration` installs `ValidataExceptionResolver` at the front of the MVC exception chain (servlet web
only). The resolver asks `ValidataExceptionTranslator` to map supported Spring failures into
`ConstraintViolationException`, then re-dispatches through Boot’s `ExceptionHandlerExceptionResolver`.
Already-translated `ConstraintViolationException`s are left alone.

Translation is **opt-in by marker**. Unmarked parameters / handlers keep Spring’s original exception.

`@Validate` detection for Jackson and method-validation failures matches bootstrap discovery: **method first, then
controller class** (`ValidateHandlerDiscovery`).

| Spring exception                                     | Marker required                      | Resulting code(s)                                                                                      | Location                                                                                        |
|------------------------------------------------------|--------------------------------------|--------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `MissingServletRequestParameterException`            | `@Required` on the query param       | `VALUE_MISSING`                                                                                        | `QUERY`                                                                                         |
| `MissingPathVariableException`                       | `@Required` on the path variable     | `VALUE_MISSING`                                                                                        | `PATH`                                                                                          |
| `MissingRequestHeaderException`                      | `@Required` on the header            | `VALUE_MISSING`                                                                                        | `HEADER`                                                                                        |
| `MethodArgumentTypeMismatchException`                | `@Required` on path / query / header | `VALUE_TYPE_MISMATCH`                                                                                  | matching section                                                                                |
| `HandlerMethodValidationException`                   | `@Validate` on method **or** class   | `VALUE_INVALID`                                                                                        | derived from `@RequestBody` / `@RequestParam` / `@PathVariable` / `@RequestHeader` when present |
| `HttpMessageNotReadableException` (Jackson databind) | `@Validate` on method **or** class   | `VALUE_MISSING` (null / missing creator property) / `PROPERTY_UNKNOWN` / `VALUE_TYPE_MISMATCH` | `BODY`                                                                                          |
| `MethodArgumentNotValidException`                    | `@Validate` on the **parameter**     | field messages; `constraint` as `List<String>?`                                                        | `BODY` when `@RequestBody`                                                                      |

Notes:

- Validata’s `@Validate` targets `FUNCTION` and `CLASS` only, so the `MethodArgumentNotValidException` branch does not
  fire for that annotation as shipped. Engine validation covers `@RequestBody` DTOs; keep the branch for a
  parameter-targeted marker if you add one.
- Kotlin **nullable** request params are often treated as optional by Spring: omission may reach the engine as `null`
  (`VALUE_MISSING` from `@Required`) instead of `MissingServletRequestParameterException`. Use a non-null parameter type
  (or `required = true`) when you want the missing-param translator path (`VALUE_MISSING`).
- Translated `constraint` values are normalized to `List<String>?` where the translator sets them.
- Unmapped causes (and handlers without the markers above) are not rewritten — Spring’s default handling continues.

The IntelliJ plugin does **not** render these runtime exceptions as inline diagnostics.

---

## Testing notes

| Goal                      | Approach                                                                                                                          |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Config-only / non-web     | `ApplicationContextRunner` + this module’s auto-config — assert no plan cache / MVC registrations / `ValidataWebMvcConfiguration` |
| Servlet web auto-config   | `WebApplicationContextRunner` — assert plan cache, `WebMvcRegistrations`, and `ValidataWebMvcConfiguration`                       |
| MVC pipeline              | `@SpringBootTest` + MockMvc against real controllers                                                                              |
| Fixtures with `@Validate` | Add `ksp` / `kspTest` for `validata-processor` on the module that declares those handlers                                         |
| Custom validator bean     | Register a `@Bean` of the validator type and assert the catalog / engine uses it                                                  |
| Host package DAG          | `HostPackageBoundaryTest` in this module                                                                                          |

```bash
./gradlew :validata:test
./gradlew :validata-samples:test
```

---

## Rules to remember

1. **Only this library module imports Spring.**
2. **KSP owns schema content** — this host only maps live methods to generated ids.
3. **No per-request annotation scanning** after the first plan resolve; optional-body schema variants are precomputed on
   the plan.
4. **Do not rebuild schemas from reflection** on the hot path — extend KSP instead.
5. Custom constraints and validators go in **`validata-core`** (or your app) + processor — not here.
6. **Do not subclass** the validating invocable — it is module-`internal`; replace beans or exclude auto-config.

---

## What it does not do

| Not handled here                         | Belongs elsewhere        |
|------------------------------------------|--------------------------|
| Constraint annotations / validators      | `validata-core`          |
| Schema IR types                          | `validata-schema`        |
| Generating schemas                       | `validata-processor`     |
| Walking object graphs                    | engine (host only calls) |
| Shipping HTTP error JSON                 | your app                 |
| OpenAPI overlay / IDE PSI                | openapi / intellij       |

It **does** normalize selected Spring / Jackson binding failures into `ConstraintViolationException`
(see [Exception translation](#exception-translation)).

**Rule of thumb:** if it is a rule or a walk over values, look in `validata-core`. If it is Spring + HTTP, look here.

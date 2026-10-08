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
package io.ghaylan.validata.model

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator

/**
 * One wire-facing validation failure for a single input location.
 *
 * Plain immutable data model — no OpenAPI annotations. Hosts choose how to document or wrap it.
 *
 * Validators usually set [code], a precise provisional [message], and [metadata] (typically the
 * [ConstraintMetadata] instance that failed) and leave [path] / [location] unset. The engine stamps
 * [path] (and often [location]) via [copy] and finalizes [message]: non-blank annotation `message`
 * wins; else the validator's [message]; else [ConstraintErrorDefinition.message] as a last-resort
 * default.
 *
 * [path] names the property or parameter; [location] is the HTTP section ([ErrorLocation])
 * when scoped. Typical paths: `"user.address[0].city"`, `"email"`, `"X-Request-Id"`, `"userId"`.
 *
 * [CodeT] is usually [ConstraintErrorCode]; apps may use their own [ConstraintErrorDefinition] enum.
 *
 * @param CodeT Error-code enum; must also implement [ConstraintErrorDefinition].
 * @property path Dot/bracket field path or flat parameter name; `null` until the engine stamps it
 *   (or when not field-scoped).
 * @property location Request section for the input; `null` until stamped (or when not HTTP-scoped).
 * @property code Machine-readable classification (e.g. [ConstraintErrorCode.VALUE_EMPTY]); key off
 *   this, not [message]. Always required — every public error must carry a code.
 * @property message Human text; annotation `message` preferred, else the validator's precise
 *   message, else the [code] default. Set via construction or [copy] after
 *   [ConstraintValidator.validate] returns (never mutated in place).
 * @property metadata Optional structured extras for the failure. Validators attach the failing
 *   [ConstraintMetadata] instance; engine limits / mapping / hand-built business errors may use
 *   `null` or an opaque payload. Typed as [Any] so hosts are not forced through the constraint
 *   hierarchy for non-constraint failures. Never secrets.
 *
 * @author Ghaylan Saada
 */
data class ConstraintError<CodeT>(
	val path: String? = null,
	val location: ErrorLocation? = null,
	val code: CodeT,
	val message: String? = null,
	val metadata: Any? = null,
) where CodeT: Enum<CodeT>, CodeT: ConstraintErrorDefinition

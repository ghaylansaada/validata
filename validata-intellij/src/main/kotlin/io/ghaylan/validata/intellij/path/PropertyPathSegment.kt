/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ghaylan.validata.intellij.path

import com.intellij.openapi.util.TextRange
import io.ghaylan.validata.schema.PropertyPath

/**
 * One dotted-path segment with its text range inside a string **value** (quotes excluded).
 *
 * Produced by [PropertyPathSegments.segmentsWithRanges] for `@PropertyRef` PsiReference /
 * completion hosts. Offsets in [rangeInValue] are relative to the unquoted value text; callers
 * shift them by `ElementManipulators.getValueTextRange(stringTemplate).startOffset` to get
 * `rangeInElement` on the `KtStringTemplateExpression`.
 *
 * Mirrors the segment spelling rules of schema [PropertyPath.split]
 * (trim, drop blanks). Soft empty segments exist only for trailing-dot / empty completion hosts —
 * they are **not** real property names and must not raise unresolved-reference errors.
 *
 * Not a JSONPath node. Not an owner-type resolver — see `PropertyRefSiblingScope` and friends.
 * Current `@PropertyRef` product rules allow only a single hard segment; nested segments may
 * still appear here so references can highlight / soft-fail later segments.
 *
 * @property name Trimmed segment spelling; empty string for a trailing-dot or empty completion host.
 * @property rangeInValue Inclusive-start / exclusive-end offsets relative to the unquoted value text.
 * @property soft When `true`, the IDE must not show an unresolved error for this host (empty /
 *   trailing-dot completion placeholder). Hard segments use `false`.
 * 
 * @author Ghaylan Saada
 */
internal data class PropertyPathSegment(
	val name: String,
	val rangeInValue: TextRange,
	val soft: Boolean = false
)

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
package io.ghaylan.validata.constraint.annotation

import io.ghaylan.validata.constraint.Constraint
import io.ghaylan.validata.constraint.ConstraintArg
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.string.html.HtmlValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import kotlin.reflect.KClass

/**
 * Checks that Html markup stays within an explicit allow-list of tags, attributes, and URL protocols.
 *
 * Use on rich-text / bio / CMS fields when validation must **fail** on disallowed markup instead of
 * stripping it. Checks stop at the first problem: parse → tag → attribute → protocol → sanitization
 * mismatch. `null` or blank are skipped; combine with [Required]/[Size] when presence matters.
 *
 * Requires `org.jsoup:jsoup` on the runtime classpath — startup fails if it is missing and a [Html]
 * field is validated.
 *
 * Each `allowed*` array defaults to `["*"]` (no restriction for that dimension). Use an explicit
 * list to tighten the policy.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Html(allowedTags = ["p", "a", "strong"], allowedAttrs = ["a:href"])
 * val bio: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_NOT_ALLOWED] (disallowed tag, attribute, or protocol)
 * - [ConstraintErrorCode.VALUE_SANITIZATION_MISMATCH]
 *
 * @property allowedTags Permitted element names (e.g. `"p"`, `"a"`); `"*"` allows any tag.
 * @property allowedAttrs Permitted `"tag:attribute"` pairs (e.g. `"a:href"`); `"*"` allows any
 *    attribute. Entries without a `:` separator are ignored when not `"*"`.
 * @property allowedProtocols Permitted `"tag:attribute:scheme1,scheme2"` triples, e.g.
 *    `"a:href:https"`; `"*"` allows any scheme. Incomplete entries are ignored.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [HtmlValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Html(
	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedTags: Array<String> = ["*"],

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedAttrs: Array<String> = ["*"],

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedProtocols: Array<String> = ["*"],

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
)

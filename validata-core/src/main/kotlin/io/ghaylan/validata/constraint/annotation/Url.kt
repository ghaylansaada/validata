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
import io.ghaylan.validata.constraint.validator.string.url.UriValidator
import io.ghaylan.validata.constraint.validator.string.url.UrlValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import java.net.URI
import kotlin.reflect.KClass

/**
 * Validates a URL/[URI] against length, syntax, and optional policy filters (host, protocol,
 * port, query parameters, file extension).
 *
 * Use on avatar, link, webhook, and media URL fields. Checks run in order and stop at the first
 * failure: [maxLength] → URI syntax (CharSequence only) → host presence (only for [Type.WEBSITE]) →
 * [allowedProtocols] → [allowedPorts] → [allowedParams] → [allowedExtensions]. `null` **or blank**
 * [CharSequence] values are skipped (unlike most format validators, which only skip `null`);
 * combine with [Required]/[Size] when the field must also be present.
 *
 * Applies to [CharSequence] and [URI] subjects.
 *
 * All the `allowed*` arrays default to `["*"]` (no restriction). For each, an explicit `"*"`
 * entry disables that filter; otherwise only the listed values pass.
 *
 * ### Example
 *
 * ```kotlin
 * @field:Url(type = Url.Type.IMAGE, allowedProtocols = ["https"])
 * val avatarUrl: String
 *
 * @field:Url(type = Url.Type.WEBSITE, allowedProtocols = ["https"])
 * val callback: URI
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.TEXT_TOO_LONG]
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 * - [ConstraintErrorCode.VALUE_INVALID]
 * - [ConstraintErrorCode.VALUE_NOT_ALLOWED]
 *
 * @property type Resource category. Non-media ([Type.ANY], [Type.WEBSITE]) skip extension checks
 *    unless [allowedExtensions] is overridden; media ([Type.IMAGE], [Type.VIDEO], …) use their
 *    built-in extension set unless overridden.
 * @property maxLength Maximum raw URL string length, in characters. Defaults to `2048`.
 * @property allowedPorts Permitted port numbers as strings (e.g. `"443"`); `"*"` allows any,
 *    including no explicit port. Defaults to `["*"]`.
 * @property allowedParams Permitted query parameter keys; `"*"` allows any. Checked only when a query
 *    string is present; an empty array (vs `["*"]`) rejects any query. Defaults to `["*"]`.
 * @property allowedProtocols Permitted schemes, case-insensitive (e.g. `"https"`); `"*"` allows any.
 *    Defaults to `["*"]`.
 * @property allowedExtensions Permitted lowercase path extensions without the dot (e.g. `"png"`);
 *    `"*"` allows any and disables [type]'s built-in media set. Defaults to `["*"]`.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [
	UrlValidator::class,
	UriValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class Url(
	val type: Type = Type.ANY,

	@ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
	val maxLength: Int = 2048,

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedPorts: Array<String> = ["*"],

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedParams: Array<String> = ["*"],

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedProtocols: Array<String> = ["*"],

	@ConstraintArg(ConstraintArgKind.NOT_BLANK, target = ConstraintArgTarget.ELEMENT)
	val allowedExtensions: Array<String> = ["*"],

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class]
) {

	/**
	 * URL resource category, controlling whether [Url.allowedExtensions] is enforced and which
	 * extensions apply by default.
	 *
	 * @property isMedia When `true`, the URL's path extension must be in [extensions] unless
	 *   [Url.allowedExtensions] overrides it with an explicit list (or `"*"`).
	 * @property extensions Default lowercase extensions accepted for this category, used only
	 *   when [Url.allowedExtensions] is left at its `["*"]` default.
	 */
	enum class Type(
		val isMedia: Boolean,
		val extensions: Array<String>
	) {

		/**
		 * No extension check at all; only the other filters ([Url.allowedProtocols], etc.) apply.
		 */
		ANY(false, emptyArray()),

		/**
		 * Like [ANY], but additionally requires the URI to have a non-blank host component.
		 */
		WEBSITE(false, emptyArray()),

		/**
		 * Office documents, archives, and executables (`pdf`, `zip`, `docx`, `xlsx`, …).
		 */
		DOCUMENT(true, arrayOf("pdf", "zip", "rar", "tar", "exe", "doc", "docx", "ppt", "pptx", "xls", "xlsx")),

		/**
		 * Raster/vector image formats (`jpg`, `png`, `svg`, …).
		 */
		IMAGE(true, arrayOf("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "tiff", "ico", "avif")),

		/**
		 * Video container formats (`mp4`, `mov`, `webm`, …).
		 */
		VIDEO(true, arrayOf("mp4", "avi", "mov", "mkv", "flv", "wmv", "webm", "mpeg")),

		/**
		 * Audio container/codec formats (`mp3`, `wav`, `flac`, …).
		 */
		AUDIO(true, arrayOf("mp3", "wav", "ogg", "flac", "aac", "wma", "m4a", "opus")),
	}
}

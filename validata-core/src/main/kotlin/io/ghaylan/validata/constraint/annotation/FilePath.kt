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
import io.ghaylan.validata.constraint.ConstraintGroups
import io.ghaylan.validata.constraint.ConstraintMessage
import io.ghaylan.validata.constraint.validator.string.filepath.FilePathValidator
import io.ghaylan.validata.groups.OnDefault
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.schema.Validate
import kotlin.reflect.KClass

/**
 * Asserts that a [CharSequence] is a safe relative file path.
 *
 * Rejects null bytes, blank values, `..` path segments, Windows-forbidden characters
 * (`< > : " | ? *`) in each path segment, and absolute paths (Unix `/…`, UNC `\\…`, and
 * Windows drive prefixes `C:…`). Path separators `/` and `\` are allowed for relative
 * multi-segment paths. `null` values are skipped; combine with [Required] when the field must
 * also be present.
 *
 * By default the last segment must also name a file extension — a `.` with at least one
 * character after it, and at least one character before it (so `.gitignore` is a dotfile, not an
 * extension). Set [requireExtension] to `false` for paths that may name a directory or an
 * extensionless file.
 *
 * ### Examples
 *
 * ```kotlin
 * // "invoices/2026/report.pdf" passes; "invoices/2026" fails
 * @field:FilePath
 * val relativePath: String
 *
 * // "uploads/archive" passes too
 * @field:FilePath(requireExtension = false)
 * val targetDirectory: String
 * ```
 *
 * On failure, reports:
 * - [ConstraintErrorCode.VALUE_FORMAT_INVALID]
 * - [ConstraintErrorCode.VALUE_NOT_ALLOWED]
 *
 * @property requireExtension When `true` (default), the last path segment must carry a non-empty
 *    extension after its last `.`. When `false`, extensionless segments are accepted.
 * @property message Error text reported on failure. Blank `""` falls back to system default.
 * @property groups Marker classes for grouping rules across endpoints (e.g. Create vs Update) via
 *    [Validate]. Defaults to `[OnDefault::class]`.
 *
 * @author Ghaylan Saada
 */
@MustBeDocumented
@Constraint(validatedBy = [FilePathValidator::class])
@Retention(AnnotationRetention.RUNTIME)
@Target(
	AnnotationTarget.TYPE,
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.ANNOTATION_CLASS)
annotation class FilePath(
	val requireExtension: Boolean = true,

	@ConstraintMessage
	val message: String = "",

	@ConstraintGroups
	val groups: Array<KClass<*>> = [OnDefault::class],
)

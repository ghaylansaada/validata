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

package io.ghaylan.validata.intellij.editor.inject

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.ghaylan.validata.intellij.resolve.reference.ConstraintEnumLiteralPsiReference
import io.ghaylan.validata.intellij.resolve.reference.PropertyRefPsiReference
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Schedules completion autopopup while typing inside `@PropertyRef` or enum typed-literal
 * string hosts (e.g. `@Compare(ref = "usern", operation = Compare.Operation.EQ)`, `@In(values = ["PH"])`).
 *
 * ## What
 * Platform [TypedHandlerDelegate] that, after identifier / `.` keystrokes, detects whether the
 * caret sits in a string template that already carries [PropertyRefPsiReference] or
 * [ConstraintEnumLiteralPsiReference], and if so asks [AutoPopupController] to open basic
 * completion.
 *
 * ## plugin.xml EP
 * Registered as `com.intellij.typedHandler`:
 * ```
 * <typedHandler
 *     implementation="…editor.inject.PropertyRefTypedHandler"/>
 * ```
 * Works together with
 * `PropertyRefCompletionConfidence`
 * (`completion.confidence`, `order="first"`): confidence allows autopopup inside strings;
 * this handler actively schedules it on each qualifying keystroke.
 *
 * ## When it fires
 * On every typed character in the editor. Returns [Result.CONTINUE] always after optionally
 * scheduling autopopup when the character is a Java identifier part or `.` **and** the caret’s
 * string host has a Validata property-ref or enum-literal reference.
 *
 * ## What it is NOT
 * - Not a completion contributor — it only triggers the existing completion pipeline.
 * - Not a language injector (despite the `inject` package) — see
 *   [ConstraintRegexLanguageInjector] for RegExp injection.
 * - Does not invent hosts; references must already be attached by the reference contributor.*
 * 
 * @author Ghaylan Saada
 */
class PropertyRefTypedHandler : TypedHandlerDelegate() {

	/**
	 * Optionally schedules autopopup after [charTyped].
	 *
	 * Validata: only identifier characters and `.` matter (path / enum-name typing). Looks up
	 * the PSI under the caret, finds the enclosing [KtStringTemplateExpression], and checks
	 * for Validata references before scheduling.
	 *
	 * @param charTyped character just inserted
	 * @param project current project
	 * @param editor active editor
	 * @param file PSI file being edited
	 * @return [Result.CONTINUE] always (autopopup may have been scheduled for a Validata host)
	 */
	override fun checkAutoPopup(
		charTyped: Char,
		project: Project,
		editor: Editor,
		file: PsiFile,
	): Result {
		if (!charTyped.isJavaIdentifierPart() && charTyped != '.') {
			return Result.CONTINUE
		}
		val offset = editor.caretModel.offset
		val element = file.findElementAt(offset - 1) ?: return Result.CONTINUE
		val stringTemplate = element as? KtStringTemplateExpression
			?: element.getStrictParentOfType()
			?: return Result.CONTINUE
		val host = stringTemplate.references.any {
			it is PropertyRefPsiReference || it is ConstraintEnumLiteralPsiReference
		}
		if (!host) return Result.CONTINUE
		AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
		// CONTINUE so other typed handlers can still react; autopopup is already scheduled.
		return Result.CONTINUE
	}
}

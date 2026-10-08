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
package io.ghaylan.validata.processor.codegen

import io.ghaylan.validata.processor.model.ConstraintModel
import io.ghaylan.validata.schema.constraint.CompiledConstraint

/**
 * Shared Kotlin source fragments for [ConstraintModel] lists.
 *
 * Used by object-schema and endpoint writers so [CompiledConstraint] emission cannot drift.
 * Leaf and OR sites use `CompiledConstraints.of` / `CompiledConstraints.or` so metadata is shared
 * without `run { }` locals.
 *
 * When an [ImportScope] is provided, fully qualified type names inside metadata / validator
 * expressions are rewritten to short names with matching imports.
 *
 * @author Ghaylan Saada
 */
internal object ConstraintSourceRenderer {
	
	/**
	 * Renders a `List<CompiledConstraint>` literal (or `emptyList()`).
	 *
	 * Side effects: may register imports on [imports].
	 *
	 * @param constraints Constraint models to emit; empty yields `emptyList()`.
	 * @param imports Optional scope used to shorten FQCNs and collect imports.
	 * @return Kotlin expression text for the constraints list.
	 */
	fun renderConstraints(
		constraints: List<ConstraintModel>,
		imports: ImportScope? = null,
	): String {
		if (constraints.isEmpty()) return "emptyList()"
		return buildString {
			append("listOf(\n")
			constraints.forEachIndexed { index, model ->
				appendIndented(renderOne(model, imports), outerIndent = 16)
				if (index < constraints.lastIndex) append(',')
				append('\n')
			}
			append("            )")
		}
	}
	
	/**
	 * `true` when any constraint is an OR composition site (needs `CompositionConstraint` import).
	 */
	fun usesComposition(constraints: List<ConstraintModel>): Boolean =
		constraints.any { it.compositionChildren != null }
	
	/**
	 * `true` when any constraint list is non-empty (needs `CompiledConstraints` import).
	 */
	fun usesCompiledConstraints(constraints: List<ConstraintModel>): Boolean =
		constraints.isNotEmpty()
	
	/**
	 * Renders one [ConstraintModel] as `CompiledConstraints.of` / `CompiledConstraints.or`.
	 *
	 * Side effects: may register imports on [imports].
	 */
	private fun renderOne(
		model: ConstraintModel,
		imports: ImportScope?,
	): String {
		val children = model.compositionChildren
		if (children != null) {
			return buildString {
				append("CompiledConstraints.or(\n")
				append("    CompositionConstraint(\n")
				append("        message = ${shorten(model.compositionMessageExpr, imports)},\n")
				append("        groups = ${shorten(model.compositionGroupsExpr, imports)},\n")
				append("        children = listOf(\n")
				children.forEachIndexed { index, child ->
					appendIndented(renderOne(child, imports), outerIndent = 12)
					if (index < children.lastIndex) append(',')
					append('\n')
				}
				append("        ),\n")
				append("    ),\n")
				append("    order = ${model.order},\n")
				append(')')
			}
		}
		return buildString {
			append("CompiledConstraints.of(\n")
			append("    ${shorten(model.validatorExpression, imports)},\n")
			appendIndented(shorten(model.metadataConstructorCall, imports), outerIndent = 4)
			append(",\n")
			append("    order = ${model.order},\n")
			append(')')
		}
	}
	
	private fun shorten(
		expr: String,
		imports: ImportScope?
	): String =
		imports?.shortenExpression(expr)
			?: expr
	
	/**
	 * Appends [text] with each line prefixed by [outerIndent] spaces.
	 */
	private fun StringBuilder.appendIndented(
		text: String,
		outerIndent: Int,
	) {
		val pad = " ".repeat(outerIndent)
		var start = 0
		while (start <= text.length) {
			val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
			append(pad)
			append(text, start, end)
			if (end == text.length) break
			append('\n')
			start = end + 1
		}
	}
}

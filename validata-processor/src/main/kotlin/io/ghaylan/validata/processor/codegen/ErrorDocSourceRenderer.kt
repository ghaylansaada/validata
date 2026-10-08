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

import io.ghaylan.validata.processor.compat.KotlinStringLiteral
import io.ghaylan.validata.processor.model.SchemaErrorDocModel
import io.ghaylan.validata.schema.docs.SchemaErrorDoc

/**
 * Renders [SchemaErrorDocModel] into Kotlin source fragments for generated `PropertySpec`
 * constructors ([SchemaErrorDoc] literals).*
 * 
 * @author Ghaylan Saada
 */
internal object ErrorDocSourceRenderer {
	
	/**
	 * Renders a `listOf(SchemaErrorDoc(…), …)` or `emptyList()` expression.
	 *
	 * Side effects: none.
	 *
	 * @param docs Doc models to emit; empty yields `emptyList()`.
	 * @return Kotlin expression text.	 
	 */
	fun renderErrorDocs(docs: List<SchemaErrorDocModel>): String {
		if (docs.isEmpty()) return "emptyList()"
		return docs.joinToString(
			prefix = "listOf(",
			postfix = ")",
			separator = ", ",
		) { renderErrorDoc(it) }
	}
	
	/**
	 * Renders one [SchemaErrorDoc] constructor call.
	 *
	 * Side effects: none.
	 *
	 * @param doc Single error-doc model.
	 * @return Kotlin expression such as `SchemaErrorDoc(code = "…", message = "…")`.	 
	 */
	private fun renderErrorDoc(doc: SchemaErrorDocModel): String {
		val messageLit = "\"${KotlinStringLiteral.escape(doc.message)}\""
		val catalogArg = if (doc.catalogFqcn == null) {
			""
		}
		else {
			", catalogFqcn = \"${KotlinStringLiteral.escape(doc.catalogFqcn)}\""
		}
		return "SchemaErrorDoc(code = \"${KotlinStringLiteral.escape(doc.code)}\", " + "message = $messageLit$catalogArg)"
	}
}

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
package io.ghaylan.validata.schema.runtime

import io.ghaylan.validata.schema.ObjectSchema

/**
 * Thrown when the registry cannot resolve a generated [ObjectSchema] for a type.
 *
 * Typical causes: the type is missing `@Validatable`, or the **module that declares the type**
 * never applied `ksp("…:validata-processor")`. In multi-module apps this is the most common miss —
 * having the processor on *other* modules does not help the module that owns the DTO. Resolution is
 * schema-only: annotate, apply the processor to *this* module, and recompile. Do not catch this and
 * invent a schema at runtime.
 *
 * @param message Human-readable checklist (see [messageFor]).
 * @property type Runtime class that lacked a generated schema.*
 * 
 * @author Ghaylan Saada
 */
class SchemaNotFoundException(
	message: String,
	val type: Class<*>,
): IllegalStateException(message) {
	
	/**
	 * Factories for the standard missing-schema checklist message.
	 */
	companion object {
		
		/**
		 * Builds the standard first-time-setup checklist message for [type].
		 *
		 * Prefer [forMissingSchema] at throw sites so wording stays identical across the engine.
		 * No I/O or mutation.
		 *
		 * @param type Class missing a generated schema.
		 * @return Multi-line setup checklist string.		 
		 */
		fun messageFor(type: Class<*>): String = buildString {
			append("No generated validation schema for ")
			append(type.name)
			append(".\n\n")
			append("First-time setup checklist:\n")
			append("  1. Annotate this type (and nested cascade targets) with @Validatable.\n")
			append("  2. In the Gradle module that *declares* this type, apply KSP:\n")
			append("       plugins { id(\"com.google.devtools.ksp\") version \"<ksp-matching-kotlin>\" }\n")
			append("       dependencies {\n")
			append("         implementation(\"io.github.ghaylansaada:validata:<version>\")\n")
			append("         ksp(\"io.github.ghaylansaada:validata-processor:<version>\")\n")
			append("       }\n")
			append("  3. Recompile *that* module. KSP on sibling modules is not enough.\n")
			append("  4. Prefer nullable DTO fields (e.g. String?) when @Required should see JSON absences.\n")
			append("  5. Or register a hand-written ObjectSchemaModule for third-party types.\n")
		}
		
		/**
		 * Factory used by [GeneratedSchemaLookup] and `ValidationRegistry`.
		 *
		 * Allocates a new exception; no shared-state mutation.
		 *
		 * @param type Class missing a generated schema.
		 * @return Exception with [messageFor] text and [SchemaNotFoundException.type] set.		 
		 */
		fun forMissingSchema(type: Class<*>): SchemaNotFoundException = SchemaNotFoundException(messageFor(type), type)
	}
}

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
package io.ghaylan.validata.schema

import io.ghaylan.validata.schema.spi.ObjectSchemaModule
import kotlin.reflect.KClass

/**
 * Marks a type as part of the validation schema universe.
 *
 * Required for every type whose [ObjectSchema] is resolved at runtime. Schemas are produced by the
 * `validata-processor` KSP module (or a hand-written [ObjectSchemaModule]).
 *
 * ## Polymorphism
 * For sealed / abstract / interface roots, concrete implementations are discovered from:
 * 1. Kotlin sealed subclasses (`getSealedSubclasses`), and/or
 * 2. Explicit [subtypes] entries on this annotation.
 *
 * When both sources are present, their concrete type sets must match — a mismatch is a compile-time
 * error. The framework does not read Jackson (or other serializer) annotations; declare subtypes
 * here. Host serializers may still use their own annotations for deserialization.
 *
 * [discriminator] names the property (or logical field) the host serializer uses to pick a subtype.
 * The engine dispatches by the bound instance’s runtime class; the discriminator string is
 * declarative metadata for authors and tooling. When non-blank, KSP and the IntelliJ plugin require
 * a Kotlin property of that declared name on the annotated type (including inherited properties).
 * The discriminator property must be a scalar typed-literal host (string, number, temporal, or
 * enum) — not a collection or nested object.
 *
 * Each [Subtype.type] must extend or implement the annotated parent — a mismatch is a compile-time
 * / IDE error (underline only in the IDE, not full red unresolved text).
 *
 * [Subtype.name] is validated as a typed literal of the discriminator property’s type (same rules
 * as `@ConstraintArg(TYPED_LITERAL)`):
 * - **enum** → must be a constant name (IDE autocomplete / Ctrl+Click)
 * - **numeric** → must parse as a decimal number string (e.g. `"1"`, `"1_000"`)
 * - **temporal** → ISO / type-specific literal
 * - **String / CharSequence** → any text
 *
 * @property discriminator Wire / logical property name used by the host serializer to choose a
 *   subtype (e.g. `"context"`). Must match a scalar property on this type when non-empty. Empty
 *   when not applicable.
 * @property subtypes Explicit concrete subtypes when the root is not sealed, or to cross-check a
 *   sealed hierarchy at compile time. Each [Subtype.type] must be a subtype of this type and
 *   itself `@Validatable`; [Subtype.name] must match the discriminator property’s scalar type.*
 * 
 * @author Ghaylan Saada
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Validatable(
	val discriminator: String = "",
	val subtypes: Array<Subtype> = [],
) {
	
	/**
	 * One concrete subtype of a polymorphic [Validatable] root.
	 *
	 * Used only as an element of [Validatable.subtypes] — never applied to a declaration itself.
	 *
	 * @property name Discriminator value that identifies this subtype (for example
	 *   `"PROFILE_AVATAR"` or `"1"`). Must be a valid typed literal of the parent’s scalar
	 *   discriminator property type.
	 * @property type Concrete [Validatable] implementation class that extends or implements the
	 *   annotated parent.
	 */
	@Target
	@Retention(AnnotationRetention.RUNTIME)
	annotation class Subtype(
		val name: String,
		val type: KClass<*>)
}

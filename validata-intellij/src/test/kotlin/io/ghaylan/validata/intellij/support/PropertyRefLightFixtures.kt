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

package io.ghaylan.validata.intellij.support

import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * Shared light-test stubs for library markers (`@Constraint`, `@PropertyRef`, compatibility enum).
 *
 * Paths match package FQCNs so PSI discovery can resolve them the same way as a real classpath.*
 * 
 * @author Ghaylan Saada
 */
internal object PropertyRefLightFixtures {
	
	fun addLibraryMarkers(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/schema/ref/PropertyRefCompatibilityKind.kt",
			"""
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

package io.ghaylan.validata.schema.ref
			enum class PropertyRefCompatibilityKind {
			  NONE, SAME_SCALAR_KIND, COMPARABLE_FAMILY
			}
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/schema/ref/ConstraintArgKind.kt",
			"""
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

package io.ghaylan.validata.schema.ref
			enum class ConstraintArgKind {
			  NOT_BLANK, NON_EMPTY, TYPED_LITERAL, NON_NEGATIVE, POSITIVE, REGEX
			}
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/schema/ref/ConstraintArgTarget.kt",
			"""
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

package io.ghaylan.validata.schema.ref
			enum class ConstraintArgTarget { VALUE, ELEMENT }
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/schema/ref/PropertyRefScope.kt",
			"""
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

package io.ghaylan.validata.schema.ref
			enum class PropertyRefScope { SIBLING, ELEMENT }
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/PropertyRef.kt",
			"""
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

package io.ghaylan.validata.constraint
			import io.ghaylan.validata.schema.ref.PropertyRefCompatibilityKind
			import io.ghaylan.validata.schema.ref.PropertyRefScope
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
			annotation class PropertyRef(
			  val scope: PropertyRefScope = PropertyRefScope.SIBLING,
			  val compatibility: PropertyRefCompatibilityKind = PropertyRefCompatibilityKind.NONE,
			)
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/ConstraintArgs.kt",
			"""
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

package io.ghaylan.validata.constraint
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
			annotation class ConstraintArgs(vararg val value: ConstraintArg)
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/ConstraintArg.kt",
			"""
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

package io.ghaylan.validata.constraint
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			@Repeatable
			@JvmRepeatable(ConstraintArgs::class)
			@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
			annotation class ConstraintArg(
			  vararg val kinds: ConstraintArgKind,
			  val target: ConstraintArgTarget = ConstraintArgTarget.VALUE,
			  val message: String = "",
			)
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/ConstraintValidator.kt",
			"""
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

package io.ghaylan.validata.constraint
			/** Light-test open stand-in for the library abstract base (typed V drives subject checks). */
			open class ConstraintValidator<V, C>
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/ConstraintMetadata.kt",
			"""
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

package io.ghaylan.validata.constraint
			abstract class ConstraintMetadata
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/Constraint.kt",
			"""
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

package io.ghaylan.validata.constraint
			import kotlin.reflect.KClass
			@Target(AnnotationTarget.CLASS)
			annotation class Constraint(
			  val validatedBy: Array<KClass<*>> = [],
			)
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/ConstraintComposition.kt",
			"""
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

package io.ghaylan.validata.constraint
			@Target(AnnotationTarget.ANNOTATION_CLASS)
			annotation class ConstraintComposition(
			  val value: Mode = Mode.AND,
			) {
			  enum class Mode { AND, OR }
			}
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/schema/Validatable.kt",
			"""
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

package io.ghaylan.validata.schema
			import kotlin.reflect.KClass
			@Target(AnnotationTarget.CLASS)
			annotation class Validatable(
			  val discriminator: String = "",
			  val subtypes: Array<Subtype> = [],
			) {
			  annotation class Subtype(val name: String, val type: KClass<*>)
			}
			""".trimIndent(),
		)
	}
	
	/** Minimal `@Min` with `@ConstraintArg` on the annotation parameter.	 */
	fun addMinConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/MinConstraint.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			data class MinConstraint(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
			  val value: String,
			) : ConstraintMetadata()
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Min.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class NumberMinValidator : ConstraintValidator<Number, MinConstraint>()
			@Constraint(validatedBy = [NumberMinValidator::class])
			annotation class Min(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
			  val value: String,
			  val inclusive: Boolean = true,
			)
			""".trimIndent(),
		)
	}
	
	/** Minimal `@In` with `@ConstraintArg` on the annotation parameter.	 */
	fun addInConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/InConstraint.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			data class InConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String>,
			) : ConstraintMetadata()
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/In.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			class InValidator : ConstraintValidator<Any, InConstraint>()
			@Constraint(validatedBy = [InValidator::class])
			annotation class In(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String>,
			)
			""".trimIndent(),
		)
	}

	/** Minimal `@NotIn` with `@ConstraintArg` on the annotation parameter.	 */
	fun addNotInConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/NotInConstraint.kt",
			"""
			package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			data class NotInConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String>,
			) : ConstraintMetadata()
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/NotIn.kt",
			"""
			package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			class NotInValidator : ConstraintValidator<Any, NotInConstraint>()
			@Constraint(validatedBy = [NotInValidator::class])
			annotation class NotIn(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String>,
			)
			""".trimIndent(),
		)
	}

	/** Minimal `@Contains` with ELEMENT [ConstraintArgKind.TYPED_LITERAL] on `values`.	 */
	fun addContainsConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/ContainsConstraint.kt",
			"""
			package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			data class ContainsConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String>,
			) : ConstraintMetadata()
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Contains.kt",
			"""
			package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			object CollectionContainsValidator : ConstraintValidator<Collection<*>, ContainsConstraint>()
			object ArrayContainsValidator : ConstraintValidator<Cloneable, ContainsConstraint>()
			@Constraint(validatedBy = [CollectionContainsValidator::class, ArrayContainsValidator::class])
			annotation class Contains(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String>,
			  val mode: Mode = Mode.ANY,
			) {
			  enum class Mode { ANY, ALL, NONE }
			}
			""".trimIndent(),
		)
	}
	
	/** Minimal `@Size` with `@ConstraintArg(NON_NEGATIVE)` on annotation params.	 */
	fun addSizeConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/SizeConstraint.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			data class SizeConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val min: Int,
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val max: Int,
			) : ConstraintMetadata()
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Size.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class CharSequenceSizeValidator : ConstraintValidator<CharSequence, SizeConstraint>()
			@Constraint(validatedBy = [CharSequenceSizeValidator::class])
			annotation class Size(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val min: Int = 0,
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val max: Int = Int.MAX_VALUE,
			)
			""".trimIndent(),
		)
	}
	
	fun addMaxConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/MaxConstraint.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintMetadata
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			data class MaxConstraint(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
			  val value: String,
			) : ConstraintMetadata()
			""".trimIndent(),
		)
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Max.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class NumberMaxValidator : ConstraintValidator<Number, MaxConstraint>()
			@Constraint(validatedBy = [NumberMaxValidator::class])
			annotation class Max(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
			  val value: String,
			  val inclusive: Boolean = true,
			)
			""".trimIndent(),
		)
	}
	
	fun addMultipleOfConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/MultipleOf.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class MultipleOfConstraint(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
			  val factor: String,
			)
			class MultipleOfValidator : ConstraintValidator<Number, MultipleOfConstraint>()
			@Constraint(validatedBy = [MultipleOfValidator::class])
			annotation class MultipleOf(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL)
			  val factor: String,
			)
			""".trimIndent(),
		)
	}
	
	fun addRegexConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Regex.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class RegexConstraint(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.REGEX)
			  val pattern: String,
			  val name: String,
			)
			class RegexValidator : ConstraintValidator<CharSequence, RegexConstraint>()
			@Constraint(validatedBy = [RegexValidator::class])
			annotation class Regex(
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.REGEX)
			  val pattern: String,
			  val name: String,
			)
			""".trimIndent(),
		)
	}
	
	fun addRelativeToNowConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/RelativeToNow.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import java.time.temporal.ChronoUnit
			class RelativeToNowConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val within: Int,
			)
			class RelativeToNowValidator : ConstraintValidator<Any, RelativeToNowConstraint>()
			@Constraint(validatedBy = [RelativeToNowValidator::class])
			annotation class RelativeToNow(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val within: Int = Int.MAX_VALUE,
			  val unit: ChronoUnit = ChronoUnit.DAYS,
			  val relation: Relation = Relation.GT,
			) {
			  enum class Relation { GT, LT, GTE, LTE, EQ }
			}
			""".trimIndent(),
		)
	}
	
	fun addDaysOfWeekConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/DaysOfWeek.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			import java.time.DayOfWeek
			class DaysOfWeekConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  val days: Array<DayOfWeek>,
			)
			class DaysOfWeekValidator : ConstraintValidator<Any, DaysOfWeekConstraint>()
			@Constraint(validatedBy = [DaysOfWeekValidator::class])
			annotation class DaysOfWeek(
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  val days: Array<DayOfWeek>,
			)
			""".trimIndent(),
		)
	}
	
	/** Minimal `@RequiredWhen` with gate-typed `value` / `values` markers.	 */
	fun addRequiredWhenConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/RequiredWhen.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.constraint.PropertyRef
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			class RequiredWhenConstraint(
			  @PropertyRef
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK)
			  val ref: String,
			  @ConstraintArg(ConstraintArgKind.TYPED_LITERAL)
			  val value: String = "",
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String> = emptyArray(),
			)
			class RequiredWhenValidator : ConstraintValidator<Any, RequiredWhenConstraint>()
			@Constraint(validatedBy = [RequiredWhenValidator::class])
			annotation class RequiredWhen(
			  @PropertyRef
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK)
			  val ref: String,
			  val condition: Condition,
			  @ConstraintArg(ConstraintArgKind.TYPED_LITERAL)
			  val value: String = "",
			  @ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
			  @ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT)
			  val values: Array<String> = [],
			) {
			  enum class Condition {
			    MISSING, PRESENT, EQ, NE, `IN`, NIN, GT, LT, GTE, LTE
			  }
			}
			""".trimIndent(),
		)
	}
	
	/** Minimal `@Uuid` with `@ConstraintArg(NON_NEGATIVE)` on `version`.	 */
	fun addUuidConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Uuid.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class UuidConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val version: Int,
			)
			class UuidValidator : ConstraintValidator<CharSequence, UuidConstraint>()
			@Constraint(validatedBy = [UuidValidator::class])
			annotation class Uuid(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val version: Int = 0,
			)
			""".trimIndent(),
		)
	}
	
	/** Minimal `@Password` with `@ConstraintArg(NON_NEGATIVE)` on length bounds.	 */
	fun addPasswordConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Password.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class PasswordConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val minLength: Int,
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val maxLength: Int,
			)
			class PasswordValidator : ConstraintValidator<CharSequence, PasswordConstraint>()
			@Constraint(validatedBy = [PasswordValidator::class])
			annotation class Password(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val minLength: Int = 6,
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val maxLength: Int = 64,
			)
			""".trimIndent(),
		)
	}
	
	/** Minimal `@Url` with `@ConstraintArg(NON_NEGATIVE)` on `maxLength`.	 */
	fun addUrlConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/Url.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			class UrlConstraint(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val maxLength: Int,
			)
			class UrlValidator : ConstraintValidator<CharSequence, UrlConstraint>()
			@Constraint(validatedBy = [UrlValidator::class])
			annotation class Url(
			  @ConstraintArg(ConstraintArgKind.NON_NEGATIVE)
			  val maxLength: Int = 2048,
			)
			""".trimIndent(),
		)
	}
	
	/**
	 * Custom constraint using `@ConstraintArgs` container with ELEMENT target (repeatable
	 * container shape used by binary stubs).	 
	 */
	fun addConstraintArgsElementConstraint(fixture: CodeInsightTestFixture) {
		fixture.addFileToProject(
			"io/ghaylan/validata/constraint/annotation/TagsIn.kt",
			"""
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

package io.ghaylan.validata.constraint.annotation
			import io.ghaylan.validata.constraint.Constraint
			import io.ghaylan.validata.constraint.ConstraintArg
			import io.ghaylan.validata.constraint.ConstraintArgs
			import io.ghaylan.validata.constraint.ConstraintValidator
			import io.ghaylan.validata.schema.ref.ConstraintArgKind
			import io.ghaylan.validata.schema.ref.ConstraintArgTarget
			class TagsInConstraint(
			  @ConstraintArgs(
			    ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE),
			    ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT),
			  )
			  val values: Array<String>,
			)
			class TagsInValidator : ConstraintValidator<Any, TagsInConstraint>()
			@Constraint(validatedBy = [TagsInValidator::class])
			annotation class TagsIn(
			  @ConstraintArgs(
			    ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE),
			    ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, target = ConstraintArgTarget.ELEMENT),
			  )
			  val values: Array<String>,
			)
			""".trimIndent(),
		)
	}
}

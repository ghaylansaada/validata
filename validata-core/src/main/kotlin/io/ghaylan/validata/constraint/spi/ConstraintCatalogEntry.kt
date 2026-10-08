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
package io.ghaylan.validata.constraint.spi

import io.ghaylan.validata.constraint.ConstraintMetadata
import io.ghaylan.validata.constraint.ConstraintValidator
import io.ghaylan.validata.internal.TypeInfo

/**
 * One resolved binding between a constraint annotation, the [ConstraintMetadata] it hydrates,
 * a compatible value type, and the validator class that evaluates it.
 *
 * Built exclusively by generated code (`ConstraintCatalogProcessor` in `validata-processor`).
 * Nothing in this library resolves this binding via reflection at runtime.
 *
 * ## Why [validatorType] is separate from [defaultInstanceFactory]
 * This SPI has no Spring dependency, so it cannot decide whether a Spring bean exists for the
 * validator — that needs a Spring application context held only by the Spring-aware consumer.
 * [validatorType] is the lookup key for a bean override; [defaultInstanceFactory] is the fallback
 * when no such bean exists.
 *
 * @property annotationType The user-facing `@Constraint` annotation class (e.g. `@Required`).
 * @property metadataType The [ConstraintMetadata] subclass hydrated from that annotation.
 * @property valueType The value type the validator accepts (catalog key for type-aware selection).
 * @property validatorType The concrete validator class, used to look up a Spring bean override.
 * @property defaultInstanceFactory Produces an instance with **no** Spring context — a direct
 *   `object` reference or a no-arg constructor call, chosen and emitted by KSP at compile time.
 * 
 * @author Ghaylan Saada
 */
data class ConstraintCatalogEntry(
	val annotationType: Class<out Annotation>,
	val metadataType: Class<out ConstraintMetadata>,
	val valueType: TypeInfo,
	val validatorType: Class<out ConstraintValidator<*, *>>,
	val defaultInstanceFactory: () -> ConstraintValidator<*, *>
)

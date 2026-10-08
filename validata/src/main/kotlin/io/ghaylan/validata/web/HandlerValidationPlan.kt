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
package io.ghaylan.validata.web

import io.ghaylan.validata.ext.MethodUniqueIdentifiers
import io.ghaylan.validata.schema.request.EndpointSchema
import java.lang.reflect.Method

/**
 * Immutable per-handler decision for the validating invocable: skip validation, or run it with a
 * pre-resolved [EndpointSchema] and section flags.
 *
 * Spring constructs a new [ValidatingServletInvocableHandlerMethod] on every request. Plans are
 * resolved once per bridged [Method] via [ValidatedEndpointPlanCache] and reused for the process
 * lifetime.
 *
 * Do not use this type outside the Spring MVC integration layer; tests may construct [Active]
 * values directly when exercising [RequestArgumentAssembler].
 * 
 * @author Ghaylan Saada
 */
internal sealed interface HandlerValidationPlan {
	
	/**
	 * Handler does not participate in request validation on the hot path.
	 *
	 * Used when the handler has no effective `@Validate`, or has zero parameters.
	 */
	data object Skip: HandlerValidationPlan
	
	/**
	 * Handler is annotated for validation and has a compile-time schema ready for every request.
	 *
	 * [schema] is the instance stored in the registry for [endpointId]; the invocable must not look
	 * it up again by id. Section flags describe whether the argument layout contains QUERY/HEADER/PATH
	 * slots, so body-only endpoints never allocate empty maps.
	 *
	 * @property endpointId Stable id matching KSP / [MethodUniqueIdentifiers.getUniqueIdentifier].
	 * @property schema Pre-resolved endpoint schema (body + flat sections + argument layout).
	 * @property schemaWhenBodyAbsent Same as [schema] with `requestBody` cleared when the plan has a
	 *   body section; equals [schema] otherwise. Used when the resolved body argument is null so the
	 *   hot path never allocates [EndpointSchema.copy].
	 * @property needsQuery `true` when [EndpointSchema.argumentLayout] contains at least one QUERY
	 *   slot.
	 * @property needsHeader `true` when the layout contains at least one HEADER slot.
	 * @property needsPath `true` when the layout contains at least one PATH slot.
	 */
	data class Active(
		val endpointId: String,
		val schema: EndpointSchema,
		val schemaWhenBodyAbsent: EndpointSchema,
		val needsQuery: Boolean,
		val needsHeader: Boolean,
		val needsPath: Boolean,
	): HandlerValidationPlan
}

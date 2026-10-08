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
package io.ghaylan.validata.processor.analyze

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import io.ghaylan.validata.processor.fqns.AnnotationAttrs
import io.ghaylan.validata.processor.fqns.ProcessorFqns

/**
 * Reads named attributes from an effective `@Validate` usage (method or class).
 *
 * Missing / unreadable args keep annotation defaults so generated IR matches runtime reflection
 * of the same annotation instance.*
 * 
 * @author Ghaylan Saada
 */
internal object ValidateFlagsReader {
	
	/**
	 * Parses effective `@Validate` attributes from [ann].
	 *
	 * No side effects.
	 *
	 * @param ann effective `@Validate` usage on a handler or class
	 * @return [ValidateFlags] with annotation defaults for missing or unreadable args	 
	 */
	fun read(ann: KSAnnotation): ValidateFlags {
		var oneErrorPerParam = true
		var failFast = false
		var groupsFqcn = listOf(ProcessorFqns.ON_DEFAULT)
		
		for (arg in ann.arguments) {
			when (arg.name?.asString()) {
				AnnotationAttrs.Validate.ONE_ERROR_PER_PARAM -> oneErrorPerParam = arg.value as? Boolean
					?: true
				
				AnnotationAttrs.Validate.FAIL_FAST -> failFast = arg.value as? Boolean
					?: false
				
				AnnotationAttrs.Validate.GROUPS -> {
					@Suppress("UNCHECKED_CAST")
					val list = arg.value as? List<KSType>
					if (!list.isNullOrEmpty()) {
						groupsFqcn = list.mapNotNull { it.declaration.qualifiedName?.asString() }
					}
				}
			}
		}
		return ValidateFlags(
			oneErrorPerParam = oneErrorPerParam,
			failFast = failFast,
			groupsFqcn = groupsFqcn,
		)
	}
}

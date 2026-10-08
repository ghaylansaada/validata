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
package io.ghaylan.validata.constraint

import io.ghaylan.validata.schema.ref.ConstraintArgKind
import io.ghaylan.validata.schema.ref.ConstraintArgTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Smoke coverage that [@ConstraintArg][ConstraintArg] is retained on constructor parameters
 * (the usual metadata authoring site), including repeatable + [ConstraintArgTarget].
 * 
 * @author Ghaylan Saada
 */
class ConstraintArgRetentionTest {
	
	class Host(
		@ConstraintArg(ConstraintArgKind.NOT_BLANK, ConstraintArgKind.TYPED_LITERAL, message = "bound")
		val sample: String,
	)
	
	class ArrayHost(
		@ConstraintArg(ConstraintArgKind.NON_EMPTY, target = ConstraintArgTarget.VALUE)
		@ConstraintArg(
			ConstraintArgKind.NOT_BLANK,
			ConstraintArgKind.TYPED_LITERAL,
			target = ConstraintArgTarget.ELEMENT,
		)
		val values: Array<String>,
	)
	
	@Test
	@DisplayName("@ConstraintArg is RUNTIME-retained with kinds and message")
	fun runtimeRetention() {
		val param = Host::class.java.declaredConstructors.single().parameters.single {
			it.name == "sample" || it.name == "arg0"
		}
		val ann = param.getAnnotation(ConstraintArg::class.java)
			?: Host::class.java.getDeclaredField("sample")
				.getAnnotation(ConstraintArg::class.java)
		assertThat(ann).isNotNull
		assertThat(ann!!.kinds).containsExactlyInAnyOrder(
			ConstraintArgKind.NOT_BLANK,
			ConstraintArgKind.TYPED_LITERAL,
		)
		assertThat(ann.target).isEqualTo(ConstraintArgTarget.VALUE)
		assertThat(ann.message).isEqualTo("bound")
	}
	
	@Test
	@DisplayName("repeatable @ConstraintArg retains VALUE and ELEMENT rules")
	fun repeatableRetention() {
		val param = ArrayHost::class.java.declaredConstructors.single().parameters.single {
			it.name == "values" || it.name == "arg0"
		}
		val all = param.getAnnotationsByType(ConstraintArg::class.java)
		assertThat(all).hasSize(2)
		assertThat(all.map { it.target }).containsExactlyInAnyOrder(
			ConstraintArgTarget.VALUE,
			ConstraintArgTarget.ELEMENT,
		)
		val valueRule = all.single { it.target == ConstraintArgTarget.VALUE }
		assertThat(valueRule.kinds).containsExactly(ConstraintArgKind.NON_EMPTY)
		val elementRule = all.single { it.target == ConstraintArgTarget.ELEMENT }
		assertThat(elementRule.kinds).containsExactlyInAnyOrder(
			ConstraintArgKind.NOT_BLANK,
			ConstraintArgKind.TYPED_LITERAL,
		)
	}
}

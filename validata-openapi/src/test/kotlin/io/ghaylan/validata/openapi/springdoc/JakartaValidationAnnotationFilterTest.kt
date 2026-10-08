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
package io.ghaylan.validata.openapi.springdoc

import io.ghaylan.validata.constraint.annotation.Min
import io.ghaylan.validata.constraint.annotation.Size
import jakarta.validation.constraints.NotNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Verifies Validata annotations are stripped while Jakarta / Hibernate BV annotations are kept.
 *
 * Guards ClassCastException while generating `/v3/api-docs` from simple-name collisions.
 * 
 * @author Ghaylan Saada
 */
class JakartaValidationAnnotationFilterTest {
	
	@Test
	@DisplayName("Validata constraint annotations are excluded from springdoc BV mapping")
	fun dropsValidataConstraints() {
		val size = Holder::class.java.getDeclaredField("name")
			.getAnnotation(Size::class.java)
		val min = Holder::class.java.getDeclaredField("age")
			.getAnnotation(Min::class.java)
		assertThat(size).isNotNull
		assertThat(min).isNotNull
		
		assertThat(JakartaValidationAnnotationFilter.isJakartaOrHibernateValidation(size)).isFalse()
		assertThat(JakartaValidationAnnotationFilter.isJakartaOrHibernateValidation(min)).isFalse()
		assertThat(JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(listOf(size, min))).isEmpty()
	}
	
	@Test
	@DisplayName("null annotation list returns null")
	fun nullListReturnsNull() {
		assertThat(JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(null)).isNull()
	}
	
	@Test
	@DisplayName("empty annotation list returns empty mutable list")
	fun emptyListReturnsEmpty() {
		val filtered = JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(emptyList())
		assertThat(filtered).isNotNull.isEmpty()
	}
	
	@Test
	@DisplayName("Jakarta NotNull is retained while Validata Size is dropped")
	fun retainsJakartaAndDropsValidata() {
		val jakartaNotNull = MixedHolder::class.java.getDeclaredField("id")
			.getAnnotation(NotNull::class.java)
		val validataSize = MixedHolder::class.java.getDeclaredField("label")
			.getAnnotation(Size::class.java)
		assertThat(jakartaNotNull).isNotNull
		assertThat(validataSize).isNotNull
		
		assertThat(JakartaValidationAnnotationFilter.isJakartaOrHibernateValidation(jakartaNotNull)).isTrue()
		val filtered = JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(listOf(jakartaNotNull, validataSize))
		assertThat(filtered).containsExactly(jakartaNotNull)
	}
	
	@Test
	@DisplayName("Hibernate Validator Length is retained")
	fun retainsHibernateLength() {
		val length = HibernateHolder::class.java.getDeclaredField("label")
			.getAnnotation(org.hibernate.validator.constraints.Length::class.java)
		assertThat(length).isNotNull
		assertThat(JakartaValidationAnnotationFilter.isJakartaOrHibernateValidation(length)).isTrue()
		assertThat(JakartaValidationAnnotationFilter.onlyJakartaOrHibernate(listOf(length))).containsExactly(length)
	}
	
	private class Holder {
		
		@field:Size(min = 1, max = 2)
		lateinit var name: String
		
		@field:Min("1")
		var age: Int = 0
	}
	
	private class MixedHolder {
		
		@field:NotNull
		lateinit var id: String
		
		@field:Size(min = 1, max = 8)
		lateinit var label: String
	}
	
	private class HibernateHolder {
		
		@field:org.hibernate.validator.constraints.Length(min = 1, max = 8)
		lateinit var label: String
	}
}

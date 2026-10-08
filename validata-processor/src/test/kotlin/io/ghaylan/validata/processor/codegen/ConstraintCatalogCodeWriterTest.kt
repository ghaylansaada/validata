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

import io.ghaylan.validata.processor.model.ConstraintCatalogAnnotationModel
import io.ghaylan.validata.processor.model.ConstraintCatalogEntryModel
import io.ghaylan.validata.processor.naming.GeneratedNames
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.ghaylan.validata.schema.types.TypeNames

/**
 * Unit coverage for [ConstraintCatalogCodeWriter] emission shape (no KSP round-trip).
 * 
 * @author Ghaylan Saada
 */
class ConstraintCatalogCodeWriterTest {
	
	@Test
	@DisplayName("per-annotation helper returns ConstraintCatalogEntry list")
	fun writeAnnotationEntries() {
		val model = ConstraintCatalogAnnotationModel(
			annotationFqcn = "com.acme.OddYears",
			annotationSimpleName = "OddYears",
			sourceFilePath = null,
			entries = listOf(
				ConstraintCatalogEntryModel(
					annotationFqcn = "com.acme.OddYears",
					metadataFqcn = "com.acme.OddYearsConstraint",
					valueTypeFqcn = TypeNames.INT_KOTLIN,
					validatorFqcn = "com.acme.OddYearsValidator",
					objectSingleton = true,
				),
			),
		)
		val source = ConstraintCatalogCodeWriter.writeAnnotationEntries(
			packageName = "com.acme.ghaylan.validata",
			model = model,
		)
		assertThat(source).contains("fun OddYearsConstraintEntries(): List<ConstraintCatalogEntry>")
		assertThat(source).contains("OddYears::class.java")
		assertThat(source).contains("OddYearsValidator")
	}
	
	@Test
	@DisplayName("aggregator implements ConstraintCatalog and delegates to helpers")
	fun writeAggregator() {
		val model = ConstraintCatalogAnnotationModel(
			annotationFqcn = "com.acme.OddYears",
			annotationSimpleName = "OddYears",
			sourceFilePath = null,
			entries = emptyList(),
		)
		val source = ConstraintCatalogCodeWriter.writeAggregator(
			packageName = "com.acme.ghaylan.validata",
			models = listOf(model),
		)
		assertThat(source).contains("class ${GeneratedNames.CONSTRAINT_CATALOG_MODULE} : ConstraintCatalog")
		assertThat(source).contains("addAll(OddYearsConstraintEntries())")
	}
}

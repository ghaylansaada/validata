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
package io.ghaylan.validata.samples.config

import io.ghaylan.validata.openapi.presentation.ErrorDocPublisher
import io.ghaylan.validata.samples.error.SampleErrorBody
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Sample-only OpenAPI wiring: registers an app-owned error-doc publisher so Swagger UI shows the
 * sample [SampleErrorBody] 400 shape. Production apps should
 * provide their own [ErrorDocPublisher] (Validata does not ship an envelope publisher).
 * 
 * @author Ghaylan Saada
 */
@Configuration
class SampleOpenApiConfig {
	
	/**
	 * @return sample-owned publisher documenting [SampleErrorBody]
	 */
	@Bean
	fun sampleErrorDocPublisher(): ErrorDocPublisher = SampleErrorDocPublisher()
}

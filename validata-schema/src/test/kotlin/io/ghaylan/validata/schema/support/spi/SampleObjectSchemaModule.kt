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
package io.ghaylan.validata.schema.support.spi

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.schema.spi.ObjectSchemaModule
import io.ghaylan.validata.schema.support.SampleObjectSchemas
import io.ghaylan.validata.schema.support.dto.Address

/**
 * Test-classpath [ObjectSchemaModule] registered via `META-INF/services` so
 * [GeneratedSchemas] exercises the real [java.util.ServiceLoader] path.
 *
 * Contributes a one-property schema for [Address] — enough to prove discovery without inventing
 * a full DTO graph. See `src/test/resources/META-INF/services/…ObjectSchemaModule`.
 * 
 * @author Ghaylan Saada
 */
class SampleObjectSchemaModule: ObjectSchemaModule {
	
	override fun schemas(): Map<Class<*>, ObjectSchema> =
		mapOf(Address::class.java to SampleObjectSchemas.of(Address::class.java))
}

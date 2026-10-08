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
package io.ghaylan.validata.schema.runtime

import io.ghaylan.validata.schema.ObjectSchema
import io.ghaylan.validata.schema.spi.GeneratedSchemas
import io.ghaylan.validata.schema.spi.ObjectSchemaModule

/**
 * Spring-free lookup for KSP-generated [ObjectSchema] graphs.
 *
 * Lives here so generated endpoint schema factories (and the processor's FQCN existence tests)
 * can resolve schemas without depending on the Spring host module. The host keeps its own
 * schema builder for ApplicationContext discovery and delegates this lookup here.*
 * 
 * @author Ghaylan Saada
 */
object GeneratedSchemaLookup {
	
	/**
	 * Returns the generated [ObjectSchema] for [rootClass], or throws [SchemaNotFoundException].
	 *
	 * No cache writes — delegates to [GeneratedSchemas.get]. Throws on miss.
	 *
	 * @param rootClass Runtime class that should have been emitted by KSP / an [ObjectSchemaModule].
	 * @return Non-null schema for [rootClass].
	 * @throws SchemaNotFoundException When no module contributed a schema for [rootClass].	 
	 */
	fun requireGeneratedSchema(rootClass: Class<*>): ObjectSchema = GeneratedSchemas.get(rootClass)
		?: throw SchemaNotFoundException.forMissingSchema(rootClass)
}

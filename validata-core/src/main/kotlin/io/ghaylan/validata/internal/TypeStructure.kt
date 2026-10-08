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
package io.ghaylan.validata.internal

/**
 * Structural classification of a [TypeInfo] — how reflection and IR producers traverse a type.
 *
 * Free of leaf kinds (`STRING`, `BOOLEAN`, …). Those live on [TypeInfo.scalarKind] or nested
 * [TypeInfo.typeArguments]. Encoding both in one enum is what T-26 removes.
 * 
 * @author Ghaylan Saada
 */
enum class TypeStructure {
	
	/**
	 * Leaf value: string, number, temporal, enum, UUID, …
	 */
	SCALAR,
	
	/**
	 * Traversable DTO / data class.
	 */
	OBJECT,
	
	/**
	 * `Map` (and subtypes). Key/value shapes are in [TypeInfo.typeArguments].
	 */
	MAP,
	
	/**
	 * Array or `Collection`. Element shape is in [TypeInfo.typeArguments].
	 */
	ARRAY,
	
	/**
	 * Unknown / erased / wildcard — opaque to traversal.
	 */
	ANY,
	
	/**
	 * `Nothing` / `Void`.
	 */
	NOTHING,
}

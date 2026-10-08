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
package io.ghaylan.validata.processor.integration

import io.ghaylan.validata.processor.SchemaProcessor

/**
 * Records deferred-list sizes returned from [SchemaProcessor.process]
 * each round.
 *
 * **Thread-confined:** sizes live in a [ThreadLocal] so parallel test workers do not clash.
 * 
 * @author Ghaylan Saada
 */
internal object DeferredRoundProbe {
	
	private class State {
		
		val deferredSizes = mutableListOf<Int>()
	}
	
	private val local = ThreadLocal.withInitial { State() }
	val deferredSizes: MutableList<Int>
		get() = local.get().deferredSizes
	
	fun reset() {
		local.get().deferredSizes.clear()
	}
}

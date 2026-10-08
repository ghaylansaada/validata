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
package io.ghaylan.validata.processor

import io.ghaylan.validata.processor.analyze.EndpointModelBuilder

/**
 * Test-visible counters for multi-round endpoint rediscovery (T8.5).
 *
 * **Thread-confined:** counters live in a [ThreadLocal] so parallel test workers do not clash.
 * Still not for cross-thread sharing of one probe state.*
 * 
 * @author Ghaylan Saada
 */
internal object SchemaProcessorRoundProbe {
	
	/**
	 * Per-thread counter state for endpoint rediscovery probes.	 
	 */
	private class State {
		
		/**
		 * Count of [EndpointModelBuilder.buildAll] invocations.		 
		 */
		var endpointBuildAllCalls: Int = 0
		
		/**
		 * Count of rounds that skipped endpoint rediscovery while schemas remained deferred.		 
		 */
		var endpointBuildAllSkippedWhileDeferred: Int = 0
		
		/**
		 * Count of [SchemaProcessor.process] calls that returned
		 * immediately because aggregators were already written.		 
		 */
		var completedEarlyReturns: Int = 0
	}
	
	/**
	 * Thread-local [State] for parallel test isolation.	 
	 */
	private val local = ThreadLocal.withInitial { State() }
	
	/**
	 * Count of [EndpointModelBuilder.buildAll] invocations.
	 *
	 * Side effects: reads or mutates thread-local [State].	 
	 */
	var endpointBuildAllCalls: Int
		get() = local.get().endpointBuildAllCalls
		set(value) {
			local.get().endpointBuildAllCalls = value
		}
	
	/**
	 * Count of rounds that skipped endpoint rediscovery while schemas remained deferred.
	 *
	 * Side effects: reads or mutates thread-local [State].	 
	 */
	var endpointBuildAllSkippedWhileDeferred: Int
		get() = local.get().endpointBuildAllSkippedWhileDeferred
		set(value) {
			local.get().endpointBuildAllSkippedWhileDeferred = value
		}
	
	/**
	 * Count of process calls that returned immediately after completion.
	 *
	 * Side effects: reads or mutates thread-local [State].	 
	 */
	var completedEarlyReturns: Int
		get() = local.get().completedEarlyReturns
		set(value) {
			local.get().completedEarlyReturns = value
		}
	
	/**
	 * Resets all counters on the current thread to zero.
	 *
	 * Side effects: mutates thread-local [State].	 
	 */
	fun reset() {
		val s = local.get()
		s.endpointBuildAllCalls = 0
		s.endpointBuildAllSkippedWhileDeferred = 0
		s.completedEarlyReturns = 0
	}
}

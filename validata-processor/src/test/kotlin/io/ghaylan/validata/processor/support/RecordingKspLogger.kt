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
package io.ghaylan.validata.processor.support

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.ghaylan.validata.processor.verify.PropertyReferenceVerifier

/**
 * Test double for [KSPLogger] that records every diagnostic for assertions.
 *
 * ## Why this exists
 *
 * Unit tests for [PropertyReferenceVerifier] and other analyzers need to assert error/warn text
 * without a KSP round-trip. Production logging goes through KSP’s real logger and fails the
 * consumer build.
 * 
 * @author Ghaylan Saada
 */
internal class RecordingKspLogger: KSPLogger {
	
	/**
	 * Messages logged at error severity (fail the build).
	 */
	val errors: MutableList<String> = mutableListOf()
	
	/**
	 * Symbols attached to error diagnostics (may include nulls).
	 */
	val errorSymbols: MutableList<KSNode?> = mutableListOf()
	
	/**
	 * Messages logged at warn severity.
	 */
	val warnings: MutableList<String> = mutableListOf()
	
	/**
	 * Messages logged at info / logging severity.
	 */
	val infos: MutableList<String> = mutableListOf()
	
	override fun error(
		message: String,
		symbol: KSNode?
	) {
		errors += message
		errorSymbols += symbol
	}
	
	override fun warn(
		message: String,
		symbol: KSNode?
	) {
		warnings += message
	}
	
	override fun info(
		message: String,
		symbol: KSNode?
	) {
		infos += message
	}
	
	override fun logging(
		message: String,
		symbol: KSNode?
	) {
		infos += message
	}
	
	override fun exception(e: Throwable) {
		errors += (e.message
			?: e.toString())
	}
}

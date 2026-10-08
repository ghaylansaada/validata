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
package io.ghaylan.validata.constraint.validator.string

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Reuses one [Matcher] per thread / [Pattern] so hot-path `matches()` does not allocate.
 *
 * [Matcher] is not thread-safe; each worker thread holds its own instance. When the cached
 * matcher was created for a different [Pattern], it is replaced (constraint sites that share
 * a compiled pattern keep the same matcher).
 *
 * @author Ghaylan Saada
 */
internal object PatternMatcherReuse {
	
	private val holder = ThreadLocal<Matcher?>()
	
	/**
	 * Whether [input] fully matches [pattern], without allocating a matcher on the steady path.
	 *
	 * Side effects: may allocate the first matcher for this thread (or when the pattern changes).
	 *
	 * @param pattern Compiled pattern (must be retained for the life of the constraint site).
	 * @param input Subject under test.
	 * @return `true` when [Matcher.matches] succeeds after [Matcher.reset].	 
	 */
	fun matches(
		pattern: Pattern,
		input: CharSequence,
	): Boolean {
		var matcher = holder.get()
		if (matcher == null || matcher.pattern() !== pattern) {
			matcher = pattern.matcher(input)
			holder.set(matcher)
			return matcher.matches()
		}
		return matcher.reset(input)
			.matches()
	}
}

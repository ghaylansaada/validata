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
package io.ghaylan.validata.engine.support

/**
 * Comparator for constraint error paths using natural (numeric-aware) ordering.
 *
 * Unlike lexicographical string comparison, consecutive digit runs are compared as numbers:
 * `items[2]` sorts before `items[10]`.*
 * 
 * @author Ghaylan Saada
 */
internal object NaturalPathOrder: Comparator<String> {
	
	/**
	 * Compares [leftPath] and [rightPath] with numeric runs ordered by integer value.
	 *
	 * No mutation.
	 *
	 * @param leftPath First error path.
	 * @param rightPath Second error path.
	 * @return Negative / zero / positive per [Comparator] contract.	 
	 */
	override fun compare(
		leftPath: String,
		rightPath: String
	): Int {
		var leftIndex = 0
		var rightIndex = 0
		
		while (leftIndex < leftPath.length && rightIndex < rightPath.length) {
			val leftChar = leftPath[leftIndex]
			val rightChar = rightPath[rightIndex]
			
			if (leftChar.isDigit() && rightChar.isDigit()) {
				val leftNumberEnd = digitSequenceEnd(leftPath, leftIndex)
				val rightNumberEnd = digitSequenceEnd(rightPath, rightIndex)
				val leftNumber = leftPath.substring(leftIndex, leftNumberEnd)
					.trimStart('0')
				val rightNumber = rightPath.substring(rightIndex, rightNumberEnd)
					.trimStart('0')
				
				if (leftNumber.length != rightNumber.length) {
					return leftNumber.length - rightNumber.length
				}
				val numberComparison = leftNumber.compareTo(rightNumber)
				if (numberComparison != 0) {
					return numberComparison
				}
				
				leftIndex = leftNumberEnd
				rightIndex = rightNumberEnd
			}
			else {
				if (leftChar != rightChar) {
					return leftChar.compareTo(rightChar)
				}
				
				leftIndex++
				rightIndex++
			}
		}
		
		return (leftPath.length - leftIndex) - (rightPath.length - rightIndex)
	}
	
	/**
	 * Finds the index past the last digit starting at [startIndex].
	 *
	 * @param text Path being scanned.
	 * @param startIndex Index of the first digit.
	 * @return Exclusive end index of the digit run.	 
	 */
	private fun digitSequenceEnd(
		text: String,
		startIndex: Int
	): Int {
		var endIndex = startIndex
		while (endIndex < text.length && text[endIndex].isDigit()) {
			endIndex++
		}
		return endIndex
	}
}

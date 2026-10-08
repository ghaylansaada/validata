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
package io.ghaylan.validata.constraint.validator.string.filepath

import io.ghaylan.validata.constraint.annotation.FilePathConstraint
import io.ghaylan.validata.model.ConstraintErrorCode
import io.ghaylan.validata.support.ValidatorTestSupport
import io.ghaylan.validata.support.ValidatorTestSupport.assertInvalid
import io.ghaylan.validata.support.ValidatorTestSupport.assertSkipsNull
import io.ghaylan.validata.support.ValidatorTestSupport.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit tests for [FilePathValidator] — single relative-path policy.
 * 
 * @author Ghaylan Saada
 */
@DisplayName("FilePathValidator")
class FilePathValidatorTest {
	
	private fun c(requireExtension: Boolean = false) = FilePathConstraint(
		requireExtension = requireExtension,
		message = "",
		groups = ValidatorTestSupport.defaultGroups,
	)
	
	@Nested
	@DisplayName("null handling")
	inner class NullHandling {
		
		@Test
		@DisplayName("null value is skipped")
		fun skipsNull() {
			assertSkipsNull(FilePathValidator, c())
		}
	}
	
	@Nested
	@DisplayName("relative paths")
	inner class RelativePaths {
		
		private val constraint = c()
		
		@Test
		@DisplayName("simple basename passes")
		fun simpleName() {
			assertValid(FilePathValidator, "report.pdf", constraint)
		}
		
		@Test
		@DisplayName("relative multi-segment path passes")
		fun relativePath() {
			assertValid(FilePathValidator, "docs/readme.md", constraint)
		}
		
		@Test
		@DisplayName("backslash relative path passes")
		fun backslashRelative() {
			assertValid(FilePathValidator, "docs\\readme.md", constraint)
		}
	}
	
	@Nested
	@DisplayName("rejections")
	inner class Rejections {
		
		private val constraint = c()
		
		@Test
		@DisplayName("absolute Unix path fails with VALUE_NOT_ALLOWED")
		fun absoluteUnix() {
			assertInvalid(
				FilePathValidator,
				"/etc/passwd",
				constraint,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("Windows drive prefix fails with VALUE_NOT_ALLOWED")
		fun drivePrefix() {
			assertInvalid(
				FilePathValidator,
				"C:\\Windows\\system.ini",
				constraint,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("UNC path fails with VALUE_NOT_ALLOWED")
		fun uncPath() {
			assertInvalid(
				FilePathValidator,
				"\\\\server\\share\\file.txt",
				constraint,
				ConstraintErrorCode.VALUE_NOT_ALLOWED,
			)
		}
		
		@Test
		@DisplayName("parent segment fails with VALUE_FORMAT_INVALID")
		fun parentSegment() {
			assertInvalid(
				FilePathValidator,
				"../secret.txt",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("forbidden character fails with VALUE_FORMAT_INVALID")
		fun forbiddenChar() {
			assertInvalid(
				FilePathValidator,
				"bad<file>.txt",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("blank value fails with VALUE_FORMAT_INVALID")
		fun blank() {
			assertInvalid(
				FilePathValidator,
				"   ",
				constraint,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
	}
	
	@Nested
	@DisplayName("requireExtension")
	inner class RequireExtension {
		
		private val required = c(requireExtension = true)
		private val optional = c(requireExtension = false)
		
		@Test
		@DisplayName("last segment carrying an extension passes")
		fun withExtension() {
			assertValid(FilePathValidator, "report.pdf", required)
			assertValid(FilePathValidator, "docs/readme.md", required)
			assertValid(FilePathValidator, "archive.tar.gz", required)
		}
		
		@Test
		@DisplayName("extensionless last segment fails with VALUE_FORMAT_INVALID")
		fun withoutExtension() {
			assertInvalid(
				FilePathValidator,
				"docs/README",
				required,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("dotfiles and trailing dots do not count as extensions")
		fun degenerateDots() {
			assertInvalid(
				FilePathValidator,
				".gitignore",
				required,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
			assertInvalid(
				FilePathValidator,
				"report.",
				required,
				ConstraintErrorCode.VALUE_FORMAT_INVALID,
			)
		}
		
		@Test
		@DisplayName("extension check is skipped when disabled")
		fun skippedWhenDisabled() {
			assertValid(FilePathValidator, "docs/README", optional)
			assertValid(FilePathValidator, ".gitignore", optional)
		}
	}
}

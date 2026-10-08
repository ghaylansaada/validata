package test.flat

import io.ghaylan.validata.constraint.annotation.Compare

data class RegisterRequest(
	val password: String,
	@Compare(ref = "password", operation = Compare.Operation.EQ)
	val confirmation: String,
)

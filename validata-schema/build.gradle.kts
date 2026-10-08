description = """
	Shared validation schema model and intermediate representation for Validata. \
	Defines ObjectSchema, EndpointSchema, path contracts, and scalar contracts. \
	Consumed by the KSP processor, runtime engine, OpenAPI integration, and IntelliJ plugin. \
	Contains no validation logic and has no Spring dependency.
""".trimIndent()

dependencies {
	testImplementation(libs.assertj.core)
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}
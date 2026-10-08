description = """
	KSP annotation processor for Validata. \
    Verifies constraint metadata at compile time and generates ObjectSchema / EndpointSchema factories plus constraint catalogs. \
    Apply with ksp(...); never ship at runtime.
""".trimIndent()

dependencies {
	// --- Validata ---
	implementation(libs.validata.schema)

	// --- KSP API ---
	implementation(libs.ksp.symbol.processing.api)

	// --- Test: Validata runtime / OpenAPI (FQCN load + @ApiError fixtures) ---
	testImplementation(libs.validata.core)
	testImplementation(libs.validata.openapi)

	// --- Test: compile-testing ---
	testImplementation(libs.kctfork.core)
	testImplementation(libs.kctfork.ksp)
	testImplementation(libs.jackson.annotations)

	// --- Test: Spring Web annotation FQCNs on the processor test classpath ---
	testImplementation(platform(libs.spring.boot.dependencies))
	testImplementation(libs.spring.web)

	// --- Test ---
	testImplementation(libs.assertj.core)
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}
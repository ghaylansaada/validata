import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
	alias(libs.plugins.ksp)
}

description = """
	Spring-free Validata runtime: constraint annotations, built-in validators, ValidatorEngine, and registry APIs. \
    Use directly for non-Spring hosts, or transitively via the validata Boot module.
""".trimIndent()

dependencies {
	// --- Validata ---
	api(libs.validata.schema)
	ksp(libs.validata.processor)

	// --- Wire names for error-context DTOs (hosts serialize ConstraintError.context) ---
	api(libs.jackson.annotations)

	// --- Optional constraint backends (@HTML / @Phone; not transitive) ---
	compileOnly(libs.jsoup)
	compileOnly(libs.libphonenumber)

	// --- Runtime ---
	implementation(libs.kotlin.reflect)

	// --- Test ---
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.assertj.core)
	testRuntimeOnly(libs.junit.platform.launcher)
}

configurations {
	testImplementation.extendsFrom(compileOnly)
}
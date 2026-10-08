import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
	alias(libs.plugins.kotlin.spring)
}

description = """
	Optional springdoc / OpenAPI companion for Validata.\
    Overlays compile-time validation IR onto OpenAPI schemas and operations. \
    Does not run validation and does not pin Spring Boot or springdoc — add both in your application.
""".trimIndent()

dependencies {
	// --- Validata ---
	// api: SPI uses ConstraintMetadata, registry, schema shapes
	api(libs.validata.core)

	// --- Spring / springdoc (provided by the app — consumer owns versions) ---
	compileOnly(platform(libs.spring.boot.dependencies))
	compileOnly(libs.spring.boot.autoconfigure)
	compileOnly(libs.springdoc.openapi.starter.webmvc.ui)
	// Required at runtime for ConstraintMetadataOpenApiSerializer (kotlin.reflect.full).
	implementation(libs.kotlin.reflect)

	// --- Test ---
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit.jupiter.params)
	testImplementation(libs.assertj.core)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.web)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.libphonenumber)
	testRuntimeOnly(libs.junit.platform.launcher)
}

configurations {
	testImplementation.extendsFrom(compileOnly)
}
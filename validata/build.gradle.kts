import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
}

description = """
	Spring Boot WebMVC integration for Validata. \
    Auto-configures request validation, endpoint-schema bootstrap, and binding-error translation. \
    Does not pin Spring Boot — your app supplies spring-boot-starter-web / webmvc.
""".trimIndent()

dependencies {
	// --- Validata ---
	// api: consumers compile against @Validate, constraints, ConstraintViolationException, etc.
	api(libs.validata.core)
	kspTest(libs.validata.processor)

	// --- Spring / Jackson (provided by the Boot app — consumer owns versions) ---
	compileOnly(platform(libs.spring.boot.dependencies))
	compileOnly(libs.spring.boot.webmvc)
	compileOnly(libs.spring.boot.autoconfigure)
	compileOnly(libs.kotlin.reflect)
	compileOnly(libs.jakarta.servlet.api)
	compileOnly(libs.jackson.annotations)
	compileOnly(libs.jackson.databind)

	// --- Test ---
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.web)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.jackson.module.kotlin)
	testRuntimeOnly(libs.junit.platform.launcher)
}

configurations {
	testImplementation.extendsFrom(compileOnly)
}
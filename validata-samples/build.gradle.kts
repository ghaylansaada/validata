import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.graalvm.native)
}

description = """
	Reference Spring Boot application demonstrating Validata end-to-end: \
	annotations, KSP, HTTP validation, optional OpenAPI enrichment, and custom constraints."
""".trimIndent()

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	// --- Validata ---
	implementation(libs.validata)
	implementation(libs.validata.openapi)
	ksp(libs.validata.processor)

	// --- Optional constraint backends (@HTML / @Phone) ---
	implementation(libs.jsoup)
	implementation(libs.libphonenumber)

	// --- Spring Boot / springdoc (app owns versions; Validata host deps are compileOnly) ---
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.kotlin.reflect)

	// --- Test ---
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_21)
		progressiveMode.set(true)
		freeCompilerArgs.add("-Xjsr305=strict")
		freeCompilerArgs.add("-Xemit-jvm-type-annotations")
		optIn.add("kotlin.RequiresOptIn")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

graalvmNative {
	binaries {
		// Main Application Binary
		named("main") {
			imageName.set("validata-samples")
			// Ensure build fails if standalone image cannot be created (disables JVM fallback)
			buildArgs.add("--no-fallback")
		}

		// Native Test Binary
		named("test") {
			buildArgs.add("--no-fallback")
		}
	}

	// Automatically pull community metadata for third-party libraries (jsoup, libphonenumber, etc.)
	metadataRepository {
		enabled.set(true)
	}
}

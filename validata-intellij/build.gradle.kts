import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm")
	id("org.jetbrains.intellij.platform")
}

description = """
	IntelliJ IDEA plugin for Validata. \
    Editor support for constraint property-path string literals: completion, unresolved highlighting, navigation, and rename. \
    Not an application dependency.
""".trimIndent()

dependencies {
	// --- Validata ---
	// Shared IR contracts (enums, PropertyPath, PropertyRefScalarCompatibility).
	implementation(libs.validata.schema)

	// --- IntelliJ Platform ---
	intellijPlatform {
		// Unified IDEA (no Community IC builds since 2025.3).
		// useInstaller=false: Maven multi-OS archive — more reliable for headless platform tests
		// than the full installer (avoids Ultimate licensing extension failures).
		intellijIdea(providers.gradleProperty("intellij.idea.version").get()) {
			useInstaller = false
		}
		bundledPlugin("com.intellij.java")
		bundledPlugin("org.jetbrains.kotlin")
		testFramework(TestFrameworkType.Platform)
	}

	// --- Test ---
	// JUnit 5 for unit tests; JUnit 4 + Vintage for platform light tests (BasePlatformTestCase).
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit4)
	testRuntimeOnly(libs.junit.platform.launcher)
	testRuntimeOnly(libs.junit.vintage.engine)
	testImplementation(libs.assertj.core)
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_21)
		progressiveMode.set(true)
		freeCompilerArgs.add("-Xjsr305=strict")
	}
}

intellijPlatform {
	pluginConfiguration {
		id = "io.ghaylansaada.validata"
		name = "Validata"
		version = "1.0.0"
		description = """
		    Intelligent IntelliJ IDEA support for Validata. Get autocomplete, navigation,
		    rename refactoring, semantic highlighting, and real-time diagnostics for
		    property references, typed values, enums, regular expressions, composed
		    constraints, polymorphism, and API error codes. Supports built-in and custom
		    Validata constraints through the same metadata-driven model used by the
		    framework's compile-time processor.
		    """.trimIndent()
		ideaVersion {
			sinceBuild = providers.gradleProperty("intellij.since.build").get()
		}
		vendor {
			name = "Ghaylan Saada"
			url = "https://github.com/ghaylansaada/validata"
		}
	}
	instrumentCode = false
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Unified IDEA 2026.2+ ships Ultimate licensing (com.intellij.modules.ultimate).
// Its post-startup extension fails constructor injection in headless platform tests
// ("Cannot create extension class=B.B.B.B.s"). We don't need Ultimate; disable it.
tasks.named<PrepareSandboxTask>("prepareTestSandbox") {
	disabledPlugins.add("com.intellij.modules.ultimate")
}
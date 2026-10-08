pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "validata"

include("validata-schema")
include("validata-core")
include("validata")
include("validata-openapi")
include("validata-processor")
include("validata-benchmarks")
include("validata-samples")
includeBuild("validata-intellij")
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "validata-intellij"

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
	plugins {
		id("org.jetbrains.kotlin.jvm") version providers.gradleProperty("kotlin.jvm.version").get()
		id("org.jetbrains.intellij.platform") version providers.gradleProperty("intellij.platform.version").get()
		id("org.jetbrains.intellij.platform.settings") version providers.gradleProperty("intellij.platform.version").get()
	}
}

plugins {
	id("org.jetbrains.intellij.platform.settings")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenLocal()
		mavenCentral()
		intellijPlatform {
			defaultRepositories()
		}
	}
	versionCatalogs {
		create("libs") {
			from(files("../gradle/libs.versions.toml"))
		}
	}
}

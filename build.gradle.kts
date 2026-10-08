import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Root project for Validata.
 *
 * This project contains no library sources. It coordinates the multi-module build.
 *
 * Published library modules:
 *
 *   schema → processor → core → validata / openapi
 *
 * Non-published modules:
 *
 *   samples
 *   benchmarks
 *
 * The IntelliJ plugin is maintained separately as an included build and therefore
 * intentionally does not participate in the common JVM/library conventions below.
 *
 * -------------------------------------------------------------------------------
 * Build conventions owned here
 * -------------------------------------------------------------------------------
 *
 * - repositories
 * - Java/Kotlin toolchain for library modules
 * - Kotlin compiler defaults
 * - common test configuration
 * - java-library plugin
 * - Maven Central publishing plugin
 * - Automatic-Module-Name
 * - Spotless
 * - Spring test configuration
 * - inter-module Maven Local publication ordering
 *
 * Module build.gradle.kts files should therefore contain primarily:
 *
 * - module description
 * - module-specific plugins
 * - dependencies
 * - exceptional compiler configuration
 * - module-specific tasks/configuration
 */
plugins {
	base
	// Plugin versions are declared centrally in the version catalog.
	// Individual modules apply only the plugins they actually need.
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.spring) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.vanniktech.maven.publish) apply false
	alias(libs.plugins.jmh) apply false
	alias(libs.plugins.spring.boot) apply false
	alias(libs.plugins.spring.dependency.management) apply false
	alias(libs.plugins.graalvm.native) apply false
	alias(libs.plugins.diffplug.spotless) apply false
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

/*
 * Keep the root description here instead of root gradle.properties.
 *
 * Gradle project properties can be inherited by subprojects, which would
 * otherwise risk overriding the individual published module descriptions.
 */
description = "Validata — compile-time validation for Kotlin and Spring Boot (multi-module library build)"

/**
 * Published Validata library modules.
 *
 * These modules receive the common Java-library and Maven-publishing conventions.
 *
 * Modules intentionally depend on published Maven coordinates instead
 * of project(...) so that the build exercises the same artifact boundaries used
 * by real consumers.
 *
 * Therefore the artifacts must be available in Maven Local in dependency order:
 *
 *     schema
 *       ↓
 *     processor
 *       ↓
 *     core
 *       ↓
 *     validata / openapi
 */
val publishedModules = setOf(
	"validata-schema",
	"validata-processor",
	"validata-core",
	"validata",
	"validata-openapi")

/**
 * Preserve a consistent group/version across every subproject.
 */
allprojects {
	group = rootProject.group
	version = rootProject.version
}

subprojects {
	
	/*
	 * ---------------------------------------------------------------------------
	 * Repositories
	 * ---------------------------------------------------------------------------
	 *
	 * mavenLocal() is intentionally first because Validata modules depend on
	 * sibling modules using Maven coordinates rather than project(...).
	 *
	 * This allows the build to test the exact published artifact shape.
	 */
	repositories {
		mavenLocal()
		mavenCentral()
	}
	
	/*
	 * ---------------------------------------------------------------------------
	 * Published-library conventions
	 * ---------------------------------------------------------------------------
	 *
	 * Every published Validata module is a Java library and is published through
	 * Vanniktech Maven Publish.
	 *
	 * Tooling modules such as samples/benchmarks are not affected.
	 */
	if (name in publishedModules) {
		pluginManager.apply("java-library")
		pluginManager.apply("org.jetbrains.kotlin.jvm")
		pluginManager.apply("com.vanniktech.maven.publish")

		/*
		 * Vanniktech maps POM name/description from POM_NAME / POM_DESCRIPTION properties,
		 * not from Gradle's project.description. Wire the module description (set in each
		 * module's build.gradle.kts) into the POM so Central Portal validation passes.
		 */
		afterEvaluate {
			// Module descriptions use "\" as soft line wraps in source — strip them for POM text.
			val pomDescription = requireNotNull(description?.trim()?.takeIf { it.isNotEmpty() }) {
				"${project.path}: set description in build.gradle.kts for Maven Central POM"
			}.replace("\\", "")
				.lines()
				.joinToString(" ") { it.trim() }
				.replace(Regex("\\s+"), " ")
				.trim()

			val pomName = name.split('-').joinToString(" ") { part ->
				when (part.lowercase()) {
					"openapi" -> "OpenAPI"
					else -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
				}
			}

			extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
				pom {
					name.set(pomName)
					description.set(pomDescription)
				}
			}
		}
	}
	
	/*
	 * ---------------------------------------------------------------------------
	 * Kotlin/JVM conventions
	 * ---------------------------------------------------------------------------
	 *
	 * All Kotlin library modules target JVM 21.
	 *
	 * This is the consumer compatibility baseline. The development environment
	 * may use a newer JDK, but the published libraries remain JVM 21 compatible.
	 *
	 * The IntelliJ plugin is an intentional exception and overrides this with
	 * JVM 25 in its own build.gradle.kts.
	 */
	plugins.withId("org.jetbrains.kotlin.jvm") {
		extensions.configure<KotlinJvmProjectExtension> {
			jvmToolchain(21)
			
			compilerOptions {
				jvmTarget.set(JvmTarget.JVM_21)
				
				/*
				 * Kotlin progressive mode is part of the project-wide compiler
				 * policy. This lets the project opt into newer compiler behavior
				 * where appropriate.
				 */
				progressiveMode.set(true)
				
				/*
				 * Treat JSR-305 annotations strictly.
				 *
				 * Particularly useful for Java/Spring APIs where nullability
				 * annotations influence Kotlin type safety.
				 */
				freeCompilerArgs.add("-Xjsr305=strict")
				
				/*
				 * All library modules are allowed to use Kotlin's opt-in mechanism.
				 */
				optIn.add("kotlin.RequiresOptIn")
			}
		}
	}
	
	/*
	 * ---------------------------------------------------------------------------
	 * Common test conventions
	 * ---------------------------------------------------------------------------
	 *
	 * Every Java/Kotlin library module uses JUnit Platform.
	 *
	 * The detailed test dependencies remain in each module because they differ
	 * between modules.
	 */
	plugins.withId("java") {
		tasks.withType<Test>().configureEach {
			useJUnitPlatform()
			testLogging {
				events("passed", "skipped", "failed")
				showStandardStreams = false
			}
		}
	}
	
	/*
	 * ---------------------------------------------------------------------------
	 * Automatic-Module-Name
	 * ---------------------------------------------------------------------------
	 *
	 * The artifact names map directly to the desired JPMS automatic module names:
	 *
	 *     validata-schema → io.ghaylan.validata.schema
	 *     validata-processor → io.ghaylan.validata.processor
	 *     validata-core → io.ghaylan.validata.core
	 *     validata → io.ghaylan.validata
	 *     validata-openapi → io.ghaylan.validata.openapi
	 *
	 * Keeping this here eliminates the same manifest block from every module.
	 */
	if (name in publishedModules) {
		tasks.withType<Jar>().configureEach {
			manifest {
				attributes("Automatic-Module-Name" to "io.ghaylan.${project.name.replace("-", ".")}")
			}
		}
	}
	
	/*
	 * ---------------------------------------------------------------------------
	 * Spotless
	 * ---------------------------------------------------------------------------
	 *
	 * Formatting/license policy is shared by every subproject.
	 */
	plugins.apply(rootProject.libs.plugins.diffplug.spotless.get().pluginId)
	
	configure<SpotlessExtension> {
		kotlin {
			// Targets all Kotlin files in src directories
			target("src/**/*.kt")
			
			licenseHeader("""
				/*
				 * Copyright 2026 Ghaylan Saada
				 *
				 * Licensed under the Apache License, Version 2.0 (the "License");
				 * you may not use this file except in compliance with the License.
				 * You may obtain a copy of the License at
				 *
				 *     https://www.apache.org/licenses/LICENSE-2.0
				 *
				 * Unless required by applicable law or agreed to in writing, software
				 * distributed under the License is distributed on an "AS IS" BASIS,
				 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
				 * See the License for the specific language governing permissions and
				 * limitations under the License.
				 */
			""".trimIndent())
		}
	}
}

/**
 * Consumer → Maven Local prerequisites required before compilation/KSP.
 *
 * These dependencies describe BUILD ordering, not runtime dependencies.
 *
 * The actual library dependencies remain declared in the individual modules.
 */
val mavenLocalCompilePrerequisites = mapOf(
	"validata-processor" to listOf("validata-schema"),
	"validata-core" to listOf("validata-schema", "validata-processor"),
	"validata" to listOf("validata-core", "validata-processor"),
	"validata-openapi" to listOf("validata-core"),
	"validata-samples" to listOf("validata", "validata-openapi", "validata-processor"),
	"validata-benchmarks" to listOf("validata", "validata-processor"))

/**
 * Additional Maven Local prerequisites needed only by tests.
 *
 * These are deliberately kept separate from the main compilation graph.
 *
 * In particular, validata-processor tests consume validata-core and
 * validata-openapi, but the processor itself must not have those dependencies
 * during main compilation because that would introduce a KSP/publish cycle.
 */
val mavenLocalTestPrerequisites = mapOf(
	"validata-processor" to listOf("validata-core", "validata-openapi"),
)

/**
 * Publish every library module to Maven Local.
 *
 * The individual module publish tasks are ordered below after project
 * evaluation.
 */
tasks.register("publishToMavenLocal") {
	group = "publishing"
	description = "Publish every Validata library module to Maven Local"
	dependsOn(publishedModules.map { ":$it:publishToMavenLocal" })
}

/**
 * Publish every library module to the configured remote repository.
 *
 * The Vanniktech Maven Publish plugin is responsible for the actual publishing
 * configuration.
 */
tasks.register("publish") {
	group = "publishing"
	description = "Publish every Validata library module"
	dependsOn(publishedModules.map { ":$it:publish" })
}

/**
 * Publish every library module to Maven Central.
 *
 * Vanniktech's Maven Central configuration determines whether the publication
 * is automatically released or remains pending in the Central Portal.
 */
tasks.register("publishToMavenCentral") {
	group = "publishing"
	description = "Publish every Validata library module to Maven Central"
	dependsOn(publishedModules.map { ":$it:publishToMavenCentral" })
}

/*
 * -----------------------------------------------------------------------------
 * Maven Local build wiring
 * -----------------------------------------------------------------------------
 *
 * The module dependencies intentionally use Maven coordinates:
 *
 *     api(libs.validata.schema)
 *
 * instead of:
 *
 *     api(project(":validata-schema"))
 *
 * This tests the actual published-artifact boundaries.
 *
 * Because Gradle does not automatically understand that the sibling artifact
 * must first be published to Maven Local, we explicitly add those publication
 * tasks as prerequisites.
 */
gradle.projectsEvaluated {
	
	/**
	 * Orders publication tasks without introducing additional dependency edges.
	 *
	 * The root publication tasks already depend on every module publication.
	 * mustRunAfter therefore gives them deterministic ordering without creating
	 * an unnecessary dependency graph.
	 */
	fun order(taskName: String) {
		publishedModules
			.zipWithNext()
			.forEach { (before, after) ->
				val earlier = tasks.findByPath(":$before:$taskName") ?: return@forEach
				val later = tasks.findByPath(":$after:$taskName") ?: return@forEach
				later.mustRunAfter(earlier)
			}
	}
	order("publishToMavenLocal")
	order("publish")
	order("publishToMavenCentral")
	
	/**
	 * Makes a consumer wait for its sibling artifacts to be published to
	 * Maven Local before the relevant build tasks execute.
	 *
	 * @param consumer module that consumes the prerequisites
	 * @param prerequisites modules that must be published first
	 * @param taskFilter selects the consumer tasks that require the artifacts
	 */
	fun wirePublishBefore(
		consumer: String,
		prerequisites: List<String>,
		taskFilter: (String) -> Boolean,
	) {
		val consumerProject = findProject(":$consumer") ?: return
		val publicationTasks = prerequisites.map { ":$it:publishToMavenLocal" }
		consumerProject.tasks
			.matching { taskFilter(it.name) }
			.configureEach {
				dependsOn(publicationTasks)
			}
	}
	
	/*
	 * Main compilation, KSP, and test compilation need fresh sibling artifacts.
	 */
	mavenLocalCompilePrerequisites.forEach { (consumer, prerequisites) ->
		wirePublishBefore(consumer = consumer, prerequisites = prerequisites) { taskName ->
			taskName == "compileKotlin" || taskName.startsWith("ksp")
		}
		wirePublishBefore(consumer = consumer, prerequisites = prerequisites) { taskName ->
			taskName == "compileTestKotlin"
		}
	}
	
	/*
	 * Some processor tests have additional dependencies that must be published
	 * first, but those dependencies must NOT be introduced into the processor's
	 * main compilation graph.
	 */
	mavenLocalTestPrerequisites.forEach { (consumer, prerequisites) ->
		wirePublishBefore(consumer = consumer, prerequisites = prerequisites) { taskName ->
			taskName == "compileTestKotlin" || taskName == "test"
		}
	}
}

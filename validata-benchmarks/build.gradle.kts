import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.ksp)
	alias(libs.plugins.jmh)
}

description = """
	"JMH in-memory benchmarks comparing Validata validation throughput
	and allocation against Hibernate Validator (fair twin matrix)."
""".trimIndent()

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	// --- Validata ---
	implementation(libs.validata)
	ksp(libs.validata.processor)

	// --- Spring (Validata harness registry bootstrap only; no web) ---
	implementation(platform(libs.spring.boot.dependencies))
	implementation(libs.spring.context)

	// --- Report CLI JSON parse ---
	implementation(libs.jackson.databind)
	implementation(libs.kotlin.reflect)

	// --- Benchmark baseline (Hibernate Validator) ---
	implementation(libs.hibernate.validator)
	implementation(libs.expressly)

	// --- Test ---
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit.jupiter.params)
	testImplementation(libs.assertj.core)
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
	testLogging {
		events("passed", "skipped", "failed")
	}
}

// --- Dynamic JMH Property Evaluation ---
// -PjmhPrecise → README publish quality (10 forks, longer iters; ~6–8 h for 64 in-mem cells)
// default → smoke (1 fork; do not publish absolute numbers)
val precise = providers.gradleProperty("jmhPrecise").map {
	it.isEmpty() || it.equals("true", ignoreCase = true)
}.orElse(false).get()

jmh {
	// Precise: 10 forks × (5×2s warmup + 10×2s measure) ≈ high confidence in-memory only
	warmupIterations = if (precise) 5 else 1
	iterations = if (precise) 10 else 3
	fork = if (precise) 10 else 1
	warmup = if (precise) "2s" else "1s"
	timeOnIteration = if (precise) "2s" else "1s"
	failOnError = true
	duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
	resultFormat.set("JSON")
	resultsFile.set(layout.buildDirectory.file("results/jmh/results.json"))

	// Same GC regime for Validata and HV
	jvmArgsAppend.addAll(listOf("-Xms2g", "-Xmx2g"))

	val prof = providers.gradleProperty("jmhProf").orNull ?: "all"
	when (prof) {
		"none" -> Unit
		"gc" -> profilers.set(listOf("gc"))
		"cpu" -> profilers.set(listOf("io.ghaylan.validata.benchmarks.profile.ProcessResourceProfiler"))
		else -> profilers.set(
			listOf("gc", "io.ghaylan.validata.benchmarks.profile.ProcessResourceProfiler"),
		)
	}

	providers.gradleProperty("jmhInclude").orNull?.let { pattern ->
		includes.set(listOf(pattern))
	}
}

tasks.named("jmh") {
	doFirst {
		val results = layout.buildDirectory.file("results/jmh/results.json").get().asFile
		results.parentFile.mkdirs()
		if (results.exists()) results.writeText("")

		if (precise) {
			logger.lifecycle("JMH precise (in-memory only): forks=10, warmup=5×2s, measure=10×2s, heap=-Xms2g/-Xmx2g (target ~6–8 hours for 64 cells)")
		} else {
			logger.lifecycle("JMH smoke (1 fork) — do not publish absolute numbers")
		}
	}
}

tasks.register<JavaExec>("benchmarkReport") {
	group = "verification"
	description = "Embed JMH in-memory comparison tables into README.md"

	dependsOn("classes", "jmh")

	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("io.ghaylan.validata.benchmarks.report.BenchmarkReportMain")
	workingDir = rootProject.projectDir

	val fragment = layout.buildDirectory.file("results/jmh/report-fragment.md")

	args(
		layout.buildDirectory.file("results/jmh/results.json").get().asFile.absolutePath,
		fragment.get().asFile.absolutePath)

	onlyIf {
		val f = layout.buildDirectory.file("results/jmh/results.json").get().asFile
		f.isFile && f.length() > 2
	}

	doLast {
		val readme = project.file("README.md")
		val start = "<!-- BENCHMARK_RESULTS_START -->"
		val end = "<!-- BENCHMARK_RESULTS_END -->"
		val body = fragment.get().asFile.readText().trimEnd() + "\n"
		val text = readme.readText()
		require(text.contains(start) && text.contains(end)) {
			"README.md must contain $start and $end markers for benchmarkReport"
		}
		val before = text.substringBefore(start)
		val after = text.substringAfter(end)
		readme.writeText(before + start + "\n\n" + body + "\n" + end + after)
		logger.lifecycle("Updated ${readme.absolutePath} with JMH results")
	}
}

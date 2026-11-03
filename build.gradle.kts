import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.pitest) apply false
    application
}

repositories {
    mavenCentral()
}

// Builds with whichever JDK runs Gradle (17 or later), producing classes that run on 17.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.pitest)
    testImplementation(libs.archunit)
}

application {
    mainClass = "martians.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

// The PIT plugin (1.19.0) calls a Gradle API deprecated in Gradle 9 as soon as it is applied, so apply it only when
// mutation testing is asked for; other builds then run without deprecation warnings.
if (gradle.startParameter.taskNames.any { it.endsWith("pitest") }) {
    apply(plugin = "info.solidsoft.pitest")
    configure<PitestPluginExtension> {
        pitestVersion = libs.versions.pitest.get()
        targetClasses = setOf("martians.*")
        mutators = setOf("STRONGER")
        threads = Runtime.getRuntime().availableProcessors()
        outputFormats = setOf("HTML", "XML")
        timestampedReports = false
        excludedMethods = setOf("main")
    }
}

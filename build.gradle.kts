plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.pitest)
    alias(libs.plugins.jmh)
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.pitest)
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

pitest {
    pitestVersion = libs.versions.pitest.get()
    targetClasses = setOf("martians.*")
    mutators = setOf("STRONGER")
    threads = Runtime.getRuntime().availableProcessors()
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
    excludedMethods = setOf("main")
}

jmh {
    jmhVersion = libs.versions.jmh.get()
    includeTests = true
}

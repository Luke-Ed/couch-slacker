import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("java-library")
    `maven-publish`
    id("project-report")
    id("com.diffplug.spotless")
    id("com.github.ben-manes.versions")
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(libs.org.springframework.boot.spring.boot)
    api(libs.org.springframework.boot.spring.boot.autoconfigure)
    api(libs.org.springframework.spring.tx)
    api(libs.org.springframework.spring.beans)
    api(libs.org.springframework.spring.context)
    api(libs.org.springframework.data.spring.data.commons)
    api(libs.org.apache.httpcomponents.httpclient)
    api(libs.com.squareup.okhttp3.okhttp)
    api(libs.commons.codec.commons.codec)
    api(libs.com.fasterxml.jackson.core.jackson.core)
    api(libs.com.fasterxml.jackson.core.jackson.annotations)
    api(libs.com.fasterxml.jackson.core.jackson.databind)
    api(libs.com.fasterxml.jackson.module.jackson.module.kotlin)
    api(libs.org.hibernate.validator.hibernate.validator)
    api(libs.spotbugs.annotations)
    api(libs.org.springframework.boot.spring.boot.configuration.processor)
    api(libs.org.jetbrains.annotations)
    implementation(libs.io.github.oshai.kotlin.logging)
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.commons.io.commons.io)
    testImplementation(libs.org.springframework.boot.spring.boot.test)
    testImplementation(libs.org.springframework.spring.test)
    testImplementation(libs.ch.qos.logback.logback.classic)
    testImplementation(libs.org.yaml.snakeyaml)
    testImplementation(libs.org.openjdk.jmh.jmh.core)
    testImplementation(libs.org.openjdk.jmh.jmh.generator.annprocess)
    compileOnly(libs.org.projectlombok.lombok)
    annotationProcessor(libs.org.projectlombok.lombok)
}

group = "com.github.Luke-Ed"
version = "0.0.1-snapshot"
description = "Couch Slacker"

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    maxParallelForks = 1
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    resolutionStrategy {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

spotless {
    // This should be origin/main, but I wanted to leave it stable or mostly stable till I moved more to kotlin
    ratchetFrom = "origin/move_to_okhttp_kotlin"

    java {
        googleJavaFormat()
        indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlin {
        ktfmt().googleStyle()
        indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

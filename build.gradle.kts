import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("java-library")
    `maven-publish`
    id("project-report")
    id("com.diffplug.spotless")
    id("com.github.ben-manes.versions")
    kotlin("jvm")
    `jvm-test-suite`
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
    api(libs.jackson.core)
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    api(libs.hibernate.validator)
    api(libs.spotbugs.annotations)
    api(libs.org.springframework.boot.spring.boot.configuration.processor)
    api(libs.org.jetbrains.annotations)
    implementation(libs.spring.web)
    implementation(libs.oshai.kotlin.logging)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockito)
    testImplementation(libs.hamcrest)
    testImplementation(libs.hamcrest.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.commons.io.commons.io)
    testImplementation(libs.org.springframework.boot.spring.boot.test)
    testImplementation(libs.logback.classic)
    testImplementation(libs.snakeyaml)
    testImplementation(libs.org.openjdk.jmh.jmh.core)
    testImplementation(libs.org.openjdk.jmh.jmh.generator.annprocess)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
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

// Despite the fact that it's incubating the jvm-test-suites plugin is useful, and to avoid having tons of warnings
// simply because it's being used I'm supressing the warning.
@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            sources {
                java {
                    setSrcDirs(listOf("src/integrationTest/java"))
                }
            }
            dependencies {
                implementation(project())
                implementation(libs.junit.jupiter)
                implementation(libs.org.springframework.data.spring.data.commons)
                implementation(libs.spring.test)
                implementation(libs.org.springframework.boot.spring.boot.test)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.junit)
                implementation(libs.spring.boot.testcontainers)
                implementation(libs.snakeyaml)
                implementation(libs.logback.classic)
                compileOnly(libs.lombok)
                annotationProcessor(libs.lombok)
            }
            targets {
                all {
                    testTask.configure {
                        environment("api.version", "1.44")
                    }
                }
            }
        }
    }
}

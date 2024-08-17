/*
 * This file was generated by the Gradle 'init' task.
 */

rootProject.name = "couch-slacker"

pluginManagement {
    plugins {
        val spotlessVersion: String by settings
        val benManesVersionsPlugin: String by settings
        val kotlinVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("com.diffplug.spotless") version spotlessVersion
        id("com.github.ben-manes.versions") version benManesVersionsPlugin
    }
}

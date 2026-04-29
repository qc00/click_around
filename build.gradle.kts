plugins {
    id("java")
    jacoco
    alias(libs.plugins.intellijPlatform)
}

group = "software.nmr"
version = "1.0.0-SNAPSHOT"

// Set the JVM language level used to build the project.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The IntelliJ Platform's test runner setup imports JUnit 4 classes, so:
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    test {
        failOnNoDiscoveredTests = false
    }

    // The IntelliJ Platform plugin heavily customises the standard `test` task to bootstrap
    // a sandboxed IDE, which (a) is unnecessary for our pure POJO tests and (b) prevents the
    // JaCoCo agent from observing class loads. Provide a dedicated, plain JVM unit-test task
    // that runs the JUnit 5 tests against the unmodified compiled classes.
    val unitTest by registering(Test::class) {
        description = "Runs plain JVM unit tests for the click_around plugin sources."
        group = "verification"
        useJUnitPlatform()

        val main = sourceSets["main"].output
        val testOut = sourceSets["test"].output
        testClassesDirs = testOut.classesDirs
        classpath = testOut + main + configurations["testRuntimeClasspath"] +
                configurations["compileClasspath"]

        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(unitTest)
        executionData.setFrom(layout.buildDirectory.file("jacoco/unitTest.exec"))
        // The unitTest task runs against `build/classes/java/main`, not the IntelliJ-instrumented
        // bytecode, so JaCoCo's hash-based class lookup must match those originals.
        // UI and PSI handlers need a running IDE to exercise meaningfully; they are excluded
        // from coverage to keep the metric focused on the unit-testable domain logic.
        classDirectories.setFrom(
            fileTree(layout.buildDirectory.dir("classes/java/main")) {
                include("**/*.class")
            }
        )
        sourceDirectories.setFrom(files("src/main/java"))
        reports {
            xml.required = true
            html.required = true
        }
    }

}

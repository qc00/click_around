import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
    jacoco
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
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.eclipse.persistence:org.eclipse.persistence.moxy:5.0.0")
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

val schemagenOutDir = layout.buildDirectory.dir("generated-resources/schemagen")
sourceSets {
    main {
        resources {
            srcDir(schemagenOutDir)
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    val schemagenWorkDir = layout.buildDirectory.dir("jaxb2")

    val schemagen by registering(JavaExec::class) {
        description = "Generates XSD schemas via Moxy"
        group = "build"
        mainClass.set("software.nmr.click_around.settings.AbsSettings")
        args(schemagenOutDir.get().toString())

        dependsOn(compileJava)
        classpath = sourceSets.main.get().output.classesDirs + sourceSets.main.get().compileClasspath

        val outDir = schemagenOutDir
        inputs.files(sourceSets.main.get().allJava, sourceSets.main.get().compileClasspath)
        outputs.dir(outDir)

        doLast {
            check(outDir.get().asFile.walk().any { it.isFile && it.extension == "xsd" && it.length() > 0L }) {
                "Expected $name to generate at least one XSD in $outDir."
            }
        }
    }

    processResources {
        dependsOn(schemagen)
    }

    val schemadoc by registering(Exec::class) {
        description = "Use maven plug-in to add docs"
        group = "build"
        workingDir = projectDir
        val cp = sourceSets.main.get().compileClasspath
        val outDir = schemagenOutDir  // Copy locally for config caching serialization

        inputs.files(sourceSets.main.get().allJava, cp)
        outputs.dir(schemagenWorkDir)
        outputs.dir(outDir)

        doFirst {
            schemagenWorkDir.get().asFile.deleteRecursively()
            commandLine(
                "/usr/bin/env", "mvn",
                "jaxb2:schemagen",
                "-Dschemagen.existingSchema=${outDir.get()}",
                "-Djaxb.classpathOverride=${cp.asPath}"
            )
        }
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
        exclude("**/*IdeTest*.class")

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

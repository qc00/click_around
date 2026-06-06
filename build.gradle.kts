import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    kotlin("jvm") version "2.1.21" // For IDE integration tests only
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

val schemagenOutDir = layout.buildDirectory.dir("generated-resources/schemagen")
sourceSets {
    main {
        resources {
            srcDir(schemagenOutDir)
        }
    }
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Starter, configurationName = "integrationTestImplementation")
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    add("integrationTestRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    implementation("org.eclipse.persistence:org.eclipse.persistence.moxy:5.0.0")

    integrationTestImplementation("org.kodein.di:kodein-di-jvm:7.20.2")
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
    // Force kotlin-stdlib to match kotlin-reflect from the Starter framework (2.2.x adds KParameter.Kind.CONTEXT)
    integrationTestImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
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

val integrationTest by intellijPlatformTesting.testIdeUi.registering {
    task {
        val integrationTestSourceSet = sourceSets.getByName("integrationTest")
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        useJUnitPlatform()
        systemProperty("platform.version", providers.gradleProperty("platformVersion").get())
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

    val schemadoc by registering(Exec::class) {
        description = "Use maven plug-in to add docs"
        group = "build"
        dependsOn(schemagen)
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

    processResources {
        dependsOn(schemadoc)
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
        exclude("**/*IT.class")

        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(unitTest)
        executionData.setFrom(layout.buildDirectory.file("jacoco/unitTest.exec"))
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

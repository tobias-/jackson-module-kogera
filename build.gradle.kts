import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `maven-publish` // for JitPack

    val kotlinVersion: String = System.getenv("KOTLIN_VERSION")?.takeIf { it.isNotEmpty() }
        ?: libs.versions.kotlin.get()

    kotlin("jvm") version kotlinVersion

    java
    alias(libs.plugins.kotlinter)
    id("com.jfrog.artifactory") version "5.2.5"
}

// Since group cannot be obtained by generateKogeraVersion, it is defined as a constant.
val groupStr = "io.github.projectmapk"
val jacksonVersion = libs.versions.jackson.get()
val generatedSrcPath = "${layout.buildDirectory.get()}/generated/kotlin"

group = groupStr
version = "${jacksonVersion}-beta13-youcruit"

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion: String = System.getenv("KOTLIN_VERSION")?.takeIf { it.isNotEmpty() }
        ?: libs.versions.kotlin.get()
    implementation("${libs.kotlin.stdlib.get()}:${kotlinVersion}")
    implementation("${libs.kotlin.metadata.jvm.get()}:$kotlinVersion")

    api(libs.jackson.databind)
    api(libs.jackson.annotations)

    // test libs
    testImplementation("${libs.kotlin.reflect.get()}:${kotlinVersion}")
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.mockk)

    testImplementation(libs.jackson.xml)
    testImplementation(libs.jackson.jsr310)
}

kotlin {
    explicitApi()
    // for PackageVersion
    sourceSets["main"].apply {
        kotlin.srcDir(generatedSrcPath)
    }

    val useK2 = System.getenv("KOTLIN_VERSION")?.takeIf { it.isNotEmpty() }?.toBoolean()
        ?: false

    sourceSets.all {
        languageSettings {
            if (useK2) {
                languageVersion = "2.0"
            }
        }
    }

    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

artifactory {
    val artifactoryUrl: String by project
    setContextUrl(artifactoryUrl)

    publish {
        repository {
            val artifactoryRepo: String by project
            val artifactory_user: String by project
            val artifactory_password: String by project
            repoKey = artifactoryRepo
            username = artifactory_user
            password = artifactory_password
        }
        defaults {
            publications("mavenJava")
            setPublishArtifacts(true)
            setPublishPom(true)
        }
    }
}

tasks {
    // For ported tests, they are excluded from the formatting because they are not new code.
    lintKotlinTest {
        exclude { it.path.contains("zPorted") }
    }
    formatKotlinTest {
        exclude { it.path.contains("zPorted") }
    }

    // Task to generate version file
    val generateKogeraVersion by registering(Copy::class) {
        val packageStr = "$groupStr.jackson.module.kogera"

        from(
            resources.text.fromString(
                """
package $packageStr

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.core.util.VersionUtil

public val kogeraVersion: Version = VersionUtil.parseVersion("$version", "$groupStr", "${rootProject.name}")

                """.trimIndent()
            )
        ) {
            rename { "KogeraVersion.kt" }
        }

        into(file("$generatedSrcPath/${packageStr.replace(".", "/")}"))
    }

    // Added to avoid failure in generating dependency graphs in CI.
    lintKotlinMain {
        dependsOn.add(generateKogeraVersion)
    }

    compileKotlin {
        dependsOn.add(generateKogeraVersion)
    }
    compileJava {
        options.compilerArgs.add("-Xlint:unchecked")
    }

    kotlinSourcesJar {
        dependsOn.add(generateKogeraVersion)
    }

    "sourcesJar" {
        dependsOn(generateKogeraVersion)
    }

    javadoc {
        options {
            this as CoreJavadocOptions
            addBooleanOption("Xdoclint:none", true)
        }
    }

    test {
        useJUnitPlatform()
    }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

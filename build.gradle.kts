import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.openrewrite.build.recipe-library-base") version "latest.release"

    // Publishes to the moderne-dev repository via the nexus publishing plugin. It also
    // applies Nebula's MavenResolvedDependenciesPlugin, which pins dynamic versions
    // (e.g. "1.5.+", "latest.release") to the concrete versions Gradle resolved when
    // writing the published POM.
    id("org.openrewrite.build.publish") version "latest.release"
    id("nebula.release") version "latest.release"

    // Configures artifact repositories used for dependency resolution to include maven central
    // and nexus snapshots.
    id("org.openrewrite.build.recipe-repositories") version "latest.release"

    // The Kotlin recipe DSL is authored in Kotlin and compiled by the K2 compiler.
    kotlin("jvm") version "2.4.10"
}

// Set as appropriate for your organization
group = "com.yourorg"
description = "OpenRewrite recipes for Kotlin, authored with the Kotlin recipe DSL."

dependencies {
    // The bom aligns every org.openrewrite* module — including rewrite-kotlin — to one version.
    // https://github.com/openrewrite/rewrite/releases
    implementation(platform("org.openrewrite:rewrite-bom:latest.release"))

    // The Kotlin LST extends the Java LST, so recipes lean on both modules.
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-kotlin")

    // The Kotlin recipe DSL (`rewrite { } to { }`, `kotlin { visit… }`) is a K2 compiler plugin
    // shipped inside rewrite-kotlin. It runs at recipe-compile time and rewrites the
    // `recipe(...)`/`recipes(...)` property initializers into synthesized Recipe classes, so no
    // reflection or ServiceLoader wiring is needed — consumers see ordinary Recipe instances.
    kotlinCompilerPluginClasspath(platform("org.openrewrite:rewrite-bom:latest.release"))
    kotlinCompilerPluginClasspath("org.openrewrite:rewrite-kotlin")

    // Parse Kotlin sources under test against a real JDK 21 runtime.
    runtimeOnly("org.openrewrite:rewrite-java-21")

    // The RewriteTest class needed for testing recipes.
    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.assertj:assertj-core:latest.release")

    // A slf4j binding surfaces any parser output during tests.
    runtimeOnly("ch.qos.logback:logback-classic:1.5.+")
}

signing {
    // To enable signing have your CI workflow set the "signingKey" and "signingPassword" Gradle project properties
    isRequired = false
}

// Use maven-style "SNAPSHOT" versioning for non-release builds
configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Recipe libraries target Java 8 bytecode so they load in older rewrite runtimes; keep Kotlin
// main compilation aligned. Tests exercise the JDK 21 Kotlin parser, so they compile to 21.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.register("licenseFormat") {
    println("License format task not implemented for kotlin-recipe-starter")
}

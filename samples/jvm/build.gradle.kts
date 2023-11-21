import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.windedge.kopybuilder")
    application
}

application {
    mainClass = "io.github.windedge.copybuilder.sample.MainKt"
}

kotlin {
    jvmToolchain(11)
}

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


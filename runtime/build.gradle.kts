@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl


plugins {
    id(libs.plugins.kotlin.kmp.get().pluginId)
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.maven.publish.get().pluginId)
}

kotlin {
    explicitApi()

    jvm()
    js(IR) {
        browser()
        nodejs()
    }

//    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    linuxX64()
    macosArm64()
    mingwX64()

    // iOS targets
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    /*
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        when {
            "Windows" in osName -> mingwX64("native")
            "Mac OS" in osName -> when {
                "aarch64" in osArch -> macosArm64("native")
                else -> macosX64("native")
            }
            else -> linuxX64("native")
        }
    */

    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly(kotlin("stdlib"))
                compileOnly(kotlin("reflect"))
            }
        }
    }

}

android {
    compileSdk = 33
    this.compileOptions {
        this.sourceCompatibility = JavaVersion.VERSION_11
        this.targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = 21
    }
    namespace = "io.github.windedge.copybuilder"
}

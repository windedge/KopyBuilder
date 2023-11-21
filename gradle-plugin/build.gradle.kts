@file:Suppress("DSL_SCOPE_VIOLATION")

import com.vanniktech.maven.publish.SonatypeHost


plugins {
    `java-gradle-plugin`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.maven.publish.get().pluginId)
    id(libs.plugins.buildconfig.get().pluginId)
}

gradlePlugin {
    plugins {
        create("copybuilder") {
            id = "io.github.windedge.kopybuilder"
            implementationClass = "io.github.windedge.copybuilder.CopyBuilderPlugin"
        }
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("gradle-plugin"))
//    compileOnly(libs.kotlin.gradle.plugin.api)
//    implementation(libs.ksp.plugin)
}


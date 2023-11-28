@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kmp) apply false
    id("io.github.windedge.kopybuilder") version "<edge>" apply false
}

repositories {
    mavenCentral()
    google()
}

subprojects {
    repositories {
        mavenCentral()
        google()
    }

//tasks.withType<KotlinCompile> {
//    val compilerPluginId = "io.github.windedge.copybuilder.compiler"
//    kotlinOptions {
//        freeCompilerArgs = freeCompilerArgs + listOf(
//            "-P",
//            "plugin:$compilerPluginId:enabled=true",
//        )
//        freeCompilerArgs = freeCompilerArgs + listOf(
//            "-P",
//            "plugin:$compilerPluginId:verbose=true",
//        )
//    }
//}

}

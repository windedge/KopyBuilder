@file:Suppress("DSL_SCOPE_VIOLATION")

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kmp) apply false
//    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.buildconfig) apply false
}

val groupId = properties["GROUP"].toString()
val currentVersion = properties["VERSION_NAME"].toString()

repositories {
    mavenCentral()
}

subprojects {

    repositories {
        mavenCentral()
        google()
    }

    afterEvaluate {

        extensions.findByType<BuildConfigExtension>()?.apply {
            val groupId = properties["GROUP"].toString()
            val currentVersion = properties["VERSION_NAME"].toString()

            val runtimeArtifactId = "copybuilder-runtime"
            val kspArtifactId = "copybuilder-ksp"
            val compilerArtifactId = "copybuilder-compiler"
            val compilerPluginId = "$groupId.compiler"

            packageName(groupId)
            className("Artifacts")

            buildConfigField("String", "groupId", "\"$groupId\"")
            buildConfigField("String", "currentVersion", "\"$currentVersion\"")
            buildConfigField("String", "runtimeArtifactId", "\"$runtimeArtifactId\"")
            buildConfigField("String", "kspArtifactId", "\"$kspArtifactId\"")
            buildConfigField("String", "compilerArtifactId", "\"$compilerArtifactId\"")
            buildConfigField("String", "compilerPluginId", "\"$compilerPluginId\"")
        }

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        extensions.configure<KotlinProjectExtension> {
            jvmToolchain(11)
        }


        /*
                extensions.configure<SourceSetContainer> {
                    getByName("main").java.srcDirs("src/main/kotlin/")
                    getByName("test").java.srcDirs("src/test/kotlin/")
                }
        */

//        extensions.configure<MavenPublishBaseExtension> {
//            publishToMavenCentral(SonatypeHost.S01)
//        }
    }


}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }

    val publishVersion = extra["VERSION_NAME"] as String
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.windedge.kopybuilder") {
                useModule("io.github.windedge.copybuilder:copybuilder-gradle:$publishVersion")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.5.0")
}


include(
    ":runtime",
    ":compiler",
    ":gradle-plugin",
)

rootProject.name = "KopyBuilder"

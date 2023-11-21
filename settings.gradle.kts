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



include(
    ":runtime",
//    ":ksp",
    ":compiler",
    ":gradle-plugin",
)

rootProject.name = "KopyBuilder"

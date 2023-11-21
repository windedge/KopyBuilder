dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
//    val isCi = System.getenv()["CI"] == "true"
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.windedge.kopybuilder") {
                useModule("io.github.windedge.copybuilder:copybuilder-gradle:${requested.version}")
            }
        }
    }
}

includeBuild("../.") {
    dependencySubstitution {
        val group = "io.github.windedge.copybuilder"
        substitute(module("$group:copybuilder-runtime")).using(project(":runtime"))
        substitute(module("$group:copybuilder-compiler")).using(project(":compiler"))
        substitute(module("$group:copybuilder-gradle")).using(project(":gradle-plugin"))
//        substitute(module("$group:copybuilder-ksp")).using(project(":ksp"))
    }
}

include("jvm")
include("kmp")

rootProject.name = "Samples"

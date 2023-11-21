import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(libs.plugins.kotlin.kmp.get().pluginId)
    id("io.github.windedge.kopybuilder")
    `application`
}

application {
    mainClass.set("test.MainKt")
}

//// k2 compatitable
//tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
//    kotlinOptions {
//        freeCompilerArgs += arrayOf(
//            "-Xskip-metadata-version-check",
//            "-Xskip-prerelease-check",
//            "-Xallow-unstable-dependencies",
//        )
//    }
//}

kotlin {

    jvmToolchain(11)

    jvm {
        withJava()
//        mainRun {
//            mainClass = "test.MainKt"
//        }

    }

    js(IR) {
        browser()
        binaries.executable()
    }

    val hostOs = System.getProperty("os.name")
    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }
    hostTarget.apply {
        binaries {
            executable {
                entryPoint = "test.main"
            }
        }
    }

}

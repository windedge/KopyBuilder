@file:Suppress("unused")

package io.github.windedge.copybuilder

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class CopyBuilderPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("kopybuilder", CopyBuilderExtension::class.java)
        target.addRuntimeSupport()
    }

    private fun Project.addRuntimeSupport() {
        val runtimeArtifact = "${Artifacts.groupId}:${Artifacts.runtimeArtifactId}:${Artifacts.currentVersion}"
//        val kspArtifact = "${Artifacts.groupId}:${Artifacts.kspArtifactId}:${Artifacts.currentVersion}"

//        plugins.withId("org.jetbrains.kotlin.jvm") {
//            dependencies.add("implementation", "com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.0-1.0.11")
//        }
//        plugins.withId("org.jetbrains.kotlin.android") {
//            dependencies.add("implementation", "com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.0-1.0.11")
//        }
//        plugins.withId("org.jetbrains.kotlin.multiplatform") {
//            dependencies.add("implementation", "com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.0-1.0.11")
//        }
//

//        plugins.apply("com.google.devtools.ksp")


        plugins.withId("org.jetbrains.kotlin.jvm") {
//            extensions.configure(DependencyHandler::class.java) {
//                it.add("implementation", "com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.0-1.0.11")
//            }
            dependencies.add("implementation", runtimeArtifact)

//            dependencies.add("ksp", kspArtifact)
//            val generatedSrcDir = "build/generated/ksp/main/kotlin"
//            val jvm = extensions.getByType(KotlinJvmProjectExtension::class.java)
//            jvm.sourceSets.named("main") {
//                it.kotlin.srcDir(generatedSrcDir)
//            }
//            jvm.sourceSets.named("test") {
//                it.kotlin.srcDir(generatedSrcDir)
//            }

        }

        plugins.withId("org.jetbrains.kotlin.android") {
            dependencies.add("implementation", runtimeArtifact)

//            dependencies.add("ksp", kspArtifact)
//            val generatedSrcDir = "build/generated/ksp/main/kotlin"
//            val sourceSets = extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer
//            sourceSets.named("main") {
//                it.java.srcDir(generatedSrcDir)
//            }
//            sourceSets.named("test") {
//                it.java.srcDir(generatedSrcDir)
//            }
        }

        plugins.withId("org.jetbrains.kotlin.multiplatform") {

            afterEvaluate {
//                dependencies.add("kspCommonMainMetadata", kspArtifact)
//                kotlinExtension.targets.filterNot { it.name == "metadata" }.forEach {
//                    dependencies.add("ksp${it.name.capitalized()}", kspArtifact)
//                }

                extensions.configure(KotlinMultiplatformExtension::class.java) { kotlinExtension ->
                    kotlinExtension.sourceSets.named("commonMain") {
                        it.dependencies {
                            implementation(runtimeArtifact)
                        }
                    }

//                    kotlinExtension.sourceSets.filterNot { it.name.startsWith("common") }.forEach {
//                        with(it) {
//                            kotlin.srcDir("build/generated/ksp/${name.substringBefore("Main")}/${name}/kotlin")
//                        }
//                    }
                }
            }
        }
    }

    override fun getCompilerPluginId() = Artifacts.compilerPluginId

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact(
            groupId = Artifacts.groupId,
            artifactId = Artifacts.compilerArtifactId,
            version = Artifacts.currentVersion
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target.name
        val sourceSetName = kotlinCompilation.defaultSourceSet.name

        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(CopyBuilderExtension::class.java)

        val enabled = extension.enabled.get()
        val verbose = extension.verbose.get()

        val outputDir = extension.outputDir.get().dir("$target/$sourceSetName/kotlin")
        kotlinCompilation.defaultSourceSet.kotlin.srcDir(outputDir.asFile)

        return project.provider {
            listOf(
                SubpluginOption(key = "enabled", value = enabled.toString()),
                SubpluginOption(key = "verbose", value = verbose.toString()),
                SubpluginOption(key = "outputDir", value = outputDir.toString()),
            )
        }
    }
}
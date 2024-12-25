package io.github.windedge.copybuilder

import com.tschuchort.compiletesting.*
import com.tschuchort.compiletesting.KotlinCompilation.*
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions

const val pluginId: PluginId = Artifacts.compilerPluginId

@OptIn(ExperimentalCompilerApi::class)
class CopyBuilderTest : StringSpec() {
    private val temporaryFolder = tempdir()

    init {
        "compile test" {
            val result = compile(
                sourceFile = kotlin(
                    "main.kt", """
fun main() {
  println(debug())
}

fun debug() = "Hello, World!"
"""
                )
            )
            Assertions.assertEquals(ExitCode.OK, result.exitCode)
        }

        "should be generated" {
            val result = compile(
                sourceFile = kotlin(
                    "main.kt", """
import io.github.windedge.copybuilder.KopyBuilder
import io.github.windedge.copybuilder.CopyBuilderHost

@KopyBuilder
data class Fruit(val name: String)

fun main() {
    val fruit = Fruit("apple")
    val fruit2 = (fruit as CopyBuilderHost<Fruit>).copyBuild {
        put("name", "Pear")
    }
    println("fruit2 = " + fruit2)
}

"""
                )
            )

            Assertions.assertEquals(ExitCode.OK, result.exitCode)
        }
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar,
        commandLineProcessor: CommandLineProcessor,
        enabled: Boolean = true,
    ): CompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles + material
            compilerPluginRegistrars = listOf(plugin)
            pluginOptions = listOf(
                PluginOption(
                    pluginId,
                    optionName = OPTION_ENABLED.optionName,
                    optionValue = enabled.toString()
                ),
                PluginOption(
                    pluginId,
                    optionName = OPTION_VERBOSE.optionName,
                    optionValue = "true"
                ),
                PluginOption(
                    pluginId,
                    optionName = OPTION_OUTPUT_DIR.optionName,
                    optionValue = temporaryFolder.absolutePath
                )
            )
            commandLineProcessors = listOf(commandLineProcessor)
            inheritClassPath = true
        }.compile()
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFile: SourceFile,
        plugin: CompilerPluginRegistrar = CopyBuilderCompilerPluginRegistrar(),
        commandLineProcessor: CommandLineProcessor = CopyBuilderCommandLineProcessor()
    ): CompilationResult {
        return compile(listOf(sourceFile), plugin, commandLineProcessor)
    }
}

private val material = kotlin(
    "KopyBuilder.kt",
    """
//package io.github.windedge.copybuilder
//
//@Retention(AnnotationRetention.SOURCE)
//@Target(AnnotationTarget.CLASS)
//annotation class KopyBuilder
    """,
)
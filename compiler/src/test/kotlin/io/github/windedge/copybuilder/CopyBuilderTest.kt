package io.github.windedge.copybuilder

import com.tschuchort.compiletesting.*
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
                sourceFile = SourceFile.kotlin(
                    "main.kt", """
fun main() {
  println(debug())
}

fun debug() = "Hello, World!"
"""
                )
            )
            Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }
    }


    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar,
        commandLineProcessor: CommandLineProcessor,
        enabled: Boolean = true,
    ): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = sourceFiles
            useIR = false
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
    ): KotlinCompilation.Result {
        return compile(listOf(sourceFile), plugin, commandLineProcessor)
    }
}
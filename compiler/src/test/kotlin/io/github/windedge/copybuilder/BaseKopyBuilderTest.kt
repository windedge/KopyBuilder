package io.github.windedge.copybuilder

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.io.TempDir
import java.io.File


@OptIn(ExperimentalCompilerApi::class)
open class BaseKopyBuilderTest {
    @TempDir
    lateinit var temporaryFolder: File

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar = CopyBuilderCompilerPluginRegistrar(),
        commandLineProcessor: CommandLineProcessor = CopyBuilderCommandLineProcessor(),
        enabled: Boolean = true,
    ): CompilationResult {
        val compilation = KotlinCompilation()
        return compilation.apply {
            sources = sourceFiles
            compilerPluginRegistrars = listOf(plugin)
            pluginOptions = listOf(
                PluginOption(pluginId, optionName = "enabled", optionValue = enabled.toString()),
                PluginOption(pluginId, optionName = "verbose", optionValue = "true"),
                PluginOption(pluginId, optionName = "outputDir", optionValue = temporaryFolder.absolutePath)
            )
            commandLineProcessors = listOf(commandLineProcessor)
            inheritClassPath = true
            messageOutputStream = System.out
            verbose = true
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

@file:OptIn(ExperimentalCompilerApi::class)

package io.github.windedge.copybuilder

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

internal val KEY_ENABLED = CompilerConfigurationKey<Boolean>(
    "Whether to disable copy() generation - default is true",
)

internal val KEY_VERBOSE = CompilerConfigurationKey<Boolean>(
    "Whether to enabled verbose logging - default is false",
)

internal val KEY_OUTPUT_DIR = CompilerConfigurationKey<File>(
    "Where to output generated source files - default is <empty>"
)

internal val OPTION_ENABLED = CliOption(
    optionName = "enabled",
    valueDescription = "<true | false>",
    description = KEY_ENABLED.toString(),
    required = true,
    allowMultipleOccurrences = false,
)

internal val OPTION_VERBOSE = CliOption(
    optionName = "verbose",
    valueDescription = "<true | false>",
    description = KEY_VERBOSE.toString(),
    required = true,
    allowMultipleOccurrences = false,
)

internal val OPTION_OUTPUT_DIR = CliOption(
    optionName = "outputDir",
    valueDescription = "<outputDir>",
    description = KEY_OUTPUT_DIR.toString(),
    required = false,
)

class CopyBuilderCommandLineProcessor : CommandLineProcessor {

    override val pluginId = Artifacts.compilerPluginId

    override val pluginOptions = listOf(
        OPTION_ENABLED,
        OPTION_VERBOSE,
        OPTION_OUTPUT_DIR
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        messageCollector.report(CompilerMessageSeverity.WARNING, "option = ${option}")

        when (val optionName = option.optionName) {
            OPTION_ENABLED.optionName -> configuration.put(KEY_ENABLED, value.toBooleanStrict())
            OPTION_VERBOSE.optionName -> configuration.put(KEY_VERBOSE, value.toBooleanStrict())
            OPTION_OUTPUT_DIR.optionName -> configuration.put(KEY_OUTPUT_DIR, File(value))
            else -> error("Unknown plugin option: $optionName")
        }
    }
}

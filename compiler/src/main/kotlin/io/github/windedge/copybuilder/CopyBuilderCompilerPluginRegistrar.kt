@file:OptIn(ExperimentalCompilerApi::class)

package io.github.windedge.copybuilder

import io.github.windedge.copybuilder.ir.CopyBuilderGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter


@OptIn(ExperimentalCompilerApi::class)
class CopyBuilderCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) return

        // K2 编译器使用 FIR 扩展
        FirExtensionRegistrarAdapter.registerExtension(CopyBuilderFirExtensionRegistar())
        IrGenerationExtension.registerExtension(CopyBuilderGenerationExtension())
    }
}

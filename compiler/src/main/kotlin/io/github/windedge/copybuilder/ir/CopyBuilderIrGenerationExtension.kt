package io.github.windedge.copybuilder.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class CopyBuilderIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        CopyBuilderIrTransformer(pluginContext).let { transformer ->
            moduleFragment.transform(transformer, null)
        }
    }
}

package io.github.windedge.copybuilder.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class CopyBuilderIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // First transform to generate CopyBuilderImpl classes
        CopyBuilderIrTransformer(pluginContext).let { transformer ->
            moduleFragment.transform(transformer, null)
        }

        // Then transform data classes to add toCopyBuilder() and copyBuild() methods
        CopyBuilderHostIrTransformer(pluginContext).let { transformer ->
            moduleFragment.transform(transformer, null)
        }
    }
}

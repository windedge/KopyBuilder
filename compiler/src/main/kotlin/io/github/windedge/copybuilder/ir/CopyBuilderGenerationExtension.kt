package io.github.windedge.copybuilder.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment


class CopyBuilderGenerationExtension(
    private val annotationName: String = "io.github.windedge.copybuilder.KopyBuilder"
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformer = CopyBuilderTransformer(pluginContext, annotationName)
        moduleFragment.transform(transformer, null)
    }
}


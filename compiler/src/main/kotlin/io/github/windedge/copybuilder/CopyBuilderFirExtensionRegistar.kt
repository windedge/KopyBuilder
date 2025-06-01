package io.github.windedge.copybuilder

import io.github.windedge.copybuilder.k2.fir.CopyBuilderFirDeclarationGenerationExtension
import io.github.windedge.copybuilder.k2.fir.CopyBuilderSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class CopyBuilderFirExtensionRegistar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CopyBuilderFirDeclarationGenerationExtension
        +::CopyBuilderSupertypeGenerationExtension
    }
}
package io.github.windedge.copybuilder.k2

import io.github.windedge.copybuilder.k2.fir.CopyBuilderFirDeclarationGenerationExtension
import io.github.windedge.copybuilder.k2.fir.CopyBuilderHostFirDeclarationGenerationExtension
import io.github.windedge.copybuilder.k2.fir.CopyBuilderSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class CopyBuilderFirExtensionRegistar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CopyBuilderSupertypeGenerationExtension
        +::CopyBuilderFirDeclarationGenerationExtension
        +::CopyBuilderHostFirDeclarationGenerationExtension
    }
}
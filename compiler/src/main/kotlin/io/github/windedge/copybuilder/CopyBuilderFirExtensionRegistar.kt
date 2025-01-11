package io.github.windedge.copybuilder

import io.github.windedge.copybuilder.fir.CopyBuilderFirDeclarationGenerationExtension
import io.github.windedge.copybuilder.fir.CopyBuilderHostFirDeclarationGenerationExtension
import io.github.windedge.copybuilder.fir.CopyBuilderSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class CopyBuilderFirExtensionRegistar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CopyBuilderSupertypeGenerationExtension
        +::CopyBuilderFirDeclarationGenerationExtension
        +::CopyBuilderHostFirDeclarationGenerationExtension
    }
}
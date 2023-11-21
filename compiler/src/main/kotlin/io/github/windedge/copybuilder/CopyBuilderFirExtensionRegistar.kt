package io.github.windedge.copybuilder

import io.github.windedge.copybuilder.fir.CopyBuilderFirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class CopyBuilderFirExtensionRegistar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        println("ExtensionRegistrarContext.configurePlugin")
        +::CopyBuilderFirDeclarationGenerationExtension
//        +::SimpleClassGenerator
    }
}
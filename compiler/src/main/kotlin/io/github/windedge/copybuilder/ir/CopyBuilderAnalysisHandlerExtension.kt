package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.KEY_OUTPUT_DIR
import io.github.windedge.copybuilder.toGeneratedCopyBuilderPath
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.PsiTreeChangeAdapter
import org.jetbrains.kotlin.com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import java.nio.file.Files
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists


class CopyBuilderAnalysisHandlerExtension(
    configuration: CompilerConfiguration,
    annotationName: String = "io.github.windedge.copybuilder.KopyBuilder"
) : AnalysisHandlerExtension {
    private val annotationFqName: FqName = FqName(annotationName)

    private var didRecompile = false

    private val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    private val outputDir = configuration.get(KEY_OUTPUT_DIR) ?: error("Output dir must not be empty.")


    /*
        override fun doAnalysis(
            project: Project,
            module: ModuleDescriptor,
            projectContext: ProjectContext,
            files: Collection<KtFile>,
            bindingTrace: BindingTrace,
            componentProvider: ComponentProvider
        ): AnalysisResult? {
            messageCollector.report(CompilerMessageSeverity.WARNING, "doAnalysis end.")
            return null
        }
    */

    override fun analysisCompleted(
        project: Project, module: ModuleDescriptor, bindingTrace: BindingTrace, files: Collection<KtFile>
    ): AnalysisResult? {
        if (didRecompile) return null
        didRecompile = true

        if (!outputDir.exists()) {
            Files.createDirectories(outputDir.toPath())
        }

        val ungeneratedAnnotatedClasses = bindingTrace.bindingContext
            .getSliceContents(BindingContext.CLASS).values.filter { it.needsToBeGenerated() }

        if (ungeneratedAnnotatedClasses.isEmpty()) {
            return null
        }

        ungeneratedAnnotatedClasses.forEach { c ->
            val file = c.generateImplClass()
            file.writeTo(outputDir)
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingContext = BindingContext.EMPTY,
            moduleDescriptor = module,
            additionalKotlinRoots = listOf(outputDir),
            additionalJavaRoots = emptyList(),
            addToEnvironment = true
        )
    }

    private fun ClassDescriptor.needsToBeGenerated(): Boolean {
        if (!annotations.hasAnnotation(annotationFqName)) return false

        if (!isData) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "${this.name} can't generate CopyBuilder, only support dataclass."
            )
            return false
        }

        val generatedPath = toGeneratedCopyBuilderPath(outputDir.toPath())
        if (generatedPath.notExists()) return true

        val sourceTimestamp = psiElement?.containingFile?.virtualFile?.timeStamp ?: 0L
        return generatedPath.getLastModifiedTime().toMillis() < sourceTimestamp
    }

}

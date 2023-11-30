package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.KEY_OUTPUT_DIR
import io.github.windedge.copybuilder.toGeneratedCopyBuilderPath
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
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
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.nio.file.Files
import kotlin.io.path.*


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
        val psiManager = PsiManager.getInstance(project)
        if (didRecompile) {
            psiManager.dropPsiCaches()
            return null
        } else {
            project.extensionArea.registerExtensionPoint(
                PsiTreeChangeListener.EP.name,
                PsiTreeChangeAdapter::class.java.canonicalName,
                ExtensionPoint.Kind.INTERFACE
            )
            didRecompile = true
        }
//        if (didRecompile) return null
//        didRecompile = true

        if (!outputDir.exists()) {
            Files.createDirectories(outputDir)
        }

        val generatedFiles = outputDir.toFile().walkTopDown().filter {
            it.isFile && it.startsWith(outputDir.pathString)
        }.map { it.toPath() }.toMutableList()
        messageCollector.report(INFO, "generatedFiles = ${generatedFiles}")

        val classes = bindingTrace.bindingContext.getSliceContents(BindingContext.CLASS).values
        val (outdatedAnnotatedClasses, uptodateAnnotatedClasses) =
            classes.filter { it.isAnnotatedClass() }.partition { it.isOutdatedClass() }

        messageCollector.report(INFO, "outdatedAnnotatedClasses = ${outdatedAnnotatedClasses}")
        messageCollector.report(INFO, "uptodateAnnotatedClasses = ${uptodateAnnotatedClasses}")

        // delete outdated generated files
        val uptodateGeneratedFiles = uptodateAnnotatedClasses.map {
            it.toGeneratedCopyBuilderPath(outputDir).absolute()
        }
        generatedFiles.filter { it !in uptodateGeneratedFiles }.ifNotEmpty {
            messageCollector.report(WARNING, "outdatedFiles = ${this}")
            this.forEach { it.deleteIfExists() }
            /*
                        return AnalysisResult.RetryWithAdditionalRoots(
                            bindingContext = BindingContext.EMPTY,
                            moduleDescriptor = module,
                            additionalKotlinRoots = listOf(outputDir.toFile()),
                            additionalJavaRoots = emptyList(),
                            addToEnvironment = true
                        )
            */
        }

        if (outdatedAnnotatedClasses.isEmpty()) {
            return null
        }

        outdatedAnnotatedClasses.forEach { c ->
            val file = c.generateImplClass()
            file.writeTo(outputDir)
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingContext = BindingContext.EMPTY,
            moduleDescriptor = module,
            additionalKotlinRoots = listOf(outputDir.toFile()),
            additionalJavaRoots = emptyList(),
            addToEnvironment = true
        )
    }

    private fun ClassDescriptor.isAnnotatedClass(): Boolean {
        if (!annotations.hasAnnotation(annotationFqName)) return false

        if (!isData) {
            messageCollector.report(WARNING, "${this.name} can't generate CopyBuilder, only support dataclass.")
            return false
        }
        return true
    }

    private fun ClassDescriptor.isOutdatedClass(): Boolean {
        val generatedPath = toGeneratedCopyBuilderPath(outputDir)
        if (generatedPath.notExists()) return true

        val sourceTimestamp = psiElement?.containingFile?.virtualFile?.timeStamp ?: 0L
        return generatedPath.getLastModifiedTime().toMillis() < sourceTimestamp
    }

}

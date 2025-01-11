package io.github.windedge.copybuilder.k1

import io.github.windedge.copybuilder.KEY_OUTPUT_DIR
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
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
import kotlin.io.path.absolute
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists
import kotlin.io.path.pathString

class CopyBuilderAnalysisHandlerExtension(
    configuration: CompilerConfiguration,
    annotationName: String = "io.github.windedge.copybuilder.KopyBuilder"
) : AnalysisHandlerExtension {
    private val annotationFqName: FqName = FqName(annotationName)

    private var didRecompile = false

    private val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.Companion.NONE)
    private val outputDir = configuration.get(KEY_OUTPUT_DIR) ?: error("Output dir must not be empty.")


    override fun analysisCompleted(
        project: Project, module: ModuleDescriptor, bindingTrace: BindingTrace, files: Collection<KtFile>
    ): AnalysisResult? {
        messageCollector.report(CompilerMessageSeverity.WARNING, "analysisCompleted")
        val psiManager = PsiManager.getInstance(project)
/*
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
*/
        messageCollector.report(CompilerMessageSeverity.WARNING, "didRecompile = ${didRecompile}")
        if (didRecompile) return null
        didRecompile = true

        if (!outputDir.exists()) {
            Files.createDirectories(outputDir)
        }

        val generatedFiles = outputDir.toFile().walkTopDown().filter {
            it.isFile && it.startsWith(outputDir.pathString)
        }.map { it.toPath() }.toMutableList()
        messageCollector.report(CompilerMessageSeverity.INFO, "generatedFiles = ${generatedFiles}")

        val classes = bindingTrace.bindingContext.getSliceContents(BindingContext.CLASS).values
        messageCollector.report(CompilerMessageSeverity.INFO, "Known classes = ${classes}")
        val (outdatedAnnotatedClasses, uptodateAnnotatedClasses) =
            classes.filter { it.isAnnotatedClass() }.partition { it.isOutdatedClass() }
        messageCollector.report(CompilerMessageSeverity.INFO, "outdatedAnnotatedClasses = ${outdatedAnnotatedClasses}")
        messageCollector.report(CompilerMessageSeverity.INFO, "uptodateAnnotatedClasses = ${uptodateAnnotatedClasses}")

        if (outdatedAnnotatedClasses.isEmpty() && uptodateAnnotatedClasses.isEmpty()) {
            return null
        }

        // delete outdated generated files
        val uptodateGeneratedFiles = uptodateAnnotatedClasses.map {
            it.toGeneratedCopyBuilderPath(outputDir).absolute()
        }
        generatedFiles.filter { it !in uptodateGeneratedFiles }.ifNotEmpty {
            messageCollector.report(CompilerMessageSeverity.WARNING, "outdatedFiles = ${this}")
            this.forEach { it.deleteIfExists() }
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
            messageCollector.report(CompilerMessageSeverity.WARNING, "${this.name} can't generate CopyBuilder, only support dataclass.")
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
package io.github.windedge.copybuilder

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.windedge.copybuilder.generator.generateImplClass

@AutoService(SymbolProcessorProvider::class)
class CopyBuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CopyBuilderProcessor(environment, CopyBuilderOptions(environment.options))
    }
}

class CopyBuilderProcessor(env: SymbolProcessorEnvironment, private val copyBuilderOptions: CopyBuilderOptions) :
    SymbolProcessor {
    private val codeGenerator: CodeGenerator = env.codeGenerator
    private val logger: KSPLogger = env.logger
    private var invoked = false

    companion object {
        lateinit var copyBuilderResolver: Resolver
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val type = copyBuilderOptions.errorsLoggingType
        copyBuilderResolver = resolver
        if (invoked) {
            return emptyList()
        }
        invoked = true

//        var classDataList = getAnnotatedFunctions(copyBuilderResolver).groupBy { it.closestClassDeclaration()!! }
//            .map { (classDec) ->
//                classDec.toClassData(io.github.windedge.copybuilder.CopyBuilderLogger(logger, type))
//            }
//        generateImplClass(classDataList, codeGenerator, resolver)

        val classes = resolver.getSymbolsWithAnnotation(KopyBuilder::class.java.name).toList()
            .filterIsInstance<KSClassDeclaration>()

        for (clazz in classes) {
            clazz.generateImplClass(codeGenerator)
        }

        return emptyList()
    }
}


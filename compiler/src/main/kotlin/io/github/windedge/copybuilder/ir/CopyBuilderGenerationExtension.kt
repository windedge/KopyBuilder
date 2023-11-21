package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.getImplClassName
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


class CopyBuilderGenerationExtension(
    private val annotationName: String = "io.github.windedge.copybuilder.KopyBuilder"
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformer = Transformer(pluginContext, annotationName)
        moduleFragment.transform(transformer, null)
    }
}

class Transformer(
    context: IrPluginContext,
    annotationName: String,
) : IrElementTransformer<Nothing?> {
    private val irBuiltIns = context.irBuiltIns
    private val annotationFqName: FqName = FqName(annotationName)

    override fun visitClass(declaration: IrClass, data: Nothing?): IrStatement {
        val klass = super.visitClass(declaration, data) as IrClass
        val hasAnnotation = declaration.annotations.hasAnnotation(annotationFqName)
        if (!hasAnnotation) return klass

        val copyBuilderFactoryClass =
            irBuiltIns.findClass(Name.identifier("CopyBuilderFactory"), FqName("io.github.windedge.copybuilder"))
                ?: error("Class: CopyBuilderFactory cannot be found!")
        val parameterizedFactoryType = copyBuilderFactoryClass.typeWith(klass.defaultType)

        klass.superTypes += parameterizedFactoryType

        val copyBuilderClass =
            irBuiltIns.findClass(Name.identifier("CopyBuilder"), FqName("io.github.windedge.copybuilder"))
                ?: error("Class: CopyBuilder cannot be found!")
        val parameterizedType = copyBuilderClass.typeWith(klass.defaultType)

        klass.addFunction("toCopyBuilder", parameterizedType).apply {
            this.modality = Modality.OPEN
            val builderImplClassName = getImplClassName(klass.name.asString())
            val builderImplClass =
                irBuiltIns.findClass(Name.identifier(builderImplClassName), klass.packageFqName!!)
                    ?: error("Class: $builderImplClassName cannot be found!")

            body = irBuiltIns.createIrBuilder(this.symbol).irBlockBody {
                val defaultConstructor = builderImplClass.constructors.first()
                val thisReceiver = irGet(this@apply.dispatchReceiverParameter!!)
                +irReturn(
                    irCall(defaultConstructor).apply {
                        putValueArgument(0, thisReceiver)
                    }
                )
            }
        }

        /*
                klass.addFakeOverrides(
                    IrTypeSystemContextImpl(irBuiltIns),
        //            listOf(toCopyBuilderFunc),
        //            ignoredParentSymbols = listOf(toCopyBuilderFunc.symbol),
                )
        */
        return klass
    }

}

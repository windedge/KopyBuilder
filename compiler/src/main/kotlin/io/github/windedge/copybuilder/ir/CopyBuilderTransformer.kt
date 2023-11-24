package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.CopyBuilderFqn
import io.github.windedge.copybuilder.getImplClassName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CopyBuilderTransformer(
    val context: IrPluginContext,
    annotationName: String,
) : IrElementTransformer<Nothing?> {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns
    private val annotationFqName: FqName = FqName(annotationName)

    @OptIn(ObsoleteDescriptorBasedAPI::class, FirIncompatiblePluginAPI::class)
    override fun visitClass(declaration: IrClass, data: Nothing?): IrStatement {
        val klass = super.visitClass(declaration, data) as IrClass
        val hasAnnotation = declaration.annotations.hasAnnotation(annotationFqName)
        if (!hasAnnotation) return klass

        val copyBuilderFactoryClass: IrClassSymbol =
            context.referenceClass(CopyBuilderFqn.child(Name.identifier("CopyBuilderFactory")))
                ?: error("Class: CopyBuilderFactory cannot be found!")
        val parameterizedFactoryType = copyBuilderFactoryClass.typeWith(klass.defaultType)
        klass.superTypes += parameterizedFactoryType

        val copyBuilderClass = context.referenceClass(CopyBuilderFqn.child(Name.identifier("CopyBuilder")))
            ?: error("Class: CopyBuilder cannot be found!")
        val parameterizedType = copyBuilderClass.typeWith(klass.defaultType)

        val toCopyBuilderFunc = klass.addFunc("toCopyBuilder", parameterizedType, Modality.OPEN).apply {
            val superFunc =
                copyBuilderFactoryClass.functions.singleOrNull { it.descriptor.name.asString() == "toCopyBuilder" }
                    ?: error("Function not found: toCopyBuilder")
            this.overriddenSymbols += superFunc

            val builderImplClassName = getImplClassName(klass.name.asString())
            val builderImplClass =
                context.referenceClass(
                    klass.packageFqName?.child(Name.identifier(builderImplClassName))
                        ?: FqName(builderImplClassName)
                )
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

        klass.addFunc("copyBuild", klass.defaultType, Modality.OPEN).apply {
            val superFunc =
                copyBuilderFactoryClass.functions.singleOrNull { it.descriptor.name.asString() == "copyBuild" }
                    ?: error("Function not found: copyBuild");
            overriddenSymbols += superFunc

            val initializerFunc = irBuiltIns.functionN(1)
            val initilizerType = initializerFunc.typeWith(parameterizedType, irBuiltIns.unitType)
            val param1 = addValueParameter("initialize", initilizerType)

            body = irBuiltIns.createIrBuilder(this.symbol).irBlockBody {
                val thisReceiver = irGet(this@apply.dispatchReceiverParameter!!)
                val localBuilder = irTemporary(irCall(toCopyBuilderFunc).apply {
                    dispatchReceiver = thisReceiver
                }, "builder")

                val invokeFunc = initializerFunc.simpleFunctions().singleOrNull { it.name.asString() == "invoke" }
                    ?: error("Can't get invoke function of ${initializerFunc.name}")
                val buildFunc =
                    parameterizedType.getClass()?.functions?.singleOrNull { it.name.asString() == "build" }
                        ?: error("build function not found")

                +irCall(invokeFunc).apply {
                    dispatchReceiver = irGet(param1)
                    putValueArgument(0, irGet(localBuilder))
                }

                +irReturn(
                    irCall(buildFunc).apply {
                        dispatchReceiver = irGet(localBuilder)
                    }
                )

            }
        }

        return klass
    }

}
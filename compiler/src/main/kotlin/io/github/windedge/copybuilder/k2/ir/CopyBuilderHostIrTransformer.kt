package io.github.windedge.copybuilder.k2.ir

import io.github.windedge.copybuilder.BUILD_NAME
import io.github.windedge.copybuilder.COPY_BUILD_NAME
import io.github.windedge.copybuilder.TO_COPY_BUILDER_NAME
import io.github.windedge.copybuilder.declarationIrBuilder
import io.github.windedge.copybuilder.isCopyBuilderHost
import io.github.windedge.copybuilder.isKopyBuilder
import io.github.windedge.copybuilder.toImplClassId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Transformer for generating toCopyBuilder() and copyBuild() methods in data classes
 */
class CopyBuilderHostIrTransformer(private val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    override fun visitFunction(declaration: IrFunction): IrStatement {
        fun originalFunction(): IrStatement = super.visitFunction(declaration)

        val irClass = declaration.parent as? IrClass ?: return originalFunction()
        if (!irClass.annotations.any { it.type.isKopyBuilder() }) {
            return originalFunction()
        }

        return when (declaration.name) {
            TO_COPY_BUILDER_NAME -> buildToCopyBuilder(declaration)
            COPY_BUILD_NAME -> buildCopyBuild(declaration)
            else -> originalFunction()
        }
    }

    /**
     * Builds the toCopyBuilder() function implementation that creates a new instance of the CopyBuilder implementation class.
     *
     * Example generated function:
     * ```kotlin
     * override fun toCopyBuilder(): CopyBuilder<SomeDataClass> = SomeDataClassCopyBuilderImpl(this)
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildToCopyBuilder(declaration: IrFunction): IrFunction {
        if (declaration !is IrSimpleFunction) return declaration

        declaration.origin = IrDeclarationOrigin.DEFINED
        declaration.isFakeOverride = false
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val dataClass = declaration.parentAsClass
            val implClassName = dataClass.name.asString() + "CopyBuilderImpl"

            val implClass = pluginContext.referenceClass(dataClass.toImplClassId()) ?: error(
                """Implementation class $implClassName not found.
                           Make sure CopyBuilderIrTransformer runs before CopyBuilderHostIrTransformer."""
            )

            val constructor = implClass.owner.primaryConstructor ?: error("No constructor found in $implClassName")
            +irReturn(
                irCall(constructor).apply {
                    type = constructor.returnType
                    putValueArgument(0, irGet(declaration.dispatchReceiverParameter!!))
                }
            )
        }
        return declaration
    }

    /**
     * Builds the copyBuild() function implementation that creates a new CopyBuilder instance,
     * applies the initialization block, and builds the final object.
     *
     * Example generated function:
     * ```kotlin
     * override fun copyBuild(initialize: CopyBuilder<SomeDataClass>.() -> Unit): SomeDataClass {
     *     val builder = toCopyBuilder()
     *     builder.initialize()
     *     return builder.build()
     * }
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildCopyBuild(declaration: IrFunction): IrFunction {
        if (declaration !is IrSimpleFunction) return declaration

        declaration.origin = IrDeclarationOrigin.DEFINED
        declaration.isFakeOverride = false
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val initialize = declaration.valueParameters[0]
            val dataClass = declaration.parentAsClass
            val thisReceiver = declaration.dispatchReceiverParameter!!

            // Create local variable for builder
            val builder = irTemporary(
                irCall(dataClass.functions.first { it.name == TO_COPY_BUILDER_NAME }).apply {
                    dispatchReceiver = irGet(thisReceiver)
                }
            )

            // Call initialize on builder
            val invokeFunction = initialize.type.classOrFail.functionByName("invoke")
            +irCall(invokeFunction).apply {
                dispatchReceiver = irGet(initialize)
                putValueArgument(0, irGet(builder))
            }

            // Return builder.build()
            val buildFunction = builder.type.classOrFail.functionByName(BUILD_NAME.asString())
            +irReturn(
                irCall(buildFunction).apply {
                    dispatchReceiver = irGet(builder)
                }
            )
        }
        return declaration
    }
}

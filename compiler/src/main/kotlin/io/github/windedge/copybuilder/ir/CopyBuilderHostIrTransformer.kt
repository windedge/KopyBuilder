package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.BUILD_NAME
import io.github.windedge.copybuilder.COPY_BUILD_NAME
import io.github.windedge.copybuilder.TO_COPY_BUILDER_NAME
import io.github.windedge.copybuilder.toImplClassId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isOverridable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Transformer for generating toCopyBuilder() and copyBuild() methods in data classes
 */
class CopyBuilderHostIrTransformer(private val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    override fun visitClass(declaration: IrClass): IrStatement {
        return super.visitClass(declaration).also {
            declaration.takeIf { it.superTypes.any { it.isCopyBuilderHost() } }?.let {
                println("transformed class: \n${it.dumpKotlinLike()}")
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        fun originalFunction(): IrStatement = super.visitFunction(declaration)


        val irClass = declaration.parent as? IrClass ?: return originalFunction()
        if (!irClass.annotations.any { it.type.isKopyBuilder() }) {
            return originalFunction()
        }

//        val origin = declaration.origin
//        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != Key) {
//            return originalFunction()
//        }

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
        declaration.isFakeOverride
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val dataClass = declaration.parentAsClass
            val implClassName = dataClass.name.asString() + "CopyBuilderImpl"

//            val parent = declaration.parentAsClass.parent as? IrDeclarationContainer
//                ?: error("Parent of ${declaration.parentAsClass.name} is not a declaration container")
//
//            val implClass = parent.declarations.firstOrNull {
//                it is IrClass && it.name.asString() == implClassName
//            } as? IrClass ?: error(
//                """Implementation class $implClassName not found.
//                   Make sure CopyBuilderIrTransformer runs before CopyBuilderHostIrTransformer."""
//            )

            val implClass = pluginContext.referenceClass(dataClass.toImplClassId()) ?: error(
                """Implementation class $implClassName not found. 
                   Make sure CopyBuilderIrTransformer runs before CopyBuilderHostIrTransformer."""
            )

            val constructor = implClass.constructors.singleOrNull()
                ?: error("No constructor found in $implClassName")
            
            +irReturn(
                irCall(constructor).apply {
                    putValueArgument(0, irGet(declaration.parentAsClass.thisReceiver!!))
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
    private fun buildCopyBuild(declaration: IrFunction): IrFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val initialize = declaration.valueParameters[0]

            // Create local variable for builder
            val builder = irTemporary(
                irCall(declaration.parentAsClass.functions.first { it.name == TO_COPY_BUILDER_NAME }).apply {
                    dispatchReceiver = irGet(declaration.parentAsClass.thisReceiver!!)
                }
            )

            // Call initialize on builder
            +irCall(initialize.type.classOrFail.functionByName("invoke")).apply {
                extensionReceiver = irGet(builder)
                dispatchReceiver = irGet(initialize)
            }

            // Return builder.build()
            +irReturn(
                irCall(builder.type.classOrFail.functionByName(BUILD_NAME.asString())).apply {
                    dispatchReceiver = irGet(builder)
                }
            )
        }
        return declaration
    }
}

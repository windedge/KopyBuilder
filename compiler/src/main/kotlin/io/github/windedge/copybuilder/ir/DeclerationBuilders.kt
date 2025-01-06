package io.github.windedge.copybuilder.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name

fun IrClass.addFunc(
    name: String,
    returnType: IrType,
    modality: Modality = Modality.FINAL,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    isStatic: Boolean = false,
    isSuspend: Boolean = false,
    isFakeOverride: Boolean = false,
    isInline: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
): IrSimpleFunction =
    addFunc {
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.modality = modality
        this.visibility = visibility
        this.isSuspend = isSuspend
        this.isFakeOverride = isFakeOverride
        this.isInline = isInline
        this.origin = origin
    }.apply {
        if (!isStatic) {
            val thisReceiver = parentAsClass.thisReceiver!!
            dispatchReceiverParameter = thisReceiver.copyTo(this, type = thisReceiver.type)
        }
    }

inline fun IrClass.addFunc(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    factory.addFunction(this, builder)


fun IrPluginContext.declarationIrBuilder(symbol: IrSymbol): DeclarationIrBuilder =
    DeclarationIrBuilder(this, symbol)

fun IrPluginContext.declarationIrBuilder(
    element: IrSymbolOwner
): DeclarationIrBuilder = DeclarationIrBuilder(this, element.symbol)

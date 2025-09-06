package io.github.windedge.copybuilder

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


fun IrPluginContext.declarationIrBuilder(symbol: IrSymbol): DeclarationIrBuilder =
    DeclarationIrBuilder(this, symbol)

fun IrPluginContext.declarationIrBuilder(
    element: IrSymbolOwner
): DeclarationIrBuilder = DeclarationIrBuilder(this, element.symbol)


fun IrProperty.getPropertyType(): IrType {
    return backingField?.type ?: getter?.returnType
    ?: throw IllegalStateException(Errors.copyBuilderAppliedWrongTarget("Unable to determine property type for $name"))
}

fun IrBuilderWithScope.kClassReference(classType: IrType): IrClassReference = IrClassReferenceImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = context.irBuiltIns.kClassClass.starProjectedType,
    symbol = classType.classifierOrFail,
    classType = classType
)

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrBuilderWithScope.createErrorCall(pluginContext: IrPluginContext, vararg messages: IrExpression): IrCall {
    val irBuiltIns = pluginContext.irBuiltIns

    // Find the error function that takes a single Any parameter
    val errorFunction = pluginContext.referenceFunctions(CallableId(FqName("kotlin"), Name.identifier("error")))
        .firstOrNull { function ->
            val owner = function.owner
            owner.parameters.count { it.kind.name == "Regular" || it.kind.name == "Context" } == 1 &&
            owner.parameters.first { it.kind.name == "Regular" || it.kind.name == "Context" }.type == irBuiltIns.anyType
        } ?: error("No suitable error function found")

    // Create a concatenated string from all messages
    val message = irConcat().apply {
        messages.forEach { message ->
            arguments.add(message)
        }
    }

    // Call the error function with the concatenated message
    return irCall(errorFunction).apply {
        arguments[symbol.owner.parameters.first { it.kind.name == "Regular" || it.kind.name == "Context" }] = message
    }
}

// Check if type is @KopyBuilder annotation
fun IrType.isKopyBuilder(): Boolean {
    return this.classFqName == CopyBuilderFqn.child(KOPY_BUILDER_NAME)
}

// Check if type is CopyBuilderHost interface
fun IrType.isCopyBuilder(): Boolean {
    return this.classFqName == CopyBuilderFqn.child(Name.identifier("CopyBuilder"))
}

fun IrType.isCopyBuilderHost(): Boolean {
    return this.classFqName == CopyBuilderFqn.child(Name.identifier("CopyBuilderHost"))
}

fun IrClass.addFunc(
    name: String,
    returnType: IrType,
    isStatic: Boolean = false,
    isSuspend: Boolean = false,
    isFakeOverride: Boolean = false,
    isInline: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.Companion.DEFINED,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
): IrSimpleFunction =
    addFunc {
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.isSuspend = isSuspend
        this.isFakeOverride = isFakeOverride
        this.isInline = isInline
        this.origin = origin
    }.apply {
        if (!isStatic) {
            val thisReceiver = parentAsClass.thisReceiver!!
            parameters = listOf(thisReceiver.copyTo(this, type = thisReceiver.type)) + parameters
        }
    }

inline fun IrClass.addFunc(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    factory.addFunction(this, builder)

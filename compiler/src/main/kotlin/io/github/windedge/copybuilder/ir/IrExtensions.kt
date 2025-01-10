package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.Errors
import io.github.windedge.copybuilder.CopyBuilderFqn
import io.github.windedge.copybuilder.KOPY_BUILDER_NAME
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun dumpDeclarationInfo(declaration: IrDeclarationWithName, prefix: String? = null) {
    val parentName = declaration.parentClassOrNull?.name ?: declaration.getPackageFragment().packageFqName
    buildString {
        append(prefix ?: "declaration")
        append(" = ")
        append(parentName)
        append(":")
        append(declaration.name)
        (declaration as? IrProperty)?.backingField?.type?.classFqName?.let {
            append("(").append(it.asString()).append(")")
        }
    }.let(::println)
}


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
fun IrBuilderWithScope.createErrorCall(pluginContext: IrPluginContext, vararg messages: IrExpression): IrCall =
    irCall(
        pluginContext.referenceFunctions(CallableId(FqName("kotlin"), Name.identifier("error")))
            .single { it.owner.valueParameters.size == 1 }
    ).apply {
        putValueArgument(0, irConcat().apply {
            messages.forEach(arguments::add)
        })
    }

// Check if type is @KopyBuilder annotation
fun IrType.isKopyBuilder(): Boolean {
    return this.classFqName == CopyBuilderFqn.child(KOPY_BUILDER_NAME)
}

// Check if type is CopyBuilderHost interface
fun IrType.isCopyBuilderHost(): Boolean {
    return this.classFqName == CopyBuilderFqn.child(Name.identifier("CopyBuilderHost"))
}

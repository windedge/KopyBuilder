package io.github.windedge.copybuilder.k1

import io.github.windedge.copybuilder.CopyBuilderFqn
import io.github.windedge.copybuilder.addFunc
import io.github.windedge.copybuilder.toImplClassName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CopyBuilderTransformer(
    val context: IrPluginContext,
    annotationName: String,
) : IrElementTransformer<Nothing?> {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns
    private val annotationFqName: FqName = FqName(annotationName)

    override fun visitClass(declaration: IrClass, data: Nothing?): IrStatement {
        val klass = super.visitClass(declaration, data) as IrClass
        val hasAnnotation = declaration.annotations.hasAnnotation(annotationFqName)
        if (!hasAnnotation) return klass

        if (!declaration.isData) {
            error("@KopyBuilder can only be applied to data classes")
        }

        // 使用ClassId来获取类引用
        val copyBuilderHostClassId = ClassId(CopyBuilderFqn, Name.identifier("CopyBuilderHost"))
        val copyBuilderHostClass = context.referenceClass(copyBuilderHostClassId)
            ?: error("Class: CopyBuilderHost cannot be found!")
        val parameterizedFactoryType = copyBuilderHostClass.typeWith(klass.defaultType)
        klass.superTypes += parameterizedFactoryType

        val copyBuilderClassId = ClassId(CopyBuilderFqn, Name.identifier("CopyBuilder"))
        val copyBuilderClass = context.referenceClass(copyBuilderClassId)
            ?: error("Class: CopyBuilder cannot be found!")
        val parameterizedType = copyBuilderClass.typeWith(klass.defaultType)

        val toCopyBuilderFunc = klass.addFunc("toCopyBuilder", parameterizedType, Modality.OPEN).apply {
            val superFunc =
                copyBuilderHostClass.functions.singleOrNull { it.owner.name.asString() == "toCopyBuilder" }
                    ?: error("Function not found: toCopyBuilder")
            this.overriddenSymbols += superFunc

            val builderImplClassName = klass.toImplClassName()
            val builderImplClassId = ClassId(
                klass.packageFqName ?: CopyBuilderFqn,
                Name.identifier(builderImplClassName)
            )
            val builderImplClass = context.referenceClass(builderImplClassId)
                ?: error("Class not found: $builderImplClassName")

            val irBuilder = context.irBuiltIns.createIrBuilder(symbol)
            body = irBuilder.irBlockBody {
                +irReturn(
                    irCall(
                        builderImplClass.constructors.firstOrNull()
                            ?: error("No constructor found in $builderImplClassName"),
                    ).apply {
                        putValueArgument(0, irGet(klass.thisReceiver!!))
                    }
                )
            }
        }

        klass.addFunc("copyBuild", klass.defaultType, Modality.OPEN).apply {
            val superFunc =
                copyBuilderHostClass.functions.singleOrNull { it.owner.name.asString() == "copyBuild" }
                    ?: error("Function not found: copyBuild");
            overriddenSymbols += superFunc

            val irBuilder = context.irBuiltIns.createIrBuilder(symbol)
            body = irBuilder.irBlockBody {
                +irReturn(irGet(klass.thisReceiver!!))
            }
        }

        return klass
    }
}
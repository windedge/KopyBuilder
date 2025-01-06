package io.github.windedge.copybuilder.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class CopyBuilderIrFunctionsTransformer(
    private val moduleFragment: IrModuleFragment,
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoidWithContext() {

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        fun originalFunction(): IrStatement = super.visitSimpleFunction(declaration)

        return when (declaration.name) {
            SpecialNames.INIT -> transformInitFunction(declaration)
            Name.identifier("contains") -> transformContainsFunction(declaration)
            Name.identifier("get") -> transformGetFunction(declaration)
            Name.identifier("put") -> transformPutFunction(declaration)
            Name.identifier("build") -> transformBuildFunction(declaration)
            else -> originalFunction()
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun transformInitFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val sourceParam = declaration.valueParameters[0]
            val sourceField = declaration.parentAsClass.fields.first { it.name.asString() == "source" }

            +irSetField(
                irGet(declaration.dispatchReceiverParameter!!),
                sourceField,
                irGet(sourceParam)
            )
        }
        return declaration
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun transformContainsFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val valuesProperty = declaration.parentAsClass.properties.first { it.name.asString() == "values" }
            val propertyParam = declaration.valueParameters[0]
            +irReturn(
                irCall(pluginContext.irBuiltIns.mapClass.functions.first { it.owner.name.asString() == "containsKey" }).apply {
                    dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), valuesProperty.backingField!!)
                    putValueArgument(0, irGet(propertyParam))
                }
            )
        }
        return declaration
    }

    private fun transformGetFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val valuesProperty = declaration.parentAsClass.properties.first { it.name.asString() == "values" }
            val propertiesProperty = declaration.parentAsClass.properties.first { it.name.asString() == "properties" }
            val privatePropertiesProperty = declaration.parentAsClass.properties.first { it.name.asString() == "privateProperties" }
            val sourceField = declaration.parentAsClass.fields.first { it.name.asString() == "source" }
            val propertyParam = declaration.valueParameters[0]

            // 检查 values map 中是否包含该属性
            +irIfThen(
                irCall(pluginContext.irBuiltIns.mapClass.functions.first { it.owner.name.asString() == "contains" }).apply {
                    dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), valuesProperty.backingField!!)
                    putValueArgument(0, irGet(propertyParam))
                },
                irCall(pluginContext.irBuiltIns.mapClass.functions.first { it.owner.name.asString() == "get" }).apply {
                    dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), valuesProperty.backingField!!)
                    putValueArgument(0, irGet(propertyParam))
                }
            )

            // 检查是否是私有属性
//            +irIfThenThrow(
//                irCall(pluginContext.irBuiltIns.setClass.functions.first { it.owner.name.asString() == "contains" }).apply {
//                    dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), privatePropertiesProperty.backingField!!)
//                    putValueArgument(0, irGet(propertyParam))
//                },
//                irString("Can't get the original value of property: ${propertyParam.name}, because it is private.")
//            )
//
//            // 检查属性是否存在
//            +irIfThenThrow(
//                irNot(
//                    irCall(pluginContext.irBuiltIns.mapClass.functions.first { it.owner.name.asString() == "containsKey" }).apply {
//                        dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), propertiesProperty.backingField!!)
//                        putValueArgument(0, irGet(propertyParam))
//                    }
//                ),
//                irString("Property: ${propertyParam.name} not found in Class: ${declaration.parentAsClass.name}")
//            )

            // 从原始对象获取属性值
            +irReturn(
                irGetField(
                    irGetField(irGet(declaration.dispatchReceiverParameter!!), sourceField),
                    declaration.parentAsClass.properties.first { it.name.asString() == propertyParam.name.asString() }.backingField!!
                )
            )
        }
        return declaration
    }

    private fun transformPutFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val valuesProperty = declaration.parentAsClass.properties.first { it.name.asString() == "values" }
            val propertiesProperty = declaration.parentAsClass.properties.first { it.name.asString() == "properties" }
            val propertyParam = declaration.valueParameters[0]
            val valueParam = declaration.valueParameters[1]

            // 检查属性是否存在
//            +irIfThenThrow(
//                irNot(
//                    irCall(pluginContext.irBuiltIns.mapClass.functions.first { it.owner.name.asString() == "containsKey" }).apply {
//                        dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), propertiesProperty.backingField!!)
//                        putValueArgument(0, irGet(propertyParam))
//                    }
//                ),
//                irString("Property: ${propertyParam.name} not found in Class: ${declaration.parentAsClass.name}")
//            )
//
//            // 检查类型是否匹配
//            val propertyType = irCall(pluginContext.irBuiltIns.mapClass.functions.first { it.owner.name.asString() == "get" }).apply {
//                dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), propertiesProperty.backingField!!)
//                putValueArgument(0, irGet(propertyParam))
//            }
//
//            +irIfThenThrow(
//                irAnd(
//                    irNotNull(irGet(valueParam)),
//                    irNot(
//                        irCall(pluginContext.irBuiltIns.kClassClass.functions.first { it.owner.name.asString() == "isInstance" }).apply {
//                            dispatchReceiver = propertyType
//                            putValueArgument(0, irGet(valueParam))
//                        }
//                    )
//                ),
//                irString("Type mismatch, property: ${propertyParam.name}, expected: ${propertyType.symbol.owner.name}, actual: ${valueParam.type.classFqName}")
//            )

            // 设置属性值
            +irCall(pluginContext.irBuiltIns.mutableMapClass.functions.first { it.owner.name.asString() == "put" }).apply {
                dispatchReceiver = irGetField(irGet(declaration.dispatchReceiverParameter!!), valuesProperty.backingField!!)
                putValueArgument(0, irGet(propertyParam))
                putValueArgument(1, irGet(valueParam))
            }
            +irReturn(irGet(declaration.dispatchReceiverParameter!!))
        }
        return declaration
    }

    private fun transformBuildFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val sourceField = declaration.parentAsClass.fields.first { it.name.asString() == "source" }
            +irReturn(irGetField(irGet(declaration.dispatchReceiverParameter!!), sourceField))
        }
        return declaration
    }
}

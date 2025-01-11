package io.github.windedge.copybuilder.ir

import io.github.windedge.copybuilder.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.SpecialNames

class CopyBuilderIrTransformer(private val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    private val irBuiltIn = pluginContext.irBuiltIns

    override fun visitClass(declaration: IrClass): IrStatement {
        return super.visitClass(declaration).also {
            declaration.takeIf { it.superTypes.any { it.isCopyBuilder() } }?.let {
                println("transformed class: \n${it.dumpKotlinLike()}")
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        fun originalFunction(): IrStatement = super.visitFunction(declaration)

        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != Key) {
            return originalFunction()
        }

        return when (declaration.name) {
            SpecialNames.INIT -> buildConstructor(declaration)
            CONTAINS_NAME -> buildContainsFunction(declaration)
            GET_NAME -> buildGetFunction(declaration)
            PUT_NAME -> buildPutFunction(declaration)
            BUILD_NAME -> buildBuildFunction(declaration)
            else -> originalFunction()
        }
    }


    /**
     * Builds the constructor for the CopyBuilder implementation class.
     * The constructor initializes the source property and all generated properties (values, properties, privateProperties).
     *
     * Example generated constructor:
     * ```kotlin
     * class SomeDataClassCopyBuilderImpl(private val source: SomeDataClass) : CopyBuilder<SomeDataClass> {
     *   private val values: MutableMap<String, Any?> = mutableMapOf()
     *   private val properties: Map<String, KClass<*>> = mapOf(
     *     "field1" to String::class,
     *     "field2" to Boolean::class
     *   )
     *   private val privateProperties: Set<String> = setOf()
     *   // ... other members
     * }
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildConstructor(declaration: IrFunction): IrFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val sourceParam = declaration.valueParameters[0]
            val sourceProperty = declaration.parentAsClass.properties.first { it.name.asString() == "source" }

            +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())

            +irSetField(
                irGet(declaration.parentAsClass.thisReceiver!!),
                sourceProperty.backingField!!,
                irGet(sourceParam)
            )

            // Initialize generated properties
            declaration.parentAsClass.properties.forEach { property ->
                if (property.origin is IrDeclarationOrigin.GeneratedByPlugin &&
                    (property.origin as IrDeclarationOrigin.GeneratedByPlugin).pluginKey == Key
                ) {

                    val initializer = when (property.name) {
                        VALUES_NAME -> buildValuesInitializer()
                        PROPERTIES_NAME -> buildPropertiesInitializer(property)
                        PRIVATE_PROPERTIES_NAME -> buildPrivatePropertiesInitializer(property)
                        else -> null
                    }


                    if (initializer != null && property.backingField != null) {
                        +irSetField(
                            irGet(declaration.parentAsClass.thisReceiver!!),
                            property.backingField!!,
                            initializer.expression
                        )
                    }
                }
            }
        }
        return declaration
    }


    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBuilderWithScope.buildValuesInitializer(): IrExpressionBody {
        return irExprBody(
            irCall(
                pluginContext.referenceFunctions(MUTABLE_MAP_OF).first { it.owner.valueParameters.isEmpty() }
            ).apply {
                putTypeArgument(0, irBuiltIn.stringType)
                putTypeArgument(1, irBuiltIn.anyNType)
            }
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBuilderWithScope.buildPropertiesInitializer(property: IrProperty): IrExpressionBody {
        val sourceClass = getSourceClass(property)
        val properties = sourceClass.properties

        val toFunction = pluginContext.referenceFunctions(TO_FUNCTION)
            .first { it.owner.valueParameters.size == 1 }

        val entries = properties.map { prop ->
            irCall(toFunction).apply {
                putTypeArgument(0, irBuiltIn.stringType)
                putTypeArgument(1, irBuiltIn.kClassClass.starProjectedType)
                extensionReceiver = irString(prop.name.asString())
                putValueArgument(0, kClassReference(prop.getPropertyType()))
            }
        }

        return irExprBody(
            irCall(
                pluginContext.referenceFunctions(MAP_OF)
                    .first { it.owner.valueParameters.firstOrNull()?.isVararg == true }
            ).apply {
                putTypeArgument(0, irBuiltIn.stringType)
                putTypeArgument(1, irBuiltIn.kClassClass.starProjectedType)
                val pairClass = pluginContext.referenceClass(PAIR_CLASS_ID)!!
                putValueArgument(0, irVararg(elementType = pairClass.defaultType, values = entries.toList()))
            }
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildPrivatePropertiesInitializer(property: IrProperty): IrExpressionBody {
        return pluginContext.declarationIrBuilder(property).run {
            val sourceClass = getSourceClass(property)
            val privateProperties = sourceClass.properties.filter { !it.visibility.isPublicAPI }
            val entries = privateProperties.map { prop ->
                irString(prop.name.asString())
            }

            irExprBody(
                irCall(
                    pluginContext.referenceFunctions(SET_OF)
                        .first { it.owner.valueParameters.size == 1 }
                ).apply {
                    putTypeArgument(0, irBuiltIn.stringType)
                    putValueArgument(0, irVararg(irBuiltIn.stringType, entries.toList()))
                }
            )
        }
    }

    /**
     * Builds the contains() function implementation that checks if a property exists in the source class.
     *
     * Example generated function:
     * ```kotlin
     * override fun contains(key: String): Boolean = properties.containsKey(key)
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildContainsFunction(declaration: IrFunction): IrFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val propertiesProperty = declaration.parentAsClass.properties.first { it.name == PROPERTIES_NAME }
            val propertyParam = declaration.valueParameters[0]
            +irReturn(
                irCall(irBuiltIn.mapClass.functions.first { it.owner.name.asString() == "containsKey" }).apply {
                    dispatchReceiver =
                        irGetField(irGet(declaration.dispatchReceiverParameter!!), propertiesProperty.backingField!!)
                    putValueArgument(0, irGet(propertyParam))
                }
            )
        }
        return declaration
    }

    /**
     * Builds the get() function implementation that retrieves property values.
     * Returns the value from the values map if present, otherwise gets it from the source object.
     * Throws error if trying to access private properties.
     *
     * Example generated function:
     * ```kotlin
     * override fun get(key: String): Any? {
     *   if (values.contains(key)) {
     *     return values[key]
     *   }
     *   if (key in privateProperties) {
     *     error("Can't get the original value of property: $key, because it is private.")
     *   }
     *   return when(key) {
     *     "field1" -> source.field1
     *     "field2" -> source.field2
     *     else -> error("Property: $key not found in Class: SomeDataClass")
     *   }
     * }
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildGetFunction(declaration: IrFunction): IrFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val valuesProperty = declaration.parentAsClass.properties.first { it.name == VALUES_NAME }
            val privatePropertiesProperty =
                declaration.parentAsClass.properties.first { it.name == PRIVATE_PROPERTIES_NAME }
            val sourceProperty = declaration.parentAsClass.properties.first { it.name == SOURCE_NAME }
            val propertyNameParam = declaration.valueParameters[0]

            // if (values.contains(key)) return values[key]
            val self = declaration.dispatchReceiverParameter
            +irIfThen(
                type = irBuiltIn.unitType,
                condition = irCall(irBuiltIn.mapClass.functionByName("containsKey")).apply {
                    dispatchReceiver = irGetField(irGet(self!!), valuesProperty.backingField!!)
                    putValueArgument(0, irGet(propertyNameParam))
                },
                thenPart = irReturn(
                    irCall(irBuiltIn.mapClass.functionByName("get")).apply {
                        dispatchReceiver = irGetField(irGet(self!!), valuesProperty.backingField!!)
                        putValueArgument(0, irGet(propertyNameParam))
                    }
                )
            )

            // if (key in privateProperties) error(...)
            +irIfThen(
                type = irBuiltIn.unitType,
                condition = irCall(irBuiltIn.setClass.functions.first { it.owner.name.asString() == "contains" }).apply {
                    dispatchReceiver = irGetField(irGet(self!!), privatePropertiesProperty.backingField!!)
                    putValueArgument(0, irGet(propertyNameParam))
                },
                thenPart = createErrorCall(
                    pluginContext,
                    irString("Can't get the original value of property: "),
                    irGet(propertyNameParam),
                    irString(", because it is private.")
                )
            )

            // when() { ... }
            val sourceClass = getSourceClass(declaration.parentAsClass.properties.first())
            val properties = sourceClass.properties
            +irReturn(
                irWhen(
                    type = context.irBuiltIns.anyNType,
                    branches =
                        properties.map { property ->
                            val propertyName = property.name.asString()
                            val condition = irEquals(irGet(propertyNameParam), irString(propertyName))
                            val source = irCall(sourceProperty.getter!!).apply { dispatchReceiver = irGet((self!!)) }
                            val getter = irCall(property.getter!!).apply { dispatchReceiver = source }
                            irBranch(condition, getter)
                        }.toList() + irElseBranch(
                            createErrorCall(
                                pluginContext,
                                irString("Property: "),
                                irGet(propertyNameParam),
                                irString(" not found in Class: "),
                                irString(sourceClass.name.asString())
                            )
                        )
                )
            )

        }
        return declaration
    }

    /**
     * Builds the put() function implementation that stores property values.
     * Validates that the property exists and the value type matches before storing.
     *
     * Example generated function:
     * ```kotlin
     * override fun put(key: String, value: Any?) {
     *   if (key !in properties) {
     *     error("Property: $key not found in Class: SomeDataClass")
     *   }
     *   val type = properties[key]!!
     *   if (value != null && !type.isInstance(value)) {
     *     error("Type mismatch, property: $key, expected: ${type.simpleName}, actual: ${value::class.simpleName}")
     *   }
     *   values[key] = value
     * }
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildPutFunction(declaration: IrFunction): IrFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val propertiesProperty = declaration.parentAsClass.properties.first { it.name == PROPERTIES_NAME }
            val valuesProperty = declaration.parentAsClass.properties.first { it.name == VALUES_NAME }
            val self = declaration.dispatchReceiverParameter!!
            val keyParam = declaration.valueParameters[0]
            val valueParam = declaration.valueParameters[1]
            val sourceClass = getSourceClass(declaration.parentAsClass.properties.first())

            // 1. Check if the property exists
            +irIfThen(
                type = irBuiltIn.unitType,
                condition = irNot(
                    irCall(irBuiltIn.mapClass.functionByName("containsKey")).apply {
                        dispatchReceiver = irGetField(irGet(self), propertiesProperty.backingField!!)
                        putValueArgument(0, irGet(keyParam))
                    }
                ),
                thenPart = createErrorCall(
                    pluginContext,
                    irString("Property: "),
                    irGet(keyParam),
                    irString(" not found in Class: "),
                    irString(sourceClass.name.asString())
                )
            )

            // 2. Get property type (KClass<*>)
            val propertyType = irTemporary(
                irCall(irBuiltIn.checkNotNullSymbol).apply {
                    putValueArgument(0, irCall(irBuiltIn.mapClass.functionByName("get")).apply {
                        dispatchReceiver = irGetField(irGet(self), propertiesProperty.backingField!!)
                        putValueArgument(0, irGet(keyParam))
                    })
                    type = irBuiltIn.kClassClass.starProjectedType
                }
            )

            // 3. Check if the type matches
            val simpleNameSymbol = propertyType.type.classOrFail.owner.getPropertyGetter("simpleName")
                ?: error("simpleName getter not found")
            val getSimpleNameCall = irCall(simpleNameSymbol).apply {
                dispatchReceiver = irGet(propertyType)
            }
            val isInstanceCall = irCall(irBuiltIn.kClassClass.functionByName("isInstance")).apply {
                dispatchReceiver = irGet(propertyType)
                putValueArgument(0, irGet(valueParam))
            }
            +irIfThen(
                type = irBuiltIn.unitType,
                condition = irCall(irBuiltIn.andandSymbol).apply {
                    putValueArgument(0, irNotEquals(irGet(valueParam), irNull()))
                    putValueArgument(1, irNot(isInstanceCall))
                },
                thenPart = createErrorCall(
                    pluginContext,
                    irString("Type mismatch, property: "),
                    irGet(keyParam),
                    irString(" , expected type: "),
                    getSimpleNameCall
                )
            )

            // 4. Store the value
            +irCall(irBuiltIn.mutableMapClass.functionByName("put")).apply {
                dispatchReceiver = irGetField(irGet(self), valuesProperty.backingField!!)
                putValueArgument(0, irGet(keyParam))
                putValueArgument(1, irGet(valueParam))
            }
        }
        return declaration
    }

    /**
     * Builds the build() function implementation that creates a new instance of the source class
     * with all the modified property values.
     *
     * Example generated function:
     * ```kotlin
     * @Suppress("UNCHECKED_CAST")
     * override fun build(): SomeDataClass = SomeDataClass(
     *   field1 = this.get("field1") as String?,
     *   field2 = this.get("field2") as Boolean,
     *   field3 = this.get("field3") as Boolean
     * )
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildBuildFunction(declaration: IrFunction): IrFunction {
        declaration.body = pluginContext.declarationIrBuilder(declaration.symbol).irBlockBody {
            val sourceClass = getSourceClass(declaration.parentAsClass.properties.first())
            val constructor = sourceClass.primaryConstructor!!

            // Create constructor call
            val constructorCall = irCall(constructor).apply {
                // Set value for each constructor parameter
                constructor.valueParameters.forEach { param ->
                    val paramName = param.name.asString()
                    val paramType = param.type
                    val getCall = irCall(declaration.parentAsClass.getSimpleFunction("get")!!).apply {
                        dispatchReceiver = irGet(declaration.dispatchReceiverParameter!!)
                        putValueArgument(0, irString(paramName))
                    }

                    // Add type casting
                    val castedValue = if (paramType.isMarkedNullable()) {
                        irAs(getCall, paramType)
                    } else {
                        irAs(getCall, paramType)
                    }

                    putValueArgument(param.index, castedValue)
                }
            }

            +irReturn(constructorCall)
        }
        return declaration
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun getSourceClass(property: IrProperty): IrClass {
        val implClass = property.parentClassOrNull!!
        val sourceProperty = implClass.properties.first { it.name.asString() == "source" }
        return sourceProperty.getter!!.returnType.getClass()!!
    }

}

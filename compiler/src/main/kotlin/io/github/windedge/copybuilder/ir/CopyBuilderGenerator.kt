package io.github.windedge.copybuilder.ir

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.windedge.copybuilder.*
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.isNullable
import kotlin.reflect.KClass


fun ClassDescriptor.generateImplClass(): FileSpec {
    val packageName = this.findPackage().fqName.asString()
    val dataClassName = this.toClassName()
    val builderClassName = ClassName(packageName, dataClassName.toImplClassName())
    val fileName = dataClassName.toImplFileName()
    val copyBuilderClassName = CopyBuilder::class.asClassName()

    return FileSpec.builder(packageName, fileName)
        .addImport(copyBuilderClassName, "")
//        .addImport(dataClassName, "")
        .addType(
            TypeSpec.classBuilder(builderClassName)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("source", dataClassName).build())
                .addProperty(
                    PropertySpec.builder("source", dataClassName, KModifier.PRIVATE).initializer("source").build()
                )
                .addProperty(valueMap())
                .addProperty(propertiesMap())
                .addProperty(privatePropertiesSet())
                .addSuperinterface(copyBuilderClassName.parameterizedBy(dataClassName))
                .addFunction(containsFunction())
                .addFunction(getterFunction())
                .addFunction(setterFunction())
                .addFunction(buildFunction())
                .build()

        )
        .addFunction(toBuilderExtension(builderClassName))
        .addFunction(copyBuildExtension())
        .build()
}


fun ClassDescriptor.toBuilderExtension(builderClassName: ClassName): FunSpec {
    val copyBuilderName = toParameterizedCopyBuilderName()
    return FunSpec.builder("toCopyBuilder")
        .receiver(this.toClassName())
        .returns(copyBuilderName)
        .addStatement("return %T(%L)", builderClassName, "this")
        .build()
}

fun ClassDescriptor.copyBuildExtension(): FunSpec {
    val copyBuilderName = toParameterizedCopyBuilderName()
    val lambdaType = LambdaTypeName.get(copyBuilderName, returnType = UNIT)

    return FunSpec.builder("copyBuild")
        .receiver(this.toClassName())
        .addParameter("initialize", lambdaType)
        .returns(this.toClassName())
        .addStatement("val builder = %L()", "toCopyBuilder")
        .addStatement("builder.%L()", "initialize")
        .addStatement("return %L.%L()", "builder", "build")
        .build()
}

private fun ClassDescriptor.toParameterizedCopyBuilderName() =
    CopyBuilder::class.asClassName().parameterizedBy(this.toClassName())

private fun valueMap() =
    PropertySpec.builder("values", MUTABLE_MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
        .addModifiers(KModifier.PRIVATE)
        .initializer("mutableMapOf()")
        .build()

fun ClassDescriptor.propertiesMap(): PropertySpec {
    val properties = this.properties
    val names = properties.map {
        val type = it.type.toClassName() ?: it.fqNameSafe
        CodeBlock.of("%S to %T::class", it.name.asString(), type)
    }.joinToCode(", ")
    return PropertySpec.builder(
        "properties", MAP.parameterizedBy(STRING, KClass::class.asClassName().parameterizedBy(STAR))
    ).addModifiers(KModifier.PRIVATE).initializer("mapOf(%L)", names).build()
}

fun ClassDescriptor.privatePropertiesSet(): PropertySpec {
    val properties = this.privateProperties
    val names = properties.map {
        CodeBlock.of("%S", it.name.asString())
    }.joinToCode(", ")
    return PropertySpec.builder("privateProperties", SET.parameterizedBy(STRING))
        .addModifiers(KModifier.PRIVATE).initializer("setOf(%L)", names).build()
}

fun containsFunction(): FunSpec {
    return FunSpec.builder("contains")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("key", STRING)
        .returns(BOOLEAN)
        .apply {
            addStatement("return properties.containsKey(%L)", "key")
        }.build()
}

private fun ClassDescriptor.buildFunction(): FunSpec {
    val properties = this.properties
    val propertyNames = properties.map { it.name.asString() }
    val constructor = this.constructors.find { ctor ->
        val parameterNames = ctor.valueParameters.map { it.name.asString() }
        propertyNames.containsAll(parameterNames)
    } ?: error("Can't find proper constructor.")

    val blocks = constructor.valueParameters.map { param ->
        buildCodeBlock {
            val paramClassName = param.type.toClassName()
                ?: error("can't figure out parameter's type, parameter: ${param.name}")

            var typeName: TypeName = paramClassName
            if (param.type.arguments.isNotEmpty()) {
                val argumentTypes = param.type.arguments.mapNotNull { it.type.toClassName() }
                typeName = paramClassName.parameterizedBy(argumentTypes)
            }
            if (param.type.isNullable()) {
                typeName = typeName.copy(nullable = true)
            }

            addStatement(
                "%L = this.get(%S) as %T",
                param.name.asString(),
                param.name.asString(),
                typeName
            )
        }
    }.joinToCode()

    val suppress = AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()
    return FunSpec.builder("build")
        .addModifiers(KModifier.OVERRIDE)
        .returns(this.toClassName())
        .addAnnotation(suppress)
        .addStatement("return %T(%L)", this.toClassName(), blocks)
        .build()
}

private fun ClassDescriptor.setterFunction(): FunSpec {
    return FunSpec.builder("put")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("key", STRING)
        .addParameter("value", ANY.copy(nullable = true))
        .beginControlFlow("if (%N !in %N)", "key", "properties")
        .addStatement("error(%P)", "Property: \$key not found in Class: ${this.name.asString()}")
        .endControlFlow()
        .addStatement("val type = properties[key]!!")
        .beginControlFlow("if (value != null && !type.isInstance(value))")
        .addStatement(
            "error(%P)",
            "Type mismatch, property: \$key , expected: \${type.simpleName}, actual: \${value::class.simpleName}"
        )
        .endControlFlow()
        .addStatement("values[%L] = %L", "key", "value")
        .build()
}


private fun ClassDescriptor.getterFunction(): FunSpec {
    val properties = this.publicProperties
    return FunSpec.builder("get")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("key", STRING)
        .returns(ANY.copy(nullable = true))
        .apply {
            beginControlFlow("if (values.contains(%L))", "key")
            addStatement("return values[%L]", "key")
            endControlFlow()
            beginControlFlow("if (%N in %N)", "key", "privateProperties")
            addStatement("error(%P)", "Can't get the original value of property: \$key, because it is private.")
            endControlFlow()
            beginControlFlow("return when(%L)", "key")
            for (property in properties) {
                val name = property.name.asString()
                addStatement("\"%L\" -> source.%L", name, name)
            }
            addStatement(
                "else -> error(%P)",
                "Property: \$key not found in Class: ${this@getterFunction.name.asString()}"
            )
            endControlFlow()
        }
        .build()
}

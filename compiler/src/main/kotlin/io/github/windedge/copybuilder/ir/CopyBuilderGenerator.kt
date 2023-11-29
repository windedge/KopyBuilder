package io.github.windedge.copybuilder.ir

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.windedge.copybuilder.*
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.reflect.KClass


val ClassDescriptor.publicProperties: List<PropertyDescriptor>
    get() = this.properties.filter { it.visibility == DescriptorVisibilities.PUBLIC }.toList()

val ClassDescriptor.privateProperties: List<PropertyDescriptor>
    get() = this.properties.filter { it.visibility != DescriptorVisibilities.PUBLIC }.toList()


fun ClassDescriptor.generateImplClass(): FileSpec {
    val packageName = this.findPackage().fqName.asString()
    val simpleName = this.name.asString()
    val dataClassName = this.toClassName()
    val builderClassName = ClassName(packageName, getImplClassName(simpleName))
    val fileName = getImplFileName(simpleName)
    val copyBuilderClassName = CopyBuilder::class.asClassName()

    return FileSpec.builder(packageName, fileName)
        .addImport(copyBuilderClassName, "")
        .addImport(packageName, simpleName)
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
                .addFunction(getterFunction())
                .addFunction(setterFunction())
                .addFunction(builderFunction())
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


private fun ClassDescriptor.builderFunction(): FunSpec {
    val constructor = checkNotNull(this.constructors.first()) {
        "Primary constructor for ${this.name} is missing"
    }
    val className = this.name.asString()

    val properties = this.properties
    val parameters = constructor.valueParameters
    val propertyNames = properties.map { it.name.asString() }
    val parameterNames = parameters.map { it.name.asString() }

    require(propertyNames.containsAll(parameterNames)) {
        "There are unknown parameter(s) in constructor parameters, class: $className."
    }

    val blocks = parameters.map { param ->
        buildCodeBlock {
            val paramClassName = param.type.toClassName()
                ?: error("can't figure out parameter's type, parameter: ${param.name}")

            var typeName: TypeName = paramClassName
            if (param.type.arguments.isNotEmpty()) {
                val argumentTypes = param.type.arguments.mapNotNull { it.type.toClassName() }
                typeName = paramClassName.parameterizedBy(argumentTypes)
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


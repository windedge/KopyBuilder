package io.github.windedge.copybuilder.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.windedge.copybuilder.CopyBuilder
import kotlin.reflect.KClass

fun KSClassDeclaration.generateImplClass(codeGenerator: CodeGenerator) {
    val packageName = this.packageName.asString()
    val simpleName = this.simpleName.asString()

    val className = ClassName(packageName, "${simpleName}CopyBuilderImpl")
    val copyBuilderClassName = CopyBuilder::class.asClassName()
    FileSpec.builder(packageName, simpleName)
        .addImport(copyBuilderClassName, "")
        .addImport(this.toClassName(), "")
        .addType(
            TypeSpec.classBuilder(className)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("source", this.toClassName()).build())
                .addProperty(
                    PropertySpec.builder("source", this.toClassName(), KModifier.PRIVATE).initializer("source").build()
                )
                .addProperty(propertiesMap())
                .addProperty(propertyNames())
                .addSuperinterface(copyBuilderClassName.parameterizedBy(this.toClassName()))
                .addFunction(getterFunction())
                .addFunction(setterFunction())
                .addFunction(builderFunction())
                .build()
        ).build()
        .writeTo(codeGenerator, false)
}

private fun propertiesMap() =
    PropertySpec.builder("values", MUTABLE_MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
        .addModifiers(KModifier.PRIVATE)
        .initializer("mutableMapOf()")
        .build()

fun KSClassDeclaration.propertyNames(): PropertySpec {
    val properties = getAllProperties().filter { it.isPublic() }.toList()
    val names = properties.map {
        CodeBlock.of("%S to %T::class", it.simpleName.asString(), it.type.resolve().toClassName())
    }.joinToCode(", ")
    return PropertySpec.builder(
        "properties", MAP.parameterizedBy(STRING, KClass::class.asClassName().parameterizedBy(STAR))
    ).addModifiers(KModifier.PRIVATE).initializer("mapOf(%L)", names).build()
}

private fun KSClassDeclaration.builderFunction(): FunSpec {
    val constructor = checkNotNull(this.primaryConstructor) {
        "Primary constructor for ${this.simpleName} is missing"
    }
    val className = this.simpleName.asString()

    val properties = getAllProperties().filter { it.isPublic() }.toList()
    val parameters = constructor.parameters
    val propertyNames = properties.map { it.simpleName.asString() }
    val parameterNames = parameters.map { it.name?.asString() ?: error("Unkown parameter in $className") }

//    println("propertyNames = ${propertyNames}")
//    println("parameterNames = ${parameterNames}")

//    require(!propertyNames.containsAll(parameterNames)) {
//        "There are unknown parameter(s) in constructor parameters, class: $className."
//    }

    val blocks = parameters.map { param ->
        buildCodeBlock {
            addStatement(
                "%L = this.get(%S) as %T",
                param.name!!.asString(),
                param.name!!.asString(),
                param.type.toTypeName()
            )
        }
    }.joinToCode(", ")

    return FunSpec.builder("populate")
        .addModifiers(KModifier.OVERRIDE)
        .returns(this.toClassName())
        .addStatement("return %T(%L)", this.toClassName(), blocks)
        .build()
}

private fun KSClassDeclaration.setterFunction(): FunSpec {
    return FunSpec.builder("put")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("key", STRING)
        .addParameter("value", ANY.copy(nullable = true))
        .beginControlFlow("if (%N !in %N)", "key", "properties")
        .addStatement("error(%P)", "Property: \$key not found in Class: ${this.simpleName.asString()}")
        .endControlFlow()
        .addStatement("val type = properties[key]!!")
        .beginControlFlow("if (value != null && !type.isInstance(value))")
        .addStatement("error(%P)", "Type mismatch, property: \$key , expected: \${type.simpleName}, actual: \${value::class.simpleName}")
        .endControlFlow()
        .addStatement("values[%L] = %L", "key", "value")
        .build()
}

private fun KSClassDeclaration.getterFunction(): FunSpec {
    val properties = getAllProperties().filter { it.isPublic() }
    return FunSpec.builder("get")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("key", STRING)
        .returns(ANY.copy(nullable = true))
        .apply {
            beginControlFlow("if (values.contains(%L))", "key")
            addStatement("return values[%L]", "key")
            endControlFlow()
            beginControlFlow("return when(%L)", "key")
            for (property in properties) {
                val name = property.simpleName.asString()
                addStatement("\"%L\" -> source.%L", name, name)
            }
            addStatement(
                "else -> error(%P)",
                "Property: \$key not found in Class: ${this@getterFunction.simpleName.asString()}"
            )
            endControlFlow()
        }
        .build()
}

///**
// * Generate the Impl class for every interface used for CopyBuilder
// */
//fun generateImplClass(classDataList: List<ClassData>, codeGenerator: CodeGenerator, resolver: Resolver) {
//    classDataList.forEach { classData ->
//        val fileSource = classData.getImplClassFileSource(resolver)
//
//        val packageName = classData.packageName
//        val className = classData.name
//        val fileName = "_${className}Impl"
//
//        codeGenerator.createNewFile(dependencies = Dependencies(false, classData.ksFile), packageName, fileName, "kt")
//            .use { output ->
//                OutputStreamWriter(output).use { writer ->
//                    writer.write(fileSource)
//                }
//            }
//    }
//}

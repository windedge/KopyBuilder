package io.github.windedge.copybuilder

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentsWithSelf
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val CopyBuilderFqn = FqName("io.github.windedge.copybuilder")

// Names for KopyBuilder
internal val KOPY_BUILDER_PACKAGE = FqName("io.github.windedge.copybuilder")
internal val KOPY_BUILDER_NAME = Name.identifier("KopyBuilder")
internal val COPY_BUILDER_NAME = Name.identifier("CopyBuilder")
internal val COPY_BUILDER_HOST_NAME = Name.identifier("CopyBuilderHost")

internal val KopyBuilderClassFqn = KOPY_BUILDER_PACKAGE.child(KOPY_BUILDER_NAME)
internal val KopyBuilderClassId = ClassId(KOPY_BUILDER_PACKAGE, KOPY_BUILDER_NAME)
internal val CopyBuilderClassId = ClassId(KOPY_BUILDER_PACKAGE, COPY_BUILDER_NAME)
internal val CopyBuilderHostClassId = ClassId(KOPY_BUILDER_PACKAGE, COPY_BUILDER_HOST_NAME)

internal val CONTAINS_NAME = Name.identifier("contains")
internal val GET_NAME = Name.identifier("get")
internal val PUT_NAME = Name.identifier("put")
internal val BUILD_NAME = Name.identifier("build")
internal val TO_COPY_BUILDER_NAME = Name.identifier("toCopyBuilder")
internal val COPY_BUILD_NAME = Name.identifier("copyBuild")
internal val SOURCE_NAME = Name.identifier("source")
internal val VALUES_NAME = Name.identifier("values")
internal val PROPERTIES_NAME = Name.identifier("properties")
internal val PRIVATE_PROPERTIES_NAME = Name.identifier("privateProperties")
internal val SOURCE_PARAMETER_NAME = Name.identifier("source")

// CallableIds for Kotlin standard library functions
internal val MUTABLE_MAP_OF = CallableId(FqName("kotlin.collections"), Name.identifier("mutableMapOf"))
internal val TO_FUNCTION = CallableId(FqName("kotlin"), Name.identifier("to"))
internal val MAP_OF = CallableId(FqName("kotlin.collections"), Name.identifier("mapOf"))
internal val SET_OF = CallableId(FqName("kotlin.collections"), Name.identifier("setOf"))

// ClassIds for Kotlin standard library classes
internal val PAIR_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Pair"))

object Key : GeneratedDeclarationKey() {
    override fun toString(): String {
        return "CopyBuilderGeneratorKey"
    }
}

internal fun IrClass.toImplClassId(): ClassId =
    ClassId(this.packageFqName!!, Name.identifier(this.name.asString() + "CopyBuilderImpl"))

internal fun IrClass.toImplClassSimpleName(): String {
    return this.parentsWithSelf.filterIsInstance<IrClass>().map { it.name }.toList().reversed().joinToString("")
}

fun IrClass.toImplClassName(): String {
    return "${this.toImplClassSimpleName()}CopyBuilderImpl"
}


internal fun ClassName.toImplClassSimpleName(): String {
    return this.simpleNames.joinToString("")
}

fun ClassName.toImplClassName(): String {
    return "${this.toImplClassSimpleName()}CopyBuilderImpl"
}


fun ClassDescriptor.toImplClassName(): String {
    return this.toClassName().toImplClassName()
}

fun ClassName.toImplFileName(): String {
    return "${this.toImplClassSimpleName()}CopyBuilder"
}

fun IrClass.toImplFileName(): String {
    return "${this.toImplClassSimpleName()}CopyBuilder"
}

fun ClassDescriptor.toImplFileName(): String {
    return this.toClassName().toImplFileName()
}

internal fun generateImplClassName(baseName: String): Name {
    return Name.identifier(baseName + "CopyBuilderImpl")
}

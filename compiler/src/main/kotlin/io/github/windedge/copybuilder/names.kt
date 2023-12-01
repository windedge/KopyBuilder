package io.github.windedge.copybuilder

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.backend.common.lower.parentsWithSelf
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.FqName

internal const val copy = "copy"
internal val CopyBuilderFqn = FqName("io.github.windedge.copybuilder")

internal fun ClassName.toImplClassSimpleName(): String {
    return this.simpleNames.joinToString("")
}

internal fun IrClass.toImplClassSimpleName(): String {
    return this.parentsWithSelf.filterIsInstance<IrClass>().map { it.name }.toList().reversed().joinToString("")
}

fun ClassName.toImplClassName(): String {
    return "${this.toImplClassSimpleName()}CopyBuilderImpl"
}

fun IrClass.toImplClassName(): String {
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


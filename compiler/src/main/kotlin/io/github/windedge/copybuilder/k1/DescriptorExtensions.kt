package io.github.windedge.copybuilder.k1

import com.squareup.kotlinpoet.ClassName
import io.github.windedge.copybuilder.toImplFileName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path


val ClassDescriptor.properties
    get() = unsubstitutedMemberScope
        .getDescriptorsFiltered(kindFilter = DescriptorKindFilter.VARIABLES)
        .filterIsInstance<PropertyDescriptor>()
        .filter {
            // Remove inherited properties that aren't overridden in this class.
            it.kind == CallableMemberDescriptor.Kind.DECLARATION
        }

val ClassDescriptor.publicProperties: List<PropertyDescriptor>
    get() = this.properties.filter { it.visibility == DescriptorVisibilities.PUBLIC }.toList()

val ClassDescriptor.privateProperties: List<PropertyDescriptor>
    get() = this.properties.filter { it.visibility != DescriptorVisibilities.PUBLIC }.toList()


val ClassDescriptor.functions
    get() = unsubstitutedMemberScope
        .getDescriptorsFiltered(kindFilter = DescriptorKindFilter.FUNCTIONS)
        .filterIsInstance<FunctionDescriptor>()
        .filter {
            // Remove inherited properties that aren't overridden in this class.
            it.kind == CallableMemberDescriptor.Kind.DECLARATION
        }


fun ClassDescriptor.toClassName(): ClassName {
    val names = this.parentsWithSelf.filterIsInstance<ClassDescriptor>()
        .map { it.name.asString() }.toList().reversed()
    return ClassName(this.findPackage().fqName.asString(), names)
}

fun ClassId.toClassName(): ClassName {
    return ClassName(this.packageFqName.asString(), this.relativeClassName.asString())
}

fun KotlinType.toClassName(): ClassName? {
    return this.constructor.declarationDescriptor.classId?.toClassName()
}

fun ClassDescriptor.toGeneratedCopyBuilderPath(
    baseDir: Path? = null,
    extension: String = "kt",
    buildClassName: (ClassDescriptor.() -> String)? = null
): Path {
    var outputDirectory = baseDir ?: Path(".")

    val packageName = this.findPackage().fqName.asString()
    if (packageName.isNotEmpty()) {
        for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
            outputDirectory = outputDirectory.resolve(packageComponent)
        }
    }

    Files.createDirectories(outputDirectory)

    val name = if (buildClassName == null) {
        this.toImplFileName()
    } else {
        this.buildClassName()
    }
    return outputDirectory.resolve("$name.$extension")
}


package io.github.windedge.copybuilder

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
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

fun ClassDescriptor.toClassName(): ClassName {
    return ClassName(this.findPackage().fqName.asString(), this.name.asString())
}

fun ClassId.toClassName(): ClassName {
    return ClassName(this.packageFqName.asString(), this.shortClassName.asString())
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
        "${this.name.asString()}CopyBuilderImpl"
    } else {
        this.buildClassName()
    }
    return outputDirectory.resolve("$name.$extension")
}


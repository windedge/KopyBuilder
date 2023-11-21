package io.github.windedge.copybuilder

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class CopyBuilderExtension @Inject constructor(project: Project) {
    /**
     * Whether CopyBuilder is enabled
     */
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.javaObjectType).convention(true)

    /**
     * Whether to show verbose logging
     */
    val verbose: Property<Boolean> = project.objects.property(Boolean::class.javaObjectType).convention(false)

    /**
     * Whether to output generated source files
     */
    val outputDir: DirectoryProperty = project.objects.directoryProperty().convention(project.layout.buildDirectory.dir("generated/kopybuilder"))
}
package io.github.windedge.copybuilder

import org.jetbrains.kotlin.name.FqName

internal const val copy = "copy"
internal val CopyBuilderFqn = FqName("io.github.windedge.copybuilder")

fun getImplClassName(className: String) = "${className}CopyBuilderImpl"

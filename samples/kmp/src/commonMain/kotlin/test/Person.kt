package test

import io.github.windedge.copybuilder.CopyBuilder
import io.github.windedge.copybuilder.CopyBuilderFactory
import io.github.windedge.copybuilder.KopyBuilder

@KopyBuilder
data class Person(
    val name: String,
    val email: String?,
    val age: Int,
)
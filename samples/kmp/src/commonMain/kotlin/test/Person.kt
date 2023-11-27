package test

import io.github.windedge.copybuilder.KopyBuilder

@KopyBuilder
data class Person(
    val name: String,
    val email: String?,
    val age: Int,
)
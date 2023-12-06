package test

import io.github.windedge.copybuilder.KopyBuilder


@KopyBuilder
data class TodoList(
    val todos: List<String>? = null,
)
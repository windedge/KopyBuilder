package io.github.windedge.copybuilder

internal object Errors {
    fun copyBuilderAppliedWrongTarget(target: String): String {
        return "CopyBuilder is only supported for data classes. ($target)"
    }
    const val CopyFunctionNotFound = "The copy function was not found."
}
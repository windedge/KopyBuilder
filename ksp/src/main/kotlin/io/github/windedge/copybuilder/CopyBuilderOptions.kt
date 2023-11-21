package io.github.windedge.copybuilder

class CopyBuilderOptions(options: Map<String, String>) {
    /**
     * 0: Turn off all CopyBuilder related error checking
     *
     * 1: Check for errors
     *
     * 2: Turn errors into warnings
     */
    val errorsLoggingType: Int = (options["CopyBuilder_Errors"]?.toIntOrNull()) ?: 1
}
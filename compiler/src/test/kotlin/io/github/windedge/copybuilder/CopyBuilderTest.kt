package io.github.windedge.copybuilder

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

const val pluginId = "io.github.windedge.copybuilder.compiler"

@OptIn(ExperimentalCompilerApi::class)
class CopyBuilderTest {
    @TempDir
    lateinit var temporaryFolder: File

    /**
     * Basic compilation test
     * Verifies that a simple function can be compiled without any errors
     */
    @Test
    fun simpleFunctionShouldCompileSuccessfully() {
        val result = compile(
            sourceFile = kotlin(
                "main.kt", """
                fun main() {
                    println(debug())
                }

                fun debug() = "Hello, World!"
                """
            )
        )
        assertEquals(ExitCode.OK, result.exitCode)
    }

    /**
     * Basic Builder Generation test
     * Tests the generation of a Builder for a simple data class
     */
    @Test
    fun shouldGenerateBuilderForSimpleDataClass() {
        val result = compile(
            sourceFile = kotlin(
                "main.kt", """
                import io.github.windedge.copybuilder.KopyBuilder
                import io.github.windedge.copybuilder.CopyBuilderHost

                @KopyBuilder
                data class Fruit(val name: String, val weight: Int)

                fun main() {
                    val fruit = Fruit("apple", 12)
                    val fruit2 = (fruit as CopyBuilderHost<Fruit>).copyBuild {
                        put("name", "Pear")
                    }
                    assert(fruit2 == Fruit("Pear", 0))
                }
                """
            )
        )
        assertEquals(ExitCode.OK, result.exitCode)
    }

    @Test
    fun shouldGenerateBuilderForSimpleDataClass2() {
        val result = compile(
            sourceFile = kotlin(
                "main.kt", """
package test

import io.github.windedge.copybuilder.KopyBuilder
import io.github.windedge.copybuilder.CopyBuilder
import io.github.windedge.copybuilder.CopyBuilderHost

@KopyBuilder
data class Fruit(val name: String, val weight: Int)

fun main() {
    val fruit = Fruit("apple", 12)

    val copyBuilder = Fruit.CopyBuilderImpl(fruit)
    copyBuilder.put("name", "Pear")
    assert(copyBuilder.get("name") == "Pear")

    val newFruit = fruit.copyBuild {
        put("name", "Pear")
    }
    assert(newFruit.name == "Pear")
}
                """
            )
        )
        assertEquals(ExitCode.OK, result.exitCode)
    }

    @Test
    fun shouldFailedGenerateBuilderForSimpleDataClass() {
        val result = compile(
            sourceFile = kotlin("main.kt", """
                import io.github.windedge.copybuilder.KopyBuilder
                import io.github.windedge.copybuilder.CopyBuilderHost

                @KopyBuilder
                data class Fruit(val name: String, val weight: Int)

                fun main() {
                    val fruit = Fruit("apple", 12)

                    val copyBuilder = FruitCopyBuilderImpl(fruit).apply {
                        // throw exception
                        put("name", 1)
                    }
                    assert(copyBuilder.get("name") == "Pear")
                }
                """
            )
        )
        result.run {
            println("this.messages = ${this.messages}")
        }
        assertEquals(ExitCode.OK, result.exitCode)
    }

    /**
     * Complex Data Class Builder test
     * Tests Builder generation for a data class with multiple properties
     */
    @Test
    fun shouldHandleComplexDataClassWithMultipleProperties() {
        val result = compile(
            sourceFile = kotlin(
                "main.kt", """
                import io.github.windedge.copybuilder.KopyBuilder
                import io.github.windedge.copybuilder.CopyBuilderHost

                @KopyBuilder
                data class Person(
                    val name: String,
                    val age: Int,
                    val email: String?
                )

                fun main() {
                    val person = Person("John", 25, "john@example.com")
                    val person2 = (person as CopyBuilderHost<Person>).copyBuild {
                        put("name", "Jane")
                        put("age", 30)
                        put("email", null)
                    }
                    assert(person2 == Person("Jane", 30, null))
                }
                """
            )
        )
        assertEquals(ExitCode.OK, result.exitCode)
    }

    /**
     * Error Handling test
     * Tests compiler behavior when @KopyBuilder is misused
     */
    @Test
    fun shouldFailWhenAnnotatingNonDataClass() {
        val result = compile(
            sourceFile = kotlin(
                "main.kt", """
                import io.github.windedge.copybuilder.KopyBuilder

                @KopyBuilder
                class NonDataClass(val name: String)

                fun main() {
                    val obj = NonDataClass("test")
                }
                """
            )
        )
        assertEquals(ExitCode.INTERNAL_ERROR, result.exitCode)
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFiles: List<SourceFile>,
        plugin: CompilerPluginRegistrar = CopyBuilderCompilerPluginRegistrar(),
        commandLineProcessor: CommandLineProcessor = CopyBuilderCommandLineProcessor(),
        enabled: Boolean = true,
    ): CompilationResult {
        val compilation = KotlinCompilation()
        return compilation.apply {
            sources = sourceFiles
            compilerPluginRegistrars = listOf(plugin)
            pluginOptions = listOf(
                PluginOption(pluginId, optionName = "enabled", optionValue = enabled.toString()),
                PluginOption(pluginId, optionName = "verbose", optionValue = "true"),
                PluginOption(pluginId, optionName = "outputDir", optionValue = temporaryFolder.absolutePath)
            )
            commandLineProcessors = listOf(commandLineProcessor)
            inheritClassPath = true
            messageOutputStream = System.out
            verbose = true
        }.compile()
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFile: SourceFile,
        plugin: CompilerPluginRegistrar = CopyBuilderCompilerPluginRegistrar(),
        commandLineProcessor: CommandLineProcessor = CopyBuilderCommandLineProcessor()
    ): CompilationResult {
        return compile(listOf(sourceFile), plugin, commandLineProcessor)
    }
}


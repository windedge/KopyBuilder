package io.github.windedge.copybuilder

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

const val pluginId = "io.github.windedge.copybuilder.compiler"

@OptIn(ExperimentalCompilerApi::class)
class CopyBuilderTest : BaseKopyBuilderTest() {

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
            sourceFile = kotlin(
                "main.kt", """
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

    @Test
    fun `should handle error case when accessing private property`() {
        val source = kotlin(
            "Test.kt",
            """
            package test
            
            import io.github.windedge.copybuilder.KopyBuilder
            
            @KopyBuilder
            data class Signup(
                val name: String = "",
                val email: String? = null,
                val age: Int? = null,
                val password: String = "",
                val confirmPassword: String = "",
                val accept: Boolean = false,
            )
            
            fun main() {
                val signup = Signup(name = "Alice", age = 25)
                val builder = signup.toCopyBuilder()
                
                // Test getting public property
                val nameValue = builder.get("name") as? String
                    ?: throw AssertionError("Expected non-null name")
                if (nameValue != "Alice") {
                    throw AssertionError("Expected name: 'Alice', but was: " + nameValue)
                }
                
                // Test getting nullable property
                val ageValue = builder.get("age") as? Int
                if (ageValue != 25) {
                    throw AssertionError("Expected age: 25, but was: " + ageValue)
                }
                
                // Test getting non-existent property
                var nonExistentError: IllegalStateException? = null
                try {
                    builder.get("nonExistent")
                } catch (e: IllegalStateException) {
                    nonExistentError = e
                }
                if (nonExistentError == null) {
                    throw AssertionError("Expected error not thrown for non-existent property")
                }
                // Verify the error message
                if (nonExistentError?.message?.startsWith("Property: nonExistent not found in Class: Signup") != true) {
                    throw AssertionError("Unexpected error message: " + nonExistentError?.message)
                }
            }
            """.trimIndent()
        )

        val result = compile(source)
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println("Compilation failed with exit code: ${result.exitCode}")
            println("Messages:")
            println(result.messages)
            if (result.exitCode == KotlinCompilation.ExitCode.COMPILATION_ERROR) {
                println("Compilation errors:")
                result.messages.lines().filter { it.contains("error:", ignoreCase = true) }.forEach { println(it) }
            }
        }
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, "Compilation failed: ${result.messages}")
    }

}


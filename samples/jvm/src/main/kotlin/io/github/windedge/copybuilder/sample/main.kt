@file:Suppress("DataClassPrivateConstructor", "LocalVariableName")

package io.github.windedge.copybuilder.sample

import io.github.windedge.copybuilder.KopyBuilder


@KopyBuilder
data class Person(
    val name: String,
    val email: String?,
    val age: Int,
)


fun main() {
    val person = Person("Karl", "kmarx@gmail.com", 68)

//    val clazz =
//        ClassLoader.getSystemClassLoader().loadClass("io.github.windedge.copybuilder.sample.PersonCopyBuilderImpl")
//
//    val builder = PersonCopyBuilderImpl(Person("Carl", "kmarx@gmail.com", 68))
//
//    builder.put("name", "Kenx")
//    builder.put("age", 70)
//    val person = builder.populate()

    runCatching {
        val clazz = Person::class.java.classLoader.loadClass("test.PersonCopyBuilderX")
        println("clazz = ${clazz}")
    }.onFailure(::println)

    runCatching {
        val clazz = Person::class.java.classLoader.loadClass("Generated")
        println("clazz = ${clazz}")
    }.onFailure(::println)

//    }.onFailure {
//        it.printStackTrace()
//    }

    runCatching {
        val clazz = Person::class.java.classLoader.loadClass("test.PersonCopyBuilderImpl")
        println("clazz = ${clazz}")
    }.onFailure(::println)


    println("person2 = ${person}")

}


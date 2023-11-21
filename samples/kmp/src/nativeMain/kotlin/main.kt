@file:Suppress("CAST_NEVER_SUCCEEDS")

package test

import io.github.windedge.copybuilder.CopyBuilderFactory

fun main() {
    val person = Person("Carl", "kmarx@gmail.com", 68)

//    val builder = PersonCopyBuilderImpl(person)
//    builder.put("name", "karl")
//    builder.put("age", 70)
//    val person = builder.populate()

    if (CopyBuilderFactory::class.isInstance(person)) {
        val builderFactory = person as CopyBuilderFactory<*>
        val builder = builderFactory.toCopyBuilder()
        println("builder = ${builder}")
        builder.put("name", "Max")
        val person2 = builder.build()
        println("person2 = ${person2}")
    }


    println("hello $person!")
}
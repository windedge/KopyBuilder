@file:Suppress("CAST_NEVER_SUCCEEDS")

package test

import io.github.windedge.copybuilder.CopyBuilderHost

fun main() {
    val person = Person("Carl", "kmarx@gmail.com", 68)
    println("hello $person!")

    if (CopyBuilderHost::class.isInstance(person)) {
        val builderFactory = person as CopyBuilderHost<Person>
        val person2 = builderFactory.copyBuild {
            put("name", "Max")
        }
        println("person2 = ${person2}")
    }
}
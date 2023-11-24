package test

import io.github.windedge.copybuilder.CopyBuilderFactory

fun main() {
    val person = Person("Karl", "kmarx@gmail.com", 68)
    println("hello $person!")

    if (CopyBuilderFactory::class.isInstance(person)) {
        val builderFactory = person as CopyBuilderFactory<Person>
        val person2 = builderFactory.copyBuild {
            put("name", "Max")
        }
        println("person2 = ${person2}")
    }
}

@file:Suppress("CAST_NEVER_SUCCEEDS")

package test

import io.github.windedge.copybuilder.CopyBuilderFactory

fun main() {
    val person = Person("Karl", "kmarx@gmail.com", 68)

//    val builder = PersonCopyBuilderImpl(person)
//    builder.put("name", "karl")
//    builder.put("age", 70)
//    val person2 = builder.populate()

    tryLoadingClass("test.PersonCopyBuilderX")
    tryLoadingClass("test.PersonCopyBuilderImpl")
    tryLoadingClass("Generated")

//    val builder = PersonCopyBuilderImpl()

    println("hello $person!")

    println("person.javaClass.interfaces = ${person.javaClass.interfaces.joinToString { it.name }}")

    if (CopyBuilderFactory::class.isInstance(person)) {
        val builderFactory = person as CopyBuilderFactory<*>
        val person2 = builderFactory.copyBuild {
            put("name", "Max")
        }
        println("person2 = ${person2}")
    }

}

private fun tryLoadingClass(className: String) {
    runCatching {
        val clazz = Person::class.java.classLoader.loadClass(className)
        println("clazz = ${clazz}")
    }.onFailure(::println)
}
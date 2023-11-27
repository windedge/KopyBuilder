@file:Suppress("CAST_NEVER_SUCCEEDS")

package test

import io.github.windedge.copybuilder.CopyBuilderHost

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

    if (CopyBuilderHost::class.isInstance(person)) {
        val builderHost = person as CopyBuilderHost<*>
        val person2 = builderHost.copyBuild {
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
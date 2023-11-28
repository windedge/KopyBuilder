
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

    println("hello $person!")

    println("person.javaClass.interfaces = ${person.javaClass.interfaces.joinToString { it.name }}")

    val person2 = person.copyBuild {
        put("name", "Marco")
        put("email", "hello@world")
    }
    println("person2 = ${person2}")


    if (CopyBuilderHost::class.isInstance(person)) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val host = person as CopyBuilderHost<Person>
        val person3 = host.copyBuild {
            put("name", "Max")
        }
        println("person3 = ${person3}")
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    val person4 = (person as CopyBuilderHost<Person>).copyBuild {
        put("name", "Carlos")
    }
    println("person4 = ${person4}")


}

private fun tryLoadingClass(className: String) {
    runCatching {
        val clazz = Person::class.java.classLoader.loadClass(className)
        println("clazz = ${clazz}")
    }.onFailure(::println)
}
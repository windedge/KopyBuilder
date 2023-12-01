package test

import io.github.windedge.copybuilder.KopyBuilder

class Outer {
    @KopyBuilder
    data class Animal(
        val kind: String? = null,
    )
}

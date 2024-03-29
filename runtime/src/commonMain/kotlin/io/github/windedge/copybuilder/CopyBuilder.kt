package io.github.windedge.copybuilder

public interface CopyBuilder<T> {

    public fun contains(key: String): Boolean

    public fun get(key: String): Any?

    public fun put(key: String, value: Any?)

    public fun build(): T
}

public interface CopyBuilderHost<T> {
    public fun toCopyBuilder(): CopyBuilder<T> = TODO("Implemented in KopyBuilder Plugin")

    public fun copyBuild(initialize: CopyBuilder<T>.() -> Unit): T {
        val builder = toCopyBuilder()
        builder.initialize()
        return builder.build()
    }
}

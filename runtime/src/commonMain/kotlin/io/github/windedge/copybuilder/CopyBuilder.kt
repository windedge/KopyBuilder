package io.github.windedge.copybuilder

public interface CopyBuilder<T> {
//    public fun getProperties(): List<KProperty1<T, *>>
//    public fun <V> getProperty(name: String): KProperty1<T, V>?

    public fun get(key: String): Any?
    public fun put(key: String, value: Any?)

    public fun build(): T
}

public interface CopyBuilderFactory<T> {
    public fun toCopyBuilder(): CopyBuilder<T> {
        TODO("Implemented in KopyBuilder Plugin")
    }

    public fun copyBuild(build: CopyBuilder<T>.() -> Unit): T {
        val builder = toCopyBuilder()
        builder.build()
        return builder.build()
    }
}

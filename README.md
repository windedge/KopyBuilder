# KopyBuilder

This is a compiler plugin that generates a builder class for Kotlin data classes. You can `get` and `put` properties in the builder, similar to a Map, and finally `build` a new instance.


## Usage

### Gradle Setup

Currently, it supports Kotlin versions 1.8, 1.9 and 2.x.

```
plugins {
    id("io.github.windedge.kopybuilder") version "$version"
}
```

[//]: # (#### Download ![maven-central]&#40;https://img.shields.io/nexus/snapshots/https/s01.oss.sonatype.org/io.github.windedge.copybuilder/kopybuilder&#41;)

### Version Compatibility

Below is a table showing the compatibility between KopyBuilder versions and Kotlin versions:

| KopyBuilder Version | Kotlin Version |
|---------------------|----------------|
| 0.1.6               | 1.8.x, 1.9.x   |
| 0.2.0               | 2.0.x, 2.1.0   |


### Code Generation

For example, consider the following data class:

```kotlin
@KopyBuilder
data class User(
    val name: String,
    val email: String?
)

```

It will generate code like this:

```kotlin
public class UserBuilder: io.github.windedge.copybuilder.CopyBuilder<User> {
    override fun `get`(key: String): Any? {
        //...
    }

    override fun put(key: String, `value`: Any?) {
        //...
    }

    override fun build(): User = User(name = ..., email = ... )
}

fun User.toCopyBuilder(): CopyBuilder<User> = ...
fun User.copyBuild(initialize: CopyBuilder<User>.() -> Unit): User { /*...*/ }
```

You can use it as follows:

```kotlin
val user = User(...)
val builder = user.toCopyBuilder()
builder.apply {
    put("name", ...)
    put("email", ...)
}
val newUser = builder.build()

// Or build with copyBuild directly
val newUser = user.copyBuild {
    put("name", ...)
}

```

You can even use it in a reflection way, making it possible to cooperate with 3rd party libraries:

```kotlin
import io.github.windedge.copybuilder.CopyBuilderHost

if (CopyBuilderHost::class.isInstance(user)) {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val host = user as CopyBuilderHost<User>
    val newUser = host.copyBuild {
        put("name", "Max")
    }
}

```

## License

This project is licensed under the MIT License. Please refer to the [LICENSE file](LICENSE) for details.

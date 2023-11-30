# KopyBuilder

This is a compiler plugin that generates a builder class for Kotlin's `data class`. You can `get` and `put` properties from the builder, similar to a `Map`, and finally `build` a new instance.

## Usage

### Gradle Setup

Currently, it supports Kotlin versions 1.8 and 1.9.

```
plugins {
    id("io.github.windedge.kopybuilder") version "$version"
}
```

[//]: # (#### Download ![maven-central]&#40;https://img.shields.io/nexus/snapshots/https/s01.oss.sonatype.org/io.github.windedge.copybuilder/kopybuilder&#41;)


### Code Generation

For example, consider the following data class:

```kotlin
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

User.toCopyBuilder(): CopyBuilder<User> = ...
User.copyBuild(initialize: CopyBuilder<User>.() -> Unit): User
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
    val host = user as CopyBuilderHost<ser>
    val newUser = host.copyBuild {
        put("name", "Max")
    }
}

```

## License

This project is licensed under the MIT License. Please refer to the [LICENSE file](LICENSE) for details.

@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.maven.publish.get().pluginId)
    id(libs.plugins.buildconfig.get().pluginId)
    alias(libs.plugins.mavenShadow)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val embeddedDependencies by configurations.creating { isTransitive = false }
dependencies {
    fun embedded(dep: Any) {
        implementation(dep)
        embeddedDependencies(dep)
    }

    embedded(project(":runtime"))
    embedded(libs.poet)

    implementation(libs.kotlin.embeddable.compiler)

    testImplementation(libs.test.kotest.framework)
    testImplementation(libs.test.strikt)
    testImplementation(libs.test.kctfork.core)
}

val shadowJar = tasks.shadowJar.apply {
    configure {
        archiveClassifier.set("shadow")
        configurations = listOf(embeddedDependencies)
        relocate("com.squareup.kotlinpoet", "shadowed.com.squareup.kotlinpoet")
    }
}

tasks.named<Jar>("jar") {
    dependsOn(shadowJar)
    from(zipTree(shadowJar.get().archiveFile))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

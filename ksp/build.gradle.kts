plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kotlin.ksp.get().pluginId)
    id(libs.plugins.maven.publish.get().pluginId)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":runtime"))
    implementation(libs.autoservice.annotation)
    ksp(libs.autoservice.processor)

    implementation(libs.ksp.api)
    implementation(libs.poet)
    implementation(libs.poet.ksp)

    testImplementation(libs.test.kotest.framework)
}

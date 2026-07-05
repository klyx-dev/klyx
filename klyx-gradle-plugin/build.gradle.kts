plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.klyx-dev"
version = property("project.version") as String

gradlePlugin {
    plugins {
        register("klyxBundle") {
            id = "io.github.klyx-dev.plugin"
            implementationClass = "com.klyx.gradle.KlyxPluginPublishingPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.android.tools)
}

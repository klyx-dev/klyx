plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.vanniktech.publish)
}

buildConfig {
    packageName("com.klyx.compiler.plugin")

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"io.github.klyx-dev.compiler.plugin\"")

    val pluginProject = project(":klyx-compiler-plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val apiProject = project(":klyx-api")
    buildConfigField(
        type = "String",
        name = "KLYX_API_LIBRARY_COORDINATES",
        expression = "\"${apiProject.group}:${apiProject.name}:${apiProject.version}\""
    )
}

gradlePlugin {
    plugins {
        create("KlyxPlugin") {
            id = "io.github.klyx-dev.compiler.plugin"
            displayName = "KlyxPlugin"
            description = "KlyxPlugin"
            implementationClass = "com.klyx.compiler.plugin.KlyxCompilerPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.android.tools)
}

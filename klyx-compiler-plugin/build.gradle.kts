plugins {
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    packageName("com.klyx.compiler.plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"com.klyx.compiler.plugin\"")
}

val klyxRuntimeClasspath = configurations.dependencyScope("klyxRuntimeClasspath") {
    isTransitive = false
}

dependencies {
    compileOnly(libs.kotlin.compiler)

    klyxRuntimeClasspath(projects.klyxApi)
}

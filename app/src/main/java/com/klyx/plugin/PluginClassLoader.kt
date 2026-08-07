package com.klyx.plugin

import dalvik.system.PathClassLoader

/**
 * A child-first class loader for plugin APKs.
 *
 * The default [PathClassLoader]] delegates parent-first, so the app's copy of a
 * library wins over the copy bundled inside the plugin. When the app's dex is
 * shrunk (R8) and a class such as an interface is kept while its implementation
 * is removed, a plugin that bundles its own copy ends up loading the
 * implementation from its own dex while the interface resolves from the app dex.
 * Two classes that share a fully-qualified name but were loaded by different
 * class loaders are unrelated, producing [ClassCastException] on a simple cast.
 *
 * This loader inverts the resolution order for everything except the packages
 * that MUST be shared between the app and its plugins: the JDK, the Android
 * framework, androidx/Compose (the app drives the composition), the Kotlin and
 * kotlinx runtimes, and the Klyx API/contract types. Everything else resolves
 * from the plugin's own dex first, falling back to the app when the plugin does
 * not bundle it.
 */
class PluginClassLoader(
    apkPath: String,
    librarySearchPath: String?,
    parent: ClassLoader
) : PathClassLoader(apkPath, librarySearchPath, parent) {

    private val parentFirstPrefixes = arrayOf(
        "java.",
        "javax.",
        "sun.",
        "org.w3c.",
        "org.xml.",
        "android.",
        "androidx.",
        "kotlin.",
        "kotlinx.coroutines.",
        "kotlinx.serialization.",
        "com.klyx.api.",
        "com.klyx.core."
    )

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(this) {
            findLoadedClass(name)?.let { return it }

            // Shared/contract packages always resolve parent-first so the app
            // and plugins observe the same framework, runtime and API types.
            if (parentFirstPrefixes.any { name.startsWith(it) }) {
                return super.loadClass(name, resolve)
            }

            // Child-first: the plugin's own bundled copy wins when present.
            try {
                val clazz = findClass(name)
                if (resolve) resolveClass(clazz)
                return clazz
            } catch (_: ClassNotFoundException) {
                // Not bundled by the plugin; fall back to the app below.
            }

            return super.loadClass(name, resolve)
        }
    }
}

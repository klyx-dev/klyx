package com.klyx.plugin

import android.annotation.SuppressLint
import android.content.ComponentCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import com.klyx.core.App
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException

/**
 * Loads a plugin's own Android resources from its APK.
 *
 * Plugins are compiled as ordinary Android modules, so the APK already contains the
 * compiled resource table (`resources.arsc`) alongside the exact `R` IDs the plugin's
 * code was compiled against. A [dalvik.system.PathClassLoader] cannot resolve these by itself,
 * resource IDs are resolved by an [AssetManager], not a class loader. By creating a
 * dedicated [AssetManager] over the plugin APK we get a [Resources] that is fully
 * isolated from the host, so the plugin's `R.drawable.x` / `R.string.y` values resolve
 * against the plugin's own table (and `android.R.*` keeps working via the system assets).
 */
internal object PluginResources {

    /**
     * Creates an [AssetManager] that can read the compiled resources and assets of the
     * APK at [apkPath].
     */
    @SuppressLint("PrivateApi")
    fun createAssetManager(apkPath: String): AssetManager {
        val apkAssetsClass = Class.forName("android.content.res.ApkAssets")
        val loaderFlag = apkAssetsClass.getField("PROPERTY_LOADER").getInt(null)
        val loadFromPath = apkAssetsClass.getMethod(
            "loadFromPath",
            String::class.java,
            Int::class.javaPrimitiveType
        )

        val apkAssets = try {
            loadFromPath.invoke(null, apkPath, loaderFlag)
        } catch (e: InvocationTargetException) {
            throw PluginLoadException(
                "Failed to load resources from plugin APK: $apkPath",
                e.cause ?: e
            )
        }

        val assetManager = AssetManager::class.java.getConstructor().newInstance()
        val apkAssetsArray = Array.newInstance(apkAssetsClass, 1)
        Array.set(apkAssetsArray, 0, apkAssets)
        AssetManager::class.java
            .getMethod("setApkAssets", apkAssetsArray.javaClass, Boolean::class.javaPrimitiveType)
            .invoke(assetManager, apkAssetsArray, false)
        return assetManager
    }
}

internal class PluginResourceManager(
    app: App,
    apkPath: String
) : ComponentCallbacks {

    private val baseContext = app.application
    private val hostResources = baseContext.resources
    private val assetManager = PluginResources.createAssetManager(apkPath)

    @Volatile
    private var current: Resources = createResources()

    init {
        baseContext.registerComponentCallbacks(this)
    }

    /** The plugin's [Resources], updated automatically on configuration changes. */
    val resources: Resources
        get() = current

    /** A [Context] whose resources/assets are the plugin's. */
    val context: Context by lazy {
        object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = current

            override fun getAssets(): AssetManager = current.assets
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        current = createResources()
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() = Unit

    @Suppress("DEPRECATION")
    private fun createResources(): Resources = Resources(
        assetManager,
        hostResources.displayMetrics,
        hostResources.configuration
    )
}

package com.klyx.plugin

import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginContextHolder
import com.klyx.core.App
import com.klyx.core.Version
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

class PluginManager(private val app: App) {

    private val plugins = mutableMapOf<String, KlyxPlugin>()
    private val appVersion: Version = Version.parse(com.klyx.BuildConfig.VERSION_NAME)

    fun loadPlugin(apkPath: String) {
        val context = app.application
        val optimizedDir = File(context.cacheDir, "klyx-dex-opt")
        optimizedDir.mkdirs()

        val loader = DexClassLoader(
            apkPath,
            optimizedDir.absolutePath,
            null,
            this::class.java.classLoader
        )

        val desc = readPluginDescriptor(apkPath)
        val cls = loader.loadClass(desc.entryClass)
        val plugin = cls.getConstructor().newInstance() as KlyxPlugin

        require(plugins[desc.id] == null) {
            "Plugin '${desc.id}' is already loaded. Unload it first."
        }

        val pluginMinVersion = Version.parse(plugin.minHostVersion)
        if (pluginMinVersion > appVersion) {
            throw PluginLoadException(
                "Plugin '${desc.id}' requires app version >= ${plugin.minHostVersion}, but app version is ${appVersion}"
            )
        }

        require(plugin.id == desc.id) {
            "Plugin id mismatch: plugin.json declares '${desc.id}' but KlyxPlugin.id is '${plugin.id}'"
        }
        require(plugin.version == desc.version) {
            "Plugin version mismatch: plugin.json declares '${desc.version}' but KlyxPlugin.version is '${plugin.version}'"
        }
        require(plugin.minHostVersion == desc.minHostVersion) {
            "Plugin minHostVersion mismatch: plugin.json declares '${desc.minHostVersion}' but KlyxPlugin.minHostVersion is '${plugin.minHostVersion}'"
        }

        val ctx = AppPluginContext(app)
        PluginContextHolder.set(ctx)
        plugin.context = ctx
        plugin.onLoad(ctx)
        (plugin.lifecycle as? androidx.lifecycle.LifecycleRegistry)?.currentState = androidx.lifecycle.Lifecycle.State.STARTED
        PluginContextHolder.clear()
        plugins[desc.id] = plugin
    }

    fun unloadPlugin(id: String) {
        val plugin = plugins.remove(id) ?: return
        try {
            (plugin.lifecycle as? androidx.lifecycle.LifecycleRegistry)?.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
            plugin.onUnload()
        } catch (e: Exception) {
            android.util.Log.e("PluginManager", "Error unloading plugin '$id'", e)
        }
        PluginContextHolder.clear()
    }

    fun getPlugin(id: String): KlyxPlugin? = plugins[id]

    private fun readPluginDescriptor(apkPath: String): PluginDescriptor {
        val zipFile = ZipFile(apkPath)
        val entry = zipFile.getEntry("assets/plugin.json")
            ?: error("Missing assets/plugin.json")
        val json = JSONObject(zipFile.getInputStream(entry).bufferedReader().readText())
        zipFile.close()
        return PluginDescriptor(
            id = json.getString("id"),
            version = json.getString("version"),
            minHostVersion = json.getString("minHostVersion"),
            entryClass = json.getString("entryClass")
        )
    }

    private data class PluginDescriptor(
        val id: String,
        val version: String,
        val minHostVersion: String,
        val entryClass: String
    )
}

class PluginLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

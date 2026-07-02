package com.klyx.plugin

import android.graphics.BitmapFactory
import android.net.Uri
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.klyx.BuildConfig
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.plugin.PluginRuntimeRegistry
import com.klyx.api.plugin.PluginRuntimeService
import com.klyx.core.App
import com.klyx.core.Global
import com.klyx.core.koin
import com.klyx.data.file.archive.extractGzipTar
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.installedPluginsJson
import com.klyx.api.data.fs.pluginsDir
import dalvik.system.PathClassLoader
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.IdentityHashMap
import kotlin.reflect.KClass

class PluginManager(private val app: App) : PluginRuntimeRegistry, Global {

    private val fs: FileSystem by koin()
    private val appVersion = BuildConfig.VERSION_NAME.removeSuffix("-debug").toVersion(strict = false)

    private val installedList = mutableListOf<String>()
    private val runtimes = IdentityHashMap<KlyxPlugin, PluginRuntime>()
    private val runtimesById = HashMap<String, PluginRuntime>()

    val loadedPlugins get() = runtimes.values.map(PluginRuntime::info)

    init {
        app.backgroundScope.launch {
            loadInstalledList()
            try {
                loadInstalledPlugins()
            } catch (t: Throwable) {
                Log.e("PluginManager", "Failed to load installed plugins", t)
            }
        }
    }

    fun interface PluginLoadProgressListener {
        data class Step(val message: String, val desc: PluginDescriptor? = null)

        fun step(step: Step)

        fun step(step: String) {
            step(Step(step))
        }
    }

    suspend fun loadPluginBundle(
        bundleUri: Uri,
        progress: PluginLoadProgressListener? = null
    ) = withContext(Dispatchers.IO) {
        //println("loading plugin from $bundleUri")
        val tmpDir = Paths.tempDir.resolve("klyx-plugin-bundles/tmp-${System.nanoTime()}")
        tmpDir.mkdirs()

        try {
            progress?.step("extracting...")
            extractBundle(bundleUri, tmpDir)
            val jsonFile = tmpDir["plugin.json"] ?: error("Missing plugin.json in bundle")
            val desc = json.decodeFromString<PluginDescriptor>(jsonFile.readText())
            val pluginId = desc.id
            progress?.step(PluginLoadProgressListener.Step("found plugin.json with id $pluginId", desc))

            if (pluginId in runtimesById) {
                throw PluginLoadException("Plugin with id '$pluginId' is already loaded. Unload it first.")
            }

            validate(desc)
            val pluginDir = Paths.pluginsDir.resolve(pluginId)
            pluginDir.mkdirs()

            if (!pluginDir.exists()) {
                Os.rename(tmpDir.absolutePath, pluginDir.absolutePath)
            } else {
                pluginDir.deleteRecursively()
                Os.rename(tmpDir.absolutePath, pluginDir.absolutePath)
            }

            val bundleCopy = File(pluginDir, "bundle.klyx")
            if (bundleCopy.exists()) bundleCopy.delete()

            fs.inputStream(bundleUri).buffered().use { inputStream ->
                bundleCopy.outputStream().buffered().use { outputStream ->
                    val _ = inputStream.copyTo(outputStream)
                }
            }

            val apkFile = File(pluginDir, "plugin.apk")
            if (!apkFile.exists()) {
                throw PluginLoadException(
                    "APK file not found at $apkFile the bundle is missing plugin.apk. " +
                            "Rebuild the bundle and reinstall."
                )
            }
            apkFile.setReadOnly()

            progress?.step("instantiating plugin")
            val plugin = instantiatePlugin(apkFile.absolutePath, desc)
            val info = createPluginInfo(pluginDir, desc)
            progress?.step("creating runtime")
            val runtime = PluginRuntime(app, plugin, info)
            progress?.step("loading plugin")
            register(runtime, progress)
            startRuntime(runtime, progress)
            progress?.step("loading successfully")
            installPlugin(pluginId)
        } catch (e: ErrnoException) {
            throw PluginLoadException("Failed to load plugin bundle", e)
        } finally {
            if (tmpDir.exists()) {
                tmpDir.deleteRecursively()
            }
        }
    }

    private suspend fun register(runtime: PluginRuntime, progress: PluginLoadProgressListener?) {
        runtimes[runtime.plugin] = runtime
        runtimesById[runtime.info.id] = runtime

        try {
            runtime.load(progress)
        } catch (t: Throwable) {
            discard(runtime)
            throw t
        }
    }

    private suspend fun startRuntime(runtime: PluginRuntime, progress: PluginLoadProgressListener?) {
        try {
            runtime.start(progress)
        } catch (t: Throwable) {
            discard(runtime)
            throw t
        }
    }

    private fun discard(runtime: PluginRuntime) {
        unregister(runtime)
        Paths.pluginsDir.resolve(runtime.info.id).deleteRecursively()
    }

    private fun unregister(runtime: PluginRuntime) {
        runtimes.remove(runtime.plugin)
        runtimesById.remove(runtime.info.id)
    }

    private fun instantiatePlugin(apkPath: String, desc: PluginDescriptor): KlyxPlugin {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            throw PluginLoadException("APK file not found: $apkPath")
        }

        val loader = PathClassLoader(
            apkPath,
            app.application.classLoader
        )

        val cls = loader.loadClass(desc.entryClass)
        val instance = cls.getConstructor().newInstance()
        return instance as? KlyxPlugin
            ?: throw PluginLoadException("Class ${desc.entryClass} does not implement KlyxPlugin")
    }

    private operator fun File.get(relative: String): File? = resolve(relative).takeIf { it.exists() }

    private suspend fun extractBundle(bundleUri: Uri, destination: File) = withContext(Dispatchers.IO) {
        val tmp = Paths.tempDir.resolve("klyx-plugin-bundles/bundles/${System.nanoTime()}.klyx")
        tmp.parentFile?.mkdirs()
        fs.inputStream(bundleUri).buffered().use { input ->
            tmp.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }

        try {
            extractGzipTar(tmp, destination)
        } catch (e: IOException) {
            if (e.message == "Input is not in the .gz format.") {
                throw PluginLoadException("File is not in the .klyx format.")
            } else {
                throw e
            }
        } finally {
            tmp.delete()
        }
    }

    private fun validate(desc: PluginDescriptor) {
        val pluginId = desc.id

        val minVersion = desc.minAppVersion.toVersion(strict = true)
        if (minVersion > appVersion) {
            throw PluginLoadException(
                "Plugin '$pluginId' requires app version >= ${desc.minAppVersion}, but current app version is $appVersion."
            )
        }

        desc.maxAppVersion?.let { max ->
            val maxVersion = max.toVersion(strict = true)
            if (appVersion > maxVersion) {
                throw PluginLoadException(
                    "Plugin '$pluginId' requires app version <= $max, but current app version is $appVersion."
                )
            }
        }
    }

    private fun createPluginInfo(
        pluginDir: File,
        desc: PluginDescriptor
    ): PluginInfo {

        val apkFile = File(pluginDir, "plugin.apk")
        val bundleFile = pluginDir["bundle.klyx"]

        val icon = desc.icon?.let { path ->
            val iconFile = File(pluginDir, path)

            // Guard against path traversal: icon path comes from untrusted plugin.json and
            // must resolve to a file inside the plugin's own directory.
            val pluginRoot = pluginDir.canonicalFile
            val canonicalIcon = iconFile.canonicalFile
            if (!canonicalIcon.path.startsWith(pluginRoot.path + File.separator)) {
                Log.w("PluginManager", "Plugin '${desc.id}' icon path escapes plugin dir: $path")
                return@let null
            }

            if (!canonicalIcon.exists()) {
                return@let null
            }

            BitmapFactory.decodeFile(canonicalIcon.absolutePath)
                ?.asImageBitmap()
                ?.let { BitmapPainter(it) }
        }

        return PluginInfo(
            descriptor = desc,
            apkPath = apkFile.absolutePath,
            bundlePath = bundleFile?.absolutePath,
            icon = icon
        )
    }

    suspend fun loadPlugin(id: String, progress: PluginLoadProgressListener? = null) {
        val runtime = runtimesById[id] ?: return
        runtime.load(progress)
    }

    suspend fun startPlugin(id: String) {
        val runtime = runtimesById[id] ?: return
        runtime.start()
    }

    suspend fun stopPlugin(id: String) {
        val runtime = runtimesById[id] ?: return
        runtime.stop()
    }

    suspend fun unloadPlugin(id: String) {
        val runtime = runtimesById.remove(id) ?: return
        runtime.unload()
        runtimes.remove(runtime.plugin)
        uninstallPlugin(id)
    }

    private suspend fun installPlugin(id: String) {
        if (!installedList.contains(id)) {
            installedList.add(id)
            saveInstalled()
        }
    }

    private suspend fun uninstallPlugin(id: String) = withContext(Dispatchers.IO) {
        installedList.remove(id)
        saveInstalled()
        val pluginDir = File(Paths.pluginsDir, id)
        if (pluginDir.exists()) {
            pluginDir.deleteRecursively()
        }
    }

    private suspend fun loadInstalledList() = withContext(Dispatchers.Default) {
        val installedJson = Paths.installedPluginsJson
        if (!installedJson.exists()) return@withContext
        try {
            val list = json.decodeFromString<InstalledList>(installedJson.readText())
            installedList.clear()
            installedList.addAll(list.ids)
        } catch (e: Exception) {
            Log.e("PluginManager", "Failed to load installed plugins list", e)
        }
    }

    private suspend fun saveInstalled() = withContext(Dispatchers.IO) {
        try {
            Paths.installedPluginsJson
                .writeText(json.encodeToString(InstalledList(installedList.toList())))
        } catch (e: Exception) {
            Log.e("PluginManager", "Failed to save installed plugins list", e)
        }
    }

    private suspend fun loadInstalledPlugins(progress: PluginLoadProgressListener? = null) =
        withContext(Dispatchers.IO) {
            // onLoad() every plugin. onStart() must not run until all plugins are
            // loaded (see KlyxPlugin.onStart docs: "all dependencies should be loaded and ready").
            val loaded = mutableListOf<PluginRuntime>()
            for (pluginId in installedList.toList()) {
                try {
                    val pluginDir = File(Paths.pluginsDir, pluginId)
                    val apkFile = File(pluginDir, "plugin.apk")
                    if (!apkFile.exists()) {
                        installedList.remove(pluginId)
                        continue
                    }

                    val runtime = loadRuntime(pluginDir, progress)
                    register(runtime, progress)
                    loaded += runtime
                    progress?.step("loaded plugin '$pluginId'")
                } catch (e: Exception) {
                    Log.e("PluginManager", "Failed to reload plugin '$pluginId'", e)
                    progress?.step("failed to load plugin '$pluginId': ${e.localizedMessage}")
                    installedList.remove(pluginId)
                    Paths.pluginsDir.resolve(pluginId)
                        .deleteRecursively()
                }
            }

            // onStart() every loaded plugin.
            for (runtime in loaded) {
                val pluginId = runtime.info.id
                try {
                    startRuntime(runtime, progress)
                    progress?.step("started plugin '$pluginId'")
                } catch (e: Exception) {
                    Log.e("PluginManager", "Failed to start plugin '$pluginId'", e)
                    progress?.step("failed to start plugin '$pluginId': ${e.localizedMessage}")
                    installedList.remove(pluginId)
                }
            }

            saveInstalled()
        }

    private fun loadRuntime(pluginDir: File, progress: PluginLoadProgressListener? = null): PluginRuntime {
        val apkFile = File(pluginDir, "plugin.apk")
        val jsonFile = File(pluginDir, "plugin.json")
        val desc = json.decodeFromString<PluginDescriptor>(jsonFile.readText())
        validate(desc)
        progress?.step("instantiating plugin (${desc.id})")
        val plugin = instantiatePlugin(apkFile.absolutePath, desc)
        val info = createPluginInfo(pluginDir, desc)
        progress?.step("creating runtime (${desc.id})")
        return PluginRuntime(app, plugin, info)
    }

    override fun <T : PluginRuntimeService> service(plugin: KlyxPlugin, type: KClass<T>): T {
        return runtimes[plugin]?.service(type) ?: error("Plugin isn't loaded.")
    }

    @Serializable
    data class InstalledList(val ids: List<String>)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }

        const val CDN = "https://plugins.klyx.workers.dev/api"
        const val API = "https://plugins.klyx.workers.dev"
    }
}

class PluginLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

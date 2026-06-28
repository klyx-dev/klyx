package com.klyx

import android.app.Application
import com.klyx.api.KlyxContext
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.PathsProvider
import com.klyx.api.data.terminal.TerminalSessionBinder
import com.klyx.api.data.terminal.TerminalSessionManager
import com.klyx.api.service.FontService
import com.klyx.api.service.SettingsService
import com.klyx.api.service.TabService
import com.klyx.api.ui.ScreenEntry
import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.core.App
import com.klyx.core.initApp
import com.klyx.data.terminal.DefaultTerminalSessionManager
import com.klyx.data.terminal.TerminalSessionBinderImpl
import com.klyx.event.initializeGlobalEventBus
import com.klyx.service.FontServiceWrapper
import com.klyx.service.SettingsServiceWrapper
import com.klyx.service.TabServiceWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin
import java.io.File

@KoinApplication
class KlyxApplication : Application() {

    lateinit var app: App
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("klyx")

        startKoin<KlyxApplication> {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        app = initApp()
        KlyxContext.application = this
        initializeGlobals()
    }

    private fun initializeGlobals() {
        initializeGlobalEventBus(app)

        app.setGlobal<TerminalSessionBinder>(TerminalSessionBinderImpl())
        app.setGlobal<TerminalSessionManager>(DefaultTerminalSessionManager())
        app.setGlobal<PathsProvider>(KlyxPathsProvider())
        app.setGlobal<FileSystem>(auto())
        app.setGlobal<ScreenRegistry>(MutableScreenRegistry())
        app.setGlobal<ToolbarRegistry>(MutableToolbarRegistry())
        app.setGlobal<SettingsService>(SettingsServiceWrapper(auto()))
        app.setGlobal<FontService>(FontServiceWrapper(auto()))
        app.setGlobal<TabService>(TabServiceWrapper(auto()))
    }

    private inline fun <reified T> auto(): T = GlobalContext.get().get()

    private class KlyxPathsProvider : PathsProvider {
        override val filesDir get() = KlyxContext.application.filesDir
        override val tempDir get() = KlyxContext.application.cacheDir
        override val externalFilesDir
            get() = KlyxContext.application.getExternalFilesDir(null)
                ?: error("shared storage is not currently available.")
        override val nativeLibraryDir get() = File(KlyxContext.application.applicationInfo.nativeLibraryDir)
        override val dataDir get() = KlyxContext.application.dataDir
        override val externalCacheDir get() = KlyxContext.application.externalCacheDir
        override val noBackupFilesDir get() = KlyxContext.application.noBackupFilesDir
        override val codeCacheDir get() = KlyxContext.application.codeCacheDir
        override val externalCacheDirs get() = KlyxContext.application.externalCacheDirs
        override val externalFilesDirs get() = KlyxContext.application.getExternalFilesDirs(null)

        override val rootFs get() = dataDir.canonicalFile.resolve("rootfs")
        override val homeDir get() = filesDir.canonicalFile.resolve("home")
        override val projectsDir get() = homeDir.resolve("projects")
        override val versionFile get() = rootFs.resolve(".bootstrap-version")
        override val proot get() = File(nativeLibraryDir, "libproot.so")
        override val prootLoader get() = File(nativeLibraryDir, "libloader.so")
    }

    private class MutableScreenRegistry : ScreenRegistry {
        private val _screens = mutableListOf<ScreenEntry>()

        override val registeredScreens: List<ScreenEntry> get() = _screens

        override fun register(screen: ScreenEntry) {
            _screens.add(screen)
        }

        override fun unregister(id: ScreenId) {
            _screens.removeAll { it.id == id }
        }
    }

    private class MutableToolbarRegistry : ToolbarRegistry {
        private val _actions = mutableListOf<ToolbarAction>()

        override val registeredActions: List<ToolbarAction> get() = _actions

        override fun register(action: ToolbarAction) {
            _actions.add(action)
        }

        override fun unregister(id: String) {
            _actions.removeAll { it.id == id }
        }
    }
}

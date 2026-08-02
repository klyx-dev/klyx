package com.klyx

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.klyx.api.InternalKlyxApi
import com.klyx.api.data.editor.FileOpenRequest
import com.klyx.api.data.editor.FileOpener
import com.klyx.api.data.editor.FileOpenerRegistration
import com.klyx.api.data.editor.FileOpenerRegistry
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.data.terminal.TerminalSessionBinder
import com.klyx.api.data.terminal.TerminalSessionManager
import com.klyx.api.event.EventBusHolder
import com.klyx.api.language.LanguageRegistry
import com.klyx.api.lsp.LanguageServerRegistry
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.plugin.PluginSettings
import com.klyx.api.plugin.PluginSettingsRegistration
import com.klyx.api.plugin.PluginSettingsRegistry
import com.klyx.api.plugin.info
import com.klyx.api.service.Logger
import com.klyx.api.ui.Content
import com.klyx.api.ui.Screen
import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistration
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.ToolbarIcon
import com.klyx.api.ui.ToolbarRegistration
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.language.LanguageRegistryImpl
import com.klyx.core.App
import com.klyx.core.initApp
import com.klyx.data.terminal.DefaultTerminalSessionManager
import com.klyx.data.terminal.TerminalSessionBinderImpl
import com.klyx.event.eventBus
import com.klyx.event.initializeGlobalEventBus
import com.klyx.plugin.PluginManager
import com.klyx.service.FontsWrapper
import com.klyx.service.SettingsWrapper
import com.klyx.service.TabsWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class KlyxApplication : Application() {

    lateinit var app: App
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        System.loadLibrary("klyx")

        startKoin<KlyxApplication> {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        app = initApp()
        initializeGlobals()
    }

    @OptIn(InternalKlyxApi::class)
    private fun initializeGlobals() {
        initializeGlobalEventBus(app)
        app.setGlobal(EventBusHolder(app.eventBus()))

        val terminalManager = TerminalManagerImpl(
            sessionBinder = TerminalSessionBinderImpl(),
            sessionManager = DefaultTerminalSessionManager()
        )
        app.setGlobal(terminalManager)
        app.setGlobal(auto<FileSystem>())
        app.setGlobal(MutableScreenRegistry())
        app.setGlobal(MutableToolbarRegistry())
        app.setGlobal(MutableFileOpenerRegistry())
        app.setGlobal(MutablePluginSettingsRegistry())
        app.setGlobal(SettingsWrapper(auto()))
        app.setGlobal(FontsWrapper(auto()))
        app.setGlobal(TabsWrapper { auto() })
        app.setGlobal(PluginManager(app))
        app.setGlobal(auto<LanguageServerRegistry>())
        app.setGlobal(LanguageRegistryImpl(this))
        app.setGlobal(auto<Logger>())
    }

    private class TerminalManagerImpl(
        override val sessionManager: TerminalSessionManager,
        override val sessionBinder: TerminalSessionBinder
    ) : TerminalManager

    private inline fun <reified T> auto(): T = GlobalContext.get().get()

    private class MutableScreenRegistry : ScreenRegistry {

        private val screens = mutableStateMapOf<ScreenId, Content>()
        private val screenOwner = mutableMapOf<ScreenId, String>()

        context(plugin: KlyxPlugin)
        override fun register(screen: Screen): ScreenRegistration {
            screens[screen.id] = screen.content
            screenOwner[screen.id] = plugin.info.id
            return ScreenRegistration {
                screens.remove(screen.id)
                screenOwner.remove(screen.id)
            }
        }

        override fun unregister(id: ScreenId) {
            screens.remove(id)
            screenOwner.remove(id)
        }

        override fun set(id: ScreenId, content: Content) {
            screens[id] = content
        }

        override fun get(id: ScreenId): Content? {
            return screens[id]
        }

        override fun ownerOf(id: ScreenId): String? = screenOwner[id]

        @InternalKlyxApi
        override fun unregisterAll(pluginId: String) {
            val toRemove = screenOwner.filterValues { it == pluginId }.keys.toList()
            toRemove.forEach { unregister(it) }
        }
    }

    private class MutableToolbarRegistry : ToolbarRegistry {
        private val _actions = mutableStateListOf<ToolbarAction>()

        context(plugin: KlyxPlugin)
        override fun register(action: ToolbarAction): ToolbarRegistration {
            val resolved = action.resolve(plugin.info)
            _actions += resolved
            return ToolbarRegistration { _actions.remove(resolved) }
        }

        fun ToolbarAction.resolve(info: PluginInfo): ToolbarAction {
            val resolved = when (val icon = icon) {
                is ToolbarIcon.Resource -> {
                    val file = Paths.pluginsDir
                        .resolve(info.id)
                        .resolve(icon.path)

                    if (file.exists()) {
                        ToolbarIcon.File(file)
                    } else {
                        Log.w("ToolbarRegistry", "Plugin '${info.id}' references missing icon '${icon.path}'.")
                        null
                    }
                }

                else -> icon
            }

            return copy(icon = resolved)
        }

        override fun unregister(id: String) {
            _actions.removeAll { it.id == id }
        }

        override fun actions(): List<ToolbarAction> {
            return _actions
        }
    }

    private class MutableFileOpenerRegistry : FileOpenerRegistry {

        private val _openers = mutableStateListOf<FileOpener>()

        context(plugin: KlyxPlugin)
        override fun register(opener: FileOpener): FileOpenerRegistration {
            _openers.removeAll { it.id == opener.id }
            _openers += opener
            return FileOpenerRegistration { _openers.removeAll { it.id == opener.id } }
        }

        override fun unregister(id: String) {
            _openers.removeAll { it.id == id }
        }

        override fun openers(): List<FileOpener> =
            _openers.sortedByDescending { it.priority }

        override suspend fun open(request: FileOpenRequest): WorkspaceTab? {
            for (opener in openers()) {
                val tab = opener.open(request)
                if (tab != null) return tab
            }
            return null
        }
    }

    private class MutablePluginSettingsRegistry : PluginSettingsRegistry {

        private val content = mutableStateMapOf<String, @Composable PluginSettings.() -> Unit>()

        context(plugin: KlyxPlugin)
        override fun register(
            content: @Composable PluginSettings.() -> Unit
        ): PluginSettingsRegistration {
            val pluginId = plugin.info.id
            this.content[pluginId] = content
            return PluginSettingsRegistration { this@MutablePluginSettingsRegistry.content.remove(pluginId) }
        }

        override fun hasSettings(pluginId: String): Boolean = content.containsKey(pluginId)

        @InternalKlyxApi
        override fun contentFor(pluginId: String): (@Composable PluginSettings.() -> Unit)? =
            content[pluginId]

        @InternalKlyxApi
        override fun unregisterAll(pluginId: String) {
            content.remove(pluginId)
        }
    }
}

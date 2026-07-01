package com.klyx

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.data.terminal.TerminalSessionBinder
import com.klyx.api.data.terminal.TerminalSessionManager
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.plugin.runtime
import com.klyx.api.ui.Content
import com.klyx.api.ui.Screen
import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistration
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.ToolbarIcon
import com.klyx.api.ui.ToolbarRegistration
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.core.App
import com.klyx.core.initApp
import com.klyx.data.terminal.DefaultTerminalSessionManager
import com.klyx.data.terminal.TerminalSessionBinderImpl
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
        System.loadLibrary("klyx")

        startKoin<KlyxApplication> {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        app = initApp()
        initializeGlobals()
    }

    private fun initializeGlobals() {
        initializeGlobalEventBus(app)

        val terminalManager = TerminalManagerImpl(
            sessionBinder = TerminalSessionBinderImpl(),
            sessionManager = DefaultTerminalSessionManager()
        )
        app.setGlobal(terminalManager)
        app.setGlobal(auto<FileSystem>())
        app.setGlobal(MutableScreenRegistry())
        app.setGlobal(MutableToolbarRegistry())
        app.setGlobal(SettingsWrapper(auto()))
        app.setGlobal(FontsWrapper(auto()))
        app.setGlobal(TabsWrapper { auto() })
        app.setGlobal(PluginManager(app))
    }

    private class TerminalManagerImpl(
        override val sessionManager: TerminalSessionManager,
        override val sessionBinder: TerminalSessionBinder
    ) : TerminalManager

    private inline fun <reified T> auto(): T = GlobalContext.get().get()

    private class MutableScreenRegistry : ScreenRegistry {

        private val screens = mutableStateMapOf<ScreenId, Content>()

        context(plugin: KlyxPlugin)
        override fun register(screen: Screen): ScreenRegistration {
            screens[screen.id] = screen.content
            return object : ScreenRegistration {
                override fun unregister() {
                    screens.remove(screen.id)
                }
            }
        }

        override fun unregister(id: ScreenId) {
            screens.remove(id)
        }

        override fun set(id: ScreenId, content: Content) {
            screens[id] = content
        }

        override fun get(id: ScreenId): Content? {
            return screens[id]
        }
    }

    private class MutableToolbarRegistry : ToolbarRegistry {
        private val _actions = mutableStateListOf<ToolbarAction>()

        private val KlyxPlugin.info: PluginInfo by runtime()

        context(plugin: KlyxPlugin)
        override fun register(action: ToolbarAction): ToolbarRegistration {
            val resolved = action.resolve(plugin.info)
            _actions += resolved

            return object : ToolbarRegistration {
                override fun unregister() {
                    _actions.remove(resolved)
                }
            }
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
}

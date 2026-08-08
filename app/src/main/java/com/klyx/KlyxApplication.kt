package com.klyx

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.klyx.api.InternalKlyxApi
import com.klyx.api.NavDestination
import com.klyx.api.Navigator
import com.klyx.api.data.editor.FileOpenRequest
import com.klyx.api.data.editor.FileOpener
import com.klyx.api.data.editor.FileOpenerRegistration
import com.klyx.api.data.editor.FileOpenerRegistry
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.data.runner.FileRunner
import com.klyx.api.data.runner.FileRunRequest
import com.klyx.api.data.runner.FileRunnerRegistration
import com.klyx.api.data.runner.FileRunnerRegistry
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
import com.klyx.data.runner.PythonFileRunner
import com.klyx.data.runner.TerminalCommandRunner
import com.klyx.di.AppModule
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

@KoinApplication(modules = [AppModule::class])
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
            sessionManager = DefaultTerminalSessionManager(),
            terminalRunner = auto(),
            app = app,
        )
        app.setGlobal(terminalManager)
        app.setGlobal(auto<FileSystem>())
        app.setGlobal(MutableScreenRegistry())
        app.setGlobal(MutableToolbarRegistry())
        app.setGlobal(MutableFileRunnerRegistry().apply {
            registerInternal(PythonFileRunner())
        })
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
        override val sessionBinder: TerminalSessionBinder,
        private val terminalRunner: TerminalCommandRunner,
        private val app: App,
    ) : TerminalManager {

        override suspend fun runInTerminal(
            command: String,
            cwd: String?,
            sessionName: String?,
        ) {
            terminalRunner.run(
                navigateToTerminal = { app.global<Navigator>().navigateTo(NavDestination.Terminal) },
                command = command,
                cwd = cwd,
                sessionName = sessionName,
            )
        }

        override fun openTerminal() {
            app.global<Navigator>().navigateTo(NavDestination.Terminal)
        }
    }

    private inline fun <reified T> auto(): T = GlobalContext.get().get()

    private class MutableScreenRegistry : ScreenRegistry {

        private val screens = mutableStateMapOf<ScreenId, Content>()
        private val transientScreens = mutableMapOf<ScreenId, Content>()
        private val screenOwner = mutableMapOf<ScreenId, String>()

        context(plugin: KlyxPlugin)
        override fun register(screen: Screen): ScreenRegistration {
            transientScreens.remove(screen.id)
            screens[screen.id] = screen.content
            screenOwner[screen.id] = plugin.info.id
            return ScreenRegistration {
                screens.remove(screen.id)
                screenOwner.remove(screen.id)
            }
        }

        override fun unregister(id: ScreenId) {
            transientScreens.remove(id)
            screens.remove(id)
            screenOwner.remove(id)
        }

        override fun set(id: ScreenId, content: Content) {
            transientScreens.remove(id)
            screens[id] = content
        }

        override fun setTransient(id: ScreenId, content: Content) {
            transientScreens[id] = content
        }

        override fun unregisterTransient(id: ScreenId) {
            transientScreens.remove(id)
        }

        override fun get(id: ScreenId): Content? {
            return transientScreens[id] ?: screens[id]
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

    private class MutableFileRunnerRegistry : FileRunnerRegistry {

        private val _runners = mutableStateListOf<FileRunner>()
        private val _sortedRunners = mutableStateListOf<FileRunner>()
        private val _owners = mutableMapOf<String, String>()

        private fun updateSortedRunners() {
            _sortedRunners.clear()
            _sortedRunners.addAll(_runners.sortedByDescending { it.priority })
        }

        context(plugin: KlyxPlugin)
        override fun register(runner: FileRunner): FileRunnerRegistration {
            _runners.removeAll { it.id == runner.id }
            _runners += runner
            updateSortedRunners()
            _owners[runner.id] = plugin.info.id
            return FileRunnerRegistration {
                _runners.removeAll { it.id == runner.id }
                updateSortedRunners()
                _owners.remove(runner.id)
            }
        }

        /**
         * Registers a built-in runner that is not owned by any plugin, so it is never removed
         * by [unregisterAll].
         */
        fun registerInternal(runner: FileRunner): FileRunnerRegistration {
            _runners.removeAll { it.id == runner.id }
            _runners += runner
            updateSortedRunners()
            return FileRunnerRegistration {
                _runners.removeAll { it.id == runner.id }
                updateSortedRunners()
                _owners.remove(runner.id)
            }
        }

        override fun unregister(id: String) {
            _runners.removeAll { it.id == id }
            updateSortedRunners()
            _owners.remove(id)
        }

        override fun runnerFor(request: FileRunRequest): FileRunner? =
            runners().firstOrNull { runCatching { it.supports(request) }.getOrDefault(false) }

        override fun supports(request: FileRunRequest): Boolean =
            runnerFor(request) != null

        override fun runners(): List<FileRunner> =
            _sortedRunners

        @InternalKlyxApi
        override fun unregisterAll(pluginId: String) {
            val toRemove = _owners.filterValues { it == pluginId }.keys.toList()
            toRemove.forEach { unregister(it) }
        }
    }

    private class MutableFileOpenerRegistry : FileOpenerRegistry {

        private val _openers = mutableStateListOf<FileOpener>()
        private val _sortedOpeners = mutableStateListOf<FileOpener>()

        private fun updateSortedOpeners() {
            _sortedOpeners.clear()
            _sortedOpeners.addAll(_openers.sortedByDescending { it.priority })
        }

        context(plugin: KlyxPlugin)
        override fun register(opener: FileOpener): FileOpenerRegistration {
            _openers.removeAll { it.id == opener.id }
            _openers += opener
            updateSortedOpeners()
            return FileOpenerRegistration {
                _openers.removeAll { it.id == opener.id }
                updateSortedOpeners()
            }
        }

        override fun unregister(id: String) {
            _openers.removeAll { it.id == id }
            updateSortedOpeners()
        }

        override fun openers(): List<FileOpener> =
            _sortedOpeners

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

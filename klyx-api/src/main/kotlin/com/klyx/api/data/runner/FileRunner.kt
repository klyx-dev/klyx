package com.klyx.api.data.runner

import android.net.Uri
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import com.klyx.api.InternalKlyxApi
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.file.KxFile
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.ui.Content
import com.klyx.api.ui.ScreenRegistration
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.pluginService
import com.klyx.api.ui.ScreenId
import com.klyx.core.LocalApp

/**
 * Describes a file the user wants to run.
 *
 * A [FileRunRequest] is handed to every registered [FileRunner] so a plugin can decide whether
 * it knows how to execute the file and, if so, actually run it.
 *
 * @property file The file being run.
 * @property uri The URI of the file being run.
 * @property projectUri The URI of the project the file belongs to, if any.
 * @property tabId The id of the active editor tab for this file, if any.
 */
data class FileRunRequest(
    val file: KxFile,
    val uri: Uri,
    val projectUri: Uri? = null,
    val tabId: String? = null,
) {
    /** The lowercase file extension without the leading dot, or an empty string if none. */
    val extension: String get() = file.extension.lowercase()
}

/**
 * A handle returned when a [FileRunner] is registered.
 */
fun interface FileRunnerRegistration {

    /**
     * Removes the runner from the registry.
     */
    fun unregister()
}

/**
 * Knows how to execute a specific kind of file.
 *
 * When the user hits the "Run" button in the editor toolbar, the [FileRunnerRegistry] consults
 * every registered [FileRunner] in descending [priority] order. The first runner whose
 * [supports] returns `true` is used to [run] the file.
 *
 * ### Example
 * ```kotlin
 * class HtmlRunner : FileRunner {
 *     override val id = "com.example.html.runner"
 *     override val priority = 10
 *
 *     override fun supports(request: FileRunRequest): Boolean =
 *         request.extension == "html"
 *
 *     override suspend fun run(request: FileRunRequest, runner: FileRunnerContext) {
 *         runner.openScreen(ScreenId("com.example.html.preview")) {
 *             HtmlPreviewScreen(uri = request.uri)
 *         }
 *     }
 * }
 *
 * class CppRunner : FileRunner {
 *     override val id = "com.example.cpp.runner"
 *
 *     override fun supports(request: FileRunRequest): Boolean =
 *         request.extension in setOf("c", "cpp", "cc")
 *
 *     override suspend fun run(request: FileRunRequest, runner: FileRunnerContext) {
 *         val path = request.uri.path ?: return
 *         val out = path.substringBeforeLast('.') + ".out"
 *         runner.runInTerminal("g++ \"$path\" -o \"$out\" && \"$out\"", cwd = path.substringBeforeLast('/'))
 *     }
 * }
 * ```
 */
interface FileRunner {

    /**
     * A unique identifier for this runner.
     *
     * Recommended format: reverse-DNS (e.g. "com.example.plugin.runner").
     */
    val id: String

    /**
     * Runners with a higher priority are consulted first. Defaults to 0.
     */
    val priority: Int get() = 0

    /**
     * Returns `true` if this runner can execute the file described by [request].
     *
     * This is invoked to decide whether to show the "Run" button for the active file, so it
     * should be cheap and side-effect free.
     */
    fun supports(request: FileRunRequest): Boolean

    /**
     * Executes the file described by [request].
     *
     * Use the provided [runner] to interact with the host (open the terminal, run a command,
     * open a custom screen, open a custom tab, etc.).
     */
    suspend fun run(request: FileRunRequest, runner: FileRunnerContext)
}

/**
 * Runtime helpers handed to a [FileRunner.run] so it can execute the file.
 */
interface FileRunnerContext {

    /**
     * Opens the terminal screen and runs [command] in a fresh terminal session.
     *
     * @param command The shell command to execute, e.g. `g++ main.cpp -o main && ./main`.
     * @param cwd The directory to `cd` into before running [command], or null to keep the default.
     * @param sessionName An optional name shown on the terminal session tab.
     */
    suspend fun runInTerminal(
        command: String,
        cwd: String? = null,
        sessionName: String? = null,
    )

    /**
     * Navigates to the terminal screen without running a command.
     */
    fun openTerminal()

    /**
     * Navigates to a screen registered by a plugin via the [ScreenRegistry][com.klyx.api.ui.ScreenRegistry].
     *
     * This is useful for runners that preview a file (e.g. open an HTML file in a WebView screen).
     */
    fun openScreen(screenId: ScreenId)

    /**
     * Registers the [content] for [screenId] and navigates to it.
     *
     * This is the direct way for a runner to start a screen without pre-registering it. Because
     * the [content] is a closure, it can capture whatever data the runner needs to pass along,
     * typically the file being run and its URI:
     *
     * ```kotlin
     * override suspend fun run(request: FileRunRequest, runner: FileRunnerContext) {
     *     runner.openScreen(ScreenId("com.example.html.preview")) {
     *         HtmlPreviewScreen(file = request.file, uri = request.uri)
     *     }
     * }
     * ```
     *
     * The screen is registered as a *transient* screen: klyx auto-unregisters it when its
     * navigation entry is popped, so no cleanup is required. It can also be removed early via the
     * returned [ScreenRegistration].
     */
    fun openScreen(screenId: ScreenId, content: Content): ScreenRegistration

    /**
     * Opens a custom [tab] in the editor workspace.
     */
    fun openTab(tab: WorkspaceTab)
}

/**
 * Manages the registration and lookup of [FileRunner]s.
 *
 * Resolve it from a plugin with `val runners: FileRunnerRegistry by plugin()` or
 * `context.service<FileRunnerRegistry>()`.
 *
 * ### Example
 * ```kotlin
 * val runners: FileRunnerRegistry by plugin()
 * private var registration: FileRunnerRegistration? = null
 *
 * override suspend fun onStart() {
 *     registration = runners.register(HtmlRunner())
 * }
 *
 * override suspend fun onStop() {
 *     registration?.unregister()
 * }
 * ```
 */
interface FileRunnerRegistry : PluginService {

    /**
     * Registers a [runner]. Remember to [FileRunnerRegistration.unregister] it when the plugin
     * stops, or call [unregister] with its id. Registering a runner whose [FileRunner.id] matches
     * an existing one replaces the previous registration.
     */
    context(plugin: KlyxPlugin)
    fun register(runner: FileRunner): FileRunnerRegistration

    /**
     * Removes a previously registered runner by its [id].
     */
    fun unregister(id: String)

    /**
     * Returns the runner that [supports][FileRunner.supports] the given [request], or null if
     * no runner claims the file.
     */
    fun runnerFor(request: FileRunRequest): FileRunner?

    /**
     * Returns `true` if any registered runner [supports][FileRunner.supports] the given [request].
     *
     * The host uses this to decide whether to show the "Run" button for the active file.
     */
    fun supports(request: FileRunRequest): Boolean

    /**
     * All registered runners, ordered by descending [FileRunner.priority].
     */
    fun runners(): List<FileRunner>

    /**
     * Unregisters all runners owned by the plugin with the given [pluginId].
     */
    @InternalKlyxApi
    fun unregisterAll(pluginId: String)
}

/**
 * A [CompositionLocal] that provides the [FileRunnerRegistry].
 *
 * Defaults to retrieving the [FileRunnerRegistry] from the current [LocalApp]'s plugin service.
 */
val LocalFileRunnerRegistry = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.pluginService(FileRunnerRegistry::class)
}

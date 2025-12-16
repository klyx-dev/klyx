package com.klyx.core.app

import com.klyx.core.KlyxBuildConfig
import com.klyx.core.extension.ExtensionHost
import com.klyx.core.httpClient
import com.klyx.core.io.Paths
import com.klyx.core.io.languagesDir
import com.klyx.core.language.LanguageRegistry
import com.klyx.core.languages.Languages
import com.klyx.core.noderuntime.NodeBinaryOptions
import com.klyx.core.noderuntime.NodeRuntime
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmStatic

/**
 * Type alias for the [Application] class.
 *
 * This provides a shorter and more convenient way to refer to the [Application] class
 * throughout the codebase.
 */
typealias App = Application

/**
 * Singleton object representing the core application context and lifecycle management.
 *
 * The `Application` object is designed to manage application-level operations
 * such as initialization, coroutine scope handling, and application shutdown.
 */
object Application : DisposableHandle {

    private val appJob = SupervisorJob()

    /**
     * A default [CoroutineScope] with the [Dispatchers.Default] context and a [SupervisorJob].
     */
    val scope = CoroutineScope(Dispatchers.Default + appJob)

    private var initialized by atomic(false)

    /**
     * Initializes the application runtime environment including language server configuration,
     * extension host setup, and environment variables. This method ensures the initialization
     * logic is executed only once during the application lifecycle.
     *
     * Note: This method is thread-safe and should not cause side effects if called redundantly.
     */
    suspend fun init() {
        if (initialized) return

        trace("Application.init")

        withContext(Dispatchers.Default) {
            val shellEnvLoaded = CompletableDeferred(Unit)
            // TODO: expose node settings
            val options = MutableStateFlow(NodeBinaryOptions())

            val nodeRuntime = NodeRuntime(httpClient, shellEnvLoaded, options.asStateFlow())
            val languageRegistry = LanguageRegistry.INSTANCE
            languageRegistry.setLanguageServerDownloadDir(Paths.languagesDir)
            Languages.init(languageRegistry, nodeRuntime)
            ExtensionHost.init(nodeRuntime)
        }

        initialized = true
    }

    fun shutdown(reason: String) {
        if (!appJob.isCancelled) appJob.cancel(reason)
    }

    override fun dispose() = shutdown("Application disposed.")
}

/**
 * Executes a given suspending block of code if the application is in debug mode.
 *
 * @param block The suspending block of code to execute when the debug mode is active.
 */
inline fun debug(crossinline block: suspend () -> Unit) {
    if (KlyxBuildConfig.IS_DEBUG) {
        Application.scope.launch { block() }
    }
}

/**
 * Represents build-related metadata for the application.
 *
 * @property versionName The user-facing version name of the application.
 * @property versionCode The internal version code of the application.
 * @property buildType The type of build (e.g., "debug", "release").
 * @property isDebug A flag indicating whether the build is a debug build.
 * @property gitCommit The short hash of the Git commit associated with the build, if available.
 * @property gitBranch The Git branch name associated with the build, if available.
 * @property buildTimestamp The timestamp of when the build was created, in milliseconds since epoch.
 * @property kotlinVersion The version of Kotlin used for the build.
 */
data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val isDebug: Boolean,
    val gitCommit: String?,
    val gitBranch: String?,
    val buildTimestamp: Long,
    val kotlinVersion: String
) {
    companion object {
        /**
         * Returns the current build information of the application.
         */
        @JvmStatic
        fun current() = BuildInfo(
            versionName = KlyxBuildConfig.VERSION_NAME,
            versionCode = KlyxBuildConfig.VERSION_CODE,
            buildType = KlyxBuildConfig.BUILD_TYPE.lowercase(),
            isDebug = KlyxBuildConfig.IS_DEBUG,
            gitCommit = KlyxBuildConfig.GIT_COMMIT_SHORT,
            gitBranch = KlyxBuildConfig.GIT_BRANCH,
            buildTimestamp = KlyxBuildConfig.BUILD_TIMESTAMP,
            kotlinVersion = KlyxBuildConfig.KOTLIN_VERSION
        )
    }
}

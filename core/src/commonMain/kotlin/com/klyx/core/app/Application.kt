package com.klyx.core.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.KlyxBuildConfig
import com.klyx.core.platform.BackgroundScope
import com.klyx.core.platform.ForegroundScope
import com.klyx.core.platform.currentPlatform
import kotlinx.coroutines.launch
import org.koin.dsl.koinApplication
import org.koin.mp.KoinPlatformTools
import kotlin.jvm.JvmStatic

@RequiresOptIn(
    message = """
    Do NOT access GlobalApp directly.
    Prefer receiving App via constructor or function parameters.
    This global is intended for framework/bootstrap code only.
    """,
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class UnsafeGlobalAccess

/**
 * A global, lazily-initialized reference to the active [App] instance.
 *
 * **WARNING:** Accessing this property before the application has been initialized will throw an [IllegalStateException].
 * It is strongly recommended to inject the [App] instance via constructor parameters instead
 * of relying on this global state, which is primarily intended for framework or bootstrap code.
 *
 * @throws IllegalStateException if accessed before the Application is initialized.
 */
@Suppress("UndeclaredKoinUsage")
@UnsafeGlobalAccess
val GlobalApp: App by lazy(mode = KoinPlatformTools.defaultLazyMode()) {
    (KoinPlatformTools
        .defaultContext()
        .getOrNull() ?: error("Koin is not started"))
        .getOrNull() ?: error("GlobalApp was accessed before the application was initialized.")
}

/**
 * A reference to a Klyx application.
 * You won't interact with this type much outside of initial configuration and startup.
 */
class Application internal constructor(val app: App) {
    /**
     * A handle to the [BackgroundScope] associated with this app, which can be used to run tasks in the background.
     */
    val backgroundScope: BackgroundScope = app.backgroundScope

    /**
     * A handle to the [ForegroundScope] associated with this app, which can be used to run tasks in the foreground.
     */
    val foregroundScope: ForegroundScope = app.foregroundScope
}

@Suppress("UndeclaredKoinUsage")
private fun application() = KoinPlatformTools
    .defaultContext()
    .getOrNull()
    ?.getOrNull<Application>()

fun Application(): Application {
    application()?.let { return it }
    val globalKoin = KoinPlatformTools.defaultContext().get()
    val app = App(currentPlatform(), koinApplication()).also(globalKoin::declare)
    return Application(app).also(globalKoin::declare)
}

/**
 * Executes a given suspending block of code if the application is in debug mode.
 *
 * @param block The suspending block of code to execute when the debug mode is active.
 */
@OptIn(UnsafeGlobalAccess::class)
inline fun debug(crossinline block: suspend () -> Unit) {
    if (KlyxBuildConfig.IS_DEBUG) {
        GlobalApp.backgroundScope.launch { block() }
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

val LocalBuildInfo = staticCompositionLocalOf { BuildInfo.current() }

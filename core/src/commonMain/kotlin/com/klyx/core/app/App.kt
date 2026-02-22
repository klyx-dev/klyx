package com.klyx.core.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.logging.log
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.platform.ForegroundScope
import com.klyx.core.platform.Platform
import com.klyx.core.platform.quit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.mp.KoinPlatformTools
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmName
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.milliseconds

typealias QuitHandler = (App) -> Deferred<Unit>

private val SHUTDOWN_TIMEOUT = 100.milliseconds

/**
 * The entry point for the Klyx application context.
 *
 * This class provides a type-safe mechanism to manage global application state (referred to as [Global]s).
 *
 * It serves as a central hub for accessing core components and shared state throughout the application lifecycle.
 */
class App internal constructor(
    /**
     * The current platform the application is running on.
     *
     * This provides access to platform-specific information and capabilities, such as the OS name,
     * type (JVM, Android, iOS, etc.), and other environment details determined at runtime.
     */
    val platform: Platform,
    private val koinApplication: KoinApplication
) : HasApp {
    override val app = this

    @PublishedApi
    @get:JvmName("koin")
    internal val koin get() = koinApplication.koin

    @PublishedApi
    internal val defaultKoin get() = KoinPlatformTools.defaultContext().get()

    private val quitObservers = mutableListOf<QuitHandler>()
    private var quitting = false

    /**
     * A [CoroutineScope] tied to the lifecycle of this application instance.
     *
     * This scope is intended for launching background tasks that should persist as long as the
     * application is alive and be cancelled when the application is disposed.
     */
    val backgroundScope = platform.backgroundScope

    /**
     * A [CoroutineScope] bound to the foreground lifecycle of the application.
     *
     * Jobs launched in this scope are automatically cancelled when the application moves
     * to the background or terminates. This is suitable for UI updates, animations, or
     * any tasks that should only run while the user is actively interacting with the app.
     */
    val foregroundScope: ForegroundScope
        get() {
            if (quitting) {
                error("Can't spawn on main thread after on_app_quit")
            }
            return platform.foregroundScope
        }

    /**
     * Check whether a global of the given type has been assigned.
     */
    inline fun <reified G : Global> hasGlobal() = globalOrNull<G>() != null

    /**
     * Access the global of the given type.
     *
     * @throws NoGlobalException if a global for that type has not been assigned.
     */
    inline fun <reified G : Global> global(): G = try {
        koin.get()
    } catch (err: Throwable) {
        throw NoGlobalException("no state of type ${G::class.simpleName} exists", typeOf<G>(), err)
    }

    /**
     * Access the global of the given type if a value has been assigned.
     */
    inline fun <reified G : Global> globalOrNull(): G? = koin.getOrNull()

    /**
     * Access the global of the given type mutably. A default value is assigned if a global of this type has not
     * yet been assigned.
     */
    inline fun <reified G : Global> globalOrDefault(default: (App) -> G): G =
        globalOrNull() ?: default(this).also { setGlobal(it) }

    /**
     * Sets the value of the global of the given type.
     */
    inline fun <reified G : Global> setGlobal(global: G) = koin.declare(global, allowOverride = true)

    /**
     * Executes a block of code within the context of a specific [Global] instance.
     *
     * This function retrieves the global instance of type [G] and applies the provided [block] to it.
     * The [Global] instance is used as the receiver (`this`) within the block, and the [App] instance
     * is passed as an argument.
     *
     * @param G The type of the global state to access.
     * @param R The return type of the block.
     * @param block A function to execute with the global instance as the receiver.
     * @return The result of the [block].
     * @throws NoGlobalException if a global for type [G] has not been assigned.
     */
    inline fun <reified G : Global, R> withGlobal(block: G.(App) -> R): R = global<G>().block(this)

    /**
     * Quit the application gracefully. Handlers registered with [onAppQuit]
     * will be given 100ms to complete before exiting.
     */
    fun shutdown() {
        val deferred = quitObservers.map { it(this) }
        quitting = true

        try {
            runBlocking {
                withTimeout(SHUTDOWN_TIMEOUT) {
                    deferred.awaitAll()
                }
            }
        } catch (_: TimeoutCancellationException) {
            log.error { "timed out waiting on app_will_quit" }
        } finally {
            backgroundScope.cancel("app_will_quit")
            //foregroundScope.cancel("app_will_quit")
        }
        quitting = false
    }

    /**
     * Gracefully quit the application via the platform's standard routine.
     */
    fun quit(): Nothing = platform.quit()

    /**
     * Register a callback to be invoked when the application is about to quit.
     * It is not possible to cancel the quit event at this point.
     */
    fun onAppQuit(onQuit: QuitHandler) {
        quitObservers.add(onQuit)
    }

    /**
     * Spawns the future returned by the given function on the main thread. The lambda will be invoked
     * with [App], which allows the application state to be accessed across await points.
     */
    @IgnorableReturnValue
    fun <R> spawn(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(App) -> R
    ): Deferred<R> {
        if (quitting) {
            error("Can't spawn on main thread after on_app_quit")
        }
        return foregroundScope.async(context, start) { block(this@App) }
    }

    @IgnorableReturnValue
    fun <R> backgroundSpawn(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(App) -> R
    ): Deferred<R> = backgroundScope.async(context, start) { block(this@App) }

    inline fun <reified T> inject(
        mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode()
    ): Lazy<T> = defaultKoin.inject(mode = mode)

    inline fun <reified T> injectOrNull(
        mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode()
    ): Lazy<T?> = defaultKoin.injectOrNull(mode = mode)

    inline fun <reified T> get(): T = defaultKoin.get()
    inline fun <reified T> getOrNull(): T? = defaultKoin.getOrNull()
}

@Composable
inline fun <reified T : Global> globalOf(): T = LocalApp.current.global()

/**
 * Represents an entity that holds a reference to an [App] instance.
 *
 * This interface is typically implemented by components that require access to the core application
 * context and its associated global state management capabilities.
 *
 * @property app The main application instance providing access to global state.
 */
interface HasApp : KoinComponent {
    val app: App
}

/**
 * CompositionLocal that provides the current [App] instance.
 */
val LocalApp = staticCompositionLocalOf<App> {
    noLocalProvidedFor("LocalApp")
}

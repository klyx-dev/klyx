package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.action.ActionDispatcher
import com.klyx.core.init.InitializerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.mp.KoinPlatform
import kotlin.reflect.KClass

/**
 * The application.
 */
class App internal constructor(
    koinApplication: KoinApplication,
    override val ownerId: String = "klyx.app"
) : Owner {

    @PublishedApi
    internal val koin = koinApplication.koin

    /** Scope for app-level tasks that must not run on the main thread. */
    val backgroundScope = BackgroundScope()

    lateinit var initializer: InitializerManager

    private val disposables = mutableSetOf<Disposable>()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val actions = ActionDispatcher(appScope)

    /** Cancels the app's scopes. Nothing is usable afterwards. */
    fun close() {
        disposables.forEach { it.dispose() }
        appScope.cancel()
        backgroundScope.cancel()
    }

    fun addDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }

    inline fun <reified T : Any> has() = getOrNull<T>() != null

    fun <T : Any> has(clazz: KClass<T>) = getOrNull(clazz) != null

    inline fun <reified T : Any> get(): T = get(T::class)

    fun <T : Any> get(clazz: KClass<T>): T = getOrNull(clazz) ?: koin.get(clazz)

    inline fun <reified T : Any> getOrNull(): T? = getOrNull(T::class)

    fun <T : Any> getOrNull(clazz: KClass<T>): T? =
        koin.getOrNull(clazz) ?: KoinPlatform.getKoinOrNull()?.getOrNull(clazz)

    inline fun <reified T : Any> getOrDefault(default: (App) -> T): T = getOrNull() ?: default(this)

    inline fun <reified T : Any> inject(): Lazy<T> = lazy { get<T>() }

    fun <T : Any> inject(clazz: KClass<T>): Lazy<T> = lazy { get(clazz) }

    inline fun <reified T : Any> declare(instance: T) = KoinPlatform.getKoin().declare(instance)

    companion object {
        /** The current [App]. Requires [init] to have run first. */
        val global: App
            get() {
                val koin = KoinPlatform.getKoinOrNull() ?: error("initKoin first")
                return koin.getOrNull<App>() ?: error("call App.init() first")
            }

        /** Creates an [App] in [koinApplication] and registers it, or returns the one already there. */
        @IgnorableReturnValue
        fun init(koinApplication: KoinApplication = koinApplication()): App {
            val koin = koinApplication.koin
            return koin.getOrNull() ?: App(koinApplication).also { koin.declare(it, allowOverride = true) }
        }
    }
}

expect fun App.isDebugMode(): Boolean

/** CompositionLocal providing the current [App]. */
val LocalApp = staticCompositionLocalOf { App.global }

/** Runs [block] on the background scope, but only in a debug build. */
inline fun debug(crossinline block: suspend App.() -> Unit) {
    runCatching { App.global }.onSuccess { app ->
        if (app.isDebugMode()) {
            app.backgroundScope.launch { block(app) }
        }
    }
}

/** Runs [block] off the main thread for as long as this composable is present, but only in a debug build. */
@Composable
inline fun Debug(crossinline block: @DisallowComposableCalls suspend App.() -> Unit) {
    val app = LocalApp.current

    if (app.isDebugMode()) {
        LaunchedEffect(app) {
            withContext(Dispatchers.Default) {
                block(app)
            }
        }
    }
}

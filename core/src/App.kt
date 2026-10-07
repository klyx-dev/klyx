package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.action.Action
import com.klyx.core.action.ActionDispatcher
import com.klyx.core.action.ActionId
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.action.ActionRegistry
import com.klyx.core.event.AppEvent
import com.klyx.core.keybinding.KeybindingRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.mp.KoinPlatform
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * The application.
 */
class App internal constructor(
    koinApplication: KoinApplication,
    override val ownerId: String = "klyx.app"
) : Owner {

    @PublishedApi
    internal val koin = koinApplication.koin

    val actionRegistry = ActionRegistry()
    val actionRegistrar = ActionRegistrar(actionRegistry, this)
    val keybindings = KeybindingRegistry()
    val actions = ActionDispatcher(actionRegistry, koin)

    /** Scope for app-level tasks that must not run on the main thread. */
    val backgroundScope = BackgroundScope()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every event the app emits. */
    val events: SharedFlow<AppEvent> = actions.events.shareIn(appScope, SharingStarted.Eagerly, replay = 0)

    init {
        koin.declare(actionRegistry, allowOverride = true)
        koin.declare(actionRegistrar, allowOverride = true)
        bindDefaultKeybindings()
    }

    /**
     * Binds every chord declared by a registered action's
     * [com.klyx.core.action.ActionMetadata.defaultKeybinding], and returns the handles that undo them.
     */
    @IgnorableReturnValue
    fun bindDefaultKeybindings(): List<Disposable> =
        keybindings.bindAll(actionRegistry.defaultKeybindings(), owner = this)

    /** Runs [action] on the background scope and returns the [Job] so callers may await it. */
    @IgnorableReturnValue
    fun dispatch(action: Action): Job = backgroundScope.launch { actions.dispatch(action) }

    /** Runs whatever [id] resolves to through its registered factory. */
    @IgnorableReturnValue
    fun dispatch(id: ActionId): Job = backgroundScope.launch { actions.dispatch(id) }

    /** Cancels the app's scopes. Nothing is usable afterwards. */
    fun close() {
        appScope.cancel()
        backgroundScope.cancel()
    }

    inline fun <reified G : Global> hasGlobal() = globalOrNull<G>() != null

    inline fun <reified G : Global> global(): G = globalOrNull<G>()
        ?: throw NoGlobalException("no state of type ${G::class.simpleName} exists", typeOf<G>(), null)

    fun <G : Global> global(clazz: KClass<G>): G = globalOrNull(clazz)
        ?: throw NoGlobalException("no state of type ${clazz.simpleName} exists")

    inline fun <reified G : Global> globalOrNull(): G? = globalOrNull(G::class)

    fun <G : Global> globalOrNull(clazz: KClass<G>): G? = getOrNull(clazz)

    inline fun <reified G : Global> globalOrDefault(default: (App) -> G): G =
        globalOrNull() ?: default(this).also { setGlobal(it) }

    inline fun <reified G : Global> setGlobal(global: G) {
        koin.declare(instance = global, allowOverride = true)
    }

    inline fun <reified G : Global, R> withGlobal(block: G.(App) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return global<G>().block(this)
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

        const val NAME = "Klyx"

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

@Composable
@ReadOnlyComposable
inline fun <reified T : Global> globalOf(): T = LocalApp.current.global()

@Composable
@ReadOnlyComposable
inline fun <reified T : Global> globalOfOrNull(): T? = LocalApp.current.globalOrNull()

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

expect val PlatformFileSystem: FileSystem

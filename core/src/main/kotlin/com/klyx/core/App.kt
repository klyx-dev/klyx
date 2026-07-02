package com.klyx.core

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.dsl.koinApplication
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.typeOf

inline fun <reified T : Any> koin() = KoinDelegate(T::class)

class KoinDelegate<T : Any>(
    private val clazz: KClass<T>
) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return GlobalContext.get().get(clazz)
    }
}

class App internal constructor(
    val application: Application,
    koinApplication: KoinApplication
) {

    @PublishedApi
    internal val koin = koinApplication.koin

    /**
     * A [CoroutineScope] tied to the lifecycle of this application instance.
     *
     * This scope is intended for launching background tasks that should persist as long as the
     * application is alive and be cancelled when the application is disposed.
     */
    val backgroundScope = BackgroundScope.default()

    /**
     * Check whether a global of the given type exists.
     */
    inline fun <reified G : Global> hasGlobal() = globalOrNull<G>() != null

    /**
     * Access the global of the given type.
     *
     * If not found in the internal registry, it falls back to the application's Koin modules.
     *
     * @throws NoGlobalException if the global does not exist.
     */
    inline fun <reified G : Global> global(): G = globalOrNull<G>()
        ?: throw NoGlobalException("no state of type ${G::class.simpleName} exists", typeOf<G>(), null)

    /**
     * Access the global of the given type.
     *
     * If not found in the internal registry, it falls back to the application's Koin modules.
     *
     * @throws NoGlobalException if the global does not exist.
     */
    fun <G : Global> global(clazz: KClass<G>): G = globalOrNull(clazz)
        ?: throw NoGlobalException("no state of type ${clazz.simpleName} exists")

    /**
     * Access the global of the given type, or null if it does not exist.
     *
     * If not found in the internal registry, it falls back to the application's Koin modules.
     */
    inline fun <reified G : Global> globalOrNull(): G? = globalOrNull(G::class)

    /**
     * Access the global of the given type, or null if it does not exist.
     *
     * If not found in the internal registry, it falls back to the application's Koin modules.
     */
    @Suppress("UNCHECKED_CAST")
    fun <G : Global> globalOrNull(clazz: KClass<G>): G? {
        val directInstance = koin.getOrNull<G>(clazz)
        if (directInstance != null) return directInstance

        val parentClasses = clazz.allSupertypes
            .mapNotNull { it.classifier as? KClass<*> }
            .filter { it != Any::class }

        for (superClazz in parentClasses) {
            val instance = koin.getOrNull<Any>(superClazz as KClass<Any>)
            if (instance != null) {
                return instance as G
            }
        }
        return GlobalContext.get().getOrNull(clazz)
    }

    /**
     * Access the global of the given type, or assign a [default] value if it doesn't exist.
     */
    inline fun <reified G : Global> globalOrDefault(default: (App) -> G): G =
        globalOrNull() ?: default(this).also { setGlobal(it) }

    /**
     * Sets the value of the global of the given type.
     * Automatically binds the instance to all of its implemented interfaces and parent types.
     */
    inline fun <reified G : Global> setGlobal(global: G) {
        val clazz = G::class

        val secondaryInterfaces = clazz.allSupertypes
            .mapNotNull { it.classifier as? KClass<*> }
            .filter { it != clazz && it != Any::class }

        koin.declare(
            instance = global,
            secondaryTypes = secondaryInterfaces,
            allowOverride = true
        )
    }

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
    @OptIn(ExperimentalContracts::class)
    inline fun <reified G : Global, R> withGlobal(block: G.(App) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return global<G>().block(this)
    }
}

/**
 * Access the global of the given type.
 *
 * If not found in the internal registry, it falls back to the application's Koin modules.
 *
 * @throws NoGlobalException if the global does not exist.
 */
@Composable
@ReadOnlyComposable
inline fun <reified T : Global> globalOf(): T = LocalApp.current.global()

/**
 * Access the global of the given type, or null if it does not exist.
 *
 * If not found in the internal registry, it falls back to the application's Koin modules.
 */
@Composable
@ReadOnlyComposable
inline fun <reified T : Global> globalOfOrNull(): T? = LocalApp.current.globalOrNull()

/**
 * CompositionLocal that provides the current [App] instance.
 */
val LocalApp = staticCompositionLocalOf<App> {
    error("LocalApp not present.")
}

@IgnorableReturnValue
fun Application.initApp(then: App.() -> Unit = {}): App {
    val koin = GlobalContext.get()
    val app: App? = koin.getOrNull()
    return (app ?: App(this, koinApplication())
        .also(koin::declare))
        .apply(then)
}

/**
 * Executes a given suspending block of code if the application is in debug mode.
 *
 * @param block The suspending block of code to execute when the debug mode is active.
 */
@OptIn(UnsafeGlobalAccess::class)
inline fun debug(crossinline block: suspend App.() -> Unit) {
    if (BuildConfig.IS_DEBUG_MODE) {
        val app = GlobalApp
        app.backgroundScope.launch { block(app) }
    }
}

@Composable
fun Debug(block: @DisallowComposableCalls suspend App.() -> Unit) {
    if (BuildConfig.IS_DEBUG_MODE) {
        val app = LocalApp.current

        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                block(app)
            }
        }
    }
}

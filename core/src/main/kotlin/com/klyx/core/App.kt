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
import kotlin.reflect.typeOf

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
    inline fun <reified G : Global> setGlobal(global: G) =
        koin.declare(global, allowOverride = true)

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
 * @throws NoGlobalException if a global for that type has not been assigned.
 */
@Composable
@ReadOnlyComposable
inline fun <reified T : Global> globalOf(): T = LocalApp.current.global()

/**
 * Access the global of the given type if a value has been assigned.
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
inline fun Debug(crossinline block: @DisallowComposableCalls suspend App.() -> Unit) {
    if (BuildConfig.IS_DEBUG_MODE) {
        val app = LocalApp.current

        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                block(app)
            }
        }
    }
}

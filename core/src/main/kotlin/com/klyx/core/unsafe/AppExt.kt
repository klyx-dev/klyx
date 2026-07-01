package com.klyx.core.unsafe

import com.klyx.core.App
import org.koin.core.context.GlobalContext
import org.koin.mp.KoinPlatformTools

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
 * @throws IllegalStateException if accessed before the App is initialized.
 */
@Suppress("UndeclaredKoinUsage")
@UnsafeGlobalAccess
val GlobalApp: App by lazy(mode = KoinPlatformTools.defaultLazyMode()) {
    (GlobalContext
        .getOrNull() ?: error("Koin is not started"))
        .getOrNull() ?: error("GlobalApp was accessed before the application was initialized.")
}

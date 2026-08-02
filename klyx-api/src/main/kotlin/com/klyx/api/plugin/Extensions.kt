@file:OptIn(DiscouragedInSuspend::class)
@file:JvmMultifileClass
@file:JvmName("KlyxPluginKt")

package com.klyx.api.plugin

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.klyx.api.DiscouragedInSuspend
import com.klyx.api.ui.ToastDuration
import com.klyx.api.ui.ToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.toastHostState
import kotlinx.coroutines.launch

/**
 * Access the [PluginInfo] for this plugin.
 */
val KlyxPlugin.info: PluginInfo by runtime()

/**
 * Access the [PluginContext] for this plugin.
 */
val KlyxPlugin.pluginContext: PluginContext by runtime()

/**
 * Access the [PluginLifecycleOwner] for this plugin.
 *
 * **Recommendation:** Use [currentLifecycleOwner] instead whenever in a suspend function.
 * Use this property only if you are in a non-suspending function.
 */
@DiscouragedInSuspend
val KlyxPlugin.lifecycleOwner: PluginLifecycleOwner by runtime()

/**
 * Access the [PluginScope] for this plugin.
 *
 * This scope is tied to the plugin's load/unload cycle. It is created when the plugin
 * is loaded and remains active until the plugin is unloaded. For coroutines that
 * should be tied to the start/stop lifecycle, use [currentLifecycleOwner] and its `lifecycleScope`.
 *
 * @see PluginScope
 */
val KlyxPlugin.pluginScope: PluginScope by runtime()

/**
 * Access the typed, per-plugin [PluginSettings] store.
 *
 * Every plugin has its own isolated settings namespace that persists across app
 * restarts. Use this to read and write configuration with a type-safe API.
 *
 * ```kotlin
 * val fontSize = settings.getInt("fontSize", 14)
 * settings.putBoolean("wordWrap", true)
 * ```
 *
 * **Security:** Do not store secrets or sensitive data (API keys, tokens, passwords,
 * credentials, personal data) in settings. Settings are stored in plain text and can
 * be read by anyone with access to the app's data directory, exported by the user from
 * the developer options, or inspected by other code running on the device. Treat every
 * value you store here as public. Use the platform's secure storage (e.g. Android
 * Keystore) for anything that must remain private.
 */
val KlyxPlugin.settings: PluginSettings by runtime()

/**
 * Wraps [content] with this plugin's [PluginContext.context] as the Compose [LocalContext].
 *
 * This makes resource-backed Compose APIs resolve against the plugin's own resources
 * instead of the klyx's:
 *
 * ```kotlin
 * fun registerScreens() {
 *     registry.register(Screen(ScreenId("my.plugin.screen")) {
 *         withResources {
 *             Icon(
 *                 painterResource(R.drawable.plugin_icon),
 *                 contentDescription = stringResource(R.string.plugin_icon_desc)
 *             )
 *         }
 *     })
 * }
 * ```
 *
 * @see PluginContext.context
 */
@SuppressLint("ComposableNaming")
@Composable
fun KlyxPlugin.withResources(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContext provides pluginContext.context) {
        content()
    }
}

/**
 * Provides access to the [ToastHostState] for this plugin.
 *
 * This property allows plugins to interact with the application's global toast notification system.
 */
val KlyxPlugin.toastHostState: ToastHostState
    get() = pluginContext.app.toastHostState

/**
 * Displays a non-blocking toast notification.
 *
 * This function is fire-and-forget; it launches a coroutine in the [KlyxPlugin.pluginScope]
 * to display the toast, so the caller's execution is not suspended.
 *
 * @param message The message text to display.
 * @param icon An optional icon to display alongside the message.
 * @param duration How long the toast should remain visible. Defaults to [ToastDuration.Short].
 */
fun KlyxPlugin.showToast(
    message: String,
    icon: ImageVector? = null,
    duration: ToastDuration = ToastDuration.Short
) {
    pluginScope.launch {
        toastHostState.showToast(message, icon, duration)
    }
}

/**
 * Displays a non-blocking failure toast notification.
 *
 * This function is fire-and-forget; it launches a coroutine in the [KlyxPlugin.pluginScope]
 * to display the toast, so the caller's execution is not suspended.
 *
 * @param message The failure message text to display.
 * @param icon An optional icon to display. Defaults to an error icon.
 */
fun KlyxPlugin.showFailureToast(
    message: String,
    icon: ImageVector? = null
) {
    pluginScope.launch {
        toastHostState.showFailureToast(message, icon)
    }
}

/**
 * Displays a non-blocking failure toast notification for a [Throwable].
 *
 * This function is fire-and-forget; it launches a coroutine in the [KlyxPlugin.pluginScope]
 * to display the toast, so the caller's execution is not suspended.
 *
 * @param throwable The exception or error to report.
 */
fun KlyxPlugin.showFailureToast(throwable: Throwable) {
    pluginScope.launch {
        toastHostState.showFailureToast(throwable)
    }
}

package com.klyx.api

import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.pluginService
import com.klyx.core.LocalApp

/**
 * Handles navigation within the Klyx application.
 *
 * This service allows plugins to trigger navigation to different screens or parts of the app,
 * including built-in destinations like Home and Settings, as well as custom screens.
 *
 * ### Example
 * ```kotlin
 * val navigator: Navigator by plugin()
 *
 * fun goToSettings() {
 *     navigator.navigateTo(NavDestination.Settings)
 * }
 * ```
 */
interface Navigator : PluginService {

    /**
     * Navigates to the specified [destination].
     */
    fun navigateTo(destination: NavDestination)

    /**
     * Navigates back to the previous screen in the stack.
     */
    fun navigateBack()
}

/**
 * CompositionLocal that provides the current [Navigator] instance.
 *
 * By default, it retrieves the [Navigator] from the current [LocalApp].
 */
val LocalNavigator = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.pluginService(Navigator::class)
}

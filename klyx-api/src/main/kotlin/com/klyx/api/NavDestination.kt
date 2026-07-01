package com.klyx.api

import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistry

/**
 * Represents a destination that can be navigated to within the app.
 *
 * This sealed class defines both core application destinations and a way to navigate
 * to custom screens provided by plugins.
 */
sealed class NavDestination {

    /** The main home screen of the application. */
    data object Home : NavDestination()

    /** The application settings screen. */
    data object Settings : NavDestination()

    /** The integrated terminal screen. */
    data object Terminal : NavDestination()

    /**
     * A custom destination defined by its [ScreenId].
     *
     * Plugins use this to navigate to screens they have registered via the [ScreenRegistry].
     */
    data class Custom(val id: ScreenId) : NavDestination()
}

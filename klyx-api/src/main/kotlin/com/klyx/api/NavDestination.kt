package com.klyx.api

import com.klyx.api.ui.ScreenId

sealed class NavDestination {
    data object Home : NavDestination()
    data object Settings : NavDestination()
    data object Terminal : NavDestination()
    data class Custom(val id: ScreenId) : NavDestination()
}

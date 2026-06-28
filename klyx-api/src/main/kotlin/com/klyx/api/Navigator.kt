package com.klyx.api

import com.klyx.core.Global

interface Navigator : Global {
    fun navigateTo(destination: NavDestination)
    fun navigateBack()
}

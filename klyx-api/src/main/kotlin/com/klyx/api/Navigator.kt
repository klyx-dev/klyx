package com.klyx.api

import com.klyx.api.plugin.PluginService

interface Navigator : PluginService {
    fun navigateTo(destination: NavDestination)
    fun navigateBack()
}

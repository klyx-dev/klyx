package com.klyx.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route

@Serializable
data object Home : Route

@Serializable
data object Settings : Route

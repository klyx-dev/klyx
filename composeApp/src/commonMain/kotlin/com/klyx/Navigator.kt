package com.klyx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

interface Navigator {
    fun navigateTo(route: AppRoute)
    fun navigateBack()
}

fun Navigator(
    onNavigateTo: (route: AppRoute) -> Unit,
    onNavigateBack: () -> Unit
) = object : Navigator {
    override fun navigateTo(route: AppRoute) {
        onNavigateTo(route)
    }

    override fun navigateBack() {
        onNavigateBack()
    }
}

@Composable
fun ProvideNavigator(
    onNavigateTo: (route: AppRoute) -> Unit,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavigator provides Navigator(onNavigateTo, onNavigateBack), content = content)
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No value provided for 'LocalNavigator'. Did you forget to wrap your content in ProvideNavigator?")
}

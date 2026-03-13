package com.klyx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {

    val currentTopLevelRoute: NavKey get() = state.topLevelRoute

    val currentBackStack: List<NavKey>
        get() = state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")

    val currentRoute: NavKey get() = currentBackStack.last()

    fun navigateTo(route: NavKey) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun navigateBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    companion object {
        const val ANIMATION_DURATION = 500
    }
}

interface NavigationScope {
    val navigator: Navigator
    val navigationState: NavigationState
}

@Composable
fun NavigationScope(content: @Composable NavigationScope.() -> Unit) {
    val navigationState = rememberNavigationState(startRoute = Route.Main)
    val navigator = remember { Navigator(navigationState) }

    val scope = remember(navigator, navigationState) {
        object : NavigationScope {
            override val navigator = navigator
            override val navigationState = navigationState
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        scope.content()
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No value provided for 'LocalNavigator'.")
}

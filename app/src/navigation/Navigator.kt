package com.klyx.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No navigator provided")
}

class Navigator(val backStack: MutableList<Route>) {

    inline val currentRoute get() = backStack.last()

    fun navigateTo(route: Route) {
        if (backStack.lastOrNull() === route) return
        backStack.add(route)
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeLast()
    }

    fun replaceCurrentRouteWith(route: Route) {
        backStack.removeLastOrNull()
        backStack.add(route)
    }
}

@Composable
fun rememberNavigator(initialRoute: Route = Home): Navigator {
    val backStack: MutableList<Route> = rememberSerializable(
        serializer = SnapshotStateListSerializer(),
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    @OptIn(ExperimentalSerializationApi::class)
                    subclassesOfSealed<Route>()
                }
            }
        }
    ) {
        mutableStateListOf(initialRoute)
    }

    return remember(backStack) { Navigator(backStack) }
}

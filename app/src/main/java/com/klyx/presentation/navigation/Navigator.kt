package com.klyx.presentation.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.savedstate.serialization.SavedStateConfiguration
import com.klyx.util.thenIf
import kotlinx.serialization.PolymorphicSerializer

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No value provided for 'LocalNavigator'.")
}

@Stable
class Navigator(
    private val backStack: NavBackStack<Screen>
) : MutableList<Screen> by backStack, StateObject by backStack {

    inline val currentScreen get() = last()

    fun navigateTo(screen: Screen) {
        if (backStack.lastOrNull() === screen) return
        backStack.add(screen)
    }

    fun navigateBack() {
        backStack.removeLastOrNull()
    }

    fun replaceCurrentScreenWith(screen: Screen) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(lastIndex)
        }
        navigateTo(screen)
    }
}

@Composable
context(navigator: Navigator)
fun ((Screen) -> NavEntry<Screen>).toEntries(): List<NavEntry<Screen>> {
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        remember { roundedNavEntryDecorator() }
    )
    val decoratedEntries = rememberDecoratedNavEntries(
        backStack = navigator,
        entryDecorators = decorators,
        entryProvider = this
    )
    return decoratedEntries
}

context(navigator: Navigator)
private fun roundedNavEntryDecorator() = NavEntryDecorator<Screen> { entry ->
    val transition = LocalNavAnimatedContentScope.current.transition

    val cornerRadius by transition.animateFloat(
        label = "cornerRadius",
        transitionSpec = { tween(durationMillis = 350, easing = LinearEasing) }
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> 32f
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> 40f
        }
    }

    val scrimAlpha by transition.animateFloat(
        label = "scrim",
        transitionSpec = { tween(350) }
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> 0.4f
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> 0f
        }
    }

    Box(
        modifier = Modifier.thenIf(cornerRadius != 0f) {
            graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clip(RoundedCornerShape(cornerRadius.dp))
        }
    ) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            entry.Content()

            if (scrimAlpha > 0f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                )
            }
        }
    }
}

@Composable
fun rememberNavigator(startScreen: Screen = Screen.Home): Navigator {
    val backStack = rememberNavBackStack(configuration = Screen.config(), startScreen)
    return remember(backStack) { Navigator(backStack = backStack) }
}

@Composable
inline fun <reified T : NavKey> rememberNavBackStack(
    configuration: SavedStateConfiguration,
    vararg elements: T,
): NavBackStack<T> {
    require(configuration.serializersModule != SavedStateConfiguration.DEFAULT.serializersModule) {
        "You must pass a `SavedStateConfiguration.serializersModule` configured to handle " +
                "`${T::class.simpleName}` open polymorphism. Define it with: `polymorphic(${T::class.simpleName}::class) { ... }`"
    }
    return rememberSerializable(
        configuration = configuration,
        serializer = NavBackStackSerializer(PolymorphicSerializer(T::class)),
    ) {
        NavBackStack(*elements)
    }
}


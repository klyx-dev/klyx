package com.klyx.core.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import com.klyx.core.ui.motion.EmphasizeEasing
import com.klyx.core.ui.motion.EmphasizedAccelerate
import com.klyx.core.ui.motion.EmphasizedDecelerate
import com.klyx.core.ui.motion.materialSharedAxisXIn
import com.klyx.core.ui.motion.materialSharedAxisXOut
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType

const val DURATION_ENTER = 400
const val DURATION_EXIT = 200
const val initialOffset = 0.10f

private fun <T> enterTween() = tween<T>(durationMillis = DURATION_ENTER, easing = EmphasizeEasing)

private fun <T> exitTween() = tween<T>(durationMillis = DURATION_ENTER, easing = EmphasizeEasing)

private val fadeSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
private val fadeTween = tween<Float>(durationMillis = DURATION_EXIT)

private val fadeSpec = fadeTween

fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    usePredictiveBack: Boolean = true,
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    if (usePredictiveBack) {
        animatedComposablePredictiveBack(route, arguments, deepLinks, content)
    } else {
        animatedComposableLegacy(route, arguments, deepLinks, content)
    }
}

inline fun <reified T : Any> NavGraphBuilder.animatedComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    usePredictiveBack: Boolean = true,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    if (usePredictiveBack) {
        animatedComposablePredictiveBack<T>(typeMap, deepLinks, content)
    } else {
        animatedComposableLegacy<T>(typeMap, deepLinks, content)
    }
}

inline fun <reified T : Any> NavGraphBuilder.animatedComposablePredictiveBack(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = { materialSharedAxisXIn(initialOffsetX = { (it * 0.15f).toInt() }) },
        exitTransition = {
            materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() })
        },
        popEnterTransition = {
            scaleIn(
                animationSpec = tween(durationMillis = 350, easing = EmphasizedDecelerate),
                initialScale = 0.9f,
            ) + materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() })
        },
        popExitTransition = {
            materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() }) +
                    scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(durationMillis = 350, easing = EmphasizedAccelerate),
                    )
        },
        content = content
    )
}

inline fun <reified T : Any> NavGraphBuilder.animatedComposableLegacy(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = {
            materialSharedAxisXIn(initialOffsetX = { (it * initialOffset).toInt() })
        },
        exitTransition = {
            materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() })
        },
        popEnterTransition = {
            materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() })
        },
        popExitTransition = {
            materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() })
        },
        content = content
    )
}

fun NavGraphBuilder.animatedComposablePredictiveBack(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = { materialSharedAxisXIn(initialOffsetX = { (it * 0.15f).toInt() }) },
    exitTransition = {
        materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() })
    },
    popEnterTransition = {
        scaleIn(
            animationSpec = tween(durationMillis = 350, easing = EmphasizedDecelerate),
            initialScale = 0.9f,
        ) + materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() })
    },
    popExitTransition = {
        materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() }) +
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(durationMillis = 350, easing = EmphasizedAccelerate),
                )
    },
    content = content,
)

fun NavGraphBuilder.animatedComposableLegacy(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = {
        materialSharedAxisXIn(initialOffsetX = { (it * initialOffset).toInt() })
    },
    exitTransition = {
        materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() })
    },
    popEnterTransition = {
        materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() })
    },
    popExitTransition = {
        materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() })
    },
    content = content,
)

fun NavGraphBuilder.animatedComposableVariant(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = {
        slideInHorizontally(enterTween(), initialOffsetX = { (it * initialOffset).toInt() }) +
                fadeIn(fadeSpec)
    },
    exitTransition = { fadeOut(fadeSpec) },
    popEnterTransition = { fadeIn(fadeSpec) },
    popExitTransition = {
        slideOutHorizontally(exitTween(), targetOffsetX = { (it * initialOffset).toInt() }) +
                fadeOut(fadeSpec)
    },
    content = content,
)

val springSpec = spring(stiffness = Spring.StiffnessMedium, visibilityThreshold = IntOffset.VisibilityThreshold)

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.slideInVerticallyComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = {
        slideInVertically(initialOffsetY = { it }, animationSpec = enterTween()) + fadeIn()
    },
    exitTransition = { slideOutVertically() },
    popEnterTransition = { slideInVertically() },
    popExitTransition = {
        slideOutVertically(targetOffsetY = { it }, animationSpec = enterTween()) + fadeOut()
    },
    content = content,
)


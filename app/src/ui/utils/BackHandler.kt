package com.klyx.ui.utils

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val navigationState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None
    )

    NavigationBackHandler(
        state = navigationState,
        isBackEnabled = enabled,
        onBackCompleted = onBack
    )
}

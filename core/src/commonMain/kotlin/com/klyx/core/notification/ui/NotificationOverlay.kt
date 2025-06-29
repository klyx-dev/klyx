package com.klyx.core.notification.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klyx.core.notification.NotificationManager
import org.koin.compose.koinInject

@Composable
fun NotificationOverlay(
    manager: NotificationManager = koinInject()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
            userScrollEnabled = false,
            reverseLayout = true
        ) {
            items(manager.notifications) { notification ->
                NotificationCard(
                    modifier = Modifier.animateItem(),
                    notification = notification,
                    onDismissRequest = { manager.dismiss(notification.id) }
                )
            }
        }
    }
}

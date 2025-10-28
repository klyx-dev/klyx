package com.klyx.core.notification.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.klyx.core.notification.LocalNotificationManager
import com.klyx.core.notification.NotificationManager
import com.klyx.core.notification.Toast

@Composable
fun NotificationOverlay(manager: NotificationManager = LocalNotificationManager.current) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .imePadding()
            .systemBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
            userScrollEnabled = false,
            reverseLayout = true
        ) {
            items(manager.toasts) { toast ->
                ToastCard(
                    modifier = Modifier.animateItem(),
                    toast = toast
                )
            }
        }
    }
}

@Composable
private fun ToastCard(
    toast: Toast,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalWindowInfo.current.containerSize.width

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.widthIn(max = with(LocalDensity.current) { (screenWidth * 0.7f).toDp() })
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = toast.message)
        }
    }
}

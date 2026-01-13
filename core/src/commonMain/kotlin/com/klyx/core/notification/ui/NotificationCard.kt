package com.klyx.core.notification.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klyx.core.notification.Notification
import com.klyx.core.notification.NotificationType
import com.klyx.core.theme.harmonizeWithPrimary
import com.klyx.icons.Close
import com.klyx.icons.Icons

@Composable
internal fun NotificationCard(
    notification: Notification,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenSize = LocalWindowInfo.current.containerSize
    val colorScheme = MaterialTheme.colorScheme

    val bgColor = when (notification.type) {
        NotificationType.Info -> Color(0xFF2196F3).harmonizeWithPrimary()
        NotificationType.Success -> Color(0xFF4CAF50).harmonizeWithPrimary()
        NotificationType.Warning -> Color(0xFFFFC107).harmonizeWithPrimary()
        NotificationType.Error -> colorScheme.errorContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = bgColor,
            contentColor = contentColorFor(bgColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.widthIn(max = with(density) { (screenSize.width * 0.6f).toDp() }),
        onClick = { notification.onClick?.invoke(notification) }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = FontWeight.Bold
                    )

                    if (notification.canUserDismiss) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                Icons.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(text = notification.message)
            }
        }
    }
}

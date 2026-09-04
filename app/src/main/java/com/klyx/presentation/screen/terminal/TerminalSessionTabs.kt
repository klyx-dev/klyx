package com.klyx.presentation.screen.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.api.data.terminal.TerminalSessionEntry
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.i18n.strings
import kotlinx.collections.immutable.ImmutableList
import kotlin.uuid.Uuid

@Composable
fun TerminalSessionTabs(
    sessions: ImmutableList<TerminalSessionEntry>,
    activeSessionId: Uuid?,
    onSelectSession: (Uuid) -> Unit,
    onCloseSession: (Uuid) -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeSessionId, sessions.size) {
        val activeIndex = sessions.indexOfFirst { it.id == activeSessionId }
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(
                    items = sessions,
                    key = { _, entry -> entry.id.toString() }
                ) { index, entry ->
                    val isActive = entry.id == activeSessionId
                    val isOnlySession = sessions.size == 1

                    SessionChip(
                        index = index + 1,
                        entry = entry,
                        isActive = isActive,
                        isOnlySession = isOnlySession,
                        onClick = { onSelectSession(entry.id) },
                        onClose = { onCloseSession(entry.id) }
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            IconButton(
                onClick = onNewSession,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = strings.newSession,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SessionChip(
    index: Int,
    entry: TerminalSessionEntry,
    isActive: Boolean,
    isOnlySession: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chip_fg"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = entry.session.title?.ifBlank { "Session $index" } ?: "Session $index",
                fontSize = 12.sp,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .widthIn(max = 100.dp)
                    .basicMarquee()
            )

            if (!isOnlySession) {
                Spacer(Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(LongPress)
                            onClose()
                        },
                    contentAlignment = Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.closeSession,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

package com.klyx.ui.component.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.klyx.icons.Close
import com.klyx.icons.Icons
import com.klyx.tab.FileTab
import com.klyx.tab.Tab

private data class Gap(val leftPx: Float, val rightPx: Float)

@Deprecated(
    "Use EditorTabRow instead",
    ReplaceWith("EditorTabRow", "com.klyx.ui.component.editor.EditorTabRow")
)
@Composable
fun EditorTabBar(
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onClose: (Int) -> Unit = {},
    isDirty: (Int) -> Boolean = { false },
) {
    val dirty by rememberUpdatedState(isDirty)
    val colorScheme = MaterialTheme.colorScheme

    var containerLeftInRoot by remember { mutableStateOf(0f) }
    var gap by remember { mutableStateOf<Gap?>(null) }

    val borderColor = colorScheme.outline.copy(alpha = 0.4f)
    val strokeWidth = with(LocalDensity.current) { Dp.Hairline.toPx() }

    Box(
        modifier = modifier
            .zIndex(5f)
            .background(colorScheme.surfaceColorAtElevation(1.dp))
            .onGloballyPositioned { coords ->
                containerLeftInRoot = coords.positionInRoot().x
            }
            .drawBehind {
                val w = size.width
                val h = size.height

                drawLine(borderColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth)
                drawLine(borderColor, Offset(0f, 0f), Offset(0f, h), strokeWidth)
                drawLine(borderColor, Offset(w, 0f), Offset(w, h), strokeWidth)

                val g = gap
                if (g == null) {
                    drawLine(borderColor, Offset(0f, h), Offset(w, h), strokeWidth)
                } else {
                    val left = g.leftPx.coerceIn(0f, w)
                    val right = g.rightPx.coerceIn(0f, w)
                    if (left > 0f) drawLine(borderColor, Offset(0f, h), Offset(left, h), strokeWidth)
                    if (right < w) drawLine(borderColor, Offset(right, h), Offset(w, h), strokeWidth)
                }
            }
            .fillMaxWidth()
    ) {
        LazyRow {
            itemsIndexed(tabs) { index, tab ->
                EditorTab(
                    tab = tab,
                    isSelected = index == selectedTab,
                    isDirty = dirty(index),
                    onClick = { onTabSelected(index) },
                    onClose = { onClose(index) },
                    onReportBounds = { leftInRoot, rightInRoot ->
                        gap = Gap(
                            leftPx = leftInRoot - containerLeftInRoot,
                            rightPx = rightInRoot - containerLeftInRoot
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTab(
    tab: Tab,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onReportBounds: (leftInRoot: Float, rightInRoot: Float) -> Unit = { _, _ -> }
) {
    val recomposeScope = currentRecomposeScope

    LaunchedEffect(tab) {
        if (tab is FileTab) {
            val file = tab.file
            if (!file.exists) return@LaunchedEffect

//            file.watchExistence(newSingleThreadContext("KxFile")) {
//                recomposeScope.invalidate()
//            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    val selectedBg = colorScheme.surfaceColorAtElevation(4.dp)
    val unselectedBg = colorScheme.surfaceColorAtElevation(1.dp)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedBg else unselectedBg,
        label = "TabBackgroundColor"
    )

    val textColor = if (isSelected) contentColorFor(selectedBg) else contentColorFor(unselectedBg)

    val strokeWidth = with(LocalDensity.current) { Dp.Hairline.toPx() }
    val borderColor = colorScheme.outline.copy(alpha = 0.4f)

    Row(
        modifier = modifier
            .onGloballyPositioned { coords ->
                if (isSelected) {
                    val pos = coords.positionInRoot()
                    val left = pos.x
                    val right = left + coords.size.width
                    onReportBounds(left, right)
                }
            }
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .zIndex(if (isSelected) 10f else 0f)
            .drawBehind {
                val w = size.width
                val h = size.height

                drawLine(borderColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth)
                drawLine(borderColor, Offset(0f, 0f), Offset(0f, h), strokeWidth)
                drawLine(borderColor, Offset(w, 0f), Offset(w, h), strokeWidth)
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDirty) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

//        if (tab is FileTab) {
//            Icon(
//                imageVector = Icons.Default.Description,
//                contentDescription = null,
//                tint = textColor.copy(alpha = 0.8f),
//                modifier = Modifier.size(16.dp)
//            )
//            Spacer(modifier = Modifier.width(6.dp))
//        }

        val isFileMissing = tab is FileTab && !tab.file.exists && tab.file.path != "/untitled"
        Text(
            text = tab.name,
            color = if (isFileMissing) colorScheme.error else textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            textDecoration = if (isFileMissing) TextDecoration.LineThrough else TextDecoration.None
        )

        if (tab is FileTab && tab.file.path != "/untitled" && !tab.isInternal) {
//            Spacer(modifier = Modifier.width(6.dp))
//            Text(
//                text = tab.file.path
//                    .replaceFirst(Environment.HomeDir, "~")
//                    .substringBeforeLast("/")
//                    .let { if (it.length > 30) it.take(30) + "…" else it },
//                color = textColor.copy(alpha = 0.6f),
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                style = MaterialTheme.typography.bodySmall
//            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TooltipBox(
            state = rememberTooltipState(isPersistent = true),
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val color = LocalContentColor.current

                    Text(buildAnnotatedString {
                        append("Close Tab  ")
                        withStyle(SpanStyle(color = color.copy(alpha = 0.7f))) {
                            append("Ctrl-W")
                        }
                    })
                }
            }
        ) {
            Icon(
                imageVector = Icons.Close,
                contentDescription = "Close tab",
                tint = textColor.copy(alpha = if (isSelected) 1f else 0.5f),
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(2.dp)
            )
        }
    }
}


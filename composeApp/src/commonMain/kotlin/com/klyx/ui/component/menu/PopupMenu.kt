package com.klyx.ui.component.menu

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.klyx.menu.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PopupMenu(
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    position: IntOffset = IntOffset.Zero,
    dismissOnClickOutside: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onItemClick: (Int, MenuItem) -> Unit = { _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme

    Popup(
        offset = position,
        alignment = Alignment.TopStart,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            dismissOnClickOutside = dismissOnClickOutside,
            dismissOnBackPress = true
        )
    ) {
        val scope = rememberCoroutineScope()

        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurface
            ),
            modifier = modifier
                .wrapContentWidth(unbounded = true)
                .width(IntrinsicSize.Max),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEachIndexed { index, item ->
                    if (item.isDivider) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onItemClick(index, item)
                                    scope.launch(Dispatchers.Main.immediate) { item.onClick() }
                                    if (item.dismissRequestOnClicked) onDismissRequest()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.title,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                ),
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (item.shortcuts.isNotEmpty()) {
                                Text(
                                    text = item.shortcuts.joinToString(" ") { it.toString() },
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .alpha(0.7f)
                                        .basicMarquee(),
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

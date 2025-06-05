package com.klyx.ui.component.menu

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

data class MenuItem(
    val title: String = "",
    val shortcutKey: String? = null,
    val isDivider: Boolean = title.isEmpty(),
    val onClick: () -> Unit = {}
)

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
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurface
            ),
            modifier = modifier,//.border(0.4.dp, colorScheme.outline, shape = RoundedCornerShape(4.dp)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .width(items.maxOf { (it.title.length + (it.shortcutKey?.length ?: 0)) * 8.5f }.dp)
            ) {
                items.forEachIndexed { index, item ->
                    if (item.isDivider) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onItemClick(index, item)
                                    item.onClick()
                                    onDismissRequest()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (item.shortcutKey != null) {
                                Text(
                                    text = item.shortcutKey,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .alpha(0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

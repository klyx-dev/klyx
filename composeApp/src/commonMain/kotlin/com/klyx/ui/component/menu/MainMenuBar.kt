package com.klyx.ui.component.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
expect fun MainMenuBar(modifier: Modifier = Modifier)

@Composable
internal fun MenuRow(
    menuItems: List<String>,
    selectedMenuIndex: Int,
    onMenuItemSelected: (Int, String) -> Unit,
    onMenuItemPositioned: (String, IntOffset) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        menuItems.forEachIndexed { index, title ->
            val isSelected = index == selectedMenuIndex

            val textColor = if (isSelected) {
                colorScheme.primary
            } else {
                colorScheme.onSurface
            }

            Text(
                text = title,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onMenuItemSelected(index, title) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        val position = layoutCoordinates.localToWindow(Offset.Zero)
                        onMenuItemPositioned(title, IntOffset(position.x.toInt() - 10, position.y.toInt() - 10))
                    }
            )
        }
    }
}

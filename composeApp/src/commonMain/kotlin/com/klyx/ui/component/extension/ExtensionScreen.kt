package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.klyx.core.extension.ExtensionFilter
import com.klyx.spacedName

@Composable
expect fun ExtensionScreen(modifier: Modifier = Modifier)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExtensionFilterBar(
    onFilterChange: (ExtensionFilter) -> Unit = {}
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndex) {
        onFilterChange(ExtensionFilter.entries[selectedIndex])
    }

    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement
    ) {
        ExtensionFilter.entries.fastForEachIndexed { index, filter ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    selectedIndex = index
                    //onFilterChange(filter)
                },
                modifier = Modifier
                    .semantics { role = Role.RadioButton }
                    .weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ExtensionFilter.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(filter.spacedName)
            }
        }
    }
}

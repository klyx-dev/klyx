package com.klyx.ui.component.extension

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.klyx.core.extension.ExtensionFilter
import com.klyx.spacedName

@Composable
fun ExtensionFilterButtons(
    onSelect: (ExtensionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = ExtensionFilter.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, filter ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = {
                    selectedIndex = index
                    onSelect(filter)
                },
                selected = index == selectedIndex,
                label = { Text(filter.spacedName) }
            )
        }
    }
}

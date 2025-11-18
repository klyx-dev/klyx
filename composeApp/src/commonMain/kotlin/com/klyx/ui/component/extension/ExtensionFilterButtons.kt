package com.klyx.ui.component.extension

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.core.extension.ExtensionFilter
import com.klyx.spacedName

@Composable
fun ExtensionFilterButtons(
    selectedFilter: ExtensionFilter,
    modifier: Modifier = Modifier,
    onSelect: (ExtensionFilter) -> Unit,
) {
    val options = ExtensionFilter.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, filter ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onSelect(filter) },
                selected = filter == selectedFilter,
                label = { Text(filter.spacedName) }
            )
        }
    }
}

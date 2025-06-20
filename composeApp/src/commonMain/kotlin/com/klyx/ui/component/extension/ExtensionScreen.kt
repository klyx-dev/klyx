package com.klyx.ui.component.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.core.extension.ExtensionFilter

@Composable
expect fun ExtensionScreen(modifier: Modifier = Modifier)

@Composable
internal expect fun ExtensionFilterBar(onFilterChange: (ExtensionFilter) -> Unit = {})

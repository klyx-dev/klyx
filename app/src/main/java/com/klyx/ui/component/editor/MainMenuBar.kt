package com.klyx.ui.component.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuBar(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surfaceColorAtElevation(5.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Text("Main Menu", color = colorScheme.onSurface)
    }
}

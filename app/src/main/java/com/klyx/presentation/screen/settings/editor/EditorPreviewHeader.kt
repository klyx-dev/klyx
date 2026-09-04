package com.klyx.presentation.screen.settings.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.i18n.strings
import com.klyx.presentation.components.CodeEditorDemo
import com.klyx.presentation.screen.settings.components.SettingsSubsectionHeader

@Composable
fun EditorPreviewHeader(
    localFontSize: Float,
    localFontFamily: FontFamily,
    localWaveWidth: Float,
    localWaveLength: Float,
    localWaveAmplitude: Float
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, bottom = 12.dp, end = 6.dp, top = 8.dp)
    ) {
        Column {
            SettingsSubsectionHeader(strings.previewSection)

            CodeEditorDemo(
                fontSize = localFontSize.sp,
                fontFamily = localFontFamily,
                indicatorWaveWidth = localWaveWidth,
                indicatorWaveLength = localWaveLength,
                indicatorWaveAmplitude = localWaveAmplitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

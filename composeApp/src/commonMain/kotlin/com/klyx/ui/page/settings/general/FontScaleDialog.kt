package com.klyx.ui.page.settings.general

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.klyx.core.settings.AppSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.res.Res.string
import com.klyx.res.font_scale
import com.klyx.res.font_scale_desc
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontScaleDialog(
    settings: AppSettings,
    onDismissRequest: () -> Unit
) {
    var fontScale by rememberSaveable { mutableFloatStateOf(settings.fontScale) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton {
                settings.update { it.copy(fontScale = fontScale) }
                onDismissRequest()
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        icon = { Icon(Icons.Outlined.FormatSize, contentDescription = null) },
        title = { Text(stringResource(string.font_scale), textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(string.font_scale_desc),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                val percentage by remember {
                    derivedStateOf {
                        "${(fontScale * 100).toInt()}%"
                    }
                }

                Text(percentage)

                val interactionSource = remember { MutableInteractionSource() }

                Slider(
                    value = fontScale,
                    valueRange = 0.5f..2.5f,
                    onValueChange = { value ->
                        fontScale = value
                        //settings.update { it.copy(fontScale = value) }
                    },
                    interactionSource = interactionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            thumbSize = DpSize(4.dp, 30.dp)
                        )
                    }
                )
            }
        }
    )
}

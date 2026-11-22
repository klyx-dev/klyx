package com.klyx.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.theme.FontOption
import com.klyx.ui.theme.KxColors
import com.klyx.ui.theme.KxTypography
import com.klyx.ui.theme.kxShape

@Composable
internal fun FontSelectorSection(
    currentFont: String,
    onFontSelected: (String) -> Unit,
) {
    val colors = KxColors.current
    val current = remember(currentFont) { FontOption.fromId(currentFont) }
    var expanded by remember { mutableStateOf(false) }
    val shape = kxShape()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("font", style = KxTypography.labelSmall, color = colors.textMuted)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.border.copy(alpha = 0.75f)),
            )
        }

        KxButton(
            onClick = { expanded = !expanded },
            variant = KxButtonVariant.Outlined,
            borderColor = if (expanded) colors.accent else colors.border,
            backgroundColor = if (expanded) colors.surfaceVariant.copy(alpha = 0.8f) else colors.surface.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(current.label, style = KxTypography.label, color = colors.textPrimary)
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = KxTypography.label,
                    color = if (expanded) colors.accent else colors.textMuted,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.textMuted.copy(alpha = 0.035f), shape)
                    .border(1.dp, colors.textMuted.copy(alpha = 0.20f), shape)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FontOption.entries.forEach { option ->
                    val selected = option == current
                    val optionShape = kxShape()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(optionShape)
                            .background(
                                if (selected) colors.accent.copy(alpha = 0.10f)
                                else colors.surface.copy(alpha = 0.20f),
                                optionShape,
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) colors.accent.copy(alpha = 0.45f)
                                else colors.border.copy(alpha = 0.35f),
                                shape = optionShape,
                            )
                            .clickable {
                                onFontSelected(option.id)
                                expanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = KxTypography.label,
                            color = if (selected) colors.accent else colors.textPrimary,
                        )
                        if (selected) {
                            Text("✓", style = KxTypography.label, color = colors.accent)
                        } else {
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                }
            }
        }
    }
}

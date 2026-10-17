package com.klyx.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.Thumb
import com.composeunstyled.Track
import com.composeunstyled.UnstyledSwitch
import com.composeunstyled.theme.Theme
import com.klyx.navigation.LocalNavigator
import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
import com.klyx.runtime.workspace.EditorTabScope
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.LocalCornerRadius
import com.klyx.ui.accent
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.headline
import com.klyx.ui.kxShape
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.onAccent
import com.klyx.ui.surfaceVariant
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.typography
import kotlin.math.roundToInt
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$ ", style = Theme[typography][headline], color = Theme[colors][textMuted])
                Text("settings", style = Theme[typography][headline])
            }
            KxButton(
                onClick = navigator::navigateBack,
                variant = KxButtonVariant.Ghost,
                bracketed = true,
                borderColor = Theme[colors][textMuted],
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("x", style = Theme[typography][label], color = Theme[colors][textMuted])
            }
        }

        HorizontalDivider(color = Theme[colors][border])

        SectionHeader("APPEARANCE")

        RoundedUiRow(modifier = Modifier.fillMaxWidth())

        CornerRadiusRow(modifier = Modifier.fillMaxWidth())

        SectionHeader("EDITOR TABS")

        TabScopeRow(modifier = Modifier.fillMaxWidth())

        SectionHeader("TOP BAR")

        UndoRedoRow(modifier = Modifier.fillMaxWidth())

        SectionHeader("EDITOR")

        AutoSaveRow(modifier = Modifier.fillMaxWidth())

        SectionHeader("LANGUAGE SERVER")

        InlayHintsRow(modifier = Modifier.fillMaxWidth())

        if (currentOperatingSystem == OperatingSystem.Android) {
            SectionHeader("TERMUX")

            TermuxSetupSection(modifier = Modifier.fillMaxWidth())
        }

        SectionHeader("LANGUAGE SERVERS")

        LspServersSection(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("──", style = Theme[typography][labelSmall], color = Theme[colors][textMuted])
        Text(title, style = Theme[typography][labelSmall], color = Theme[colors][textMuted])
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Theme[colors][textMuted]),
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = Theme[typography][label],
                color = Theme[colors][textPrimary],
            )
            Text(
                text = subtitle,
                style = Theme[typography][bodySmall],
                color = Theme[colors][textMuted],
            )
        }
        SettingSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            accessibilityLabel = accessibilityLabel,
        )
    }
}

@Composable
private fun SettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    val trackRadius = if (LocalCornerRadius.current > 0.dp) 13.dp else 0.dp
    val thumbRadius = if (LocalCornerRadius.current > 0.dp) 9.dp else 0.dp
    val trackShape = kxShape(trackRadius)
    val thumbShape = kxShape(thumbRadius)

    UnstyledSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        indication = LocalIndication.current,
        accessibilityLabel = accessibilityLabel,
        modifier = modifier,
    ) {
        Track(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(trackShape)
                .background(
                    if (checked) Theme[colors][accent] else Theme[colors][surfaceVariant],
                    trackShape,
                )
                .border(1.dp, Theme[colors][border], trackShape),
        ) {
            Thumb(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(thumbShape)
                    .background(
                        if (checked) Theme[colors][onAccent] else Theme[colors][textMuted],
                        thumbShape,
                    )
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun RoundedUiRow(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val roundedUi by workspace.roundedUi.collectAsState()

    SettingRow(
        title = "rounded ui",
        subtitle = "enable rounded corners across the interface",
        checked = roundedUi,
        onCheckedChange = workspace::setRoundedUi,
        accessibilityLabel = "Enable rounded UI",
        modifier = modifier,
    )
}

@Composable
private fun CornerRadiusRow(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val roundedUi by workspace.roundedUi.collectAsState()
    val radius by workspace.cornerRadius.collectAsState()

    AnimatedVisibility(
        visible = roundedUi,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "corner radius",
                        style = Theme[typography][label],
                        color = Theme[colors][textPrimary],
                    )
                    Text(
                        text = "adjust corner radius ($radius dp)",
                        style = Theme[typography][bodySmall],
                        color = Theme[colors][textMuted],
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KxButton(
                        onClick = { workspace.setCornerRadius(radius - 2) },
                        enabled = radius > 2,
                        variant = KxButtonVariant.Outlined,
                        bracketed = true,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("-", style = Theme[typography][label])
                    }
                    Text(
                        text = "$radius dp",
                        style = Theme[typography][label],
                        color = Theme[colors][accent],
                    )
                    KxButton(
                        onClick = { workspace.setCornerRadius(radius + 2) },
                        enabled = radius < 24,
                        variant = KxButtonVariant.Outlined,
                        bracketed = true,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("+", style = Theme[typography][label])
                    }
                }
            }

            Slider(
                value = radius.toFloat(),
                onValueChange = { workspace.setCornerRadius(it.roundToInt()) },
                valueRange = 2f..24f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Theme[colors][accent],
                    activeTrackColor = Theme[colors][accent],
                    inactiveTrackColor = Theme[colors][surfaceVariant],
                ),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(4, 8, 12, 16, 20).forEach { preset ->
                    KxButton(
                        onClick = { workspace.setCornerRadius(preset) },
                        variant = if (radius == preset) KxButtonVariant.Filled else KxButtonVariant.Ghost,
                        bracketed = true,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("${preset}dp", style = Theme[typography][labelSmall])
                    }
                }
            }
        }
    }
}

@Composable
private fun TabScopeRow(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val tabScope by workspace.tabScope.collectAsState()

    SettingRow(
        title = "show all projects' tabs",
        subtitle = "off shows only the current project's files",
        checked = tabScope == EditorTabScope.AllProjects,
        onCheckedChange = {
            workspace.setTabScope(
                if (it) EditorTabScope.AllProjects else EditorTabScope.CurrentProject,
            )
        },
        accessibilityLabel = "Show tabs from all projects",
        modifier = modifier,
    )
}

@Composable
private fun UndoRedoRow(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val show by workspace.showUndoRedoButtons.collectAsState()

    SettingRow(
        title = "undo / redo buttons",
        subtitle = "show undo and redo in the top bar",
        checked = show,
        onCheckedChange = workspace::setShowUndoRedoButtons,
        accessibilityLabel = "Show undo and redo buttons",
        modifier = modifier,
    )
}

@Composable
private fun AutoSaveRow(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val save by workspace.autoSave.collectAsState()

    SettingRow(
        title = "auto-save",
        subtitle = "write files to disk 500ms after the last edit (top-bar save still works)",
        checked = save,
        onCheckedChange = workspace::setAutoSave,
        accessibilityLabel = "Auto-save files after editing",
        modifier = modifier,
    )
}

@Composable
private fun InlayHintsRow(modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val show by workspace.showInlayHints.collectAsState()

    SettingRow(
        title = "inlay hints",
        subtitle = "show inline parameter and type hints from the language server",
        checked = show,
        onCheckedChange = workspace::setShowInlayHints,
        accessibilityLabel = "Show inlay hints",
        modifier = modifier,
    )
}

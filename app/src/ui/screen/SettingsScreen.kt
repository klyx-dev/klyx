package com.klyx.ui.screen

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.Thumb
import com.composeunstyled.Track
import com.composeunstyled.UnstyledSwitch
import com.composeunstyled.theme.Theme
import com.klyx.navigation.LocalNavigator
import com.klyx.runtime.workspace.EditorTabScope
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.accent
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.headline
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.onAccent
import com.klyx.ui.surfaceVariant
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.typography
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

        SectionHeader("EDITOR TABS")

        TabScopeRow(modifier = Modifier.fillMaxWidth())

        SectionHeader("TOP BAR")

        UndoRedoRow(modifier = Modifier.fillMaxWidth())
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
                .clip(RectangleShape)
                .background(
                    if (checked) Theme[colors][accent] else Theme[colors][surfaceVariant],
                    RectangleShape,
                )
                .border(1.dp, Theme[colors][border], RectangleShape),
        ) {
            Thumb(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RectangleShape)
                    .background(
                        if (checked) Theme[colors][onAccent] else Theme[colors][textMuted],
                        RectangleShape,
                    )
                    .size(18.dp),
            )
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

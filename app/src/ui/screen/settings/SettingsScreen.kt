package com.klyx.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.klyx.runtime.settings.Settings
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.workspace.EditorTabScope
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.theme.KxColors
import com.klyx.ui.theme.KxTypography
import com.klyx.ui.theme.LocalCornerRadius
import com.klyx.ui.theme.accent
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.border
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.kxShape
import com.klyx.ui.theme.label
import com.klyx.ui.theme.labelSmall
import com.klyx.ui.theme.onAccent
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography
import com.klyx.ui.utils.BackHandler
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

enum class SettingsCategory(val label: String) {
    General("general"),
    LanguageServers("lsps"),
    Termux("termux"),
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val palette = KxColors.current
    val navigator = LocalNavigator.current
    var selectedCategory by remember { mutableStateOf(SettingsCategory.General) }

    val availableCategories = remember(currentOperatingSystem) {
        SettingsCategory.entries.filter { category ->
            category != SettingsCategory.Termux || currentOperatingSystem == OperatingSystem.Android
        }
    }

    BackHandler(onBack = navigator::navigateBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp)
                .imePadding(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$ ", style = KxTypography.headline, color = palette.textMuted)
                        Text("settings", style = KxTypography.headline, color = palette.textPrimary)
                    }
                    KxButton(
                        onClick = navigator::navigateBack,
                        variant = KxButtonVariant.Ghost,
                        bracketed = true,
                        borderColor = palette.border,
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                    ) {
                        Text("x", style = KxTypography.label, color = palette.textMuted)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(1.dp)
                        .background(palette.border),
                )
            }

            item {
                SettingsCategoryTabs(
                    categories = availableCategories,
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
            }

            item(key = "settings-${selectedCategory.name}") {
                when (selectedCategory) {
                    SettingsCategory.General -> GeneralSettings()
                    SettingsCategory.LanguageServers -> LspServersSection(modifier = Modifier.fillMaxWidth())
                    SettingsCategory.Termux -> TermuxSettings()
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryTabs(
    categories: List<SettingsCategory>,
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
) {
    val palette = KxColors.current
    val railShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(railShape)
            .background(palette.textMuted.copy(alpha = 0.035f), railShape)
            .border(1.dp, palette.border.copy(alpha = 0.9f), railShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            val itemShape = RoundedCornerShape(9.dp)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(
                        if (isSelected) palette.accent.copy(alpha = 0.12f)
                        else palette.surface.copy(alpha = 0.12f),
                        itemShape,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) palette.accent.copy(alpha = 0.42f)
                        else palette.border.copy(alpha = 0.25f),
                        shape = itemShape,
                    )
                    .clickable { onSelect(category) }
                    .padding(horizontal = 5.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Text("›", style = KxTypography.label, color = palette.accent)
                }
                Text(
                    text = category.label,
                    style = KxTypography.labelSmall,
                    color = if (isSelected) palette.accent else palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = KxColors.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.textMuted.copy(alpha = 0.035f), shape)
            .border(1.dp, palette.textMuted.copy(alpha = 0.20f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = KxTypography.label,
                color = palette.textPrimary,
            )
            Text(
                text = subtitle,
                style = KxTypography.bodySmall,
                color = palette.textMuted,
            )
        }
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KxColors.current.border.copy(alpha = 0.65f)),
    )
}

@Composable
private fun GeneralSettings() {
    val scope = rememberCoroutineScope()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settings.collectAsState()

    fun updateSettings(block: suspend (Settings) -> Settings) {
        scope.launch { settingsStore.update(block) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsCard(
            title = "appearance",
            subtitle = "Theme, font, and interface shape.",
        ) {
            ThemeSelectorSection(
                darkThemeName = settings.themeName,
                onDarkThemeSelected = { name -> updateSettings { it.copy(themeName = name) } },
                lightThemeName = settings.lightThemeName,
                onLightThemeSelected = { name -> updateSettings { it.copy(lightThemeName = name) } },
                themeMode = settings.themeMode,
                onThemeModeChanged = { mode -> updateSettings { it.copy(themeMode = mode) } },
            )
            SettingsDivider()
            FontSelectorSection(
                currentFont = settings.fontName,
                onFontSelected = { name -> updateSettings { it.copy(fontName = name) } },
            )
            SettingsDivider()
            RoundedUiRow(modifier = Modifier.fillMaxWidth())
            CornerRadiusRow(modifier = Modifier.fillMaxWidth())
        }

        SettingsCard(
            title = "editor tabs",
            subtitle = "Choose which projects' tabs are shown.",
        ) {
            TabScopeRow(modifier = Modifier.fillMaxWidth())
        }

        SettingsCard(
            title = "top bar",
            subtitle = "Show or hide the undo and redo controls.",
        ) {
            UndoRedoRow(modifier = Modifier.fillMaxWidth())
        }

        SettingsCard(
            title = "editor",
            subtitle = "Configure saving and inline language hints.",
        ) {
            AutoSaveRow(modifier = Modifier.fillMaxWidth())
            SettingsDivider()
            InlayHintsRow(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TermuxSettings() {
    TermuxSetupSection(modifier = Modifier.fillMaxWidth())
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
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

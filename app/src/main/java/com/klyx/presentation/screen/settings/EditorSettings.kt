package com.klyx.presentation.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.R
import com.klyx.api.data.preferences.AutoSaveScope
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.data.preferences.MouseMode
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.app.icons.Backspace
import com.klyx.app.icons.ContentPaste
import com.klyx.app.icons.CopyAll
import com.klyx.app.icons.DataArray
import com.klyx.app.icons.DataObject
import com.klyx.app.icons.FilterCenterFocus
import com.klyx.app.icons.FolderOpen
import com.klyx.app.icons.FontDownload
import com.klyx.app.icons.FormatBold
import com.klyx.app.icons.FormatIndentIncrease
import com.klyx.app.icons.FormatLineSpacing
import com.klyx.app.icons.FormatListNumbered
import com.klyx.app.icons.KeyboardReturn
import com.klyx.app.icons.KeyboardTab
import com.klyx.app.icons.MenuOpen
import com.klyx.app.icons.Mouse
import com.klyx.app.icons.PushPin
import com.klyx.app.icons.RoundedCorner
import com.klyx.app.icons.Save
import com.klyx.app.icons.SpaceBar
import com.klyx.app.icons.Storage
import com.klyx.app.icons.TextFormat
import com.klyx.app.icons.TouchApp
import com.klyx.app.icons.UnfoldLess
import com.klyx.app.icons.UnfoldMore
import com.klyx.app.icons.Update
import com.klyx.app.icons.VisibilityOff
import com.klyx.data.preferences.FontManager
import com.klyx.data.preferences.updateEditorSettings
import com.klyx.i18n.strings
import com.klyx.presentation.components.CodeEditorDemo
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.settings.components.SegmentedSettingsItem
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SettingsSubsectionHeader
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettings() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val settings = LocalAppSettings.current.editor

    fun update(transform: suspend EditorSettings.() -> EditorSettings) {
        scope.launch {
            updateEditorSettings(transform)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(strings.editor) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        var localFontSize by remember(settings.fontSize) { mutableFloatStateOf(settings.fontSize) }
        var localWaveLength by remember(settings.indicatorWaveLength) { mutableFloatStateOf(settings.indicatorWaveLength) }
        var localWaveWidth by remember(settings.indicatorWaveWidth) { mutableFloatStateOf(settings.indicatorWaveWidth) }
        var localWaveAmplitude by remember(settings.indicatorWaveAmplitude) {
            mutableFloatStateOf(settings.indicatorWaveAmplitude)
        }
        var localFastScroll by remember(settings.fastScrollSensitivity) {
            mutableFloatStateOf(settings.fastScrollSensitivity)
        }
        var localWheelFactor by remember(settings.mouseWheelScrollFactor) {
            mutableFloatStateOf(settings.mouseWheelScrollFactor)
        }
        var localStickyMax by remember(settings.stickyScrollMaxLines) { mutableFloatStateOf(settings.stickyScrollMaxLines.toFloat()) }
        var localAutoSaveDelay by remember(settings.autoSave.delayMillis) { mutableFloatStateOf(settings.autoSave.delayMillis.toFloat()) }
        var localLargeFileThreshold by remember(settings.autoSave.largeFileThresholdKb) { mutableFloatStateOf(settings.autoSave.largeFileThresholdKb.toFloat()) }
        var localPeriodicInterval by remember(settings.autoSave.periodicIntervalMillis) {
            mutableFloatStateOf((settings.autoSave.periodicIntervalMillis ?: 30000L).toFloat())
        }

        val fontManager: FontManager = koinInject()
        var localFontFamily by remember { mutableStateOf(JetBrainsMonoFontFamily) }

        LaunchedEffect(settings.customFontUri) {
            localFontFamily = fontManager.getFontFamily(settings.customFontUri)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
        ) {
            stickyHeader {
                EditorPreviewHeader(
                    localFontSize = localFontSize,
                    localFontFamily = localFontFamily,
                    localWaveWidth = localWaveWidth,
                    localWaveLength = localWaveLength,
                    localWaveAmplitude = localWaveAmplitude
                )
            }

            item {
                CommonSettingsSection(
                    settings = settings,
                    update = ::update,
                    localFontSize = localFontSize,
                    onLocalFontSizeChange = { localFontSize = it },
                    localFontFamily = localFontFamily,
                    customFontUri = settings.customFontUri,
                    scope = scope
                )
            }

            item { EditingSettingsSection(settings = settings, update = ::update) }

            item { KeyboardInputSettingsSection(settings = settings, update = ::update) }

            item {
                IndicatorsVisualsSettingsSection(
                    settings = settings,
                    update = ::update,
                    localWaveLength = localWaveLength,
                    onLocalWaveLengthChange = { localWaveLength = it },
                    localWaveWidth = localWaveWidth,
                    onLocalWaveWidthChange = { localWaveWidth = it },
                    localWaveAmplitude = localWaveAmplitude,
                    onLocalWaveAmplitudeChange = { localWaveAmplitude = it }
                )
            }

            item {
                MouseScrollingSettingsSection(
                    settings = settings,
                    update = ::update,
                    localFastScroll = localFastScroll,
                    onLocalFastScrollChange = { localFastScroll = it },
                    localWheelFactor = localWheelFactor,
                    onLocalWheelFactorChange = { localWheelFactor = it }
                )
            }

            item {
                StickyScrollSettingsSection(
                    settings = settings,
                    update = ::update,
                    localStickyMax = localStickyMax,
                    onLocalStickyMaxChange = { localStickyMax = it }
                )
            }

            item {
                AutoSaveSettingsSection(
                    settings = settings,
                    update = ::update,
                    localDelay = localAutoSaveDelay,
                    onLocalDelayChange = { localAutoSaveDelay = it },
                    localThreshold = localLargeFileThreshold,
                    onLocalThresholdChange = { localLargeFileThreshold = it },
                    localInterval = localPeriodicInterval,
                    onLocalIntervalChange = { localPeriodicInterval = it }
                )
            }

            item { AdvancedSettingsSection(settings = settings, update = ::update) }
        }
    }
}

@Composable
private fun EditorPreviewHeader(
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

@Composable
private fun CommonSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localFontSize: Float,
    onLocalFontSizeChange: (Float) -> Unit,
    localFontFamily: FontFamily,
    customFontUri: String?,
    scope: CoroutineScope
) {
    val s = strings

    SettingsSubsection(strings.commonSection) {
        FontFamilySettingItem(
            currentFontFamily = localFontFamily,
            customFontUri = customFontUri,
            onClearCustomFont = {
                scope.launch {
                    updateEditorSettings { copy(customFontUri = null) }
                }
            },
            onCustomFontPicked = { uriString ->
                scope.launch {
                    updateEditorSettings { copy(customFontUri = uriString) }
                }
            }
        )

        SliderSettingsItem(
            label = strings.fontSize,
            value = localFontSize,
            onValueChange = onLocalFontSizeChange,
            valueRange = 10f..32f,
            // (32 - 10) / 1 step - 1 = 21 steps
            steps = 21,
            onValueChangeFinished = {
                if (localFontSize != settings.fontSize) {
                    update { copy(fontSize = localFontSize) }
                }
            },
            valueText = { "${it.roundToInt()}sp" }
        )

        SegmentedSettingsItem(
            label = strings.tabSize,
            options = persistentListOf(2, 4, 8),
            currentValue = settings.tabSize,
            onValueChange = { update { copy(tabSize = it) } },
            valueText = { s.nSpaces(it) }
        )

        SwitchSettingItem(
            title = strings.wordWrap,
            subtitle = strings.wordWrapDesc,
            checked = settings.wordWrap,
            onCheckedChange = { update { copy(wordWrap = it) } },
            leadingIcon = { Icon(Icons.Rounded.FormatLineSpacing, null) }
        )
    }
}

@Composable
private fun EditingSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit
) {
    val s = strings

    SettingsSubsection(strings.editingSection) {
        SwitchSettingItem(
            title = strings.pinLineNumbers,
            subtitle = strings.pinLineNumbersDesc,
            checked = settings.pinLineNumbers,
            onCheckedChange = {
                update { copy(pinLineNumbers = it) }
            },
            leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, null) }
        )

        SwitchSettingItem(
            title = strings.deleteEmptyLinesFast,
            subtitle = strings.deleteEmptyLinesFastDesc,
            checked = settings.deleteEmptyLineFast,
            onCheckedChange = { update { copy(deleteEmptyLineFast = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Backspace, null) }
        )

        SelectorItem(
            label = strings.deleteMultipleSpaces,
            description = strings.deleteMultipleSpacesDesc,
            options = persistentListOf(-1, 1, 2, 4, 8),
            selected = settings.deleteMultiSpaces,
            optionLabel = { value ->
                when (value) {
                    -1 -> s.followTabSize
                    1 -> s.oneSpace
                    else -> s.nSpaces(value)
                }
            },
            onSelectionChanged = { update { copy(deleteMultiSpaces = it) } },
            leadingIcon = { Icon(Icons.Rounded.SpaceBar, null) }
        )

        SwitchSettingItem(
            title = strings.symbolPairAutoCompletion,
            subtitle = strings.symbolPairAutoCompletionDesc,
            checked = settings.symbolPairAutoCompletion,
            onCheckedChange = { update { copy(symbolPairAutoCompletion = it) } },
            leadingIcon = { Icon(Icons.Rounded.DataArray, null) }
        )

        SwitchSettingItem(
            title = strings.autoIndent,
            subtitle = strings.autoIndentDesc,
            checked = settings.autoIndent,
            onCheckedChange = { update { copy(autoIndent = it) } },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Rounded.FormatIndentIncrease,
                    null
                )
            }
        )

        SwitchSettingItem(
            title = strings.formatOnPaste,
            subtitle = strings.formatOnPasteDesc,
            checked = settings.formatPastedText,
            onCheckedChange = { update { copy(formatPastedText = it) } },
            leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) }
        )
    }
}

@Composable
private fun KeyboardInputSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit
) {
    SettingsSubsection(strings.keyboardInputSection) {
        SwitchSettingItem(
            title = strings.disableKeyboardSuggestions,
            subtitle = strings.disableKeyboardSuggestionsDesc,
            checked = settings.disallowSuggestions,
            onCheckedChange = { update { copy(disallowSuggestions = it) } },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.keyboard_off_24px),
                    null
                )
            }
        )

        SwitchSettingItem(
            title = strings.enhancedHomeAndEnd,
            subtitle = strings.enhancedHomeAndEndDesc,
            checked = settings.enhancedHomeAndEnd,
            onCheckedChange = { update { copy(enhancedHomeAndEnd = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardTab, null) }
        )

        SwitchSettingItem(
            title = strings.reselectOnLongPress,
            subtitle = strings.reselectOnLongPressDesc,
            checked = settings.reselectOnLongPress,
            onCheckedChange = { update { copy(reselectOnLongPress = it) } },
            leadingIcon = { Icon(Icons.Rounded.TouchApp, null) }
        )
    }
}

@Composable
private fun IndicatorsVisualsSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localWaveLength: Float,
    onLocalWaveLengthChange: (Float) -> Unit,
    localWaveWidth: Float,
    onLocalWaveWidthChange: (Float) -> Unit,
    localWaveAmplitude: Float,
    onLocalWaveAmplitudeChange: (Float) -> Unit
) {
    SettingsSubsection(strings.indicatorsVisualsSection) {
        SwitchSettingItem(
            title = strings.roundTextBackground,
            subtitle = strings.roundTextBackgroundDesc,
            checked = settings.enableRoundTextBackground,
            onCheckedChange = { update { copy(enableRoundTextBackground = it) } },
            leadingIcon = { Icon(Icons.Rounded.RoundedCorner, null) }
        )

        SwitchSettingItem(
            title = strings.highlightMatchingDelimiters,
            subtitle = strings.highlightMatchingDelimitersDesc,
            checked = settings.highlightMatchingDelimiters,
            onCheckedChange = { update { copy(highlightMatchingDelimiters = it) } },
            leadingIcon = { Icon(Icons.Rounded.DataObject, null) }
        )

        SwitchSettingItem(
            title = strings.boldMatchingDelimiters,
            subtitle = strings.boldMatchingDelimitersDesc,
            checked = settings.boldMatchingDelimiters,
            onCheckedChange = { update { copy(boldMatchingDelimiters = it) } },
            leadingIcon = { Icon(Icons.Rounded.FormatBold, null) }
        )

        SwitchSettingItem(
            title = strings.inlayHints,
            subtitle = strings.inlayHintsDesc,
            checked = settings.inlayHints,
            onCheckedChange = { update { copy(inlayHints = it) } },
            leadingIcon = { Icon(Icons.Rounded.TextFormat, null) }
        )

        SliderSettingsItem(
            label = strings.errorWaveLength,
            value = localWaveLength,
            onValueChange = onLocalWaveLengthChange,
            valueRange = 5f..30f,
            steps = 24,
            onValueChangeFinished = {
                if (localWaveLength != settings.indicatorWaveLength) {
                    update { copy(indicatorWaveLength = localWaveLength) }
                }
            },
            valueText = { "${it.roundToInt()}dp" }
        )

        SliderSettingsItem(
            label = strings.errorWaveWidth,
            value = localWaveWidth,
            onValueChange = onLocalWaveWidthChange,
            valueRange = 0.5f..5f,
            steps = 8,
            onValueChangeFinished = {
                if (localWaveWidth != settings.indicatorWaveWidth) {
                    update { copy(indicatorWaveWidth = localWaveWidth) }
                }
            },
            valueText = { String.format(Locale.ROOT, "%.1f", it) }
        )

        SliderSettingsItem(
            label = strings.errorWaveAmplitude,
            value = localWaveAmplitude,
            onValueChange = onLocalWaveAmplitudeChange,
            valueRange = 1f..10f,
            steps = 8,
            onValueChangeFinished = {
                if (localWaveAmplitude != settings.indicatorWaveAmplitude) {
                    update { copy(indicatorWaveAmplitude = localWaveAmplitude) }
                }
            },
            valueText = { "${it.roundToInt()}dp" }
        )
    }
}

@Composable
private fun MouseScrollingSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localFastScroll: Float,
    onLocalFastScrollChange: (Float) -> Unit,
    localWheelFactor: Float,
    onLocalWheelFactorChange: (Float) -> Unit
) {
    val s = strings

    SettingsSubsection(strings.mouseScrollingSection) {
        SelectorItem(
            label = strings.mouseMode,
            description = strings.mouseModeDesc,
            options = persistentListOf(
                MouseMode.Auto,
                MouseMode.Always,
                MouseMode.Never
            ),
            selected = settings.mouseMode,
            optionLabel = {
                when (it) {
                    MouseMode.Auto -> s.mouseAuto
                    MouseMode.Always -> s.mouseAlways
                    MouseMode.Never -> s.mouseNever
                    else -> it.name
                }
            },
            optionDescription = { mode ->
                when (mode) {
                    MouseMode.Auto -> s.mouseModeAutoDesc
                    MouseMode.Always -> s.mouseModeAlwaysDesc
                    MouseMode.Never -> s.mouseModeNeverDesc
                    else -> null
                }
            },
            onSelectionChanged = { update { copy(mouseMode = it) } },
            leadingIcon = { Icon(Icons.Rounded.Mouse, null) }
        )

        SwitchSettingItem(
            title = strings.alwaysShowScrollbars,
            subtitle = strings.alwaysShowScrollbarsDesc,
            checked = settings.mouseModeAlwaysShowScrollbars,
            onCheckedChange = { update { copy(mouseModeAlwaysShowScrollbars = it) } },
            leadingIcon = { Icon(Icons.Rounded.UnfoldMore, null) }
        )

        SwitchSettingItem(
            title = strings.mouseContextMenu,
            subtitle = strings.mouseContextMenuDesc,
            checked = settings.mouseContextMenu,
            onCheckedChange = { update { copy(mouseContextMenu = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuOpen, null) }
        )

        SliderSettingsItem(
            label = strings.fastScrollSensitivity,
            value = localFastScroll,
            onValueChange = onLocalFastScrollChange,
            valueRange = 1f..10f,
            steps = 8,
            onValueChangeFinished = {
                if (localFastScroll != settings.fastScrollSensitivity) {
                    update { copy(fastScrollSensitivity = localFastScroll) }
                }
            },
            valueText = { "${it.roundToInt()}x" }
        )

        SliderSettingsItem(
            label = strings.mouseWheelFactor,
            value = localWheelFactor,
            onValueChange = onLocalWheelFactorChange,
            valueRange = 0.5f..5f,
            steps = 8,
            onValueChangeFinished = {
                if (localWheelFactor != settings.mouseWheelScrollFactor) {
                    update { copy(mouseWheelScrollFactor = localWheelFactor) }
                }
            },
            valueText = { String.format(Locale.ROOT, "%.1fx", it) }
        )
    }
}

@Composable
private fun StickyScrollSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localStickyMax: Float,
    onLocalStickyMaxChange: (Float) -> Unit
) {
    SettingsSubsection(strings.stickyScrollSection) {
        SwitchSettingItem(
            title = strings.enableStickyScroll,
            subtitle = strings.enableStickyScrollDesc,
            checked = settings.stickyScroll,
            onCheckedChange = { update { copy(stickyScroll = it) } },
            leadingIcon = { Icon(Icons.Rounded.PushPin, null) }
        )

        AnimatedVisibility(
            visible = settings.stickyScroll,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            ) + fadeIn(animationSpec = spring(stiffness = 400f)),
            exit = shrinkVertically(animationSpec = spring(stiffness = 500f)) + fadeOut(
                animationSpec = spring(stiffness = 500f)
            )
        ) {
            SliderSettingsItem(
                label = strings.maxStickyLines,
                value = localStickyMax,
                onValueChange = onLocalStickyMaxChange,
                valueRange = 1f..10f,
                steps = 8,
                onValueChangeFinished = {
                    if (localStickyMax.roundToInt() != settings.stickyScrollMaxLines) {
                        update { copy(stickyScrollMaxLines = localStickyMax.roundToInt()) }
                    }
                },
                valueText = { "${it.roundToInt()}" }
            )
        }


        SwitchSettingItem(
            title = strings.preferInnerScope,
            subtitle = strings.preferInnerScopeDesc,
            checked = settings.stickyScrollPreferInnerScope,
            onCheckedChange = { update { copy(stickyScrollPreferInnerScope = it) } },
            leadingIcon = { Icon(Icons.Rounded.FilterCenterFocus, null) }
        )

        SwitchSettingItem(
            title = strings.autoCollapse,
            subtitle = strings.autoCollapseDesc,
            checked = settings.stickyScrollAutoCollapse,
            onCheckedChange = { update { copy(stickyScrollAutoCollapse = it) } },
            leadingIcon = { Icon(Icons.Rounded.UnfoldLess, null) }
        )
    }
}

@Composable
private fun AutoSaveSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localDelay: Float,
    onLocalDelayChange: (Float) -> Unit,
    localThreshold: Float,
    onLocalThresholdChange: (Float) -> Unit,
    localInterval: Float,
    onLocalIntervalChange: (Float) -> Unit
) {
    val s = strings
    SettingsSubsection(strings.autoSaveSection) {
        SwitchSettingItem(
            title = strings.autoSaveEnabled,
            subtitle = strings.autoSaveEnabledDesc,
            checked = settings.autoSave.enabled,
            onCheckedChange = { update { copy(autoSave = autoSave.copy(enabled = it)) } },
            leadingIcon = { Icon(Icons.Outlined.Save, null) }
        )

        AnimatedVisibility(
            visible = settings.autoSave.enabled,
            enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(
                spring(
                    stiffness = 400f
                )
            ),
            exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(spring(stiffness = 500f))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SliderSettingsItem(
                    label = strings.autoSaveDelay,
                    value = localDelay,
                    onValueChange = onLocalDelayChange,
                    valueRange = 200f..10000f,
                    steps = 48,
                    onValueChangeFinished = {
                        val coerced = localDelay.roundToInt().toLong().coerceIn(200L, 10000L)
                        if (coerced != settings.autoSave.delayMillis) {
                            update { copy(autoSave = autoSave.copy(delayMillis = coerced)) }
                        }
                    },
                    valueText = { "${it.roundToInt()} ms" }
                )

                SwitchSettingItem(
                    title = strings.autoSaveOnTyping,
                    subtitle = strings.autoSaveOnTypingDesc,
                    checked = settings.autoSave.onTyping,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(onTyping = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.Info, null) }
                )

                SwitchSettingItem(
                    title = strings.autoSaveOnAppPause,
                    subtitle = strings.autoSaveOnAppPauseDesc,
                    checked = settings.autoSave.onAppPause,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(onAppPause = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null) } // app background ~ visibility
                )

                SwitchSettingItem(
                    title = strings.autoSaveOnTabSwitch,
                    subtitle = strings.autoSaveOnTabSwitchDesc,
                    checked = settings.autoSave.onTabSwitch,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(onTabSwitch = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }
                )

                SelectorItem(
                    label = strings.autoSaveScope,
                    description = strings.autoSaveScopeDesc,
                    options = persistentListOf(AutoSaveScope.ACTIVE_TAB, AutoSaveScope.ALL_TABS),
                    selected = settings.autoSave.scope,
                    optionLabel = {
                        when (it) {
                            AutoSaveScope.ACTIVE_TAB -> s.autoSaveScopeActiveTab
                            AutoSaveScope.ALL_TABS -> s.autoSaveScopeAllTabs
                        }
                    },
                    optionDescription = { scope ->
                        when (scope) {
                            AutoSaveScope.ACTIVE_TAB -> s.autoSaveScopeActiveTabDesc
                            AutoSaveScope.ALL_TABS -> s.autoSaveScopeAllTabsDesc
                        }
                    },
                    onSelectionChanged = { update { copy(autoSave = autoSave.copy(scope = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.CopyAll, null) }
                )

                SwitchSettingItem(
                    title = strings.autoSaveSkipLargeFiles,
                    subtitle = strings.autoSaveSkipLargeFilesDesc,
                    checked = settings.autoSave.skipLargeFiles,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(skipLargeFiles = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.Storage, null) }
                )

                AnimatedVisibility(
                    visible = settings.autoSave.skipLargeFiles,
                    enter = expandVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        )
                    ) + fadeIn(spring(stiffness = 400f)),
                    exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(spring(stiffness = 500f))
                ) {
                    SliderSettingsItem(
                        label = strings.autoSaveLargeFileThreshold,
                        value = localThreshold,
                        onValueChange = onLocalThresholdChange,
                        valueRange = 50f..2048f,
                        steps = 15,
                        onValueChangeFinished = {
                            val v = localThreshold.roundToInt().coerceIn(50, 5000)
                            if (v != settings.autoSave.largeFileThresholdKb) {
                                update { copy(autoSave = autoSave.copy(largeFileThresholdKb = v)) }
                            }
                        },
                        valueText = { "${it.roundToInt()} KB" }
                    )
                }

                val periodicEnabled = settings.autoSave.periodicIntervalMillis != null
                SwitchSettingItem(
                    title = strings.autoSavePeriodic,
                    subtitle = strings.autoSavePeriodicDesc,
                    checked = periodicEnabled,
                    onCheckedChange = { enabled ->
                        update {
                            copy(
                                autoSave = autoSave.copy(
                                    periodicIntervalMillis = if (enabled) {
                                        localInterval.roundToInt().toLong().coerceIn(5000L, 120000L)
                                    } else null
                                )
                            )
                        }
                    },
                    leadingIcon = { Icon(Icons.Rounded.Update, null) }
                )

                AnimatedVisibility(
                    visible = periodicEnabled,
                    enter = expandVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        )
                    ) + fadeIn(spring(stiffness = 400f)),
                    exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(spring(stiffness = 500f))
                ) {
                    SliderSettingsItem(
                        label = strings.autoSaveInterval,
                        value = localInterval,
                        onValueChange = onLocalIntervalChange,
                        valueRange = 5000f..120000f,
                        steps = 22,
                        onValueChangeFinished = {
                            val v = localInterval.roundToInt().toLong().coerceIn(5000L, 120000L)
                            if (v != settings.autoSave.periodicIntervalMillis) {
                                update { copy(autoSave = autoSave.copy(periodicIntervalMillis = v)) }
                            }
                        },
                        valueText = { "${(it / 1000).roundToInt()} sec" }
                    )
                }

                SwitchSettingItem(
                    title = strings.autoSaveShowToast,
                    subtitle = strings.autoSaveShowToastDesc,
                    checked = settings.autoSave.showToast,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(showToast = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.Info, null) }
                )

                Text(
                    text = strings.autoSavePerformanceNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit
) {
    SettingsSubsection(strings.advancedSection) {
        SwitchSettingItem(
            title = strings.selectCompletionOnEnter,
            subtitle = strings.selectCompletionOnEnterDesc,
            checked = settings.selectCompletionItemOnEnterForSoftKbd,
            onCheckedChange = { update { copy(selectCompletionItemOnEnterForSoftKbd = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, null) }
        )

        SwitchSettingItem(
            title = strings.useIcuLibrary,
            subtitle = strings.useIcuLibraryDesc,
            checked = settings.useICULibToSelectWords,
            onCheckedChange = { update { copy(useICULibToSelectWords = it) } },
            leadingIcon = { Icon(Icons.Rounded.TextFormat, null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontFamilySettingItem(
    currentFontFamily: FontFamily,
    customFontUri: String?,
    onClearCustomFont: () -> Unit,
    onCustomFontPicked: (String) -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = Hidden)
    val coroutineScope = rememberCoroutineScope()

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onCustomFontPicked(it.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
    }

    val isBuiltIn = customFontUri.isNullOrEmpty()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { showSheet = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FontDownload, contentDescription = null)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.fontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.fontFamilyDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (isBuiltIn) strings.jetbrainsMono else strings.customFontShort,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = strings.chooseFontFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                FontOptionRow(
                    title = strings.jetbrainsMono,
                    subtitle = strings.builtinDefaultFont,
                    isSelected = isBuiltIn,
                    icon = Icons.Rounded.FontDownload,
                    fontFamily = JetBrainsMonoFontFamily,
                    onClick = {
                        onClearCustomFont()
                        coroutineScope.launch { sheetState.hide() }
                            .invokeOnCompletion { showSheet = false }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FontOptionRow(
                    title = strings.customFont,
                    subtitle = strings.customFontDesc,
                    isSelected = !isBuiltIn,
                    icon = Icons.Rounded.FolderOpen,
                    fontFamily = if (currentFontFamily == JetBrainsMonoFontFamily) null else currentFontFamily,
                    onClick = {
                        fontPickerLauncher.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "application/font-sfnt"
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FontOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: ImageVector,
    fontFamily: FontFamily?,
    onClick: () -> Unit
) {
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = strings.selected,
                    tint = contentColor
                )
            }
        }
    }
}

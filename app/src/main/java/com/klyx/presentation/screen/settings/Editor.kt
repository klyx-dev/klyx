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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.KeyboardTab
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DataArray
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.klyx.data.preferences.EditorSettings
import com.klyx.data.preferences.LocalAppSettings
import com.klyx.data.preferences.MouseMode
import com.klyx.data.preferences.updateEditorSettings
import com.klyx.presentation.components.CodeEditorDemo
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.SettingScreens
import com.klyx.presentation.screen.settings.components.SegmentedSettingsItem
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SettingsSubsectionHeader
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import com.klyx.ui.theme.JetBrainsMonoFontFamily
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingScreens.Editor() {
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
                title = { Text("Editor") },
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
                            contentDescription = "Back"
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

        var localFontFamily by remember(settings.customFontUri) {
            mutableStateOf(settings.currentFontFamily)
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, bottom = 12.dp, end = 6.dp, top = 8.dp)
                ) {
                    Column {
                        SettingsSubsectionHeader("Preview")

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

            item {
                SettingsSubsection("Common") {
                    FontFamilySettingItem(
                        currentFontFamily = settings.currentFontFamily,
                        customFontUri = settings.customFontUri,
                        onClearCustomFont = {
                            scope.launch {
                                val newSettings =
                                    updateEditorSettings { copy(customFontUri = null) }
                                localFontFamily = newSettings.currentFontFamily
                            }
                        },
                        onCustomFontPicked = { uriString ->
                            scope.launch {
                                val newSettings =
                                    updateEditorSettings { copy(customFontUri = uriString) }
                                localFontFamily = newSettings.currentFontFamily
                            }
                        }
                    )

                    SliderSettingsItem(
                        label = "Font Size",
                        value = localFontSize,
                        onValueChange = { localFontSize = it },
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
                        label = "Tab Size",
                        options = persistentListOf(2, 4, 8),
                        currentValue = settings.tabSize,
                        onValueChange = { update { copy(tabSize = it) } },
                        valueText = { "$it Spaces" }
                    )
                }
            }

            item {
                SettingsSubsection("Editing") {
                    SwitchSettingItem(
                        title = "Pin Line Numbers",
                        subtitle = "Keep line numbers visible when scrolling horizontally",
                        checked = settings.pinLineNumbers,
                        onCheckedChange = {
                            update { copy(pinLineNumbers = it) }
                        },
                        leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, null) }
                    )

                    SwitchSettingItem(
                        title = "Delete Empty Lines Fast",
                        subtitle = "Delete the entire line instantly if it contains only whitespace",
                        checked = settings.deleteEmptyLineFast,
                        onCheckedChange = { update { copy(deleteEmptyLineFast = it) } },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Backspace, null) }
                    )

                    SelectorItem(
                        label = "Delete Multiple Spaces",
                        description = "How many leading spaces to delete at once when pressing backspace",
                        options = persistentListOf(-1, 1, 2, 4, 8),
                        selected = settings.deleteMultiSpaces,
                        optionLabel = { value ->
                            when (value) {
                                -1 -> "Follow Tab Size"
                                1 -> "1 Space"
                                else -> "$value Spaces"
                            }
                        },
                        onSelectionChanged = { update { copy(deleteMultiSpaces = it) } },
                        leadingIcon = { Icon(Icons.Rounded.SpaceBar, null) }
                    )

                    SwitchSettingItem(
                        title = "Symbol Pair Auto-Completion",
                        subtitle = "Automatically insert closing brackets and quotes",
                        checked = settings.symbolPairAutoCompletion,
                        onCheckedChange = { update { copy(symbolPairAutoCompletion = it) } },
                        leadingIcon = { Icon(Icons.Rounded.DataArray, null) }
                    )

                    SwitchSettingItem(
                        title = "Auto Indent",
                        subtitle = "Copy the leading indentation to the new line when pressing enter",
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
                        title = "Format on Paste",
                        subtitle = "Automatically format code blocks when pasted",
                        checked = settings.formatPastedText,
                        onCheckedChange = { update { copy(formatPastedText = it) } },
                        leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) }
                    )
                }
            }

            item {
                SettingsSubsection("Keyboard & Input") {
                    SwitchSettingItem(
                        title = "Disable Keyboard Suggestions",
                        subtitle = "Force the IME to hide autocorrect suggestions (May cause keyboard restarts)",
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
                        title = "Enhanced Home and End",
                        subtitle = "Jumps to the first non-whitespace character before jumping to line start",
                        checked = settings.enhancedHomeAndEnd,
                        onCheckedChange = { update { copy(enhancedHomeAndEnd = it) } },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardTab, null) }
                    )

                    SwitchSettingItem(
                        title = "Reselect on Long Press",
                        subtitle = "Select new words under finger even if text is already highlighted",
                        checked = settings.reselectOnLongPress,
                        onCheckedChange = { update { copy(reselectOnLongPress = it) } },
                        leadingIcon = { Icon(Icons.Rounded.TouchApp, null) }
                    )
                }
            }

            item {
                SettingsSubsection("Indicators & Visuals") {
                    SwitchSettingItem(
                        title = "Round Text Background",
                        subtitle = "Use squircle edges for text selection and highlights",
                        checked = settings.enableRoundTextBackground,
                        onCheckedChange = { update { copy(enableRoundTextBackground = it) } },
                        leadingIcon = { Icon(Icons.Rounded.RoundedCorner, null) }
                    )

                    SwitchSettingItem(
                        title = "Highlight Matching Delimiters",
                        subtitle = "Highlight the corresponding opening/closing bracket",
                        checked = settings.highlightMatchingDelimiters,
                        onCheckedChange = { update { copy(highlightMatchingDelimiters = it) } },
                        leadingIcon = { Icon(Icons.Rounded.DataObject, null) }
                    )

                    SwitchSettingItem(
                        title = "Bold Matching Delimiters",
                        subtitle = "Apply bold styling to matching brackets",
                        checked = settings.boldMatchingDelimiters,
                        onCheckedChange = { update { copy(boldMatchingDelimiters = it) } },
                        leadingIcon = { Icon(Icons.Rounded.FormatBold, null) }
                    )

                    SliderSettingsItem(
                        label = "Error Wave Length",
                        value = localWaveLength,
                        onValueChange = { localWaveLength = it },
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
                        label = "Error Wave Width",
                        value = localWaveWidth,
                        onValueChange = { localWaveWidth = it },
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
                        label = "Error Wave Amplitude",
                        value = localWaveAmplitude,
                        onValueChange = { localWaveAmplitude = it },
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

            item {
                SettingsSubsection("Mouse & Scrolling") {
                    SelectorItem(
                        label = "Mouse Mode",
                        description = "Behavior of editor windows and selection handles with a mouse",
                        options = persistentListOf(
                            MouseMode.Auto,
                            MouseMode.Always,
                            MouseMode.Never
                        ),
                        selected = settings.mouseMode,
                        optionLabel = { it.name },
                        optionDescription = { mode ->
                            when (mode) {
                                MouseMode.Auto -> "Enable mouse mode if a mouse is currently hovering"
                                MouseMode.Always -> "Force mouse handles permanently (Good for Desktop)"
                                MouseMode.Never -> "Strictly touch interface"
                                else -> null
                            }
                        },
                        onSelectionChanged = { update { copy(mouseMode = it) } },
                        leadingIcon = { Icon(Icons.Rounded.Mouse, null) }
                    )

                    SwitchSettingItem(
                        title = "Always Show Scrollbars",
                        subtitle = "Keep scrollbars visible when in Mouse Mode",
                        checked = settings.mouseModeAlwaysShowScrollbars,
                        onCheckedChange = { update { copy(mouseModeAlwaysShowScrollbars = it) } },
                        leadingIcon = { Icon(Icons.Rounded.UnfoldMore, null) }
                    )

                    SwitchSettingItem(
                        title = "Mouse Context Menu",
                        subtitle = "Show native right-click context menus",
                        checked = settings.mouseContextMenu,
                        onCheckedChange = { update { copy(mouseContextMenu = it) } },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuOpen, null) }
                    )

                    SliderSettingsItem(
                        label = "Fast Scroll Sensitivity",
                        value = localFastScroll,
                        onValueChange = { localFastScroll = it },
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
                        label = "Mouse Wheel Factor",
                        value = localWheelFactor,
                        onValueChange = { localWheelFactor = it },
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

            item {
                SettingsSubsection("Sticky Scroll") {
                    SwitchSettingItem(
                        title = "Enable Sticky Scroll",
                        subtitle = "Pin class and method headers to the top while scrolling",
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
                            label = "Max Sticky Lines",
                            value = localStickyMax,
                            onValueChange = { localStickyMax = it },
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
                        title = "Prefer Inner Scope",
                        subtitle = "Push out top stuck lines to show nested inner scopes if max lines are exceeded",
                        checked = settings.stickyScrollPreferInnerScope,
                        onCheckedChange = { update { copy(stickyScrollPreferInnerScope = it) } },
                        leadingIcon = { Icon(Icons.Rounded.FilterCenterFocus, null) }
                    )

                    SwitchSettingItem(
                        title = "Auto Collapse",
                        subtitle = "Hide stuck lines temporarily when selecting text behind them",
                        checked = settings.stickyScrollAutoCollapse,
                        onCheckedChange = { update { copy(stickyScrollAutoCollapse = it) } },
                        leadingIcon = { Icon(Icons.Rounded.UnfoldLess, null) }
                    )
                }
            }

            item {
                SettingsSubsection("Advanced") {
                    SwitchSettingItem(
                        title = "Select Completion on Enter",
                        subtitle = "Accept the first autocomplete suggestion using the Enter key on software keyboards",
                        checked = settings.selectCompletionItemOnEnterForSoftKbd,
                        onCheckedChange = { update { copy(selectCompletionItemOnEnterForSoftKbd = it) } },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, null) }
                    )

                    SwitchSettingItem(
                        title = "Use ICU Library",
                        subtitle = "Use the advanced ICU library for calculating word boundaries on double-tap",
                        checked = settings.useICULibToSelectWords,
                        onCheckedChange = { update { copy(useICULibToSelectWords = it) } },
                        leadingIcon = { Icon(Icons.Rounded.TextFormat, null) }
                    )
                }
            }
        }
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
    val sheetState = rememberModalBottomSheetState()
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
                        text = "Font Family",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The font used to render code in the editor",
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
                            text = if (isBuiltIn) "JetBrains Mono" else "Custom Font",
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
                    text = "Choose Font Family",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                FontOptionRow(
                    title = "JetBrains Mono",
                    subtitle = "Built-in default editor font",
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
                    title = "Custom Font...",
                    subtitle = "Choose a .ttf or .otf file from your device",
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
                    contentDescription = "Selected",
                    tint = contentColor
                )
            }
        }
    }
}

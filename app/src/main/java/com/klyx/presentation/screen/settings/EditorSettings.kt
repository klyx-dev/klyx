package com.klyx.presentation.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.data.preferences.FontManager
import com.klyx.data.preferences.updateEditorSettings
import com.klyx.i18n.strings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.settings.editor.AdvancedSettingsSection
import com.klyx.presentation.screen.settings.editor.AutoSaveSettingsSection
import com.klyx.presentation.screen.settings.editor.CommonSettingsSection
import com.klyx.presentation.screen.settings.editor.EditingSettingsSection
import com.klyx.presentation.screen.settings.editor.EditorPreviewHeader
import com.klyx.presentation.screen.settings.editor.IndicatorsVisualsSettingsSection
import com.klyx.presentation.screen.settings.editor.KeyboardInputSettingsSection
import com.klyx.presentation.screen.settings.editor.MouseScrollingSettingsSection
import com.klyx.presentation.screen.settings.editor.StickyScrollSettingsSection
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

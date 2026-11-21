package com.klyx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.document.DocumentPersistence
import com.klyx.document.UnsavedChangesDialog
import com.klyx.lsp.EditorLsp
import com.klyx.lsp.findReferences
import com.klyx.lsp.goBack
import com.klyx.lsp.goToDefinition
import com.klyx.lsp.selectReference
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.Settings
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.settings.resolveActiveThemeName
import com.klyx.ui.palette.EditorCommand
import com.klyx.ui.palette.LocalRunEditorCommand
import com.klyx.ui.palette.LocalShortcutRecording
import com.klyx.ui.palette.Palette
import com.klyx.ui.palette.ReferenceDialog
import com.klyx.ui.palette.binding
import com.klyx.ui.palette.rememberRecentFiles
import com.klyx.ui.provider.ProvideCompositionLocals
import com.klyx.ui.screen.HomeScreen
import com.klyx.ui.screen.OnboardingScreen
import com.klyx.ui.screen.settings.SettingsScreen
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.LocalColorScheme
import com.klyx.ui.theme.LocalCornerRadius
import com.klyx.ui.theme.background
import com.klyx.ui.theme.colors
import com.klyx.ui.utils.BackHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.compose.koinInject

@Composable
fun Screen(
    onExit: () -> Unit = {},
    enableOnboarding: Boolean = false,
    storageAccessGranted: Boolean = true,
    onRequestStorageAccess: (() -> Unit)? = null,
) {
    val settingsStore: SettingsStore = koinInject()
    val persistence: DocumentPersistence = koinInject()
    val toastMessages = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }

    val shortcutRecording = remember { mutableStateOf(false) }
    val settings by settingsStore.settings.collectAsState()
    val onboardingCompleted by produceState<Boolean?>(null, enableOnboarding) {
        value = !enableOnboarding || settingsStore.read().onboardingCompleted
    }

    val effectiveCornerRadius = if (settings.roundedUi) settings.cornerRadius.dp else 0.dp

    val resolvedThemeName = resolveActiveThemeName(
        themeMode = settings.themeMode,
        darkThemeName = settings.themeName,
        lightThemeName = settings.lightThemeName
    )

    ProvideCompositionLocals(
        themeName = resolvedThemeName,
        fontName = settings.fontName
    ) {
        CompositionLocalProvider(
            LocalShortcutRecording provides shortcutRecording,
            LocalCornerRadius provides effectiveCornerRadius,
        ) {
            KlyxTheme(colorScheme = LocalColorScheme.current) {
                if (onboardingCompleted == null) return@KlyxTheme
                val needsFileAccess = !storageAccessGranted && onRequestStorageAccess != null
                val needsOnboarding = enableOnboarding && onboardingCompleted == false && !settings.onboardingCompleted
                if (needsFileAccess || needsOnboarding) {
                    OnboardingScreen(
                        storageAccessGranted = storageAccessGranted,
                        onRequestStorageAccess = { onRequestStorageAccess?.invoke() },
                    )
                    return@KlyxTheme
                }
                val navigator = LocalNavigator.current
                val showSettings = navigator.currentRoute is Settings

                var picker by remember { mutableStateOf<EditorCommand?>(null) }
                val recent = rememberRecentFiles()
                val lsp: EditorLsp = koinInject()

                fun runCommand(command: EditorCommand) {
                    when (command) {
                        EditorCommand.QuickOpen, EditorCommand.FindInFiles, EditorCommand.Palette -> {
                            if (showSettings) navigator.navigateBack()
                            picker = command
                        }

                        EditorCommand.Definition -> lsp.goToDefinition()
                        EditorCommand.References -> lsp.findReferences()
                        EditorCommand.Back -> lsp.goBack()
                        EditorCommand.Settings -> navigator.navigateTo(Settings)
                        else -> command.action?.let { action(it)() }
                    }
                }

                val workspaceFocus = remember { FocusRequester() }
                var hasWorkspaceFocus by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (!hasWorkspaceFocus) workspaceFocus.requestFocus()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (shortcutRecording.value) return@onPreviewKeyEvent false
                            val binding = event.binding() ?: return@onPreviewKeyEvent false
                            val command = EditorCommand.entries.firstOrNull {
                                it.binding(settings.keybindings) == binding
                            } ?: return@onPreviewKeyEvent false

                            if (event.type == KeyEventType.KeyDown) runCommand(command)
                            true
                        }
                        .onFocusChanged { hasWorkspaceFocus = it.hasFocus }
                        .focusRequester(workspaceFocus)
                        .focusable()
                        .background(Theme[colors][background]),
                ) {
                    BackHandler(enabled = !showSettings) {
                        persistence.requestExit(onExit)
                    }
                    UnsavedChangesDialog()

                    CompositionLocalProvider(LocalRunEditorCommand provides ::runCommand) {
                        HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            onCommands = { runCommand(EditorCommand.Palette) },
                            toastMessages = toastMessages
                        )
                    }

                    val references by lsp.referenceResults.collectAsState()

                    references?.let { results ->
                        ReferenceDialog(results.locations, { lsp.referenceResults.value = null }) {
                            lsp.selectReference(results, it)
                        }
                    }

                    picker?.let { Palette(it, recent, settings.keybindings, ::runCommand) { picker = null } }

                    AnimatedVisibility(
                        visible = showSettings,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SettingsScreen(
                            onAppFilesOpened = { toastMessages.tryEmit("App files opened in the drawer") },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Theme[colors][background]),
                        )
                    }
                }
            }
        }
    }
}

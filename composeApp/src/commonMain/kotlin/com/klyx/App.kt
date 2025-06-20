package com.klyx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.klyx.core.cmd.CommandManager
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.theme.ThemeManager
import com.klyx.ui.component.ThemeSelector
import com.klyx.ui.component.cmd.CommandPalette
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.menu.MainMenuBar
import com.klyx.ui.theme.KlyxTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App(
    dynamicColor: Boolean = true,
    darkTheme: Boolean = false,
    useThemeExtension: Boolean = true,
    themeName: String? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    KlyxTheme(
        dynamicColor = dynamicColor,
        darkTheme = darkTheme,
        useThemeExtension = useThemeExtension,
        themeName = themeName
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val background = remember(colorScheme) { colorScheme.background }

        LaunchedEffect(Unit) {
            lifecycleOwner.subscribeToEvent<KeyEvent> { event ->
                if (event.isCtrlPressed && event.isShiftPressed && event.key == Key.P) {
                    CommandManager.showPalette()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.surfaceColorAtElevation(5.dp),
            contentColor = colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                MainMenuBar()

                if (CommandManager.showCommandPalette) {
                    CommandPalette(
                        commands = CommandManager.commands,
                        recentlyUsedCommands = CommandManager.recentlyUsedCommands,
                        onDismissRequest = CommandManager::hidePalette
                    )
                }

                if (ThemeManager.showThemeSelector) {
                    ThemeSelector(
                        onDismissRequest = ThemeManager::hideThemeSelector
                    )
                }

                Surface(color = background) {
                    EditorScreen()
                }
            }
        }
    }
}

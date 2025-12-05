@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.page

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.klyx.LocalDrawerState
import com.klyx.LocalNavigator
import com.klyx.Route
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.key.KeyShortcut
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.Worktree
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.toKxFile
import com.klyx.core.icon.Klyx
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.theme.blend
import com.klyx.core.ui.component.ShortcutText
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.openIfClosed
import com.klyx.viewmodel.openExtensionScreen
import com.klyx.viewmodel.openUntitledFile
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch

@RememberInComposition
fun createWelcomePage() = movableContentOf { WelcomePage() }

@Composable
fun WelcomePage() {
    val klyxViewModel = LocalKlyxViewModel.current
    val editorViewModel = LocalEditorViewModel.current

    val containerSize = LocalWindowInfo.current.containerDpSize
    val coroutineScope = rememberCoroutineScope()
    val drawerState = LocalDrawerState.current
    val navigator = LocalNavigator.current

    val directoryPicker = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            val kx = file.toKxFile()

            if (kx.isPermissionRequired(R_OK or W_OK)) {
                klyxViewModel.showPermissionDialog()
            } else {
                klyxViewModel.openProject(Worktree(kx))
                coroutineScope.launch { drawerState.openIfClosed() }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .sizeIn(maxWidth = containerSize.width * 0.8f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val hue by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 5000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val colors by remember {
                derivedStateOf {
                    List(5) { i ->
                        val shiftedHue = (hue + i * 72) % 360f
                        Color.hsv(shiftedHue, 0.8f, 1f).blend(primaryColor, fraction = 0.6f)
                    }
                }
            }

            val brush by remember {
                derivedStateOf {
                    Brush.linearGradient(
                        colors = colors,
                        start = Offset.Zero,
                        end = Offset(300f, 300f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    KlyxIcons.Klyx,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer { alpha = 0.99f }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush, blendMode = BlendMode.SrcAtop)
                            }
                        },
                    tint = Color.Unspecified
                )

                Text(
                    text = "Welcome to Klyx",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = brush,
                        fontWeight = FontWeight.SemiBold
                    ),
                    letterSpacing = 0.08.em
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextWithDivider("GET STARTED")
            Spacer(modifier = Modifier.height(12.dp))

            TextButtonWithShortcutAndIcon(
                text = "New",
                shortcut = null,
                icon = Icons.Default.Add,
                onClick = editorViewModel::openUntitledFile
            )

            TextButtonWithShortcutAndIcon(
                text = "Open Project",
                shortcut = keyShortcutOf(ctrl = true, key = Key.O),
                icon = Icons.Outlined.DriveFolderUpload,
                onClick = directoryPicker::launch
            )

            TextButtonWithShortcutAndIcon(
                text = "Open Command Palette",
                shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.P),
                icon = Icons.Default.KeyboardCommandKey,
                onClick = CommandManager::showCommandPalette
            )

            Spacer(modifier = Modifier.height(12.dp))
            TextWithDivider("CONFIGURE")
            Spacer(modifier = Modifier.height(12.dp))

            TextButtonWithShortcutAndIcon(
                text = "Open Settings",
                shortcut = null,
                icon = Icons.Outlined.Settings,
                onClick = { navigator.navigateTo(Route.Settings) }
            )

            TextButtonWithShortcutAndIcon(
                text = "Explore Extensions",
                shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.X),
                icon = Icons.Outlined.Extension,
                onClick = editorViewModel::openExtensionScreen
            )
        }
    }
}

@Composable
private fun TextButtonWithShortcutAndIcon(
    text: String,
    shortcut: KeyShortcut?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProvideTextStyle(MaterialTheme.typography.labelMediumEmphasized) {

                Icon(modifier = Modifier.size(18.dp), imageVector = icon, contentDescription = null)
                Text(modifier = Modifier.padding(start = 8.dp), text = text)
                Spacer(modifier = Modifier.weight(1f))

                shortcut?.let {
                    ShortcutText(
                        shortcut = it,
                        style = LocalTextStyle.current.merge(MaterialTheme.typography.labelSmallEmphasized)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextWithDivider(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ProvideTextStyle(MaterialTheme.typography.labelSmallEmphasized) {
            Text(text = text)
        }

        Spacer(modifier = Modifier.width(6.dp))
        HorizontalDivider()
    }
}

package com.klyx.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.klyx.core.DocsUrl
import com.klyx.core.theme.ThemeManager
import com.klyx.res.Res.drawable
import com.klyx.res.klyx_transparent
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.openExtensionScreen
import com.klyx.viewmodel.openSettings
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val viewModel: EditorViewModel = koinViewModel()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(drawable.klyx_transparent),
            contentDescription = "Logo",
            modifier = Modifier.size(90.dp),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )

        Text(
            text = "Welcome to Klyx",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "The editor for what's next",
            style = MaterialTheme.typography.labelLarge,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .alpha(0.5f)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                ClickableText(
                    text = "Choose a Theme",
                    icon = Icons.Outlined.Palette
                ) {
                    ThemeManager.toggleThemeSelector()
                }

                ClickableText(
                    text = "Choose a Keymap",
                    enabled = false,
                    icon = Icons.Outlined.Keyboard
                )

                ClickableText(
                    text = "Edit Settings",
                    icon = Icons.Outlined.Settings,
                    onClick = viewModel::openSettings
                )
            }

            VerticalDivider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = "Resources",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .alpha(0.5f)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                ClickableText(
                    text = "View Documentation",
                    icon = Icons.Outlined.Code
                ) {
                    uriHandler.openUri(DocsUrl)
                }

                ClickableText(
                    text = "Explore Extensions",
                    icon = Icons.Outlined.Extension,
                    onClick = viewModel::openExtensionScreen
                )
            }
        }
    }
}

@Composable
private fun ClickableText(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

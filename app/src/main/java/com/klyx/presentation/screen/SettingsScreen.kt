package com.klyx.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.BuildConfig
import com.klyx.presentation.model.IconSource
import com.klyx.presentation.model.SettingsCategory
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.ui.theme.LocalIsDarkMode

object SettingScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
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
        LazyColumn(
            contentPadding = innerPadding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SettingsCategoryItem(
                    category = SettingsCategory.Editor,
                    shape = FirstItemShape,
                    onClick = { navigator.navigateTo(SettingsScreen.Editor) }
                )
            }

            item {
                SettingsCategoryItem(
                    category = SettingsCategory.Appearance,
                    onClick = { navigator.navigateTo(SettingsScreen.Appearance) }
                )
            }

            item {
                SettingsCategoryItem(
                    category = SettingsCategory.Terminal,
                    onClick = { navigator.navigateTo(SettingsScreen.Terminal) }
                )
            }

            //if (BuildConfig.DEBUG) {
                item {
                    SettingsCategoryItem(
                        category = SettingsCategory.DeveloperOptions,
                        onClick = { navigator.navigateTo(SettingsScreen.DeveloperOptions) }
                    )
                }
            //}

            item {
                SettingsCategoryItem(
                    category = SettingsCategory.SystemDiagnostics,
                    onClick = { navigator.navigateTo(SettingsScreen.SystemDiagnostics) }
                )
            }

            item {
                SettingsCategoryItem(
                    category = SettingsCategory.About,
                    shape = LastItemShape,
                    onClick = { navigator.navigateTo(SettingsScreen.About) }
                )
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }
        }
    }
}

private data class CategoryColors(val container: Color, val onContainer: Color)

@Composable
private fun SettingsCategory.colors(): CategoryColors {
    val isDark = LocalIsDarkMode.current
    return if (isDark) {
        when (this) {
            SettingsCategory.Editor -> CategoryColors(Color(0xFF2D333B), Color(0xFFADBAC7))
            SettingsCategory.Appearance -> CategoryColors(Color(0xFF7D5260), Color(0xFFFFD8E4))

            SettingsCategory.Terminal -> CategoryColors(Color(0xFF202A25), Color(0xFF81C995))

            SettingsCategory.DeveloperOptions -> CategoryColors(
                Color(0xFF324F34),
                Color(0xFFCBEFD0)
            )

            SettingsCategory.SystemDiagnostics -> CategoryColors(
                Color(0xFF004D61),
                Color(0xFFACEFEE)
            )

            SettingsCategory.About -> CategoryColors(Color(0xFF3F474D), Color(0xFFDEE3EB))
        }
    } else {
        when (this) {
            SettingsCategory.Editor -> CategoryColors(Color(0xFFF6F8FA), Color(0xFF24292F))
            SettingsCategory.Appearance -> CategoryColors(Color(0xFFFFD8E4), Color(0xFF631835))

            SettingsCategory.Terminal -> CategoryColors(Color(0xFFE6F4EA), Color(0xFF0D5323))

            SettingsCategory.DeveloperOptions -> CategoryColors(
                Color(0xFFCBEFD0),
                Color(0xFF042106)
            )

            SettingsCategory.SystemDiagnostics -> CategoryColors(
                Color(0xFFACEFEE),
                Color(0xFF002022)
            )

            SettingsCategory.About -> CategoryColors(Color(0xFFEFF1F7), Color(0xFF44474F))
        }
    }
}

private val FirstItemShape =
    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val LastItemShape =
    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
private val MiddleItemShape = RoundedCornerShape(4.dp)

@Composable
private fun SettingsCategoryItem(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CategoryColors = category.colors(),
    shape: Shape = MiddleItemShape,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.container)
            ) {
                when (val iconSource = category.icon) {
                    is IconSource.Vector -> {
                        Icon(
                            imageVector = iconSource.imageVector,
                            contentDescription = null,
                            tint = colors.onContainer
                        )
                    }

                    is IconSource.DrawableRes -> {
                        Icon(
                            painter = painterResource(iconSource.id),
                            contentDescription = null,
                            tint = colors.onContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = category.subtitle,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

package com.klyx.ui.page.settings.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardTab
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import com.klyx.LocalNavigator
import com.klyx.core.icon.BrandFamily
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.settings.LocalEditorSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitch
import com.klyx.res.Res.string
import com.klyx.res.common
import com.klyx.res.editor_settings
import com.klyx.res.font_family
import com.klyx.res.font_size
import com.klyx.res.pin_line_numbers
import com.klyx.res.pin_line_numbers_desc
import com.klyx.res.properties
import com.klyx.res.tab_size
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPreferences() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val settings = LocalEditorSettings.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(string.editor_settings)) },
                navigationIcon = { BackButton(navigator::navigateBack) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        var showFontFamilyDialog by rememberSaveable { mutableStateOf(false) }
        var showFontSizeDialog by rememberSaveable { mutableStateOf(false) }
        var showTabSizeDialog by rememberSaveable { mutableStateOf(false) }

        LazyColumn(contentPadding = paddingValues) {
            item { PreferenceSubtitle(stringResource(string.common)) }

            item {
                PreferenceItem(
                    title = stringResource(string.font_family),
                    icon = KlyxIcons.BrandFamily,
                    description = settings.fontFamily.name,
                    onClick = { showFontFamilyDialog = true }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(string.font_size),
                    icon = Icons.Outlined.TextFields,
                    description = "${settings.fontSize} sp",
                    onClick = { showFontSizeDialog = true }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(string.tab_size),
                    icon = Icons.AutoMirrored.Outlined.KeyboardTab,
                    description = "${settings.tabSize}",
                    onClick = { showTabSizeDialog = true }
                )
            }

            item { PreferenceSubtitle(stringResource(string.properties)) }

            item {
                PreferenceSwitch(
                    title = stringResource(string.pin_line_numbers),
                    description = stringResource(string.pin_line_numbers_desc),
                    icon = Icons.Outlined.FormatListNumbered,
                    isChecked = settings.pinLineNumbers,
                    onClick = { checked ->
                        settings.update { it.copy(pinLineNumbers = checked) }
                    }
                )
            }
        }

        if (showFontFamilyDialog) {
            FontFamilyDialog(
                settings = settings,
                onDismissRequest = { showFontFamilyDialog = false }
            )
        }

        if (showFontSizeDialog) {
            FontSizeDialog(
                settings = settings,
                onDismissRequest = { showFontSizeDialog = false }
            )
        }

        if (showTabSizeDialog) {
            TabSizeDialog(
                settings = settings,
                onDismissRequest = { showTabSizeDialog = false }
            )
        }
    }
}

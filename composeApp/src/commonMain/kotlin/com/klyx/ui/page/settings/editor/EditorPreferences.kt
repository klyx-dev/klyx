package com.klyx.ui.page.settings.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardTab
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.klyx.core.LocalAppSettings
import com.klyx.core.icon.BrandFamily
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.settings.update
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.core.ui.component.OutlinedButtonChip
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitch
import com.klyx.res.Res.string
import com.klyx.res.common
import com.klyx.res.editor_settings
import com.klyx.res.font_family
import com.klyx.res.font_family_name
import com.klyx.res.font_size
import com.klyx.res.no_font_family_set
import com.klyx.res.pin_line_numbers
import com.klyx.res.pin_line_numbers_desc
import com.klyx.res.properties
import com.klyx.res.tab_size
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPreferences(onNavigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val settings = LocalAppSettings.current.editor
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(string.editor_settings)) },
                navigationIcon = { BackButton(onNavigateBack) },
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
                    description = settings.fontFamily.ifBlank { stringResource(string.no_font_family_set) },
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
            var fontFamily by rememberSaveable { mutableStateOf(settings.fontFamily) }

            AlertDialog(
                onDismissRequest = { showFontFamilyDialog = false },
                confirmButton = {
                    ConfirmButton {
                        settings.update { it.copy(fontFamily = fontFamily) }
                        showFontFamilyDialog = false
                    }
                },
                dismissButton = { DismissButton { showFontFamilyDialog = false } },
                icon = { Icon(KlyxIcons.BrandFamily, contentDescription = null) },
                title = { Text(stringResource(string.font_family), textAlign = TextAlign.Center) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = buildAnnotatedString {
                                appendLine("The name of a font to use for rendering text in the editor.")
                                appendLine()
                                append("Any font from ")
                                withLink(LinkAnnotation.Url("https://fonts.google.com")) {
                                    append("Google Fonts")
                                }
                                append(" can be used.")
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedTextField(
                            modifier = Modifier.padding(vertical = 8.dp),
                            value = fontFamily,
                            onValueChange = { fontFamily = it },
                            shape = MaterialTheme.shapes.small,
                            placeholder = { Text(stringResource(string.font_family_name)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            OutlinedButtonChip(
                                label = "Google Fonts",
                                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            ) {
                                uriHandler.openUri("https://fonts.google.com")
                            }
                        }
                    }
                }
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

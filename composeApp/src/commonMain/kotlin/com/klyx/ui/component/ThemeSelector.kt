package com.klyx.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.klyx.core.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.theme.ThemeManager
import com.klyx.ui.theme.DefaultKlyxShape

@Composable
fun ThemeSelector(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allThemes = ThemeManager.availableThemes
    val settings = LocalAppSettings.current

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            //excludeFromSystemGesture = true
        ),
        alignment = Alignment.TopCenter
    ) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .imePadding()
                .heightIn(min = 100.dp, max = 500.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            var searchQuery by remember { mutableStateOf("") }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable()
                    .focusRequester(focusRequester),
                placeholder = { Text("Select Theme...") }
            )

            val filteredTheme = remember(searchQuery) {
                allThemes.fastFilter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            if (filteredTheme.isNotEmpty()) {
                LazyColumn(modifier = Modifier.padding(3.dp)) {
                    items(filteredTheme) { theme ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(DefaultKlyxShape)
                                .clickable {
                                    settings.update {
                                        it.copy(
                                            theme = theme.name,
                                            dynamicColor = false
                                        )
                                    }
                                    //onDismissRequest()
                                }
                                .then(
                                    if (settings.theme == theme.name) {
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        val themeName = theme.name
                                        val query = searchQuery.lowercase()
                                        val startIndex = themeName.lowercase().indexOf(query)
                                        if (startIndex != -1) {
                                            append(themeName.take(startIndex))
                                            withStyle(
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                append(themeName.substring(startIndex, startIndex + query.length))
                                            }
                                            append(themeName.substring(startIndex + query.length))
                                        } else {
                                            append(themeName)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("No themes available")
                }
            }
        }
    }
}

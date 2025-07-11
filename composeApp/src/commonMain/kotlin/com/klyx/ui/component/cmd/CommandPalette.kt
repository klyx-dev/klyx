package com.klyx.ui.component.cmd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.klyx.core.cmd.Command
import com.klyx.core.cmd.CommandManager
import com.klyx.core.theme.harmonizeWithPrimary

@Composable
fun CommandPalette(
    commands: Set<Command>,
    recentlyUsedCommands: Set<Command>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val allCommands = remember {
        recentlyUsedCommands + commands.filter {
            it !in recentlyUsedCommands
        }.toMutableList().apply { sortBy { it.name.lowercase() } }
    }

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
                placeholder = { Text("Execute a command...") }
            )

            val filteredCommands = remember(searchQuery) {
                allCommands.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(modifier = Modifier.padding(3.dp)) {
                items(filteredCommands) { command ->
                    val isRecentlyUsed = remember(command) { command in recentlyUsedCommands }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                CommandManager.addRecentlyUsedCommand(command)
                                command.execute(command)
                                onDismissRequest()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = command.name,
                                modifier = if (isRecentlyUsed) {
                                    Modifier
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                },
                                fontSize = 14.sp
                            )

                            if (isRecentlyUsed) {
                                Text(
                                    text = "Recently used",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp)
                                        .weight(1f),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.harmonizeWithPrimary(
                                        0.6f
                                    )
                                )
                            }

                            if (command.shortcuts.isNotEmpty()) {
                                Text(
                                    text = command.shortcuts.fastJoinToString(" ") { it.toString() },
                                    color = Color(0xFF1369FF).harmonizeWithPrimary(0.5f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

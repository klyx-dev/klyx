package com.klyx.ui.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.build.BuildInfo
import com.klyx.core.utils.clipEntryOfText
import com.klyx.runtime.currentOperatingSystem
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxTextField
import com.klyx.ui.theme.label
import com.klyx.ui.theme.labelSmall
import com.klyx.ui.theme.typography
import kotlinx.coroutines.launch

private const val ISSUE_URL = "https://github.com/klyx-dev/klyx/issues/new"
private val CATEGORIES = listOf("bug", "confusing", "missing", "slow")

@Composable
fun FeedbackDialog(visible: Boolean, onDismiss: () -> Unit) {
    val message = rememberTextFieldState()
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var copied by remember { mutableStateOf(false) }

    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(visible) {
        if (visible) copied = false
    }

    fun body(): String = buildString {
        appendLine("### What happened")
        appendLine(message.text.toString().trim().ifBlank { "(not provided)" })
        appendLine()
        appendLine("### Build")
        appendLine("- Version: ${BuildInfo.VERSION_NAME} (${BuildInfo.BUILD_NUMBER})")
        appendLine("- Commit: ${BuildInfo.COMMIT} (${BuildInfo.BRANCH})")
        appendLine("- Platform: ${currentOperatingSystem.name}")
        appendLine("- Category: $category")
    }.trim()

    KxDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "send feedback",
        subtitle = "alpha ${BuildInfo.VERSION_NAME} · ${BuildInfo.COMMIT.take(7)}",
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KxButton(
                    onClick = onDismiss,
                    variant = KxButtonVariant.Ghost,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("cancel", style = Theme[typography][label])
                }

                KxButton(
                    onClick = {
                        val text = body()
                        scope.launch {
                            clipboard.setClipEntry(clipEntryOfText(text))
                        }
                    },
                    variant = KxButtonVariant.Outlined,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(if (copied) "copied" else "copy", style = Theme[typography][label])
                }

                KxButton(
                    onClick = {
                        val title = "[alpha] $category: " + message.text.toString().trim().take(60)
                        val url = ISSUE_URL +
                                "?title=${title.percentEncode()}" +
                                "&body=${body().percentEncode()}" +
                                "&labels=alpha,feedback"
                        try {
                            uriHandler.openUri(url)
                        } catch (_: Throwable) {
                            //
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("open issue", style = Theme[typography][label])
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "what kind of feedback?",
                style = Theme[typography][labelSmall],
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CATEGORIES.forEach { option ->
                    KxButton(
                        onClick = { category = option },
                        variant = if (category == option) {
                            KxButtonVariant.Filled
                        } else {
                            KxButtonVariant.Outlined
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(option, style = Theme[typography][label])
                    }
                }
            }

            KxTextField(
                state = message,
                label = "what happened?",
                placeholder = "steps, expected result, actual result",
                singleLine = false,
            )
        }
    }
}

private fun String.percentEncode(): String {
    val hex = "0123456789ABCDEF"
    val bytes = encodeToByteArray()

    return buildString(bytes.size * 3) {
        for (byte in bytes) {
            when (val value = byte.toInt() and 0xFF) {
                0x20 -> append("%20")
                in 0x30..0x39, in 0x41..0x5A, in 0x61..0x7A, 0x2D, 0x2E, 0x5F, 0x7E -> append(value.toChar())
                else -> {
                    append('%')
                    append(hex[value shr 4])
                    append(hex[value and 0x0F])
                }
            }
        }
    }
}

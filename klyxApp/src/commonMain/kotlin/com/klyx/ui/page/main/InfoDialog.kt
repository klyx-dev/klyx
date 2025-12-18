package com.klyx.ui.page.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.klyx.core.GitHub
import com.klyx.core.clipboard.clipEntryOf
import com.klyx.core.ui.component.FilledButtonWithIcon
import com.klyx.core.ui.component.OutlinedButtonChip
import com.klyx.core.ui.component.OutlinedButtonWithIcon
import com.klyx.platform
import com.klyx.platform.PlatformInfo
import com.klyx.resources.Res
import com.klyx.resources.release
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun InfoDialog(onDismissRequest: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val systemInfo = remember {
        buildString {
            appendLine("Klyx | ${PlatformInfo.appVersion} (${PlatformInfo.buildNumber})")
            appendLine("${PlatformInfo.name} ${PlatformInfo.version} | ${PlatformInfo.deviceModel}")
            appendLine("${platform().os} | ${PlatformInfo.architecture}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            OutlinedButtonWithIcon(
                icon = Icons.Outlined.Close,
                text = "Close",
                onClick = onDismissRequest
            )
        },
        dismissButton = {
            FilledButtonWithIcon(
                icon = Icons.Outlined.ContentCopy,
                text = "Copy and Close",
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(clipEntryOf(systemInfo))
                    }
                    onDismissRequest()
                }
            )
        },
        icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
        title = { Text("Info", textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(
                        text = buildAnnotatedString {
                            append(systemInfo)
                            appendLine()
                            append("It's an open-source project available on ")
                            withLink(LinkAnnotation.Url(GitHub.KLYX_REPO_URL)) {
                                append("GitHub")
                            }
                            append(", built using ")
                            withLink(LinkAnnotation.Url("https://www.jetbrains.com/compose-multiplatform/")) {
                                append("Compose Multiplatform")
                            }
                            appendLine(".")
                            appendLine()
                            appendLine("Contributions are welcome, check out the repository for setup and development details.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButtonChip(
                        label = "GitHub",
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    ) {
                        uriHandler.openUri(GitHub.KLYX_REPO_URL)
                    }

                    OutlinedButtonChip(
                        label = stringResource(Res.string.release),
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    ) {
                        uriHandler.openUri(GitHub.RELEASE_URL)
                    }
                }
            }
        }
    )
}

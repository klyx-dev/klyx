package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.GitHub
import com.klyx.core.LocalNotifier
import com.klyx.core.icon.GithubAlt
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.ui.component.TextButtonWithIcon
import com.klyx.icons.BugReport
import com.klyx.icons.Feedback
import com.klyx.icons.Icons
import com.klyx.icons.Mail
import com.klyx.icons.Star

@Composable
fun GiveFeedbackDialog(onDismissRequest: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val notifier = LocalNotifier.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { },
        icon = { Icon(Icons.Feedback, contentDescription = null) },
        title = { Text("Give Feedback", textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Thanks for using Klyx! To share your experience with us, " +
                            "reach for the channel that's the most appropriate:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButtonWithIcon(
                    icon = Icons.BugReport,
                    text = "File a Bug Report",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { uriHandler.openUri(GitHub.BUG_REPORT_URL) }
                )

                TextButtonWithIcon(
                    icon = Icons.Star,
                    text = "Request a Feature",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { uriHandler.openUri(GitHub.FEATURE_REQUEST_URL) }
                )

                TextButtonWithIcon(
                    icon = Icons.Mail,
                    text = "Send an Email",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        try {
                            uriHandler.openUri("mailto:itsvks19@gmail.com")
                        } catch (_: IllegalArgumentException) {
                            notifier.toast("No app found to handle sending email")
                        }
                    }
                )

                TextButtonWithIcon(
                    icon = KlyxIcons.GithubAlt,
                    text = "GitHub Repository",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { uriHandler.openUri(GitHub.KLYX_REPO_URL) }
                )
            }
        }
    )
}

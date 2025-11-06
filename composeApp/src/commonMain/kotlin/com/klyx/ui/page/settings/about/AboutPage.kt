package com.klyx.ui.page.settings.about

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import com.klyx.LocalNavigator
import com.klyx.core.GitHub
import com.klyx.core.LocalNotifier
import com.klyx.core.clipboard.clipEntryOf
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.platform.PlatformInfo
import com.klyx.res.Res
import com.klyx.res.readme
import com.klyx.res.readme_desc
import com.klyx.res.release
import com.klyx.res.release_desc
import com.klyx.res.version
import com.klyx.ui.page.main.GiveFeedbackDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AboutPage() {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val notifier = LocalNotifier.current
    val scope = rememberCoroutineScope()

    val navigator = LocalNavigator.current
    @Suppress("DEPRECATION")
    BackHandler { navigator.navigateBack() }

    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    var showFeedbackDialog by remember { mutableStateOf(false) }

    LazyColumn {
        item {
            PreferenceItem(
                title = stringResource(Res.string.readme),
                description = stringResource(Res.string.readme_desc),
                icon = Icons.Outlined.Description,
            ) {
                openUrl(GitHub.KLYX_REPO_URL)
            }
        }

        item {
            PreferenceItem(
                title = stringResource(Res.string.release),
                description = stringResource(Res.string.release_desc),
                icon = Icons.Outlined.NewReleases,
            ) {
                openUrl(GitHub.RELEASE_URL)
            }
        }

        item {
            PreferenceItem(
                title = stringResource(Res.string.version),
                description = PlatformInfo.appVersion,
                icon = Icons.Outlined.Info
            ) {
                scope.launch {
                    clipboard.setClipEntry(
                        clipEntryOf(
                            """
                                App version: ${PlatformInfo.appVersion} (${PlatformInfo.buildNumber})
                                Device information: ${PlatformInfo.name} ${PlatformInfo.version} (${PlatformInfo.architecture})
                                Device model: ${PlatformInfo.deviceModel}
                            """.trimIndent()
                        )
                    )

                    notifier.toast("Info copied to clipboard")
                }
            }
        }

        item {
            PreferenceItem(
                title = "Give Feedback",
                icon = Icons.Outlined.Feedback,
                onClick = { showFeedbackDialog = !showFeedbackDialog }
            )
        }
    }

    if (showFeedbackDialog) {
        GiveFeedbackDialog(
            onDismissRequest = { showFeedbackDialog = false }
        )
    }
}

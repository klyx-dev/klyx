package com.klyx.ui.page.settings.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import com.klyx.LocalNavigator
import com.klyx.core.GitHub
import com.klyx.core.LocalNotifier
import com.klyx.core.app.LocalBuildInfo
import com.klyx.core.clipboard.clipEntryOf
import com.klyx.core.platform.LocalPlatform
import com.klyx.core.platform.deviceModel
import com.klyx.core.platform.version
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.icons.Description
import com.klyx.icons.Feedback
import com.klyx.icons.Icons
import com.klyx.icons.Info
import com.klyx.icons.NewReleases
import com.klyx.resources.Res
import com.klyx.resources.about
import com.klyx.resources.readme
import com.klyx.resources.readme_desc
import com.klyx.resources.release
import com.klyx.resources.release_desc
import com.klyx.resources.version
import com.klyx.ui.page.main.GiveFeedbackDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val notifier = LocalNotifier.current
    val scope = rememberCoroutineScope()

    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.about)) },
                navigationIcon = { BackButton(navigator::navigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        var showFeedbackDialog by remember { mutableStateOf(false) }

        LazyColumn(contentPadding = padding) {
            item {
                PreferenceItem(
                    title = stringResource(Res.string.readme),
                    description = stringResource(Res.string.readme_desc),
                    icon = Icons.Description,
                ) {
                    openUrl(GitHub.KLYX_REPO_URL)
                }
            }

            item {
                PreferenceItem(
                    title = stringResource(Res.string.release),
                    description = stringResource(Res.string.release_desc),
                    icon = Icons.NewReleases,
                ) {
                    openUrl(GitHub.RELEASE_URL)
                }
            }

            item {
                val buildInfo = LocalBuildInfo.current
                val platform = LocalPlatform.current

                PreferenceItem(
                    title = stringResource(Res.string.version),
                    description = buildInfo.versionName,
                    icon = Icons.Info
                ) {
                    scope.launch {
                        clipboard.setClipEntry(
                            clipEntryOf(
                                """
                                App version: ${buildInfo.versionName} (${buildInfo.versionCode})
                                Device information: ${platform.os} ${platform.version} (${platform.architecture})
                                Device model: ${platform.deviceModel}
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
                    icon = Icons.Feedback,
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
}

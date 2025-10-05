package com.klyx.ui.page.settings.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import com.klyx.core.GitHub
import com.klyx.core.LocalNotifier
import com.klyx.core.clipboard.clipEntryOf
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.platform.PlatformInfo
import com.klyx.res.Res
import com.klyx.res.about
import com.klyx.res.readme
import com.klyx.res.readme_desc
import com.klyx.res.release
import com.klyx.res.release_desc
import com.klyx.res.version
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
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
        }
    }
}

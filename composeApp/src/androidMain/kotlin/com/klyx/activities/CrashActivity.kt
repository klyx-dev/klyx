package com.klyx.activities

import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.core.ReportIssueUrl
import com.klyx.ui.theme.KlyxTheme
import java.util.Date

class CrashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CRASH_LOG = "extra_crash_log"
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashLog = intent.getStringExtra(EXTRA_CRASH_LOG) ?: "No crash log"

        setContent {
            KlyxTheme {
                val uriHandler = LocalUriHandler.current
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                Scaffold(
                    modifier = Modifier.Companion.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeFlexibleTopAppBar(
                            title = {
                                Text(
                                    "App Crash Report",
                                    maxLines = 1,
                                    overflow = TextOverflow.Companion.Ellipsis
                                )
                            },
                            subtitle = {
                                Text(
                                    Date().toString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Companion.Ellipsis
                                )
                            },
                            navigationIcon = {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text("Close App") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(onClick = {
                                        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close App"
                                        )
                                    }
                                }
                            },
                            actions = {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text("Report Crash") } },
                                    state = rememberTooltipState(),
                                ) {
                                    TextButton(onClick = {
                                        uriHandler.openUri(ReportIssueUrl)
                                    }) {
                                        Text("Report Crash")
                                    }
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.Companion
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                text = crashLog,
                                modifier = Modifier.Companion.padding(16.dp),
                                fontFamily = FontFamily.Companion.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
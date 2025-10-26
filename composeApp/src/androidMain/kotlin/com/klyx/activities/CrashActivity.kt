package com.klyx.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
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
import com.blankj.utilcode.util.ClipboardUtils
import com.klyx.core.REPORT_ISSUE_URL
import com.klyx.ui.theme.KlyxTheme

class CrashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CRASH_LOG = "extra_crash_log"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val crashLog = intent.getStringExtra(EXTRA_CRASH_LOG) ?: "No crash log"

        setContent {
            KlyxTheme {
                val uriHandler = LocalUriHandler.current
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = {
                                Text(
                                    "App Crash Report",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
//                            subtitle = {
//                                Text(
//                                    Date().toString(),
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis
//                                )
//                            },
                            navigationIcon = {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Below),
                                    tooltip = { PlainTooltip { Text("Close App") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(onClick = ::finishAffinity) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close App"
                                        )
                                    }
                                }
                            },
                            actions = {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Below),
                                    tooltip = { PlainTooltip { Text("Report Crash") } },
                                    state = rememberTooltipState(),
                                ) {
                                    TextButton(onClick = {
                                        uriHandler.openUri(REPORT_ISSUE_URL)
                                    }) {
                                        Text("Report Crash")
                                    }
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                ClipboardUtils.copyText(crashLog).also {
                                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            text = { Text("Copy") },
                            icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                text = crashLog,
                                modifier = Modifier.padding(16.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

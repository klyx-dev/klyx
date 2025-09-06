package com.klyx.ui.component.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.klyx.core.logging.backgroundColor
import com.klyx.core.logging.color
import com.klyx.core.logging.toLogString
import com.klyx.ui.theme.rememberFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn

@OptIn(FlowPreview::class)
@Composable
fun LogViewer(
    buffer: LogBuffer,
    modifier: Modifier = Modifier
) {
    val fontFamily = rememberFontFamily("JetBrains Mono")
    val listState = rememberLazyListState()

    val logs by buffer.logs
        .flowWithLifecycle(LocalLifecycleOwner.current.lifecycle)
        .flowOn(Dispatchers.IO.limitedParallelism(1))
        .collectAsState(emptyList())

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.lastIndex)
        }
    }

    Surface(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        color = MaterialTheme.colorScheme.surface
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = log.level.color,
                                    background = log.level.backgroundColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(" ${log.level.displayName.first()} ")
                            }

//                            append(" [${log.timestamp.toLogString()}]")
//                            append(" (${log.threadName})")

                            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                                append(" [${log.tag}]")
                            }

                            append(": ${log.message}")

                            log.throwable?.let {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(" ⚠ ${it.message}")
                                }
                            }
                        },
                        color = log.level.color,
                        softWrap = false,
                        fontFamily = fontFamily,
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }
            }
        }
    }
}

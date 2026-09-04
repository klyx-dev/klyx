package com.klyx.presentation.screen.settings.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.api.data.log.LogEntry
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.i18n.strings
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.extendedspans.SquigglyUnderlineSpanPainter
import com.mikepenz.markdown.compose.extendedspans.rememberSquigglyUnderlineAnimator
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.parseMarkdown
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun availableTabs(changelog: String?, pluginLogs: List<LogEntry>) = buildList {
    add("Details")
    if (!changelog.isNullOrBlank()) add("Changelog")
    if (pluginLogs.isNotEmpty()) add("Logs")
}

@Composable
fun PluginDetailsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    pluginLogs: List<LogEntry>,
    changelog: String?
) {
    val tabs = availableTabs(changelog, pluginLogs)
    if (tabs.size > 1) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.coerceIn(0, tabs.size - 1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    divider = { }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val label = when (title) {
                            "Details" -> strings.tabDetails
                            "Changelog" -> strings.tabChangelog
                            "Logs" -> strings.tabLogs
                            else -> title
                        }
                        Tab(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            text = { Text(label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PluginMarkdownContent(
    content: String?,
    emptyText: String,
) {
    if (content.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Center
        ) {
            Text(
                text = emptyText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isDarkMode = LocalIsDarkMode.current
    val highlightBuilder = remember(isDarkMode) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkMode))
    }

    val state by produceState<State>(State.Loading(), content) {
        withContext(Dispatchers.Default) {
            value = parseMarkdown(content)
        }
    }

    Markdown(
        state = state,
        extendedSpans = markdownExtendedSpans {
            val animator = rememberSquigglyUnderlineAnimator()
            remember {
                ExtendedSpans(
                    RoundedCornerSpanPainter(),
                    SquigglyUnderlineSpanPainter(animator = animator)
                )
            }
        },
        imageTransformer = Coil3ImageTransformerImpl,
        components = markdownComponents(
            codeBlock = {
                MarkdownHighlightedCodeBlock(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightBuilder,
                    showHeader = true
                )
            },
            codeFence = {
                MarkdownHighlightedCodeFence(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightBuilder,
                    showHeader = true
                )
            }
        )
    )
}

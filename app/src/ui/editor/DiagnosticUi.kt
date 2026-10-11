package com.klyx.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.lsp.CodeAction
import com.klyx.lsp.Diagnostic
import com.klyx.lsp.DiagnosticSeverity
import com.klyx.lsp.HoverState
import com.klyx.lsp.LspProgressItem
import com.klyx.lsp.LspSessionState
import com.klyx.lsp.types.fold
import com.klyx.ui.accent
import com.klyx.ui.accentSecondary
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.currentFontFamily
import com.klyx.ui.error
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.surface
import com.klyx.ui.surfaceVariant
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.textSecondary
import com.klyx.ui.typography
import com.klyx.ui.warning

fun Diagnostic.messageText(): String =
    message.fold(left = { it }, right = { it.value })

@Composable
fun diagnosticColor(severity: DiagnosticSeverity?): Color = when (severity) {
    DiagnosticSeverity.Error -> Theme[colors][error]
    DiagnosticSeverity.Warning -> Theme[colors][warning]
    DiagnosticSeverity.Information -> Theme[colors][accentSecondary]
    DiagnosticSeverity.Hint -> Theme[colors][textMuted]
    else -> Theme[colors][error]
}

fun diagnosticSeverityLabel(severity: DiagnosticSeverity?): String = when (severity) {
    DiagnosticSeverity.Error -> "ERROR"
    DiagnosticSeverity.Warning -> "WARN"
    DiagnosticSeverity.Information -> "INFO"
    DiagnosticSeverity.Hint -> "HINT"
    else -> "ERROR"
}

@Composable
fun DiagnosticBanner(
    diagnostics: List<Diagnostic>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickFix: ((Diagnostic) -> Unit)? = null,
) {
    if (diagnostics.isEmpty()) return

    var currentIndex by remember(diagnostics) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, diagnostics.lastIndex)
    val diag = diagnostics[safeIndex]
    var expanded by remember { mutableStateOf(false) }

    val sevColor = diagnosticColor(diag.severity)
    val sevLabel = diagnosticSeverityLabel(diag.severity)
    val msg = diag.messageText()
    val codeStr = diag.code?.let { code ->
        code.fold(left = { it.toString() }, right = { it })
    }
    val sourceStr = listOfNotNull(diag.source, codeStr).joinToString(" ").ifEmpty { null }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Theme[colors][surfaceVariant])
                .border(1.dp, sevColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sevColor.copy(alpha = 0.18f))
                        .border(1.dp, sevColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = sevLabel,
                        style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                        color = sevColor,
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (sourceStr != null) {
                    Text(
                        text = sourceStr,
                        style = Theme[typography][labelSmall],
                        color = Theme[colors][textSecondary],
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                val line1 = diag.range.start.line.toInt() + 1
                val col1 = diag.range.start.character.toInt() + 1
                Text(
                    text = "Ln $line1:$col1",
                    style = Theme[typography][labelSmall],
                    color = Theme[colors][textMuted],
                )

                Spacer(modifier = Modifier.weight(1f))

                if (diagnostics.size > 1) {
                    Text(
                        text = "${safeIndex + 1}/${diagnostics.size}",
                        style = Theme[typography][labelSmall],
                        color = Theme[colors][textMuted],
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    KxButton(
                        onClick = {
                            currentIndex = if (safeIndex > 0) safeIndex - 1 else diagnostics.lastIndex
                        },
                        variant = KxButtonVariant.Ghost,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text("◀", style = Theme[typography][labelSmall], color = Theme[colors][textPrimary])
                    }
                    KxButton(
                        onClick = {
                            currentIndex = (safeIndex + 1) % diagnostics.size
                        },
                        variant = KxButtonVariant.Ghost,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text("▶", style = Theme[typography][labelSmall], color = Theme[colors][textPrimary])
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (onQuickFix != null) {
                    KxButton(
                        onClick = { onQuickFix(diag) },
                        variant = KxButtonVariant.Ghost,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text("Fix", style = Theme[typography][labelSmall], color = Theme[colors][accent])
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                KxButton(
                    onClick = { expanded = !expanded },
                    variant = KxButtonVariant.Ghost,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = Theme[typography][labelSmall],
                        color = Theme[colors][textMuted],
                    )
                }

                KxButton(
                    onClick = onDismiss,
                    variant = KxButtonVariant.Ghost,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("✕", style = Theme[typography][labelSmall], color = Theme[colors][textMuted])
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = msg,
                        style = Theme[typography][bodySmall].copy(
                            fontFamily = currentFontFamily(),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        color = Theme[colors][textPrimary],
                    )
                }
            } else {
                Text(
                    text = msg,
                    style = Theme[typography][bodySmall].copy(
                        fontFamily = currentFontFamily(),
                        fontSize = 12.sp,
                    ),
                    color = Theme[colors][textPrimary],
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expanded = true },
                )
            }
        }
    }
}

@Composable
fun ProblemsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    documentName: String,
    diagnostics: List<Diagnostic>,
    onSelectDiagnostic: (Diagnostic) -> Unit,
    onQuickFix: ((Diagnostic) -> Unit)? = null,
) {
    if (!visible) return

    val errors = diagnostics.count { it.severity == DiagnosticSeverity.Error || it.severity == null }
    val warnings = diagnostics.count { it.severity == DiagnosticSeverity.Warning }
    val subtitle = buildString {
        if (errors > 0) append("$errors error${if (errors > 1) "s" else ""}")
        if (warnings > 0) {
            if (isNotEmpty()) append(", ")
            append("$warnings warning${if (warnings > 1) "s" else ""}")
        }
        val others = diagnostics.size - errors - warnings
        if (others > 0) {
            if (isNotEmpty()) append(", ")
            append("$others other")
        }
    }

    KxDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "Problems in $documentName",
        subtitle = subtitle.ifEmpty { "No problems reported" },
        actions = {
            KxButton(
                onClick = onDismiss,
                variant = KxButtonVariant.Ghost,
            ) {
                Text("close", style = Theme[typography][label])
            }
        },
    ) {
        if (diagnostics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No problems found", style = Theme[typography][bodySmall], color = Theme[colors][textMuted])
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(diagnostics) { diag ->
                    val sevColor = diagnosticColor(diag.severity)
                    val sevLabel = diagnosticSeverityLabel(diag.severity)
                    val line1 = diag.range.start.line.toInt() + 1
                    val col1 = diag.range.start.character.toInt() + 1
                    val codeStr = diag.code?.let { code ->
                        code.fold(left = { it.toString() }, right = { it })
                    }
                    val sourceStr = listOfNotNull(diag.source, codeStr).joinToString(" ").ifEmpty { null }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Theme[colors][surface])
                            .border(1.dp, Theme[colors][border], RoundedCornerShape(6.dp))
                            .clickable {
                                onSelectDiagnostic(diag)
                                onDismiss()
                            }
                            .padding(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(sevColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = sevLabel,
                                    style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                                    color = sevColor,
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Line $line1:$col1",
                                style = Theme[typography][labelSmall],
                                color = Theme[colors][accent],
                            )
                            if (sourceStr != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sourceStr,
                                    style = Theme[typography][labelSmall],
                                    color = Theme[colors][textMuted],
                                )
                            }
                            if (onQuickFix != null) {
                                Spacer(modifier = Modifier.weight(1f))
                                KxButton(
                                    onClick = {
                                        onQuickFix(diag)
                                        onDismiss()
                                    },
                                    variant = KxButtonVariant.Ghost,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                ) {
                                    Text("Fix", style = Theme[typography][labelSmall], color = Theme[colors][accent])
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diag.messageText(),
                            style = Theme[typography][bodySmall].copy(
                                fontFamily = currentFontFamily(),
                                fontSize = 12.sp,
                            ),
                            color = Theme[colors][textPrimary],
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoverCard(
    hover: HoverState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Theme[colors][surfaceVariant])
            .border(1.dp, Theme[colors][accent].copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Theme[colors][accent].copy(alpha = 0.18f))
                    .border(1.dp, Theme[colors][accent].copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "DOCS",
                    style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                    color = Theme[colors][accent],
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Ln ${hover.line + 1}:${hover.column + 1}",
                style = Theme[typography][labelSmall],
                color = Theme[colors][textMuted],
            )
            Spacer(modifier = Modifier.weight(1f))
            KxButton(
                onClick = onDismiss,
                variant = KxButtonVariant.Ghost,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text("✕", style = Theme[typography][labelSmall], color = Theme[colors][textMuted])
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = hover.content,
                style = Theme[typography][bodySmall].copy(
                    fontFamily = currentFontFamily(),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                color = Theme[colors][textPrimary],
            )
        }
    }
}

@Composable
fun CodeActionsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    actions: List<CodeAction>,
    onSelectAction: (CodeAction) -> Unit,
) {
    if (!visible) return

    KxDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "Quick Fixes & Code Actions",
        subtitle = "${actions.size} action${if (actions.size != 1) "s" else ""} available",
        actions = {
            KxButton(
                onClick = onDismiss,
                variant = KxButtonVariant.Ghost,
            ) {
                Text("cancel", style = Theme[typography][label])
            }
        },
    ) {
        if (actions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No code actions available", style = Theme[typography][bodySmall], color = Theme[colors][textMuted])
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(actions) { action ->
                    val kind = action.kind?.value ?: "quickfix"
                    val isPreferred = action.isPreferred == true
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Theme[colors][surface])
                            .border(
                                1.dp,
                                if (isPreferred) Theme[colors][accent] else Theme[colors][border],
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                onSelectAction(action)
                                onDismiss()
                            }
                            .padding(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = action.title,
                                style = Theme[typography][bodySmall].copy(fontWeight = FontWeight.Medium),
                                color = Theme[colors][textPrimary],
                                modifier = Modifier.weight(1f),
                            )
                            if (isPreferred) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Theme[colors][accent].copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = "preferred",
                                        style = Theme[typography][labelSmall],
                                        color = Theme[colors][accent],
                                    )
                                }
                            }
                        }
                        if (kind.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = kind,
                                style = Theme[typography][labelSmall],
                                color = Theme[colors][textMuted],
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LspStatusDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    sessions: Map<String, LspSessionState>,
    progressItems: Map<String, LspProgressItem>,
    onRestartLanguageServer: ((String) -> Unit)? = null,
) {
    if (!visible) return

    KxDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "Language Servers & Tasks",
        subtitle = "${sessions.size} server${if (sessions.size != 1) "s" else ""}, ${progressItems.size} active task${if (progressItems.size != 1) "s" else ""}",
        actions = {
            KxButton(
                onClick = onDismiss,
                variant = KxButtonVariant.Ghost,
            ) {
                Text("close", style = Theme[typography][label])
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "SERVERS",
                style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                color = Theme[colors][textMuted],
            )
            if (sessions.isEmpty()) {
                Text(
                    text = "No active language servers",
                    style = Theme[typography][bodySmall],
                    color = Theme[colors][textMuted],
                )
            } else {
                sessions.forEach { entry ->
                    val lang = entry.key
                    val state = entry.value
                    val color = when (state) {
                        LspSessionState.Running -> Theme[colors][accent]
                        LspSessionState.Starting -> Theme[colors][warning]
                        LspSessionState.Error -> Theme[colors][error]
                        LspSessionState.Stopped -> Theme[colors][textMuted]
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Theme[colors][surface])
                            .border(1.dp, Theme[colors][border], RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(color.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = state.name.uppercase(),
                                style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                                color = color,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lang,
                            style = Theme[typography][bodySmall].copy(fontWeight = FontWeight.Medium),
                            color = Theme[colors][textPrimary],
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (onRestartLanguageServer != null) {
                            KxButton(
                                onClick = { onRestartLanguageServer(lang) },
                                variant = KxButtonVariant.Ghost,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("restart", style = Theme[typography][labelSmall], color = Theme[colors][accent])
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "BACKGROUND TASKS",
                style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                color = Theme[colors][textMuted],
            )
            if (progressItems.isEmpty()) {
                Text(
                    text = "No background tasks running",
                    style = Theme[typography][bodySmall],
                    color = Theme[colors][textMuted],
                )
            } else {
                progressItems.values.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Theme[colors][surface])
                            .border(1.dp, Theme[colors][border], RoundedCornerShape(6.dp))
                            .padding(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                style = Theme[typography][bodySmall].copy(fontWeight = FontWeight.Medium),
                                color = Theme[colors][textPrimary],
                                modifier = Modifier.weight(1f),
                            )
                            if (item.percentage != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${item.percentage}%",
                                    style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                                    color = Theme[colors][accent],
                                )
                            }
                        }
                        if (!item.message.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.message,
                                style = Theme[typography][labelSmall],
                                color = Theme[colors][textMuted],
                            )
                        }
                    }
                }
            }
        }
    }
}

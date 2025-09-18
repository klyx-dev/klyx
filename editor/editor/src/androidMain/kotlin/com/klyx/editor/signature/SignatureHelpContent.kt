package com.klyx.editor.signature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureInformation
import org.eclipse.lsp4j.jsonrpc.messages.Either

@Composable
internal fun SignatureHelpContent(
    signatureHelp: SignatureHelp?,
    maxWidth: Dp,
    maxHeight: Dp,
    onNavigateOverload: (Int) -> Unit = {}
) {
    if (signatureHelp == null) return

    val activeSignatureIndex = (signatureHelp.activeSignature ?: 0).coerceAtLeast(0)
    val activeParameterIndex = signatureHelp.activeParameter ?: -1
    val signatures = signatureHelp.signatures.orEmpty()

    if (signatures.isEmpty()) return

    Card(
        modifier = Modifier
            .heightIn(max = maxHeight)
            .widthIn(max = maxWidth)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .wrapContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            if (signatures.size > 1) {
                SignatureNavigationHeader(
                    currentIndex = activeSignatureIndex,
                    totalCount = signatures.size,
                    onNavigate = onNavigateOverload
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f, false)
            ) {
                signatures.getOrNull(activeSignatureIndex)?.let { signature ->
                    HighlightedSignatureLabel(
                        signature = signature,
                        activeParamIndex = activeParameterIndex
                    )

                    signature.documentation?.let { doc ->
                        DocumentationContent(doc)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureNavigationHeader(
    currentIndex: Int,
    totalCount: Int,
    onNavigate: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Overload ${currentIndex + 1} of $totalCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    if (currentIndex > 0) onNavigate(currentIndex - 1)
                },
                enabled = currentIndex > 0,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Previous overload",
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = {
                    if (currentIndex < totalCount - 1) onNavigate(currentIndex + 1)
                },
                enabled = currentIndex < totalCount - 1,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Next overload",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HighlightedSignatureLabel(
    signature: SignatureInformation,
    activeParamIndex: Int
) {
    val label = signature.label
    val parameters = signature.parameters.orEmpty()

    val annotatedString = buildAnnotatedString {
        if (parameters.isEmpty()) {
            append(label)
        } else {
            var lastEnd = 0

            parameters.forEachIndexed { index, param ->
                val (start, end) = when {
                    param.label.isLeft -> {
                        val paramLabel = param.label.left
                        val startIndex = label.indexOf(paramLabel, lastEnd)
                        if (startIndex >= 0) {
                            startIndex to (startIndex + paramLabel.length)
                        } else {
                            lastEnd to lastEnd
                        }
                    }

                    param.label.isRight -> {
                        val range = param.label.right
                        val s = (range.first ?: lastEnd).coerceAtLeast(lastEnd)
                        val e = (range.second ?: s).coerceAtLeast(s)
                        s to e
                    }

                    else -> lastEnd to lastEnd
                }

                if (start > lastEnd) {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append(label.substring(lastEnd, start))
                    }
                }

                if (end > start) {
                    withStyle(
                        style = if (index == activeParamIndex) {
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        } else {
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    ) {
                        append(label.substring(start, end))
                    }
                    lastEnd = end
                }
            }

            if (lastEnd < label.length) {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(label.substring(lastEnd))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        )
    }
}

@Composable
private fun DocumentationContent(documentation: Either<String, MarkupContent>) {
    when {
        documentation.isLeft -> {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                modifier = Modifier.padding(8.dp),
                text = documentation.left,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        documentation.isRight -> {
            val content = documentation.right.value

            if (!content.equals("```\n\n```\n")) {
                Spacer(modifier = Modifier.height(4.dp))

                Markdown(
                    content = content,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

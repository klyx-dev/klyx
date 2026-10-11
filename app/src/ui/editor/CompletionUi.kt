package com.klyx.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.lsp.CompletionItem
import com.klyx.lsp.CompletionItemKind
import com.klyx.runtime.workspace.DocumentId
import com.klyx.ui.accent
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.currentFontFamily
import com.klyx.ui.labelSmall
import com.klyx.ui.surface
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.typography
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorTheme
import kotlin.math.roundToInt
import io.github.lumkit.sweeteditor.CompletionItem as SweetCompletionItem
import io.github.lumkit.sweeteditor.CompletionItemKind as SweetCompletionItemKind

private fun Int.toComposeColor(): Color = Color(this)

data class LspCompletionItemUi(
    val label: String,
    val detail: String? = null,
    val labelDetail: String? = null,
    val labelDescription: String? = null,
    val documentation: String? = null,
    val kind: CompletionItemKind? = null,
    val isDeprecated: Boolean = false,
    val isSnippet: Boolean = false,
    val rawItem: CompletionItem,
    val sweetItem: SweetCompletionItem,
    val line: Int = 0,
    val prefixStart: Int? = null,
    val prefixEnd: Int? = null,
) {
    val hasDocs: Boolean
        get() = !documentation.isNullOrBlank() || rawItem.documentation != null
}

data class CompletionUiState(
    val documentId: DocumentId,
    val items: List<LspCompletionItemUi>,
    val selectedIndex: Int = 0,
    val anchor: EditorCursorRect?,
)

data class KindBadge(
    val text: String,
    val color: Color,
)

fun completionKindBadge(kind: CompletionItemKind?): KindBadge = when (kind) {
    CompletionItemKind.Function,
    CompletionItemKind.Method,
    CompletionItemKind.Constructor -> KindBadge("fn", Color(0xFFC678DD))

    CompletionItemKind.Variable,
    CompletionItemKind.Field,
    CompletionItemKind.Value -> KindBadge("var", Color(0xFF61AFEF))

    CompletionItemKind.Constant -> KindBadge("const", Color(0xFFE5C07B))

    CompletionItemKind.Keyword -> KindBadge("kwd", Color(0xFFD19A66))

    CompletionItemKind.Class,
    CompletionItemKind.Struct,
    CompletionItemKind.Interface -> KindBadge("type", Color(0xFFE5C07B))

    CompletionItemKind.Enum,
    CompletionItemKind.EnumMember -> KindBadge("enum", Color(0xFF98C379))

    CompletionItemKind.Module -> KindBadge("mod", Color(0xFF56B6C2))

    CompletionItemKind.Property -> KindBadge("prop", Color(0xFF4EC9B0))

    CompletionItemKind.Snippet -> KindBadge("snip", Color(0xFFE06C75))

    CompletionItemKind.Text -> KindBadge("txt", Color(0xFF9DA5B4))

    else -> KindBadge("sym", Color(0xFF9DA5B4))
}

fun sweetKindBadge(kind: SweetCompletionItemKind?): KindBadge = when (kind) {
    SweetCompletionItemKind.FUNCTION -> KindBadge("fn", Color(0xFFC678DD))
    SweetCompletionItemKind.VARIABLE -> KindBadge("var", Color(0xFF61AFEF))
    SweetCompletionItemKind.KEYWORD -> KindBadge("kwd", Color(0xFFD19A66))
    SweetCompletionItemKind.CLASS,
    SweetCompletionItemKind.INTERFACE -> KindBadge("type", Color(0xFFE5C07B))

    SweetCompletionItemKind.MODULE -> KindBadge("mod", Color(0xFF56B6C2))
    SweetCompletionItemKind.PROPERTY -> KindBadge("prop", Color(0xFF4EC9B0))
    SweetCompletionItemKind.SNIPPET -> KindBadge("snip", Color(0xFFE06C75))
    else -> KindBadge("txt", Color(0xFF9DA5B4))
}


fun scoreCompletion(item: CompletionItem, prefix: String): Int {
    if (prefix.isEmpty()) return 0
    val label = item.label
    val filterText = item.filterText ?: label

    if (label.equals(prefix, ignoreCase = false)) return 0
    if (label.equals(prefix, ignoreCase = true)) return 1

    if (label.startsWith(prefix, ignoreCase = false)) return 2
    if (label.startsWith(prefix, ignoreCase = true)) return 3
    if (filterText.startsWith(prefix, ignoreCase = false)) return 4
    if (filterText.startsWith(prefix, ignoreCase = true)) return 5

    val boundaryIndex = label.indexOf(prefix, ignoreCase = true)
    if (boundaryIndex > 0) {
        val prevChar = label[boundaryIndex - 1]
        if (prevChar == '_' || prevChar == '$' || prevChar == ':') return 6
        if (label[boundaryIndex].isUpperCase() && prevChar.isLowerCase()) return 6
    }

    if (label.contains(prefix, ignoreCase = false)) return 7
    if (label.contains(prefix, ignoreCase = true)) return 8
    if (filterText.contains(prefix, ignoreCase = true)) return 9

    return 10
}


@Composable
fun CompletionItemRow(
    item: SweetCompletionItem,
    selected: Boolean,
    theme: EditorTheme,
    modifier: Modifier = Modifier,
) {
    val badge = sweetKindBadge(item.kind)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 18.dp)
                .clip(RectangleShape)
                .background(badge.color.copy(alpha = 0.2f))
                .border(0.5.dp, badge.color.copy(alpha = 0.6f), RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge.text,
                style = Theme[typography][labelSmall].copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = badge.color,
            )
        }

        Text(
            text = item.label,
            style = Theme[typography][bodySmall].copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
            ),
            color = theme.textColor.toComposeColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}


class CompletionPopupPositionProvider(
    private val anchor: EditorCursorRect,
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val cursorWindowX = anchorBounds.left + anchor.x.roundToInt()
        val cursorWindowY = anchorBounds.top + anchor.y.roundToInt()
        val cursorHeight = anchor.height.roundToInt().coerceAtLeast(1)

        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = cursorWindowX.coerceIn(marginPx, maxX)

        val belowY = cursorWindowY + cursorHeight + 4
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        val minY = marginPx

        val y = if (belowY <= maxY) {
            belowY
        } else {
            val spaceAbove = cursorWindowY - minY
            val spaceBelow = windowSize.height - (cursorWindowY + cursorHeight) - marginPx
            if (popupContentSize.height in (spaceBelow + 1)..spaceAbove) {
                (cursorWindowY - popupContentSize.height - 4).coerceAtLeast(minY)
            } else {
                maxY
            }
        }

        return IntOffset(x, y)
    }
}

@Composable
fun CustomCompletionPopup(
    state: CompletionUiState,
    onSelectItem: (Int) -> Unit,
    onConfirmItem: (LspCompletionItemUi) -> Unit,
    onShowDoc: (LspCompletionItemUi) -> Unit,
    onDismiss: () -> Unit,
) {
    val anchor = state.anchor ?: return
    if (state.items.isEmpty()) return

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val marginPx = with(density) { 12.dp.roundToPx() }

    val positionProvider = remember(anchor, marginPx) {
        CompletionPopupPositionProvider(anchor, marginPx)
    }

    LaunchedEffect(state.selectedIndex) {
        if (state.selectedIndex in state.items.indices) {
            listState.animateScrollToItem(state.selectedIndex)
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 380.dp)
                .shadow(12.dp, RectangleShape)
                .clip(RectangleShape)
                .background(Theme[colors][surface])
                .border(1.dp, Theme[colors][border], RectangleShape)
                .padding(vertical = 4.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
            ) {
                itemsIndexed(state.items) { index, item ->
                    val selected = index == state.selectedIndex
                    (val text, val color) = completionKindBadge(item.kind)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Theme[colors][accent].copy(alpha = 0.14f)
                                else Color.Transparent
                            )
                            .combinedClickable(
                                onClick = {
                                    onSelectItem(index)
                                    onConfirmItem(item)
                                },
                                onLongClick = {
                                    onSelectItem(index)
                                    onShowDoc(item)
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 30.dp, height = 18.dp)
                                .clip(RectangleShape)
                                .background(color.copy(alpha = 0.2f))
                                .border(0.5.dp, color.copy(alpha = 0.6f), RectangleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = text,
                                style = Theme[typography][labelSmall].copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = color,
                            )
                        }

                        Text(
                            text = item.label,
                            style = Theme[typography][bodySmall].copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                textDecoration = if (item.isDeprecated) TextDecoration.LineThrough else null,
                            ),
                            color = if (item.isDeprecated) {
                                Theme[colors][textMuted]
                            } else {
                                Theme[colors][textPrimary]
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        val labelDetail = item.labelDetail
                        if (!labelDetail.isNullOrBlank()) {
                            Text(
                                text = labelDetail,
                                style = Theme[typography][labelSmall].copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                ),
                                color = Theme[colors][textMuted],
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionDocDialog(
    item: LspCompletionItemUi,
    onDismiss: () -> Unit,
) {
    (val text, val color) = completionKindBadge(item.kind)
    val docContent = item.documentation?.trim()
        ?.ifEmpty { "No documentation available for this completion item." }
        ?: "No documentation available for this completion item."

    val detail = item.detail?.trim() ?: item.labelDetail?.trim()

    KxDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = item.label,
        actions = {
            KxButton(
                onClick = onDismiss,
                variant = KxButtonVariant.Outlined,
            ) {
                Text(
                    text = "Close",
                    style = Theme[typography][bodySmall],
                    fontFamily = currentFontFamily(),
                    color = Theme[colors][textPrimary],
                )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .background(color.copy(alpha = 0.2f))
                        .border(0.5.dp, color.copy(alpha = 0.6f), RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = text,
                        style = Theme[typography][labelSmall].copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = color,
                    )
                }

                Text(
                    text = item.label,
                    style = Theme[typography][bodySmall].copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = Theme[colors][textPrimary],
                )

                if (item.isSnippet) {
                    Text(
                        text = "[Snippet]",
                        style = Theme[typography][labelSmall].copy(
                            fontSize = 11.sp,
                            color = Color(0xFFE06C75),
                        ),
                    )
                }
            }

            if (!detail.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Theme[colors][surface])
                        .border(1.dp, Theme[colors][border], RoundedCornerShape(6.dp))
                        .padding(8.dp),
                ) {
                    Text(
                        text = detail,
                        style = Theme[typography][labelSmall].copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        ),
                        color = Theme[colors][textPrimary],
                    )
                }
            }

            Text(
                text = docContent,
                style = Theme[typography][bodySmall].copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                color = Theme[colors][textPrimary],
            )
        }
    }
}

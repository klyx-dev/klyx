package com.klyx.editor.compose.selection.contextmenu

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.round
import com.klyx.editor.compose.selection.contextmenu.ContextMenuState.Status

/**
 * Wraps [content] with the necessary components to show a context menu in it.
 *
 * @param state The state that controls the context menu popup.
 * @param onDismiss Lambda to execute when the user clicks outside of the popup.
 * @param contextMenuBuilderBlock Block which builds the context menu.
 * @param modifier Modifier to apply to the Box surrounding the context menu and the content.
 * @param enabled Whether the context menu is enabled.
 * @param onOpenGesture The callback that will be invoked on a right click (open menu gesture).
 * @param content The content that will have the context menu enabled.
 */
@Composable
internal fun ContextMenuArea(
    state: ContextMenuState,
    onDismiss: () -> Unit,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onOpenGesture: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val finalModifier =
        if (enabled) {
            modifier.contextMenuGestures {
                onOpenGesture()
                state.status = Status.Open(offset = it)
            }
        } else {
            modifier
        }
    Box(finalModifier, propagateMinConstraints = true) {
        content()
        ContextMenu(
            state = state,
            onDismiss = onDismiss,
            contextMenuBuilderBlock = contextMenuBuilderBlock,
        )
    }
}

@VisibleForTesting
@Composable
internal fun ContextMenu(
    state: ContextMenuState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    val status = state.status
    if (status !is Status.Open) return

    val popupPositionProvider =
        remember(status) { ContextMenuPopupPositionProvider(status.offset.round()) }

    ContextMenuPopup(
        modifier = modifier,
        popupPositionProvider = popupPositionProvider,
        onDismiss = onDismiss,
        contextMenuBuilderBlock = contextMenuBuilderBlock,
    )
}

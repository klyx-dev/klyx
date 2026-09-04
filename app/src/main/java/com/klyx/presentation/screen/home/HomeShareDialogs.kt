package com.klyx.presentation.screen.home

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.ui.ToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.api.util.share
import com.klyx.api.util.shareText
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.data.file.share
import com.klyx.i18n.getLocaleStrings
import com.klyx.presentation.components.dialogs.ImageShareDialog
import com.klyx.presentation.components.dialogs.ShareDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShareDialogs(
    activeTab: WorkspaceTab?,
    registry: EditorStateRegistry,
    context: Context,
    scope: CoroutineScope,
    toastHostState: ToastHostState,
    onDismiss: () -> Unit,
) {
    when (val tab = activeTab) {
        is WorkspaceTab.TextFile -> {
            val editorState = registry[tab.id]
            val hasSelection = editorState?.cursor?.isSelected ?: false

            ShareDialog(
                fileName = tab.title,
                hasSelection = hasSelection,
                onDismiss = onDismiss,
                onShareSelection = {
                    onDismiss()
                    val cursor = editorState?.cursor
                    val text = editorState?.text

                    if (cursor != null && text != null && hasSelection) {
                        val start = minOf(cursor.left, cursor.right)
                        val end = maxOf(cursor.left, cursor.right)
                        val selectedText = text.substring(start, end)

                        shareText(selectedText)
                    } else {
                        scope.launch {
                            val s = getLocaleStrings()
                            toastHostState.showFailureToast(s.noTextSelectedToShare)
                        }
                    }
                },
                onShareFileText = {
                    scope.launch(Dispatchers.IO) {
                        val wholeText = context.contentResolver.openInputStream(
                            tab.file.uri
                        )?.bufferedReader()?.readText() ?: ""
                        withContext(Dispatchers.Main.immediate) {
                            shareText(wholeText)
                        }
                    }
                    onDismiss()
                },
                onShareFile = {
                    onDismiss()
                    tab.file.share()
                }
            )
        }

        is WorkspaceTab.ImageFile -> {
            ImageShareDialog(
                fileName = tab.title,
                imageUri = tab.uri,
                onDismiss = onDismiss,
                onShare = {
                    onDismiss()
                    tab.uri.share()
                }
            )
        }

        else -> Unit
    }
}

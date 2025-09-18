package com.klyx.editor.compose

import android.view.View
import androidx.compose.runtime.CompositionContext
import com.klyx.editor.KlyxEditor

abstract class ComposeInfoPopup(
    editor: KlyxEditor,
    localView: View,
    compositionContext: CompositionContext
) : ComposeEditorPopup(
    editor,
    localView,
    compositionContext,
    FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SCROLL_AS_CONTENT
)

abstract class ComposeActionPopup(
    editor: KlyxEditor,
    localView: View,
    compositionContext: CompositionContext
) : ComposeEditorPopup(
    editor,
    localView,
    compositionContext,
    FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED
) {
    protected open fun onActionPerformed(actionId: String) {
        dismiss()
    }
}

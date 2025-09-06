package com.klyx.editor.util

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

internal fun ComposeView.createParent(
    context: Context,
    viewTreeLifecycleOwner: LifecycleOwner?,
    viewTreeSavedStateRegistryOwner: SavedStateRegistryOwner?
): FrameLayout {
    return FrameLayout(context).apply {
        id = android.R.id.content
        setViewTreeLifecycleOwner(viewTreeLifecycleOwner)
        setViewTreeSavedStateRegistryOwner(viewTreeSavedStateRegistryOwner)
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addView(this@createParent)
    }
}

package com.klyx.ui.util

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.runtime.Composable
import com.klyx.core.ui.component.PreferenceSubtitle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

inline fun LazyListScope.section(
    title: String,
    block: LazyListScope.() -> Unit
) {
    item { PreferenceSubtitle(title) }
    block()
}

inline fun LazyListScope.section(
    title: StringResource,
    block: LazyListScope.() -> Unit
) {
    item { PreferenceSubtitle(stringResource(title)) }
    block()
}

@LazyScopeMarker
inline fun LazyListScope.pref(
    crossinline content: @Composable LazyItemScope.() -> Unit
) {
    item { content() }
}

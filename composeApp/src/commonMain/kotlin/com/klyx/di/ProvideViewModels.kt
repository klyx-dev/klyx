package com.klyx.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.noLocalProvidedFor
import com.klyx.filetree.FileTreeViewModel
import com.klyx.ui.component.extension.ExtensionViewModel
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.StatusBarViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProvideViewModels(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalEditorViewModel provides koinViewModel(),
        LocalKlyxViewModel provides koinViewModel(),
        LocalFileTreeViewModel provides koinViewModel(),
        LocalStatusBarViewModel provides koinViewModel(),
        LocalExtensionViewModel provides koinViewModel(),
        content = content
    )
}

val LocalEditorViewModel = staticCompositionLocalOf<EditorViewModel> { noLocalProvidedFor("LocalEditorViewModel") }
val LocalKlyxViewModel = staticCompositionLocalOf<KlyxViewModel> { noLocalProvidedFor("LocalKlyxViewModel") }
val LocalFileTreeViewModel =
    staticCompositionLocalOf<FileTreeViewModel> { noLocalProvidedFor("LocalFileTreeViewModel") }
val LocalStatusBarViewModel =
    staticCompositionLocalOf<StatusBarViewModel> { noLocalProvidedFor("LocalStatusBarViewModel") }
val LocalExtensionViewModel =
    staticCompositionLocalOf<ExtensionViewModel> { noLocalProvidedFor("LocalExtensionViewModel") }

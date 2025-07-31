package com.klyx.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.noLocalProvidedFor
import org.koin.compose.koinInject

val LocalFileDownloader = staticCompositionLocalOf<FileDownloader> {
    noLocalProvidedFor<FileDownloader>()
}

@Composable
fun rememberFileDownloader(): FileDownloader = koinInject()

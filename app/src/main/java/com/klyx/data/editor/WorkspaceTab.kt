package com.klyx.data.editor

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.klyx.data.file.KxFile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Stable
sealed class WorkspaceTab {

    abstract val title: String

    @OptIn(ExperimentalUuidApi::class)
    open val id by lazy { Uuid.generateV7().toString() }

    @Immutable
    data class TextFile(
        val file: KxFile,
        val text: String,
        val projectUri: Uri? = null,
        val hasUnsavedChanges: Boolean = false,
        override val title: String = file.name,
        override val id: String = file.uri.toString(),
    ) : WorkspaceTab()

    @Immutable
    data class ImageFile(
        val uri: Uri,
        val projectUri: Uri? = null,
        override val title: String,
        override val id: String = uri.toString()
    ) : WorkspaceTab()

    @Stable
    data object Welcome : WorkspaceTab() {
        override val title: String = "Welcome"
    }
}

package com.klyx.api.data.editor

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.klyx.api.data.file.KxFile
import kotlin.uuid.Uuid

/**
 * Base class for all tabs displayed in the main workspace/editor area.
 *
 * Each tab represents a piece of content that the user is currently working on or viewing.
 */
@Stable
sealed class WorkspaceTab {

    /**
     * The display title of the tab.
     */
    abstract val title: String

    /**
     * A unique identifier for this tab instance.
     */
    open val id by lazy { Uuid.generateV7().toString() }

    /**
     * A tab representing a text file being edited.
     *
     * @property file The file associated with this tab.
     * @property text The current text content of the file.
     * @property projectUri The URI of the project this file belongs to, if any.
     * @property hasUnsavedChanges Whether the file has modifications that haven't been saved to disk yet.
     */
    @Immutable
    data class TextFile(
        val file: KxFile,
        val text: String,
        val projectUri: Uri? = null,
        val hasUnsavedChanges: Boolean = false,
        override val title: String = file.name,
        override val id: String = file.uri.toString(),
    ) : WorkspaceTab()

    /**
     * A tab representing an image file being viewed.
     *
     * @property uri The URI of the image.
     * @property projectUri The URI of the project this image belongs to, if any.
     */
    @Immutable
    data class ImageFile(
        val uri: Uri,
        val projectUri: Uri? = null,
        override val title: String,
        override val id: String = uri.toString()
    ) : WorkspaceTab()

    /**
     * The default welcome screen tab.
     */
    @Stable
    data object Welcome : WorkspaceTab() {
        override val title: String = "Welcome"
    }

    /**
     * A tab with custom UI content, typically contributed by a plugin.
     *
     * @property content The Composable UI to be rendered inside the tab.
     */
    @Stable
    data class Custom(
        override val title: String,
        override val id: String,
        val content: @Composable () -> Unit,
    ) : WorkspaceTab()
}

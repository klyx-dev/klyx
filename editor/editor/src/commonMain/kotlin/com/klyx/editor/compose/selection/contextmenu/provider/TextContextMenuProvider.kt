package com.klyx.editor.compose.selection.contextmenu.provider

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuData

/**
 * The provider determines how the context menu is shown and its appearance.
 *
 * The context menu can be customized by providing another implementation of this to
 * [LocalTextContextMenuDropdownProvider] or [LocalTextContextMenuToolbarProvider] via a
 * [CompositionLocalProvider].
 *
 * If you want to modify the contents of the context menu, see
 * [Modifier.appendTextContextMenuComponents][com.klyx.editor.compose.selection.contextmenu.modifier.appendTextContextMenuComponents] and
 * [Modifier.filterTextContextMenuComponents][com.klyx.editor.compose.selection.contextmenu.modifier.filterTextContextMenuComponents]
 */
interface TextContextMenuProvider {
    /**
     * Shows the text context menu.
     *
     * This function suspends until the context menu is closed. If the coroutine is cancelled, the
     * context menu will be closed.
     *
     * @param dataProvider provides the data necessary to show the text context menu.
     */
    suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider)
}

/** Provide a [TextContextMenuProvider] to be used for the text context menu dropdown. */
val LocalTextContextMenuDropdownProvider: ProvidableCompositionLocal<TextContextMenuProvider?> =
    compositionLocalOf {
        null
    }

/** Provide a [TextContextMenuProvider] to be used for the text context menu toolbar. */
val LocalTextContextMenuToolbarProvider: ProvidableCompositionLocal<TextContextMenuProvider?> =
    compositionLocalOf {
        null
    }

/**
 * Provides the data necessary to show the text context menu.
 *
 * All functions on this interface are expected to be snapshot-aware.
 */
interface TextContextMenuDataProvider {
    /**
     * Provides the position to place the context menu around. The position should be relative to
     * the provided [destinationCoordinates].
     *
     * This function is snapshot-aware.
     */
    fun position(destinationCoordinates: LayoutCoordinates): Offset

    /**
     * Provides a bounding box to place the context menu around. The position should be relative to
     * the provided [destinationCoordinates].
     *
     * This function is snapshot-aware.
     */
    fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect

    /**
     * Provides the components used to fill the context menu.
     *
     * This function is snapshot-aware.
     */
    fun data(): TextContextMenuData
}

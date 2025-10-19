package com.klyx.editor.compose.selection.contextmenu.builder

import androidx.collection.mutableObjectListOf
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuComponent
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuData
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuSeparator

class TextContextMenuBuilderScope internal constructor() {
    private val components = mutableObjectListOf<TextContextMenuComponent>()
    private val filters = mutableObjectListOf<(TextContextMenuComponent) -> Boolean>()

    /**
     * Build the current state of this into a [TextContextMenuData]. This applies a few filters to
     * the components:
     * * No back-to-back separators.
     * * No heading or tailing separators.
     * * Applies [filters] to each component, excluding separators.
     */
    internal fun build(): TextContextMenuData {
        val resultList = mutableObjectListOf<TextContextMenuComponent>()

        var headIsSeparator = true
        var previous: TextContextMenuComponent? = null
        components.forEach { current ->
            // remove heading separators
            if (headIsSeparator && current === TextContextMenuSeparator) return@forEach
            headIsSeparator = false

            // remove back-to-back separators
            if (current.isSeparator && previous.isSeparator) return@forEach

            // apply `filters` unless a component is a separator
            if (!current.isSeparator && filters.any { filter -> !filter(current) }) return@forEach

            resultList += current
            previous = current
        }

        // remove the remaining trailing separator, if there is one.
        if (resultList.lastOrNull().isSeparator) {
            @Suppress("Range") // lastIndex will not be -1 because the list cannot be empty
            resultList.removeAt(resultList.lastIndex)
        }

        @Suppress("AsCollectionCall") // need to use asList as this enters a public api.
        return TextContextMenuData(components = resultList.asList())
    }

    /**
     * Adds a [filter] to be applied to each component in [components] when the [build] method is
     * called.
     *
     * @param filter A predicate that determines whether a [TextContextMenuComponent] should be
     *   added to the final list of components. This filter will never receive a
     *   [TextContextMenuSeparator] since they are excluded from filtering.
     */
    internal fun addFilter(filter: (TextContextMenuComponent) -> Boolean) {
        filters += filter
    }

    internal fun addComponent(component: TextContextMenuComponent) {
        components += component
    }

    /**
     * Adds a separator to the list of text context menu components. Successive separators will be
     * combined into a single separator.
     */
    fun separator() {
        components += TextContextMenuSeparator
    }
}

internal val TextContextMenuComponent?.isSeparator: Boolean
    get() = this === TextContextMenuSeparator

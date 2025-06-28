package com.klyx.menu

import com.klyx.core.string
import org.jetbrains.compose.resources.StringResource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MenuBuilder {
    private val groups = mutableMapOf<String, List<MenuItem>>()

    fun group(title: String, builder: MenuGroupBuilder.() -> Unit) {
        val groupBuilder = MenuGroupBuilder().apply(builder)
        groups[title] = groupBuilder.items
    }

    fun group(resource: StringResource, builder: MenuGroupBuilder.() -> Unit) {
        group(string(resource), builder)
    }

    operator fun String.invoke(builder: MenuGroupBuilder.() -> Unit) {
        group(this, builder)
    }

    fun build(): Map<String, List<MenuItem>> = groups
}

class MenuGroupBuilder {
    internal val items = mutableListOf<MenuItem>()

    fun item(
        title: String,
        shortcutKey: String? = null,
        dismissRequestOnClicked: Boolean = true,
        onClick: suspend () -> Unit = {}
    ) {
        items += MenuItem(
            title = title,
            shortcutKey = shortcutKey,
            dismissRequestOnClicked = dismissRequestOnClicked,
            onClick = onClick
        )
    }

    fun item(
        resource: StringResource,
        shortcutKey: String? = null,
        dismissRequestOnClicked: Boolean = true,
        onClick: suspend () -> Unit = {}
    ) {
        item(string(resource), shortcutKey, dismissRequestOnClicked, onClick)
    }

    fun divider() {
        items += MenuItem() // title empty means divider
    }
}

@OptIn(ExperimentalContracts::class)
inline fun menu(builder: MenuBuilder.() -> Unit): Map<String, List<MenuItem>> {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return MenuBuilder().apply(builder).build()
}

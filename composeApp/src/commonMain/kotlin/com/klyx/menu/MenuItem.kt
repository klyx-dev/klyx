package com.klyx.menu

import com.klyx.core.cmd.key.KeyShortcut
import com.klyx.core.string
import org.jetbrains.compose.resources.StringResource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@DslMarker
annotation class MenuItemDsl

data class MenuItem(
    val title: String = "",
    @Deprecated(
        message = "Use `shortcuts` instead.",
        replaceWith = ReplaceWith("shortcuts")
    )
    val shortcutKey: String? = null,
    val shortcuts: List<KeyShortcut> = emptyList(),
    val isDivider: Boolean = title.isEmpty(),
    val dismissRequestOnClicked: Boolean = true,
    val onClick: suspend () -> Unit = {}
)

@MenuItemDsl
class MenuItemBuilder {
    private var title: String = ""
    private val shortcuts = mutableListOf<KeyShortcut>()
    private var isDivider: Boolean = false
    private var dismissRequestOnClicked: Boolean = true
    private var onClick: suspend () -> Unit = {}

    fun title(title: String) = apply { this.title = title }
    fun title(resource: StringResource) = title(string(resource))

    fun shortcut(vararg shortcut: KeyShortcut) = apply { shortcuts += shortcut }
    fun shortcut(shortcuts: Collection<KeyShortcut>) = apply {
        this.shortcuts += shortcuts
    }

    fun divider() = apply { isDivider = true }
    fun dismissRequestOnClicked(value: Boolean) = apply { dismissRequestOnClicked = value }

    fun onClick(block: suspend () -> Unit) = apply { onClick = block }

    fun build(): MenuItem {
        return MenuItem(
            title = title,
            shortcuts = shortcuts,
            isDivider = isDivider,
            dismissRequestOnClicked = dismissRequestOnClicked,
            onClick = onClick
        )
    }
}

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun menuItem(
    @BuilderInference builder: MenuItemBuilder.() -> Unit
): MenuItem {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return MenuItemBuilder().apply(builder).build()
}

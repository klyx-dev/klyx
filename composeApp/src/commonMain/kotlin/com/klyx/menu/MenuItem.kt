package com.klyx.menu

data class MenuItem(
    val title: String = "",
    val shortcutKey: String? = null,
    val isDivider: Boolean = title.isEmpty(),
    val dismissRequestOnClicked: Boolean = true,
    val onClick: suspend () -> Unit = {}
)

package com.klyx.tab

data class TabMenuState(
    val enabled: (TabMenuAction) -> Boolean = { true },
    val visible: (TabMenuAction) -> Boolean = { true }
)

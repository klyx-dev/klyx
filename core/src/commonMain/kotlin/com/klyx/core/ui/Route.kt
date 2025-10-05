package com.klyx.core.ui

object Route {
    const val HOME = "home"
    const val SETTINGS = "settings"

    const val SETTINGS_PAGE = "settings_page"

    const val APPEARANCE = "appearance"
    const val ABOUT = "about"
    const val DARK_THEME = "dark_theme"
    const val GENERAL_PREFERENCES = "general_preferences"
    const val EDITOR_PREFERENCES = "editor_preferences"
}

infix fun String.arg(arg: String) = "$this/{$arg}"

infix fun String.id(id: Int) = "$this/$id"

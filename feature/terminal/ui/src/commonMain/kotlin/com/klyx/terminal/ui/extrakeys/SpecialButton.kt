package com.klyx.terminal.ui.extrakeys

data class SpecialButton(val key: String) {
    init {
        map[key] = this
    }

    override fun toString() = key

    companion object {
        private val map = mutableMapOf<String, SpecialButton>()

        val Ctrl = SpecialButton("CTRL")
        val Alt = SpecialButton("ALT")
        val Shift = SpecialButton("SHIFT")
        val Fn = SpecialButton("FN")

        fun valueOf(key: String) = map[key]
    }
}

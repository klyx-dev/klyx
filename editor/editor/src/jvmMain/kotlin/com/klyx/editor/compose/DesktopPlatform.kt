package com.klyx.editor.compose

internal enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        private var overriddenCurrent: DesktopPlatform? = null

        private val _current: DesktopPlatform by lazy {
            val name = System.getProperty("os.name")
            when {
                name?.startsWith("Linux") == true -> Linux
                name?.startsWith("Win") == true -> Windows
                name == "Mac OS X" -> MacOS
                else -> Unknown
            }
        }

        /**
         * Identify OS on which the application is currently running.
         */
        val Current get() = overriddenCurrent ?: _current

        /**
         * Override [DesktopPlatform.Current] during [body] execution
         */
        inline fun <T> withOverriddenCurrent(newCurrent: DesktopPlatform, body: () -> T): T {
            try {
                overriddenCurrent = newCurrent
                return body()
            } finally {
                overriddenCurrent = null
            }
        }
    }
}

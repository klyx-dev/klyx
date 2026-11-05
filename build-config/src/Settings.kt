package com.klyx.build

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface Settings {
    /**
     * Properties file name (without extension)
     * that is located in the module root.
     */
    val propertiesFileName: String get() = "config"
}

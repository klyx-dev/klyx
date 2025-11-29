package com.klyx.core

import arrow.core.Option
import kotlinx.io.files.Path

expect object dirs {
    val homeDir: Option<Path>
    val cacheDir: Option<Path>
    val configDir: Option<Path>
    val configLocalDir: Option<Path>
    val dataDir: Option<Path>
    val dataLocalDir: Option<Path>
    val executableDir: Option<Path>
    val preferenceDir: Option<Path>
    val runtimeDir: Option<Path>
    val stateDir: Option<Path>
    val audioDir: Option<Path>
    val desktopDir: Option<Path>
    val documentDir: Option<Path>
    val downloadDir: Option<Path>
    val fontDir: Option<Path>
    val pictureDir: Option<Path>
    val publicDir: Option<Path>
    val templateDir: Option<Path>
    val videoDir: Option<Path>
}

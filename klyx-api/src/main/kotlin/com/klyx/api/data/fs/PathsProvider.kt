package com.klyx.api.data.fs

import com.klyx.core.Global
import java.io.File

interface PathsProvider : Global {
    val filesDir: File
    val tempDir: File
    val externalFilesDir: File
    val nativeLibraryDir: File
    val dataDir: File
    val externalCacheDir: File?
    val noBackupFilesDir: File
    val codeCacheDir: File
    val externalCacheDirs: Array<File>
    val externalFilesDirs: Array<File>

    val rootFs: File
    val homeDir: File
    val projectsDir: File
    val versionFile: File
    val proot: File
    val prootLoader: File
}

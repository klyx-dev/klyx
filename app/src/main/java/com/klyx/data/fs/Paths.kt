package com.klyx.data.fs

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

object Paths : KoinComponent {

    private val context by inject<Context>()

    val filesDir: File get() = context.filesDir

    val tempDir: File get() = context.cacheDir

    val externalFilesDir: File
        get() = context.getExternalFilesDir(null)
            ?: error("shared storage is not currently available.")

    val nativeLibraryDir: File
        get() = File(context.applicationInfo.nativeLibraryDir)

    val dataDir: File get() = context.dataDir

    val externalCacheDir: File? get() = context.externalCacheDir

    val noBackupFilesDir: File get() = context.noBackupFilesDir

    val codeCacheDir: File get() = context.codeCacheDir

    val externalCacheDirs: Array<File> get() = context.externalCacheDirs

    val externalFilesDirs: Array<File> get() = context.getExternalFilesDirs(null)
}

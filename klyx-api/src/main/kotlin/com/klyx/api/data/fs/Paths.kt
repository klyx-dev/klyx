package com.klyx.api.data.fs

import android.content.Context
import com.klyx.core.koin
import java.io.File

/**
 * Utility for accessing standard Android application paths and Klyx-specific directories.
 *
 * Provides a type-safe way to retrieve various [File] paths used for data storage,
 * caching, and plugin management.
 *
 * ### Example
 * ```kotlin
 * val cache = Paths.tempDir
 * val plugins = Paths.pluginsDir
 * ```
 */
object Paths {

    private val context by koin<Context>()

    /** The absolute path to the internal directory where the application can place persistent files it owns. */
    val filesDir: File get() = context.filesDir

    /** The absolute path to the application-specific cache directory on the filesystem. */
    val tempDir: File get() = context.cacheDir

    /** The absolute path to the directory on the primary shared/external storage where the application can place persistent files it owns. */
    val externalFilesDir: File
        get() = context.getExternalFilesDir(null)
            ?: error("shared storage is not currently available.")

    /** The absolute path to the directory on the filesystem where all native libraries are stored. */
    val nativeLibraryDir: File
        get() = File(context.applicationInfo.nativeLibraryDir)

    /** The absolute path to the directory on the filesystem where all private data is stored. */
    val dataDir: File get() = context.dataDir

    /** The absolute path to the directory on the primary shared/external storage where the application can place cache files it owns. */
    val externalCacheDir: File? get() = context.externalCacheDir

    /** The absolute path to the directory on the filesystem where all private data that should not be backed up is stored. */
    val noBackupFilesDir: File get() = context.noBackupFilesDir

    /** The absolute path to the directory on the filesystem where all private data that should be backed up is stored. */
    val codeCacheDir: File get() = context.codeCacheDir

    /** Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place cache files it owns. */
    val externalCacheDirs: Array<File> get() = context.externalCacheDirs

    /** Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place persistent files it owns. */
    val externalFilesDirs: Array<File> get() = context.getExternalFilesDirs(null)
}

/**
 * The directory where installed Klyx plugins are stored.
 */
val Paths.pluginsDir get() = dataDir.resolve("klyx/plugins").also { it.createDirIfMissing() }

/**
 * The JSON file containing ids of currently installed plugins.
 */
val Paths.installedPluginsJson get() = pluginsDir.resolve("installed.json")

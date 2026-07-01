package com.klyx.api.data.fs

import com.klyx.api.plugin.PluginService
import java.io.File

/**
 * A service that provides access to important file system paths used by the application.
 *
 * This includes standard Android directories (like filesDir and cacheDir) as well as
 * specific paths for the terminal runtime (like rootfs and home).
 *
 * ### Example
 * ```kotlin
 * val paths: Paths by plugin()
 *
 * // Get the terminal home directory
 * val home = paths.homeDir
 *
 * // Get the projects directory
 * val projects = paths.projectsDir
 * ```
 */
interface Paths : PluginService {

    /** The absolute path to the internal directory where the application can place persistent files it owns. */
    val filesDir: File

    /** The absolute path to the application-specific cache directory on the filesystem. */
    val tempDir: File

    /** The absolute path to the directory on the primary shared/external storage where the application can place persistent files it owns. */
    val externalFilesDir: File

    /** The absolute path to the directory on the filesystem where all libraries are stored. */
    val nativeLibraryDir: File

    /** The absolute path to the directory on the filesystem where all private data is stored. */
    val dataDir: File

    /** The absolute path to the directory on the primary shared/external storage where the application can place cache files it owns. */
    val externalCacheDir: File?

    /** The absolute path to the directory on the filesystem where all private data that should not be backed up is stored. */
    val noBackupFilesDir: File

    /** The absolute path to the directory on the filesystem where all private data that should be backed up is stored. */
    val codeCacheDir: File

    /** Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place cache files it owns. */
    val externalCacheDirs: Array<File>

    /** Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place persistent files it owns. */
    val externalFilesDirs: Array<File>

    /** The root filesystem (rootfs) directory for the terminal environment. */
    val rootFs: File

    /** The home directory for the terminal user. */
    val homeDir: File

    /** The directory where user projects are stored. */
    val projectsDir: File

    /** The file that stores the current version of the terminal bootstrap. */
    val versionFile: File

    /** The absolute path to the PRoot executable. */
    val proot: File

    /** The absolute path to the PRoot loader library. */
    val prootLoader: File
}

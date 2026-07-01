package com.klyx.api.data.fs

/**
 * Describes the operations supported by a file or directory.
 *
 * Since Klyx supports multiple file systems (Local, SAF, etc.), different files
 * may have different capabilities depending on the underlying provider and permissions.
 *
 * @property canWrite Whether the file content can be modified.
 * @property canDelete Whether the file or directory can be deleted.
 * @property canRename Whether the file or directory can be renamed.
 * @property canCreate Whether new files or directories can be created within this directory.
 */
data class FileCapabilities(
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canRename: Boolean,
    val canCreate: Boolean,
)

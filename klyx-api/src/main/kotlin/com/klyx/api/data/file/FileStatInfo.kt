package com.klyx.api.data.file

/**
 * Represents detailed Unix-like file statistics.
 *
 * This class provides low-level information about a file.
 *
 * @property mode The raw file mode (type and permissions).
 * @property permissions A human-readable string representation of permissions (e.g., "rw-r--r--").
 * @property ownerUid The numeric user ID of the file owner.
 * @property ownerName The username of the file owner.
 * @property groupGid The numeric group ID of the file.
 * @property groupName The name of the group associated with the file.
 * @property hardLinks The number of hard links to the file.
 * @property inode The file's inode number.
 * @property deviceId The ID of the device containing the file.
 * @property blockSize The file system's preferred block size for I/O operations.
 * @property blocksAllocated The number of 512B blocks allocated for the file.
 * @property lastAccessed The time of last access (atime) in milliseconds since the epoch.
 * @property lastModified The time of last modification (mtime) in milliseconds since the epoch.
 * @property lastChanged The time of last status change (ctime) in milliseconds since the epoch.
 */
data class FileStatInfo(
    val mode: Int,
    val permissions: String,
    val ownerUid: Int,
    val ownerName: String,
    val groupGid: Int,
    val groupName: String,
    val hardLinks: Long,
    val inode: Long,
    val deviceId: Long,
    val blockSize: Long,
    val blocksAllocated: Long,
    val lastAccessed: Long,
    val lastModified: Long,
    val lastChanged: Long,
)

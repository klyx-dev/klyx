package com.klyx.api.data.file

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

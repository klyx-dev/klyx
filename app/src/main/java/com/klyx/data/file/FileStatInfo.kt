package com.klyx.data.file

import android.system.Os
import com.klyx.util.applicationContext

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
    val lastAccessed: Long, // epoch seconds
    val lastModified: Long,
    val lastChanged: Long, // metadata change, NOT creation
)

val KxFile.statInfo: FileStatInfo?
    get() {
        val stat = try {
            if (file != null) {
                Os.stat(file!!.absolutePath)
            } else if (!isDirectory) {
                applicationContext().contentResolver
                    .openFileDescriptor(uri, "r")
                    ?.use { Os.fstat(it.fileDescriptor) }
            } else null
        } catch (_: Exception) {
            null
        } ?: return null

        val ownerName = try {
            com.klyx.native.Os.getpwuid(stat.st_uid)?.pw_name ?: stat.st_uid.toString()
        } catch (_: Exception) {
            stat.st_uid.toString()
        }

        val groupName = try {
            com.klyx.native.Os.getgrgid(stat.st_gid)?.gr_name ?: stat.st_gid.toString()
        } catch (_: Exception) {
            stat.st_gid.toString()
        }

        return FileStatInfo(
            mode = stat.st_mode,
            permissions = permissionsString,
            ownerUid = stat.st_uid,
            ownerName = ownerName,
            groupGid = stat.st_gid,
            groupName = groupName,
            hardLinks = stat.st_nlink,
            inode = stat.st_ino,
            deviceId = stat.st_dev,
            blockSize = stat.st_blksize,
            blocksAllocated = stat.st_blocks,
            lastAccessed = stat.st_atim.tv_sec,
            lastModified = stat.st_mtim.tv_sec,
            lastChanged = stat.st_ctim.tv_sec,
        )
    }

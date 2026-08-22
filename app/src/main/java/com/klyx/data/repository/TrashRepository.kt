package com.klyx.data.repository

import com.klyx.data.database.TrashDao
import com.klyx.data.database.TrashEntity
import com.klyx.data.fs.SafeFileOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

class TrashRepository(
    private val trashDao: TrashDao,
    private val trashRoot: File
) {

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    fun observeItems(): Flow<List<TrashEntity>> = trashDao.observeAll()

    fun observeCount(): Flow<Int> = trashDao.observeCount()

    suspend fun moveToTrash(source: File): Result = withContext(Dispatchers.IO) {
        if (!source.exists()) {
            return@withContext Result.Failure("Item no longer exists.")
        }

        val isDirectory = source.isDirectory
        val entryName = "${System.currentTimeMillis()}-${source.name}"

        val entry = try {
            SafeFileOps.moveInto(source.toPath(), trashRoot.toPath(), entryName)
        } catch (e: Exception) {
            val _ = runCatching { SafeFileOps.delete(trashRoot.resolve(entryName).toPath()) }
            return@withContext Result.Failure(e.message ?: "Could not move item to Trash.")
        }

        trashDao.insert(
            TrashEntity(
                originalPath = source.absolutePath,
                displayName = source.name,
                isDirectory = isDirectory,
                sizeBytes = SafeFileOps.sizeOf(entry),
                deletedAt = System.currentTimeMillis(),
                trashName = entryName
            )
        )
        Result.Success
    }

    suspend fun restore(id: Long): Result = withContext(Dispatchers.IO) {
        val entity = trashDao.getById(id)
            ?: return@withContext Result.Failure("Item is no longer in Trash.")

        val item = trashRoot.resolve(entity.trashName)
        if (!item.exists()) {
            trashDao.deleteById(id)
            return@withContext Result.Failure("Trashed item is missing on disk.")
        }

        val original = File(entity.originalPath)
        original.parentFile?.mkdirs()

        var destination = original.toPath()
        var suffix = 1
        while (destination.toFile().exists()) {
            val base = original.nameWithoutExtension.ifEmpty { original.name }
            val ext = original.extension.takeIf { original.nameWithoutExtension.isNotEmpty() }
            val name = buildString {
                append(base)
                append(" ($suffix)")
                if (!ext.isNullOrEmpty()) append(".").append(ext)
            }
            destination = destination.resolveSibling(name)
            suffix++
        }

        try {
            Files.move(item.toPath(), destination)
        } catch (e: Exception) {
            return@withContext Result.Failure(e.message ?: "Could not restore item.")
        }

        trashDao.deleteById(id)
        Result.Success
    }

    suspend fun deletePermanently(id: Long): Result = withContext(Dispatchers.IO) {
        val entity = trashDao.getById(id) ?: return@withContext Result.Success

        try {
            SafeFileOps.delete(trashRoot.resolve(entity.trashName).toPath())
        } catch (e: Exception) {
            return@withContext Result.Failure(e.message ?: "Could not delete item.")
        }

        trashDao.deleteById(id)
        Result.Success
    }

    suspend fun emptyTrash(): Unit = withContext(Dispatchers.IO) {
        trashDao.observeAll().first().forEach { entity ->
            val _ = runCatching { SafeFileOps.delete(trashRoot.resolve(entity.trashName).toPath()) }
            trashDao.deleteById(entity.id)
        }
    }

    suspend fun purgeExpired(retentionDays: Int): Unit = withContext(Dispatchers.IO) {
        if (retentionDays <= 0) return@withContext

        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        trashDao.getOlderThan(cutoff).forEach { entity ->
            val _ = runCatching { SafeFileOps.delete(trashRoot.resolve(entity.trashName).toPath()) }
            trashDao.deleteById(entity.id)
        }
    }
}

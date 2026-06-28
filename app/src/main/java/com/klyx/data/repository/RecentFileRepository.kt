package com.klyx.data.repository

import android.net.Uri
import com.klyx.data.database.RecentFileDao
import com.klyx.data.database.RecentFileEntity
import com.klyx.api.data.file.KxFile
import org.koin.core.annotation.Single

@Single
class RecentFileRepository(
    private val dao: RecentFileDao
) {
    fun observeRecentFiles() = dao.observeRecentFiles()

    suspend fun getRecentFiles() = dao.getRecentFiles()

    suspend fun addRecentFile(file: KxFile, projectUri: Uri? = null) {
        dao.insert(
            RecentFileEntity(
                uri = file.uri.toString(),
                name = file.name,
                projectUri = projectUri?.toString(),
                lastOpened = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAll() = dao.clear()

    suspend fun removeFile(file: KxFile) = dao.deleteByUri(file.uri.toString())
    suspend fun removeByUri(uri: Uri) = dao.deleteByUri(uri.toString())
}

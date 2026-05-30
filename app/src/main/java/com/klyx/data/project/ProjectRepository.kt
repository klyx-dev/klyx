package com.klyx.data.project

import android.net.Uri
import com.klyx.data.file.wrap
import com.klyx.data.fs.FileSystem
import com.klyx.data.repository.RecentProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ProjectRepository(
    private val fs: FileSystem,
    private val recentProjectRepository: RecentProjectRepository
) {

    suspend fun listFiles(uri: Uri) = withContext(Dispatchers.IO) {
        fs.list(uri)
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun recentProjects() = recentProjectRepository.getProjects()

    suspend fun addRecent(uri: Uri, isExpanded: Boolean) {
        val file = withContext(Dispatchers.IO) { uri.wrap() }
        recentProjectRepository.addProject(
            uri = file.uri,
            name = file.name,
            isExpanded = isExpanded
        )
    }

    suspend fun removeRecent(uri: Uri) = recentProjectRepository.removeProject(uri)
}

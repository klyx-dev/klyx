package com.klyx.data.repository

import android.net.Uri
import com.klyx.data.database.RecentProjectDao
import com.klyx.data.database.RecentProjectEntity
import org.koin.core.annotation.Single

@Single
class RecentProjectRepository(
    private val dao: RecentProjectDao,
) {

    fun observeProjects() = dao.observeProjects()

    suspend fun getProjects() = dao.getProjects()

    suspend fun addProject(uri: Uri, name: String, isExpanded: Boolean) {
        dao.insert(
            RecentProjectEntity(
                uri = uri.toString(),
                name = name,
                isExpanded = isExpanded
            )
        )
    }

    suspend fun removeProject(uri: Uri) {
        dao.deleteByUri(uri.toString())
    }
}

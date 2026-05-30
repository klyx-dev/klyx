package com.klyx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentProjectDao {

    @Query(
        """
        SELECT * FROM recent_projects
        ORDER BY isPinned DESC, lastAccessed DESC
    """
    )
    fun observeProjects(): Flow<List<RecentProjectEntity>>

    @Query(
        """
        SELECT * FROM recent_projects
        ORDER BY isPinned DESC, lastAccessed DESC
    """
    )
    suspend fun getProjects(): List<RecentProjectEntity>

    @Query(
        """
        SELECT * FROM recent_projects
        WHERE uri = :uri
        LIMIT 1
    """
    )
    suspend fun getProject(uri: String): RecentProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: RecentProjectEntity)

    @Delete
    suspend fun delete(project: RecentProjectEntity)

    @Query(
        """
        DELETE FROM recent_projects
        WHERE uri = :uri
    """
    )
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM recent_projects")
    suspend fun clear()
}

package com.klyx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query(
        """
        SELECT * FROM recent_files
        ORDER BY isPinned DESC, lastOpened DESC
    """
    )
    fun observeRecentFiles(): Flow<List<RecentFileEntity>>

    @Query(
        """
        SELECT * FROM recent_files
        ORDER BY isPinned DESC, lastOpened DESC
    """
    )
    suspend fun getRecentFiles(): List<RecentFileEntity>

    @Query(
        """
        SELECT * FROM recent_files 
        WHERE projectUri = :projectUri
        ORDER BY isPinned DESC, lastOpened DESC
    """
    )
    fun observeProjectFiles(projectUri: String): Flow<List<RecentFileEntity>>

    @Query(
        """
        SELECT * FROM recent_files 
        WHERE projectUri IS NULL
        ORDER BY isPinned DESC, lastOpened DESC
    """
    )
    fun observeStandaloneFiles(): Flow<List<RecentFileEntity>>

    @Query(
        """
        SELECT * FROM recent_files
        WHERE uri = :uri
        LIMIT 1
    """
    )
    suspend fun getFile(uri: String): RecentFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: RecentFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<RecentFileEntity>)

    @Delete
    suspend fun delete(file: RecentFileEntity)

    @Query(
        """
        DELETE FROM recent_files
        WHERE uri = :uri
    """
    )
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM recent_files")
    suspend fun clear()
}

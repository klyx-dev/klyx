package com.klyx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Insert
    suspend fun insert(entity: TrashEntity): Long

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Query("SELECT COUNT(*) FROM trash_items")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM trash_items WHERE id = :id")
    suspend fun getById(id: Long): TrashEntity?

    @Query("SELECT * FROM trash_items WHERE deletedAt < :cutoff")
    suspend fun getOlderThan(cutoff: Long): List<TrashEntity>

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trash_items")
    suspend fun deleteAll()
}

package com.klyx.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_files",
    indices = [Index("projectUri")]
)
data class RecentFileEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val projectUri: String? = null,
    val lastOpened: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
)

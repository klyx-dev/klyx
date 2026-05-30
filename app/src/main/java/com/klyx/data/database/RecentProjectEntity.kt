package com.klyx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_projects")
data class RecentProjectEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val lastAccessed: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isExpanded: Boolean = false
)

package com.klyx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val displayName: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val deletedAt: Long,
    val trashName: String
)

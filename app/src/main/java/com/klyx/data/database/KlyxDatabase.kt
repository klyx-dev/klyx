package com.klyx.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecentProjectEntity::class,
        RecentFileEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class KlyxDatabase : RoomDatabase() {

    abstract fun recentProjectDao(): RecentProjectDao

    abstract fun recentFileDao(): RecentFileDao
}

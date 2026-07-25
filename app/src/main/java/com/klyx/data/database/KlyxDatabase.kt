package com.klyx.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecentProjectEntity::class,
        RecentFileEntity::class,
        PluginTabEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class KlyxDatabase : RoomDatabase() {

    abstract fun recentProjectDao(): RecentProjectDao

    abstract fun recentFileDao(): RecentFileDao

    abstract fun pluginTabDao(): PluginTabDao
}

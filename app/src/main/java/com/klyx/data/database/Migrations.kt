package com.klyx.data.database

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = Migration(1, 2) { db ->
    db.execSQL("CREATE TABLE IF NOT EXISTS `recent_projects` (`uri` TEXT NOT NULL, `name` TEXT NOT NULL, `lastAccessed` INTEGER NOT NULL DEFAULT 0, `isPinned` INTEGER NOT NULL DEFAULT 0, `isExpanded` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`uri`))")
}

object Migration12Spec : AutoMigrationSpec

package com.klyx.data.database

import androidx.room.migration.Migration

val MIGRATION_1_2 = Migration(1, 2) { db ->
    db.execSQL("CREATE TABLE IF NOT EXISTS `recent_projects` (`uri` TEXT NOT NULL, `name` TEXT NOT NULL, `lastAccessed` INTEGER NOT NULL DEFAULT 0, `isPinned` INTEGER NOT NULL DEFAULT 0, `isExpanded` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`uri`))")
}

val MIGRATION_2_3 = Migration(2, 3) { db ->
    db.execSQL("CREATE TABLE IF NOT EXISTS `plugin_tabs` (`tabId` TEXT NOT NULL, `pluginId` TEXT NOT NULL, `title` TEXT NOT NULL, `metadata` TEXT, `lastOpened` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`tabId`))")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_plugin_tabs_pluginId` ON `plugin_tabs` (`pluginId`)")
}

val MIGRATION_3_4 = Migration(3, 4) { db ->
    db.execSQL("CREATE TABLE IF NOT EXISTS `trash_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `originalPath` TEXT NOT NULL, `displayName` TEXT NOT NULL, `isDirectory` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, `deletedAt` INTEGER NOT NULL, `trashName` TEXT NOT NULL)")
}

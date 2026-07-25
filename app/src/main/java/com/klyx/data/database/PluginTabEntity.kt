package com.klyx.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plugin_tabs",
    indices = [Index("pluginId")]
)
data class PluginTabEntity(
    @PrimaryKey
    val tabId: String,
    val pluginId: String,
    val title: String,
    val metadata: String? = null,
    val lastOpened: Long = System.currentTimeMillis(),
)

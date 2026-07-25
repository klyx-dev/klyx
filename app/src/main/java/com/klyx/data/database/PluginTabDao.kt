package com.klyx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PluginTabDao {

    @Query("SELECT * FROM plugin_tabs ORDER BY lastOpened DESC")
    suspend fun getPluginTabs(): List<PluginTabEntity>

    @Query("SELECT * FROM plugin_tabs WHERE pluginId = :pluginId ORDER BY lastOpened DESC")
    suspend fun getPluginTabsByPlugin(pluginId: String): List<PluginTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: PluginTabEntity)

    @Query("DELETE FROM plugin_tabs WHERE tabId = :tabId")
    suspend fun deleteByTabId(tabId: String)

    @Query("DELETE FROM plugin_tabs WHERE pluginId = :pluginId")
    suspend fun deleteByPlugin(pluginId: String)

    @Query("DELETE FROM plugin_tabs")
    suspend fun clear()
}

package com.klyx.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.klyx.BuildConfig
import com.klyx.data.database.KlyxDatabase
import com.klyx.data.database.MIGRATION_1_2
import com.klyx.data.database.MIGRATION_2_3
import com.klyx.data.database.MIGRATION_3_4
import com.klyx.data.database.TrashDao
import com.klyx.data.preferences.SettingsDataStore
import com.klyx.data.preferences.dataStore
import com.klyx.data.repository.TrashRepository
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import java.io.File

@Module
@ComponentScan("com.klyx")
object AppModule

@Singleton
fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

@Singleton
fun provideAppDatabase(context: Context) = Room
    .databaseBuilder(
        context = context.applicationContext,
        klass = KlyxDatabase::class.java,
        name = "klyx_database"
    )
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    .apply {
        if (BuildConfig.DEBUG) {
            fallbackToDestructiveMigration(dropAllTables = true)
        }
    }
    .build()

@Singleton
fun provideRecentFileDao(db: KlyxDatabase) = db.recentFileDao()

@Singleton
fun provideRecentProjectDao(db: KlyxDatabase) = db.recentProjectDao()

@Singleton
fun providePluginTabDao(db: KlyxDatabase) = db.pluginTabDao()

@Singleton
fun provideTrashDao(db: KlyxDatabase) = db.trashDao()

@Singleton
fun provideTrashRepository(context: Context, trashDao: TrashDao): TrashRepository =
    TrashRepository(trashDao, File(context.filesDir, "trash/items"))

@Singleton
fun provideAppPreferences(context: Context): SettingsDataStore = context.dataStore

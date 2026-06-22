package com.klyx.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.klyx.BuildConfig
import com.klyx.data.database.KlyxDatabase
import com.klyx.data.database.MIGRATION_1_2
import com.klyx.data.preferences.SettingsDataStore
import com.klyx.data.preferences.dataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

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
    .addMigrations(MIGRATION_1_2)
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
fun provideAppPreferences(context: Context): SettingsDataStore = context.dataStore

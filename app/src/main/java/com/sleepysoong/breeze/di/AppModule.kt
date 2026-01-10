package com.sleepysoong.breeze.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.sleepysoong.breeze.data.local.BreezeDatabase
import com.sleepysoong.breeze.data.local.dao.RunningRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "breeze_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BreezeDatabase {
        return Room.databaseBuilder(
            context,
            BreezeDatabase::class.java,
            "breeze_database"
        )
            .addMigrations(BreezeDatabase.MIGRATION_1_2)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRunningRecordDao(database: BreezeDatabase): RunningRecordDao {
        return database.runningRecordDao()
    }
    
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}

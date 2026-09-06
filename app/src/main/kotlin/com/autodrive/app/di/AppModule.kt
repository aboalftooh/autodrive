package com.autodrive.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.autodrive.app.core.database.AutoDriveDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autodrive_cache")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AutoDriveDatabase =
        Room.databaseBuilder(context, AutoDriveDatabase::class.java, "autodrive.db")
            .addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)
            .build()

    @Provides @Singleton
    fun provideConversationDao(db: AutoDriveDatabase) = db.conversationDao()

    @Provides @Singleton
    fun provideChatMessageDao(db: AutoDriveDatabase) = db.chatMessageDao()

    @Provides @Singleton
    fun provideDynamoContentDao(db: AutoDriveDatabase) = db.dynamoContentDao()

    @Provides @Singleton
    fun provideWeeklyLeaderboardDao(db: AutoDriveDatabase) = db.weeklyLeaderboardDao()
}

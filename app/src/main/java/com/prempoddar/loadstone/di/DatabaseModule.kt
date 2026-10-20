package com.prempoddar.loadstone.di

import android.content.Context
import androidx.room.Room
import com.prempoddar.loadstone.data.db.LoadstoneDatabase
import com.prempoddar.loadstone.data.db.PinDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LoadstoneDatabase =
        Room.databaseBuilder(context, LoadstoneDatabase::class.java, "loadstone.db")
            .addMigrations(LoadstoneDatabase.MIGRATION_1_2, LoadstoneDatabase.MIGRATION_2_3, LoadstoneDatabase.MIGRATION_3_4, LoadstoneDatabase.MIGRATION_4_5)
            .build()

    @Provides
    fun providePinDao(db: LoadstoneDatabase): PinDao = db.pinDao()
}

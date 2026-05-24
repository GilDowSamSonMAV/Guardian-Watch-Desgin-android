package com.gltech.guardianwatch.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [VitalSignsEntity::class, CasualtyEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vitalSignsDao(): VitalSignsDao
    abstract fun casualtyDao(): CasualtyDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "guardian_watch.db")
            .fallbackToDestructiveMigration()   // MVP — no migrations needed yet
            .build()

    @Provides fun provideVitalSignsDao(db: AppDatabase): VitalSignsDao = db.vitalSignsDao()
    @Provides fun provideCasualtyDao(db: AppDatabase): CasualtyDao = db.casualtyDao()
}

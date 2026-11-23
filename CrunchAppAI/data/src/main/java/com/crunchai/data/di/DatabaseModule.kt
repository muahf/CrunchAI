package com.crunchai.data.di

import android.content.Context
import androidx.room.Room
import com.crunchai.data.local.db.CrunchDatabase
import com.crunchai.data.local.db.DiaryCacheDao
import com.crunchai.data.local.db.PetDao
import com.crunchai.data.local.db.SoundEventDao
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): CrunchDatabase =
        Room.databaseBuilder(context, CrunchDatabase::class.java, "crunchai.db")
            .addMigrations(CrunchDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideSoundEventDao(db: CrunchDatabase): SoundEventDao = db.soundEventDao()

    @Provides
    fun providePetDao(db: CrunchDatabase): PetDao = db.petDao()

    @Provides
    fun provideDiaryCacheDao(db: CrunchDatabase): DiaryCacheDao = db.diaryCacheDao()
}

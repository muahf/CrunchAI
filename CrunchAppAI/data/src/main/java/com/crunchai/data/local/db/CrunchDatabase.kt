package com.crunchai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SoundEventEntity::class,
        PetEntity::class,
        MealEntity::class,
        AnomalyEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class CrunchDatabase : RoomDatabase() {
    abstract fun soundEventDao(): SoundEventDao

    abstract fun petDao(): PetDao

    abstract fun diaryCacheDao(): DiaryCacheDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE pets ADD COLUMN notes TEXT")
                }
            }
    }
}

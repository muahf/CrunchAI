package com.crunchai.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SoundEventEntity)

    @Query("SELECT * FROM sound_events WHERE syncStatus = 'LOCAL' ORDER BY startedAtEpochMillis ASC LIMIT :limit")
    suspend fun getUnsent(limit: Int): List<SoundEventEntity>

    @Query("UPDATE sound_events SET syncStatus = 'SENT' WHERE id IN (:ids)")
    suspend fun markSent(ids: List<String>)
}

@Dao
interface PetDao {
    @Query("SELECT * FROM pets LIMIT 1")
    fun observePet(): Flow<PetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPet(pet: PetEntity)
}

@Dao
interface DiaryCacheDao {
    @Query("SELECT * FROM cached_meals WHERE startedAtEpochMillis >= :fromEpochMillis ORDER BY startedAtEpochMillis DESC")
    suspend fun getMealsSince(fromEpochMillis: Long): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Query("SELECT * FROM cached_anomalies WHERE startedAtEpochMillis >= :fromEpochMillis ORDER BY startedAtEpochMillis DESC")
    suspend fun getAnomaliesSince(fromEpochMillis: Long): List<AnomalyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomalies(anomalies: List<AnomalyEntity>)
}

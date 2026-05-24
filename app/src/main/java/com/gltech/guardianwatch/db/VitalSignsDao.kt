package com.gltech.guardianwatch.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalSignsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VitalSignsEntity)

    /** Live stream of the last N readings for a casualty — used by the HR chart. */
    @Query("SELECT * FROM vital_signs WHERE casualtyId = :id ORDER BY timestampMs DESC LIMIT :limit")
    fun recentForCasualty(id: String, limit: Int = 60): Flow<List<VitalSignsEntity>>

    /** Latest single reading — used for the vitals tile. */
    @Query("SELECT * FROM vital_signs WHERE casualtyId = :id ORDER BY timestampMs DESC LIMIT 1")
    suspend fun latestForCasualty(id: String): VitalSignsEntity?

    /** Purge readings older than a cutoff (keep DB lean on a field tablet). */
    @Query("DELETE FROM vital_signs WHERE timestampMs < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}

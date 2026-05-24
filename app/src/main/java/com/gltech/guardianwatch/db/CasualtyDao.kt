package com.gltech.guardianwatch.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CasualtyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CasualtyEntity)

    @Query("SELECT * FROM casualties")
    suspend fun all(): List<CasualtyEntity>

    @Query("DELETE FROM casualties WHERE id = :id")
    suspend fun delete(id: String)
}

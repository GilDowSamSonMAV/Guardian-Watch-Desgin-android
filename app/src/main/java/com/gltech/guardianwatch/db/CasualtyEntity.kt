package com.gltech.guardianwatch.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists a Casualty as a JSON blob.
 * Using a single JSON column keeps schema migrations simple — Casualty evolves
 * without needing DB migrations for every field addition.
 */
@Entity(tableName = "casualties")
data class CasualtyEntity(
    @PrimaryKey val id: String,
    val json: String,          // kotlinx.serialization JSON of Casualty
    val updatedAtMs: Long,
)

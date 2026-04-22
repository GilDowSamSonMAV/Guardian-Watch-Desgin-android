package com.gltech.guardianwatch.mode

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Operating modes for the tablet.
 * - MEDIC_DASHBOARD: receives from multiple (≤16) casualty watches, shows squad overview,
 *   full chain-of-custody. Primary mode for Role-1/Role-2 medics.
 * - SINGLE_PAIRED: 1 watch ↔ 1 tablet. Offline triage device. Buddy-aid scenario.
 *   Simplest mode, no network.
 * - RELAY: receives BLE vitals, forwards upstream (radio/cloud). Minimal UI —
 *   status of each bridge. Runs as foreground service, tablet may be in a ruck.
 *
 * Mode is user-selectable at launch and persists in DataStore until changed.
 * Changing mode requires app restart (BLE service lifecycle is mode-specific).
 */
enum class AppMode(val displayName: String, val description: String) {
    MEDIC_DASHBOARD(
        displayName = "Medic Dashboard",
        description = "Multi-casualty view. Role-1/2. Full chain-of-custody."
    ),
    SINGLE_PAIRED(
        displayName = "Single Paired",
        description = "1 watch → 1 tablet. Offline triage. Buddy-aid."
    ),
    RELAY(
        displayName = "Relay Node",
        description = "BLE → upstream (radio/cloud). Background operation."
    );

    companion object {
        fun fromName(name: String?): AppMode =
            entries.firstOrNull { it.name == name } ?: MEDIC_DASHBOARD
    }
}

// DataStore for persisting mode selection
private val Context.modeStore by preferencesDataStore(name = "gw_mode_store")
private val MODE_KEY = stringPreferencesKey("selected_mode")

class ModeController(private val context: Context) {

    /** Emits the persisted mode, or null if the user has never selected one. */
    val currentMode: Flow<AppMode?> = context.modeStore.data.map { prefs ->
        prefs[MODE_KEY]?.let { AppMode.fromName(it) }
    }

    suspend fun setMode(mode: AppMode) {
        context.modeStore.edit { prefs ->
            prefs[MODE_KEY] = mode.name
        }
    }

    suspend fun clear() {
        context.modeStore.edit { it.remove(MODE_KEY) }
    }
}

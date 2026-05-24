package com.gltech.guardianwatch.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the medic's (tablet's) GPS position as a StateFlow.
 * Used to show the medic's pin on the tactical map.
 *
 * Requires ACCESS_FINE_LOCATION permission — checked before calling start().
 */
@Singleton
class MedicLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            _location.value = loc
        }
        @Deprecated("Kept for API < 29 compatibility")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    /** Call after location permission is granted. Safe to call multiple times. */
    fun start() {
        try {
            // Try GPS first (most accurate), fall back to network.
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> return
            }
            // Seed with last known so the pin appears immediately.
            locationManager.getLastKnownLocation(provider)?.let { _location.value = it }
            locationManager.requestLocationUpdates(
                provider,
                2_000L,   // min 2 s between updates
                2f,       // min 2 m movement
                listener,
            )
        } catch (_: SecurityException) {
            // Permission not granted — will try again when MainActivity requests it.
        }
    }

    fun stop() {
        try { locationManager.removeUpdates(listener) } catch (_: Throwable) {}
    }
}

package com.gltech.guardianwatch.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.gltech.guardianwatch.MainActivity
import com.gltech.guardianwatch.R
import com.gltech.guardianwatch.casualty.Casualty
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service owning all BLE connections.
 *
 * WHY FOREGROUND: Android 14+ aggressively kills background BT connections
 * (battery policy). Only FGS with foregroundServiceType="connectedDevice"
 * is guaranteed to run a BLE session reliably long-term.
 *
 * WHY LifecycleService: we collect flows from BleManagers; lifecycleScope
 * cancels cleanly on stop.
 */
@AndroidEntryPoint
class BleService : LifecycleService() {

    @Inject lateinit var vitalsRepository: VitalsRepository

    private val managers = mutableMapOf<String, WatchBleManager>()   // key = MAC
    private val casualtiesByMac = mutableMapOf<String, String>()     // MAC -> casualtyId

    /** Lazily-created BLE scanner. Held here so the service can supervise its
     *  lifetime and callers don't each instantiate their own. */
    val scanner: BleScanner by lazy { BleScanner(applicationContext) }

    inner class LocalBinder : Binder() {
        fun service(): BleService = this@BleService
    }
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        startForegroundWithNotification(watchCount = 0)
        lifecycleScope.launch {
            vitalsRepository.loadPersistedCasualties().forEach { pairAndConnect(it) }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY   // auto-restart if OS kills us
    }

    override fun onDestroy() {
        managers.values.forEach { it.disconnect().enqueue() }
        managers.clear()
        super.onDestroy()
    }

    /** Add a casualty + MAC pairing, then connect. */
    fun pairAndConnect(casualty: Casualty) {
        val mac = casualty.watchDeviceAddress
        if (managers.containsKey(mac)) return

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val device = adapter.getRemoteDevice(mac)

        val manager = WatchBleManager(applicationContext)
        managers[mac] = manager
        casualtiesByMac[mac] = casualty.id

        vitalsRepository.registerCasualty(casualty)
        vitalsRepository.attachWatch(lifecycleScope, casualty.id, manager)

        manager.connectWithProfile(device)
        updateForegroundNotification()
    }

    fun disconnect(mac: String) {
        managers[mac]?.disconnect()?.enqueue()
        managers.remove(mac)
        val casId = casualtiesByMac.remove(mac)
        casId?.let { vitalsRepository.removeCasualty(it) }
        updateForegroundNotification()
    }

    // ---------- Notifications ----------

    private fun createChannelIfNeeded() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ble_service_channel),
                NotificationManager.IMPORTANCE_LOW,  // silent — not an alert channel
            ).apply {
                description = "Streaming vitals from paired watches."
                setShowBadge(false)
            }
            mgr.createNotificationChannel(ch)
        }
    }

    private fun startForegroundWithNotification(watchCount: Int) {
        val notif = buildNotification(watchCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun updateForegroundNotification() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(managers.size))
    }

    private fun buildNotification(watchCount: Int): Notification {
        val tapPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.ble_service_notif_title))
            .setContentText(getString(R.string.ble_service_notif_body, watchCount))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(tapPendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gw_ble_channel"
        private const val NOTIF_ID = 1337
    }
}

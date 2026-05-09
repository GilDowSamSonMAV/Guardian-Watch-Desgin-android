package com.gltech.guardianwatch.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives device-admin lifecycle events.
 *
 * To activate:
 *   adb shell dpm set-device-owner com.gltech.guardianwatch/.kiosk.KioskAdminReceiver
 *
 * Note: `set-device-owner` only works on a *freshly provisioned* device
 * (factory-reset, no other accounts). See DEPLOYMENT.md for the reset workflow.
 */
class KioskAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "KioskAdminReceiver"
        fun getComponent(ctx: Context) = ComponentName(ctx, KioskAdminReceiver::class.java)
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled.")
        KioskController(context).enableLockdown()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device admin disabled — kiosk mode inactive.")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        KioskController(context).enableLockdown()
    }
}

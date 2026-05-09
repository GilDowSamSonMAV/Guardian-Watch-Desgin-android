package com.gltech.guardianwatch.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.gltech.guardianwatch.BuildConfig
import com.gltech.guardianwatch.MainActivity

/**
 * Kiosk lifecycle helpers.
 *
 * There are three layers of "lock":
 *  1. Screen Pinning (API 21+, any device). User can pin one app manually.
 *     → Not strong enough for field deployment; user can unpin with back+recents.
 *  2. LockTask (requires Device Owner). App is truly unexitable.
 *     Activated via [Activity.startLockTask].
 *  3. HOME intent category in manifest → this app becomes the launcher, so even
 *     if user escapes via system UI, home button returns here.
 *
 * We use all three. Layer 3 (HOME) is in the manifest. Layer 2 (LockTask) is
 * activated here when Device Owner is available.
 */
class KioskController(private val context: Context) {

    private val dpm: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val adminComponent = KioskAdminReceiver.getComponent(context)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /**
     * Full lockdown. Call from [Activity.onResume] after [Activity.startLockTask].
     * Requires Device Owner.
     */
    fun enableLockdown() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not device owner — skipping DPM lockdown setup.")
            return
        }
        // Whitelist only this package for LockTask.
        dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))

        // Suppress system UI even while locked (Android 9+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val features = (
                DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS or
                DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS or
                DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD or
                DevicePolicyManager.LOCK_TASK_FEATURE_HOME
            )
            dpm.setLockTaskFeatures(adminComponent, features)
        }

        // Make our app the persistent launcher.
        val filter = android.content.IntentFilter(Intent_ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            addCategory(android.content.Intent.CATEGORY_DEFAULT)
        }
        val main = android.content.ComponentName(context, MainActivity::class.java)
        dpm.addPersistentPreferredActivity(adminComponent, filter, main)
    }

    /**
     * Call from [Activity.onResume] to enter LockTask mode.
     * Safe to call even when not device owner — it will just pin-like (recoverable).
     */
    fun startLockTask(activity: Activity) {
        if (isDeviceOwner()) {
            activity.startLockTask()
        } else {
            Log.i(TAG, "Device-owner not set — running in normal mode. See DEPLOYMENT.md to provision.")
        }
    }

    fun stopLockTask(activity: Activity) {
        try {
            activity.stopLockTask()
        } catch (t: Throwable) {
            Log.w(TAG, "stopLockTask failed: ${t.message}")
        }
    }

    /** Permanent disable (for factory reset workflows). */
    fun fullyRelinquish() {
        if (!isDeviceOwner()) return
        dpm.clearDeviceOwnerApp(context.packageName)
    }

    companion object {
        private const val TAG = "KioskController"
        private const val Intent_ACTION_MAIN = android.content.Intent.ACTION_MAIN
    }
}

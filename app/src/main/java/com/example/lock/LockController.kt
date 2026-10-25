package com.example.lock

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.LockHistoryItem
import com.example.data.TabLockPreferences
import com.example.data.UnlockMode
import com.example.receiver.TabLockDeviceAdminReceiver
import com.example.ui.lock.LockActivity

object LockController {
    private const val TAG = "LockController"

    fun isDeviceAdminActive(context: Context): Boolean {
        return TabLockDeviceAdminReceiver.isAdminActive(context)
    }

    fun lockNow(
        context: Context,
        triggerSource: String = "Timer",
        durationMinutes: Int = 0
    ) {
        val prefs = TabLockPreferences(context)
        val adminActive = isDeviceAdminActive(context)
        val mode = prefs.unlockMode

        Log.d(TAG, "lockNow called. adminActive=$adminActive, mode=$mode, source=$triggerSource")

        // Vibrate if enabled
        if (prefs.vibrateEnabled) {
            triggerVibration(context)
        }

        // Add history record
        val historyItem = LockHistoryItem(
            id = System.currentTimeMillis().toString(),
            timestamp = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            triggerSource = triggerSource,
            unlockMethodUsed = if (mode == UnlockMode.IN_APP_PIN) "In-App PIN (${prefs.inAppPin})" else "Default Device Password",
            completedSuccessfully = true
        )
        prefs.addHistoryItem(historyItem)

        // If in-app lock screen is needed or if admin is not active:
        val shouldLaunchLockActivity = mode == UnlockMode.IN_APP_PIN || mode == UnlockMode.HYBRID_BOTH || !adminActive

        if (shouldLaunchLockActivity) {
            val lockIntent = Intent(context, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra("EXTRA_TRIGGER_SOURCE", triggerSource)
            }
            context.startActivity(lockIntent)
        }

        // If system lock is active and requested:
        if (adminActive && (mode == UnlockMode.SYSTEM_DEFAULT_PASSWORD || mode == UnlockMode.HYBRID_BOTH)) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            try {
                dpm?.lockNow()
                Log.d(TAG, "DevicePolicyManager.lockNow() executed successfully")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to call lockNow() due to SecurityException", e)
            }
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 400), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not trigger vibration", e)
        }
    }
}

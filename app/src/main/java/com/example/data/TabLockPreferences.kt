package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class TabLockPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "tab_lock_preferences"
        private const val KEY_IN_APP_PIN = "in_app_pin"
        private const val KEY_UNLOCK_MODE = "unlock_mode"
        private const val KEY_DEFAULT_DURATION = "default_duration_sec"
        private const val KEY_WARNING_SEC = "warning_sec"
        private const val KEY_PLAY_WARNING_CHIME = "play_warning_chime"
        private const val KEY_VIBRATE = "vibrate_enabled"
        private const val KEY_BEDTIME_ENABLED = "bedtime_enabled"
        private const val KEY_BEDTIME_HOUR = "bedtime_hour"
        private const val KEY_BEDTIME_MINUTE = "bedtime_minute"
        private const val KEY_HISTORY = "lock_history_json"
        private const val KEY_LAST_CUSTOM_DURATION = "last_custom_duration"
    }

    var inAppPin: String
        get() = prefs.getString(KEY_IN_APP_PIN, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_IN_APP_PIN, value).apply()

    var unlockMode: UnlockMode
        get() {
            val name = prefs.getString(KEY_UNLOCK_MODE, UnlockMode.HYBRID_BOTH.name)
            return try {
                UnlockMode.valueOf(name ?: UnlockMode.HYBRID_BOTH.name)
            } catch (e: Exception) {
                UnlockMode.HYBRID_BOTH
            }
        }
        set(value) = prefs.edit().putString(KEY_UNLOCK_MODE, value.name).apply()

    var defaultDurationSeconds: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 15 * 60)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_DURATION, value).apply()

    var lastCustomDurationSeconds: Int
        get() = prefs.getInt(KEY_LAST_CUSTOM_DURATION, 15 * 60)
        set(value) = prefs.edit().putInt(KEY_LAST_CUSTOM_DURATION, value).apply()

    var warningSeconds: Int
        get() = prefs.getInt(KEY_WARNING_SEC, 60)
        set(value) = prefs.edit().putInt(KEY_WARNING_SEC, value).apply()

    var playWarningChime: Boolean
        get() = prefs.getBoolean(KEY_PLAY_WARNING_CHIME, true)
        set(value) = prefs.edit().putBoolean(KEY_PLAY_WARNING_CHIME, value).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var bedtimeEnabled: Boolean
        get() = prefs.getBoolean(KEY_BEDTIME_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BEDTIME_ENABLED, value).apply()

    var bedtimeHour: Int
        get() = prefs.getInt(KEY_BEDTIME_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_BEDTIME_HOUR, value).apply()

    var bedtimeMinute: Int
        get() = prefs.getInt(KEY_BEDTIME_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_BEDTIME_MINUTE, value).apply()

    fun getHistory(): List<LockHistoryItem> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<LockHistoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LockHistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        durationMinutes = obj.optInt("durationMinutes", 0),
                        triggerSource = obj.optString("triggerSource", "Timer"),
                        unlockMethodUsed = obj.optString("unlockMethodUsed", "Default Password"),
                        completedSuccessfully = obj.optBoolean("completedSuccessfully", true)
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return list
    }

    fun addHistoryItem(item: LockHistoryItem) {
        val current = getHistory().toMutableList()
        current.add(0, item) // Add latest first
        val limited = current.take(50) // Keep latest 50 entries
        val array = JSONArray()
        for (h in limited) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("timestamp", h.timestamp)
            obj.put("durationMinutes", h.durationMinutes)
            obj.put("triggerSource", h.triggerSource)
            obj.put("unlockMethodUsed", h.unlockMethodUsed)
            obj.put("completedSuccessfully", h.completedSuccessfully)
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}

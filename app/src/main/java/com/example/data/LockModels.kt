package com.example.data

data class LockHistoryItem(
    val id: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val triggerSource: String, // "Countdown Timer", "Instant Lock", "Bedtime Schedule"
    val unlockMethodUsed: String = "Device Password / PIN",
    val completedSuccessfully: Boolean = true
)

enum class UnlockMode {
    SYSTEM_DEFAULT_PASSWORD, // Lock screen using Device Admin (device's own lock screen password)
    IN_APP_PIN,              // In-app PIN lock screen (default: 1234)
    HYBRID_BOTH              // Both system lock and in-app PIN protection
}

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    LOCKED
}

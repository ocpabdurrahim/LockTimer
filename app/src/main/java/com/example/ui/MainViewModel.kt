package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LockHistoryItem
import com.example.data.TabLockPreferences
import com.example.data.TimerState
import com.example.data.UnlockMode
import com.example.lock.LockController
import com.example.receiver.TabLockDeviceAdminReceiver
import com.example.service.TabLockTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = TabLockPreferences(application)

    // Current selected duration in seconds (for setting up before starting)
    private val _selectedDurationSeconds = MutableStateFlow(prefs.defaultDurationSeconds)
    val selectedDurationSeconds: StateFlow<Int> = _selectedDurationSeconds.asStateFlow()

    // Observe service timer state
    val timerRemainingSeconds: StateFlow<Int> = TabLockTimerService.remainingSeconds
    val timerTotalSeconds: StateFlow<Int> = TabLockTimerService.totalSeconds
    val timerState: StateFlow<TimerState> = TabLockTimerService.timerState

    // Device Admin status
    private val _isDeviceAdminActive = MutableStateFlow(false)
    val isDeviceAdminActive: StateFlow<Boolean> = _isDeviceAdminActive.asStateFlow()

    // Preferences states
    private val _inAppPin = MutableStateFlow(prefs.inAppPin)
    val inAppPin: StateFlow<String> = _inAppPin.asStateFlow()

    private val _unlockMode = MutableStateFlow(prefs.unlockMode)
    val unlockMode: StateFlow<UnlockMode> = _unlockMode.asStateFlow()

    private val _warningSeconds = MutableStateFlow(prefs.warningSeconds)
    val warningSeconds: StateFlow<Int> = _warningSeconds.asStateFlow()

    private val _playWarningChime = MutableStateFlow(prefs.playWarningChime)
    val playWarningChime: StateFlow<Boolean> = _playWarningChime.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(prefs.vibrateEnabled)
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

    private val _bedtimeEnabled = MutableStateFlow(prefs.bedtimeEnabled)
    val bedtimeEnabled: StateFlow<Boolean> = _bedtimeEnabled.asStateFlow()

    private val _bedtimeHour = MutableStateFlow(prefs.bedtimeHour)
    val bedtimeHour: StateFlow<Int> = _bedtimeHour.asStateFlow()

    private val _bedtimeMinute = MutableStateFlow(prefs.bedtimeMinute)
    val bedtimeMinute: StateFlow<Int> = _bedtimeMinute.asStateFlow()

    private val _historyList = MutableStateFlow<List<LockHistoryItem>>(emptyList())
    val historyList: StateFlow<List<LockHistoryItem>> = _historyList.asStateFlow()

    init {
        refreshAdminStatus()
        refreshHistory()
    }

    fun refreshAdminStatus() {
        _isDeviceAdminActive.value = TabLockDeviceAdminReceiver.isAdminActive(getApplication())
    }

    fun refreshHistory() {
        _historyList.value = prefs.getHistory()
    }

    fun setDuration(seconds: Int) {
        val clamped = seconds.coerceIn(5, 24 * 3600)
        _selectedDurationSeconds.value = clamped
        prefs.lastCustomDurationSeconds = clamped
    }

    fun addDuration(deltaSeconds: Int) {
        val current = _selectedDurationSeconds.value
        setDuration(current + deltaSeconds)
    }

    fun startTimer(source: String = "Countdown Timer") {
        val context = getApplication<Application>()
        TabLockTimerService.startTimer(context, _selectedDurationSeconds.value, source)
    }

    fun pauseTimer() {
        val context = getApplication<Application>()
        TabLockTimerService.pauseTimer(context)
    }

    fun resumeTimer() {
        val context = getApplication<Application>()
        TabLockTimerService.resumeTimer(context)
    }

    fun stopTimer() {
        val context = getApplication<Application>()
        TabLockTimerService.stopTimer(context)
    }

    fun add5MinutesToActiveTimer() {
        val context = getApplication<Application>()
        TabLockTimerService.add5Minutes(context)
    }

    fun lockImmediately() {
        val context = getApplication<Application>()
        if (timerState.value == TimerState.RUNNING) {
            TabLockTimerService.lockImmediately(context)
        } else {
            LockController.lockNow(context, "Instant Lock", 0)
        }
        refreshHistory()
    }

    fun testLockIn5Seconds() {
        val context = getApplication<Application>()
        setDuration(5)
        TabLockTimerService.startTimer(context, 5, "Test Lock (5s)")
    }

    fun updateInAppPin(newPin: String): Boolean {
        if (newPin.length >= 4 && newPin.all { it.isDigit() }) {
            prefs.inAppPin = newPin
            _inAppPin.value = newPin
            return true
        }
        return false
    }

    fun updateUnlockMode(mode: UnlockMode) {
        prefs.unlockMode = mode
        _unlockMode.value = mode
    }

    fun updateWarningSettings(seconds: Int, playChime: Boolean, vibrate: Boolean) {
        prefs.warningSeconds = seconds
        prefs.playWarningChime = playChime
        prefs.vibrateEnabled = vibrate
        _warningSeconds.value = seconds
        _playWarningChime.value = playChime
        _vibrateEnabled.value = vibrate
    }

    fun updateBedtimeSchedule(enabled: Boolean, hour: Int, minute: Int) {
        prefs.bedtimeEnabled = enabled
        prefs.bedtimeHour = hour
        prefs.bedtimeMinute = minute
        _bedtimeEnabled.value = enabled
        _bedtimeHour.value = hour
        _bedtimeMinute.value = minute
    }

    fun clearHistory() {
        prefs.clearHistory()
        _historyList.value = emptyList()
    }
}

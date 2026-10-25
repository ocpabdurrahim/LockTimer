package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.TabLockPreferences
import com.example.data.TimerState
import com.example.lock.LockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TabLockTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var toneGenerator: ToneGenerator? = null

    companion object {
        private const val TAG = "TabLockTimerService"
        const val CHANNEL_ID = "tab_lock_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_ADD_5_MIN = "com.example.service.ACTION_ADD_5_MIN"
        const val ACTION_LOCK_IMMEDIATELY = "com.example.service.ACTION_LOCK_IMMEDIATELY"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_TRIGGER_SOURCE = "extra_trigger_source"

        private val _remainingSeconds = MutableStateFlow(0)
        val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

        private val _totalSeconds = MutableStateFlow(0)
        val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

        private val _timerState = MutableStateFlow(TimerState.IDLE)
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

        fun startTimer(context: Context, durationSeconds: Int, source: String = "Countdown Timer") {
            val intent = Intent(context, TabLockTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_TRIGGER_SOURCE, source)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, TabLockTimerService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resumeTimer(context: Context) {
            val intent = Intent(context, TabLockTimerService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, TabLockTimerService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun add5Minutes(context: Context) {
            val intent = Intent(context, TabLockTimerService::class.java).apply { action = ACTION_ADD_5_MIN }
            context.startService(intent)
        }

        fun lockImmediately(context: Context) {
            val intent = Intent(context, TabLockTimerService::class.java).apply { action = ACTION_LOCK_IMMEDIATELY }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed", e)
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TabLock:TimerWakeLock")
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // Safe limit
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, 900)
                val source = intent.getStringExtra(EXTRA_TRIGGER_SOURCE) ?: "Timer"
                handleStart(duration, source)
            }
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
            ACTION_ADD_5_MIN -> handleAddMinutes(5)
            ACTION_LOCK_IMMEDIATELY -> handleLockImmediately()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(durationSeconds: Int, source: String) {
        _totalSeconds.value = durationSeconds
        _remainingSeconds.value = durationSeconds
        _timerState.value = TimerState.RUNNING

        startForegroundWithNotification(buildNotification(durationSeconds, isWarning = false))
        startCountdown(source)
    }

    private fun handlePause() {
        if (_timerState.value == TimerState.RUNNING) {
            _timerState.value = TimerState.PAUSED
            timerJob?.cancel()
            updateNotification(_remainingSeconds.value, isWarning = false)
        }
    }

    private fun handleResume() {
        if (_timerState.value == TimerState.PAUSED && _remainingSeconds.value > 0) {
            _timerState.value = TimerState.RUNNING
            startCountdown("Timer")
        }
    }

    private fun handleStop() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value = 0
        _totalSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleAddMinutes(minutes: Int) {
        val extraSec = minutes * 60
        _remainingSeconds.value += extraSec
        _totalSeconds.value += extraSec
        updateNotification(_remainingSeconds.value, isWarning = false)
    }

    private fun handleLockImmediately() {
        timerJob?.cancel()
        _timerState.value = TimerState.LOCKED
        val totalMins = _totalSeconds.value / 60
        LockController.lockNow(this, "Manual Immediate Lock", totalMins)
        handleStop()
    }

    private fun startCountdown(source: String) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val prefs = TabLockPreferences(this@TabLockTimerService)
            var warningPlayed = false

            while (_remainingSeconds.value > 0 && _timerState.value == TimerState.RUNNING) {
                delay(1000L)
                _remainingSeconds.value -= 1
                val rem = _remainingSeconds.value

                // Check warning threshold
                val isWarning = rem <= prefs.warningSeconds && rem > 0
                if (isWarning && !warningPlayed && prefs.playWarningChime) {
                    warningPlayed = true
                    playChime()
                }

                updateNotification(rem, isWarning)
            }

            if (_remainingSeconds.value <= 0 && _timerState.value == TimerState.RUNNING) {
                _timerState.value = TimerState.LOCKED
                val totalMins = (_totalSeconds.value / 60).coerceAtLeast(1)
                LockController.lockNow(this@TabLockTimerService, source, totalMins)
                handleStop()
            }
        }
    }

    private fun playChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        } catch (e: Exception) {
            Log.w(TAG, "Tone playback error", e)
        }
    }

    private fun startForegroundWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(secondsRemaining: Int, isWarning: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(secondsRemaining, isWarning))
    }

    private fun buildNotification(secondsRemaining: Int, isWarning: Boolean): Notification {
        val hours = secondsRemaining / 3600
        val mins = (secondsRemaining % 3600) / 60
        val secs = secondsRemaining % 60
        val timeFormatted = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val add5Intent = Intent(this, TabLockTimerService::class.java).apply { action = ACTION_ADD_5_MIN }
        val add5PendingIntent = PendingIntent.getService(
            this, 1, add5Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lockNowIntent = Intent(this, TabLockTimerService::class.java).apply { action = ACTION_LOCK_IMMEDIATELY }
        val lockNowPendingIntent = PendingIntent.getService(
            this, 2, lockNowIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TabLockTimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isWarning) "⚠️ TabLock: Screen locking soon!" else "🔒 TabLock Active"
        val text = if (_timerState.value == TimerState.PAUSED) {
            "Paused at $timeFormatted — Default password to unlock"
        } else {
            "Honor Tab will lock in $timeFormatted"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(!isWarning)
            .setPriority(if (isWarning) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .addAction(R.mipmap.ic_launcher, "+5 Min", add5PendingIntent)
            .addAction(R.mipmap.ic_launcher, "Lock Now", lockNowPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Cancel", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TabLock Timer Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining countdown before tablet screen locks"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // Ignore
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

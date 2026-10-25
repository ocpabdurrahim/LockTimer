package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.TimerState
import com.example.receiver.TabLockDeviceAdminReceiver
import com.example.ui.MainViewModel
import com.example.ui.components.TimerDial

@Composable
fun TimerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsState()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val selectedDuration by viewModel.selectedDurationSeconds.collectAsState()
    val isDeviceAdminActive by viewModel.isDeviceAdminActive.collectAsState()

    // Activity result launcher for Device Admin activation
    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshAdminStatus()
    }

    // Permission launcher for Android 13+ Notifications
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAdminStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentDisplaySeconds = if (timerState == TimerState.IDLE) selectedDuration else remainingSeconds
    val currentTotal = if (timerState == TimerState.IDLE) selectedDuration else totalSeconds

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth > 650.dp

        if (isTabletLandscape) {
            // Tablet Landscape Two-Column Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Timer Dial
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TimerDial(
                        secondsRemaining = currentDisplaySeconds,
                        totalSeconds = currentTotal,
                        timerState = timerState,
                        dialSize = 300.dp
                    )
                }

                // Right Column: Controls & Settings
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AdminStatusBanner(
                        isActive = isDeviceAdminActive,
                        onActivateClick = {
                            val intent = TabLockDeviceAdminReceiver.getAddDeviceAdminIntent(context)
                            adminLauncher.launch(intent)
                        }
                    )

                    TimerControlPanel(
                        timerState = timerState,
                        selectedDuration = selectedDuration,
                        onStart = { viewModel.startTimer("Countdown Timer") },
                        onPause = { viewModel.pauseTimer() },
                        onResume = { viewModel.resumeTimer() },
                        onStop = { viewModel.stopTimer() },
                        onAdd5Min = { viewModel.add5MinutesToActiveTimer() },
                        onLockNow = { viewModel.lockImmediately() },
                        onSetDuration = { viewModel.setDuration(it) },
                        onAddDuration = { viewModel.addDuration(it) },
                        onTest5s = { viewModel.testLockIn5Seconds() }
                    )
                }
            }
        } else {
            // Phone / Tablet Portrait Stacked Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AdminStatusBanner(
                    isActive = isDeviceAdminActive,
                    onActivateClick = {
                        val intent = TabLockDeviceAdminReceiver.getAddDeviceAdminIntent(context)
                        adminLauncher.launch(intent)
                    }
                )

                TimerDial(
                    secondsRemaining = currentDisplaySeconds,
                    totalSeconds = currentTotal,
                    timerState = timerState,
                    dialSize = 260.dp
                )

                TimerControlPanel(
                    timerState = timerState,
                    selectedDuration = selectedDuration,
                    onStart = { viewModel.startTimer("Countdown Timer") },
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onStop = { viewModel.stopTimer() },
                    onAdd5Min = { viewModel.add5MinutesToActiveTimer() },
                    onLockNow = { viewModel.lockImmediately() },
                    onSetDuration = { viewModel.setDuration(it) },
                    onAddDuration = { viewModel.addDuration(it) },
                    onTest5s = { viewModel.testLockIn5Seconds() }
                )
            }
        }
    }
}

@Composable
private fun AdminStatusBanner(
    isActive: Boolean,
    onActivateClick: () -> Unit
) {
    if (isActive) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF064E3B).copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "System Screen Lock Enabled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Device turns off & locks at 0:00. Use default password to unlock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_banner")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enable Honor Tab Screen Lock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Grant Device Admin permission so TabLock can automatically turn off and lock the tablet screen. You will use your default tablet password to unlock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                ElevatedButton(
                    onClick = onActivateClick,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_admin_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Screen Lock Permission", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TimerControlPanel(
    timerState: TimerState,
    selectedDuration: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onAdd5Min: () -> Unit,
    onLockNow: () -> Unit,
    onSetDuration: (Int) -> Unit,
    onAddDuration: (Int) -> Unit,
    onTest5s: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (timerState == TimerState.IDLE) {
            // Quick Preset Chips
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF94A3B8)
            )

            val presets = listOf(
                "5 min" to 5 * 60,
                "15 min" to 15 * 60,
                "30 min" to 30 * 60,
                "45 min" to 45 * 60,
                "1 hr" to 60 * 60,
                "2 hrs" to 120 * 60
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((label, seconds) in presets) {
                    val isSelected = selectedDuration == seconds
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSetDuration(seconds) }
                            .testTag("preset_$label")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Time Adjusters (-5m, +5m, +15m, +30m)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAddDuration(-300) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_minus_5")
                ) {
                    Text("-5m", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onAddDuration(300) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_plus_5")
                ) {
                    Text("+5m", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onAddDuration(900) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_plus_15")
                ) {
                    Text("+15m", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onAddDuration(1800) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_plus_30")
                ) {
                    Text("+30m", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            }

            // Primary Start Button
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF0B192C)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_timer_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "START AUTO-LOCK TIMER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            // RUNNING or PAUSED controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (timerState == TimerState.RUNNING) {
                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("resume_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onAdd5Min,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("add_5_min_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+5 Min", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("cancel_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Secondary Instant Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedButton(
                onClick = onLockNow,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("lock_now_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock Tablet Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onTest5s,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("test_5s_button")
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test 5s Lock", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

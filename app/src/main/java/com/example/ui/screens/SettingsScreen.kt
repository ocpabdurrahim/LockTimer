package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnlockMode
import com.example.receiver.TabLockDeviceAdminReceiver
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inAppPin by viewModel.inAppPin.collectAsState()
    val unlockMode by viewModel.unlockMode.collectAsState()
    val warningSeconds by viewModel.warningSeconds.collectAsState()
    val playWarningChime by viewModel.playWarningChime.collectAsState()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsState()
    val isDeviceAdminActive by viewModel.isDeviceAdminActive.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshAdminStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Unlock Mode
        Text(
            text = "UNLOCK METHOD",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            letterSpacing = 1.5.sp
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Option 1: System Default Password
                UnlockModeRow(
                    title = "System Default Password (Recommended)",
                    subtitle = "Turns off & securely locks your Honor Tab. Enter your tablet's default password/PIN/pattern to unlock.",
                    isSelected = unlockMode == UnlockMode.SYSTEM_DEFAULT_PASSWORD,
                    onClick = { viewModel.updateUnlockMode(UnlockMode.SYSTEM_DEFAULT_PASSWORD) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Hybrid Both
                UnlockModeRow(
                    title = "Hybrid Mode (System Lock + In-App PIN)",
                    subtitle = "Locks the device with system password, and also requires the in-app PIN for double security.",
                    isSelected = unlockMode == UnlockMode.HYBRID_BOTH,
                    onClick = { viewModel.updateUnlockMode(UnlockMode.HYBRID_BOTH) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: In-App PIN Only
                UnlockModeRow(
                    title = "In-App Lock Screen Only",
                    subtitle = "Displays full-screen PIN lock over tablet apps without powering off screen.",
                    isSelected = unlockMode == UnlockMode.IN_APP_PIN,
                    onClick = { viewModel.updateUnlockMode(UnlockMode.IN_APP_PIN) }
                )
            }
        }

        // Section: In-App PIN Management
        Text(
            text = "DEFAULT IN-APP PASSWORD",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            letterSpacing = 1.5.sp
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("In-App Password", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Current PIN: $inAppPin", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                    }

                    Button(
                        onClick = {
                            newPinInput = inAppPin
                            pinError = null
                            showPinDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("change_pin_button")
                    ) {
                        Text("Change PIN")
                    }
                }

                if (inAppPin != "1234") {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            viewModel.updateInAppPin("1234")
                            Toast.makeText(context, "Reset to default password: 1234", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reset to default (1234)", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                }
            }
        }

        // Section: Warnings & Notifications
        Text(
            text = "ALERTS & FEEDBACK",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            letterSpacing = 1.5.sp
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Audio Warning Chime
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Warning Chime", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("Plays a gentle chime 60 seconds before locking", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                    }
                    Switch(
                        checked = playWarningChime,
                        onCheckedChange = { viewModel.updateWarningSettings(warningSeconds, it, vibrateEnabled) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                    )
                }

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Vibration", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("Vibrate tablet when screen lock triggers", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                    }
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = { viewModel.updateWarningSettings(warningSeconds, playWarningChime, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                    )
                }
            }
        }

        // Section: Device Administrator
        Text(
            text = "SYSTEM PERMISSIONS",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            letterSpacing = 1.5.sp
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isDeviceAdminActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Device Admin Screen Lock", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(
                                text = if (isDeviceAdminActive) "Active (Ready to lock)" else "Permission needed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDeviceAdminActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = TabLockDeviceAdminReceiver.getAddDeviceAdminIntent(context)
                            adminLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isDeviceAdminActive) "Manage" else "Grant", color = Color(0xFF38BDF8))
                    }
                }
            }
        }
    }

    // PIN Change Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set In-App Password", color = Color.White) },
            text = {
                Column {
                    Text(
                        "Enter a 4 to 8-digit numeric PIN for in-app unlocking. (Default is 1234)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 8) {
                                newPinInput = input
                                pinError = null
                            }
                        },
                        label = { Text("New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError != null,
                        supportingText = {
                            pinError?.let { Text(it, color = Color(0xFFEF4444)) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits"
                        } else {
                            viewModel.updateInAppPin(newPinInput)
                            Toast.makeText(context, "In-App PIN updated", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun UnlockModeRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), lineHeight = 16.sp)
        }
    }
}

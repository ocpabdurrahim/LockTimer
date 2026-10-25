package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import java.util.Calendar

@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val bedtimeEnabled by viewModel.bedtimeEnabled.collectAsState()
    val bedtimeHour by viewModel.bedtimeHour.collectAsState()
    val bedtimeMinute by viewModel.bedtimeMinute.collectAsState()

    var selectedHour by remember(bedtimeHour) { mutableStateOf(bedtimeHour) }
    var selectedMinute by remember(bedtimeMinute) { mutableStateOf(bedtimeMinute) }

    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

    // Calculate time until next schedule
    val timeUntilSchedule = remember(bedtimeEnabled, selectedHour, selectedMinute) {
        if (!bedtimeEnabled) return@remember null
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val diffMillis = target.timeInMillis - now.timeInMillis
        val diffHours = diffMillis / (1000 * 3600)
        val diffMinutes = (diffMillis % (1000 * 3600)) / (1000 * 60)
        "${diffHours}h ${diffMinutes}m"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Schedule Master Toggle Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Daily Bedtime Auto-Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (bedtimeEnabled) "Active at $formattedTime every day" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (bedtimeEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                }

                Switch(
                    checked = bedtimeEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.updateBedtimeSchedule(isChecked, selectedHour, selectedMinute)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7)
                    ),
                    modifier = Modifier.testTag("bedtime_switch")
                )
            }
        }

        // Time Picker / Clock Dial Display
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111827)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SCHEDULED LOCK TIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Big Clock Display
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    color = if (bedtimeEnabled) Color(0xFF38BDF8) else Color(0xFF64748B)
                )

                if (timeUntilSchedule != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "Locks in $timeUntilSchedule • Default password to unlock",
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Hour Buttons
                Text(
                    text = "Adjust Hour",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickHours = listOf(20, 21, 22, 23)
                    for (h in quickHours) {
                        val isSel = selectedHour == h
                        OutlinedButton(
                            onClick = {
                                selectedHour = h
                                viewModel.updateBedtimeSchedule(bedtimeEnabled, h, selectedMinute)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${h}:00",
                                color = if (isSel) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fine Adjust Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedHour = (selectedHour - 1 + 24) % 24
                            viewModel.updateBedtimeSchedule(bedtimeEnabled, selectedHour, selectedMinute)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-1 Hour", color = Color(0xFF38BDF8))
                    }

                    OutlinedButton(
                        onClick = {
                            selectedHour = (selectedHour + 1) % 24
                            viewModel.updateBedtimeSchedule(bedtimeEnabled, selectedHour, selectedMinute)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1 Hour", color = Color(0xFF38BDF8))
                    }

                    OutlinedButton(
                        onClick = {
                            selectedMinute = (selectedMinute + 15) % 60
                            viewModel.updateBedtimeSchedule(bedtimeEnabled, selectedHour, selectedMinute)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+15 Min", color = Color(0xFF38BDF8))
                    }
                }
            }
        }

        // Information Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "When the bedtime schedule triggers, your Honor Tab will be automatically locked. Next morning or whenever you return, simply turn on the screen and enter your tablet's default password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

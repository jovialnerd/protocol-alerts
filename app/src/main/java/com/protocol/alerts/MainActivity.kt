package com.protocol.alerts

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.protocol.alerts.model.AlertEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val NAVY = Color(0xFF183A53)
private val TEAL = Color(0xFF1E88A8)
private val BG = Color(0xFFEEF6F8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = TEAL, surface = Color.White)) {
                Surface(color = BG, modifier = Modifier.fillMaxSize()) { ProtocolApp() }
            }
        }
    }
}

private fun cycle(type: String): String = when (type.uppercase()) {
    "TRAINING" -> "MATCH"; "MATCH" -> "REST"; else -> "TRAINING"
}

@Composable
private fun ProtocolApp() {
    val context = LocalContext.current
    val prefs = remember { PrefsStore(context) }
    val schedule = remember { ScheduleRepository.load(context) }

    var refresh by remember { mutableStateOf(0) }              // bump to recompute
    var master by remember { mutableStateOf(prefs.masterEnabled) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    // Re-plan whenever settings change.
    LaunchedEffect(refresh, master) { DailyPlanner.planToday(context) }

    val (todayType, events) = remember(refresh, master) { DailyPlanner.todaysEvents(context) }
    val notifsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val exactOn = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarmsCompat()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Protocol Alerts", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NAVY)
        Text("Today: ${dayName(LocalDate.now().dayOfWeek)} · $todayType",
            fontSize = 15.sp, color = TEAL, fontWeight = FontWeight.SemiBold)

        // ---- Setup / permissions ----
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Setup")
                StatusRow("Notifications", notifsOn) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else openAppSettings(context)
                }
                StatusRow("Exact alarms", exactOn) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + context.packageName)))
                }
                OutlinedButton(onClick = { requestIgnoreBattery(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Disable battery optimization (recommended)")
                }
                Text("OEMs like Samsung/Xiaomi may still delay alarms — exclude this app in the system battery settings.",
                    fontSize = 11.sp, color = Color.Gray)
            }
        }

        // ---- Master switch + test ----
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Alerts enabled", Modifier.weight(1f), fontSize = 15.sp, color = NAVY)
                    Switch(checked = master, onCheckedChange = {
                        master = it; prefs.masterEnabled = it; refresh++
                    })
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    NotificationChannels.ensure(context)
                    AlarmReceiver.post(context, "test",
                        "Test alert", "If you can see this, notifications are working.",
                        NotificationChannels.channelFor(todayType))
                }, modifier = Modifier.fillMaxWidth()) { Text("Send test notification") }
            }
        }

        // ---- Today's plan ----
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Today's plan — ${events.size} alerts")
                if (events.isEmpty()) Text("No events for this day type.", color = Color.Gray)
                events.forEach { EventRow(it) }
            }
        }

        // ---- Weekly schedule ----
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Weekly schedule (tap to change)")
                DayOfWeek.values().forEach { dow ->
                    val key = dow.name
                    val def = schedule.weekdayDefault[key] ?: "REST"
                    val current = prefs.dayTypeFor(key, def)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(dayName(dow), Modifier.weight(1f), fontSize = 14.sp, color = NAVY)
                        AssistChip(onClick = {
                            prefs.setDayType(key, cycle(current)); refresh++
                        }, label = { Text(current) })
                    }
                }
            }
        }

        Text("Content sourced from MetabolicIntelligence_Dashboard_v4. Educational use only.",
            fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun SectionTitle(t: String) =
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TEAL)

@Composable private fun StatusRow(label: String, ok: Boolean, onFix: () -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text((if (ok) "✓ " else "✗ ") + label, Modifier.weight(1f),
            fontSize = 14.sp, color = if (ok) Color(0xFF2E9E6B) else Color(0xFFC2503F))
        if (!ok) TextButton(onClick = onFix) { Text("Grant") }
    }
}

@Composable private fun EventRow(e: AlertEvent) {
    Column {
        Text("${e.time}${if (e.leadMinutes > 0) " (−${e.leadMinutes}m)" else ""}  ·  ${e.title}",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NAVY)
        if (e.body.isNotBlank())
            Text(e.body, fontSize = 12.sp, color = Color(0xFF45565E))
        Spacer(Modifier.height(4.dp))
    }
}

private fun dayName(d: DayOfWeek) = d.getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun openAppSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun requestIgnoreBattery(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + context.packageName)))
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

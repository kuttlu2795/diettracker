package com.dietary.tracker.ui.screens.water

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.theme.BlueWater

@Composable
fun WaterReminderScreen(viewModel: WaterReminderViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled implicitly */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Water Reminders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Based on your weight, height and activity level, your recommended daily water intake is:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))

        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = BlueWater, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "${state.dailyGoalMl} ml / day",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Switch(
                checked = state.settings.enabled,
                onCheckedChange = { checked ->
                    viewModel.updateSettings { it.copy(enabled = checked) }
                    viewModel.applySchedule(context)
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Cup size (ml)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(150, 200, 250, 300).forEach { size ->
                FilterChip(
                    selected = state.settings.cupSizeMl == size,
                    onClick = {
                        viewModel.updateSettings { it.copy(cupSizeMl = size) }
                        viewModel.applySchedule(context)
                    },
                    label = { Text("${size}ml") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Wake-up time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        TimeStepper(
            hour = state.settings.wakeHour,
            minute = state.settings.wakeMinute,
            onChange = { h, m ->
                viewModel.updateSettings { it.copy(wakeHour = h, wakeMinute = m) }
                viewModel.applySchedule(context)
            }
        )

        Spacer(Modifier.height(16.dp))
        Text("Sleep time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        TimeStepper(
            hour = state.settings.sleepHour,
            minute = state.settings.sleepMinute,
            onChange = { h, m ->
                viewModel.updateSettings { it.copy(sleepHour = h, sleepMinute = m) }
                viewModel.applySchedule(context)
            }
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Today's reminder schedule (${state.slotTimes.size} reminders)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.slotTimes) { time ->
                AssistChip(onClick = {}, label = { Text(time) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Reminders are spread evenly between your wake and sleep times so each notification " +
                "represents one cup, adding up to your full daily water goal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TimeStepper(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = {
            val newHour = (hour - 1 + 24) % 24
            onChange(newHour, minute)
        }) { Text("-1h") }
        Spacer(Modifier.width(12.dp))
        Text(
            "%02d:%02d".format(hour, minute),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = {
            val newHour = (hour + 1) % 24
            onChange(newHour, minute)
        }) { Text("+1h") }
    }
}

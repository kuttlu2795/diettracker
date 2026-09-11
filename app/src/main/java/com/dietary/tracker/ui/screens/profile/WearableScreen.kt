package com.dietary.tracker.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.dietary.tracker.health.HealthConnectManager
import kotlinx.coroutines.launch

@Composable
fun WearableScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf<Long?>(null) }
    var weight by remember { mutableStateOf<Double?>(null) }
    var heart by remember { mutableStateOf<Long?>(null) }
    var bloodPressure by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var oxygen by remember { mutableStateOf<Double?>(null) }
    var sleep by remember { mutableStateOf<Double?>(null) }
    var message by remember { mutableStateOf("Connect Health Connect to import wearable/health-app data") }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        connected = granted.containsAll(manager.permissions)
        if (connected) message = "Connected — tap Sync now to import the latest data."
    }

    fun sync() {
        scope.launch {
            if (!manager.hasAllPermissions()) {
                permissionLauncher.launch(manager.permissions)
                return@launch
            }
            connected = true
            steps = manager.todaySteps()
            weight = manager.latestWeightKg()
            heart = manager.latestHeartRate()
            bloodPressure = manager.latestBloodPressure()
            oxygen = manager.latestOxygenSaturation()
            sleep = manager.latestSleepHours()
            message = "Last sync: now"
        }
    }

    LaunchedEffect(Unit) { sync() }

    Scaffold(topBar = { TopAppBar(title = { Text("Wearables & Health Sync") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Health Connect", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Use Android Health Connect as the hub for compatible watch/health apps. MyFitnessPal follows the same hub approach for steps, weight, workouts, sleep and other health data.", style = MaterialTheme.typography.bodyMedium)
            Card { Column(Modifier.padding(16.dp)) { Text(if (connected) "● Connected" else "○ Not connected", fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(message, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(10.dp)); Button(onClick = { sync() }) { Text(if (connected) "Sync now" else "Connect Health Connect") } } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Steps", steps?.toString() ?: "—", Modifier.weight(1f))
                StatCard("Heart rate", heart?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Weight", weight?.let { "%.1f kg".format(it) } ?: "—", Modifier.weight(1f))
                StatCard("Sleep", sleep?.let { "%.1f h".format(it) } ?: "—", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("SpO₂", oxygen?.let { "%.0f%%".format(it) } ?: "—", Modifier.weight(1f))
                StatCard("Blood pressure", bloodPressure?.let { "%.0f/%.0f".format(it.first, it.second) } ?: "—", Modifier.weight(1f))
            }
            Divider()
            Text("GOBOULT / Boult watch profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Boult watches pair through the Boult Track / GOBoult app. The app is designed so Health Connect is the preferred bridge when the watch companion exposes its health records there; this avoids pretending the watch has a generic direct BLE nutrition API.", style = MaterialTheme.typography.bodySmall)
            WatchCard("Crown R Pro 2", "1.43\" AMOLED • BT 5.2 • 120+ sports modes • HR • SpO₂ • sleep • activity • water reminder • sedentary reminder • BP • breath training • IP67")
            WatchCard("Drift+", "1.85\" HD / 500 nits • BT 5.1 calling • 100+ sports modes • HR • SpO₂ • activity • water reminder • sedentary reminder • IP68 • smart notifications")
            WatchCard("Rover Ultra", "1.43\" AMOLED • 600 nits • BT 5.2 calling • 120+ sports modes • HR • SpO₂ • sleep • activity • breath training • IP67")
            Text("Stress: Health Connect does not provide a standard stress record. If a future GOBOULT companion integration exposes stress, it can be mapped here without inventing values.", style = MaterialTheme.typography.labelSmall)
            Text("Health readings from consumer wearables are for wellness tracking and should not be treated as medical measurements.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }
}

@Composable private fun WatchCard(name: String, details: String) {
    Card { Column(Modifier.padding(16.dp)) { Text(name, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(details, style = MaterialTheme.typography.bodySmall) } }
}

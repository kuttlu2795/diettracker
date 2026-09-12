package com.dietary.tracker.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.theme.GreenPrimary

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        if (saved) {
            snackbarHostState.showSnackbar("Profile saved")
            viewModel.clearSavedFlag()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Profile & Goals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = profile.heightCm.toString(),
                onValueChange = { v -> v.toFloatOrNull()?.let { h -> viewModel.update { it.copy(heightCm = h) } } },
                label = { Text("Height (cm)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = profile.currentWeightKg.toString(),
                onValueChange = { v -> v.toFloatOrNull()?.let { w -> viewModel.update { it.copy(currentWeightKg = w) } } },
                label = { Text("Current Weight (kg)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = profile.goalWeightKg.toString(),
                onValueChange = { v -> v.toFloatOrNull()?.let { w -> viewModel.update { it.copy(goalWeightKg = w) } } },
                label = { Text("Goal Weight (kg)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lose", "Maintain", "Gain").forEach { g ->
                    FilterChip(
                        selected = profile.goalType == g,
                        onClick = { viewModel.update { it.copy(goalType = g) } },
                        label = { Text("$g weight") }
                    )
                }
            }
            if (profile.goalType != "Maintain") {
                Spacer(Modifier.height(10.dp))
                Text(
                    "How fast do you want to ${profile.goalType.lowercase()} weight?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { rate ->
                        FilterChip(
                            selected = profile.goalRateKgPerWeek == rate,
                            onClick = { viewModel.update { it.copy(goalRateKgPerWeek = rate) } },
                            label = { Text("${rate} kg/wk") }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "A safe, sustainable pace is generally 0.25-1 kg per week.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = profile.age.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { a -> viewModel.update { it.copy(age = a) } } },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Gender", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Male", "Female").forEach { g ->
                    FilterChip(
                        selected = profile.gender == g,
                        onClick = { viewModel.update { it.copy(gender = g) } },
                        label = { Text(g) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Activity Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column {
                listOf("Sedentary", "Light", "Moderate", "Active", "Very Active").forEach { level ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = profile.activityLevel == level,
                            onClick = { viewModel.update { it.copy(activityLevel = level) } }
                        )
                        Text(level)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Daily Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = profile.dailyCalorieGoal.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { c -> viewModel.update { it.copy(dailyCalorieGoal = c) } } },
                label = { Text("Calorie goal (kcal)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = profile.dailyProteinGoalG.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { p -> viewModel.update { it.copy(dailyProteinGoalG = p) } } },
                label = { Text("Protein goal (g)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = profile.dailyWaterGoalMl.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { w -> viewModel.update { it.copy(dailyWaterGoalMl = w) } } },
                label = { Text("Water goal (ml)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = profile.dailyStepGoal.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { s -> viewModel.update { it.copy(dailyStepGoal = s) } } },
                label = { Text("Step goal") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Save Profile")
            }

            Spacer(Modifier.height(32.dp))
            Text("Wearable Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            WearableSyncCard(viewModel)

            Spacer(Modifier.height(28.dp))
            Text("Community & Social", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔥 Streaks & badges are live now - check the Home tab for your logging streak and earned badges.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "👥 Friends, a social feed, and community challenges need a backend server and user " +
                            "accounts to work across devices - they're not built here on purpose, since this app " +
                            "keeps all your data on-device with no account required. If you'd like real multi-user " +
                            "social features, that's a separate project (a server + accounts) rather than an " +
                            "in-app toggle - let me know if you want that scoped out.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Your Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.exportCsv(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Data (CSV)")
            }
            Text(
                "Exports your full food, water, and weight log as a CSV file you can save or share.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun WearableSyncCard(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val wearableState by viewModel.wearable.collectAsState()
    val manager = remember { com.dietary.tracker.wearable.HealthConnectManager(context) }

    val permissionLauncher = rememberLauncherForActivityResult(manager.requestPermissionsContract()) {
        viewModel.syncWearable(context)
    }

    LaunchedEffect(Unit) { viewModel.checkWearableAvailability(context) }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Reads steps, weight, and active calories that your wearable's own app (boAt " +
                    "Wearables/ProGear, Fitbit, Galaxy Wearable, Mi Fitness, etc.) has written to " +
                    "Android's Health Connect - the shared health-data hub built into Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = androidx.compose.ui.graphics.Color(0xFFFFF7E6),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Note on boAt watches: your data only appears here if the boAt Wearables/ProGear " +
                        "app on your phone has Health Connect sync turned on. If you don't see that option " +
                        "in the boAt app yet, this app's own phone step sensor keeps working regardless " +
                        "(see the Home tab).",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color(0xFF8A6D00),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.height(12.dp))

            var showWatchSpecs by remember { mutableStateOf(false) }
            TextButton(onClick = { showWatchSpecs = !showWatchSpecs }) {
                Text(if (showWatchSpecs) "Hide supported watch specs" else "See supported watch brands & specs")
            }
            if (showWatchSpecs) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {

                    Text("boAt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.padding(top = 4.dp))
                    listOf(
                        Triple("boAt Wave Sigma 3", "2.01\" HD display, 700+ sports modes, BT calling, up to 10 days battery"),
                        Triple("boAt Ultima Connect Max", "1.96\" AMOLED, 100+ sports modes, built-in Alexa, BT calling"),
                        Triple("boAt Lunar Discovery Pro", "1.39\" display, heart rate + SpO2, BT calling, up to 7 days battery"),
                        Triple("boAt Enigma X600", "1.43\" AMOLED, metal build, BT calling, up to 7 days battery"),
                        Triple("boAt Wave Fortune", "1.96\" display (240x282), heart rate monitor, up to 7 days battery")
                    ).forEach { (name, specs) ->
                        Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        Text(specs, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    }
                    Text(
                        "Syncs via the boAt Wearables/ProGear app. Reaches this screen only if/when boAt adds " +
                            "Health Connect support to that app - not something this app controls.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("GoBoult (Boult)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    listOf(
                        Triple("GoBoult Trail Pro", "2.01\" 3D curved AMOLED, 600 nits, BT 5.3 calling, heart rate/SpO2/BP/sleep/stress, 123+ sports modes, IP68, up to 7 days battery"),
                        Triple("GoBoult Trail", "2.01\" curved HD display, BT 5.3 calling, heart rate/SpO2/BP/sleep, 120+ sports modes, IP68")
                    ).forEach { (name, specs) ->
                        Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        Text(specs, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    }
                    Text(
                        "Syncs via the Boult Fit / GOBOULT Fit app. As of this build, that app's listed sync path is " +
                            "Bluetooth only - no Health Connect support found. Same situation as boAt: this app reads " +
                            "real Health Connect data generically (no watch-specific code), so Trail Pro data would " +
                            "start appearing here automatically the moment Boult adds Health Connect support to their " +
                            "app - no update to this app would be needed. Until then, log your weight/steps manually " +
                            "here, or check the Boult Fit app's own settings for a Health Connect / Google Fit toggle " +
                            "in case one was added after this was written.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        "This app doesn't hardcode support for any specific watch brand - it reads whatever Android's " +
                            "Health Connect has, from any wearable app that writes to it (Fitbit, Galaxy Wearable, Mi " +
                            "Fitness, and others already do). The two brands above are listed because they were asked " +
                            "about specifically, not because they're special-cased in the code.",
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            when (wearableState.availability) {
                com.dietary.tracker.wearable.HealthConnectAvailability.NOT_SUPPORTED,
                com.dietary.tracker.wearable.HealthConnectAvailability.NOT_INSTALLED -> {
                    Text(
                        "Health Connect isn't installed on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                com.dietary.tracker.wearable.HealthConnectAvailability.AVAILABLE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { permissionLauncher.launch(manager.permissions) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Connect") }
                        Button(
                            onClick = { viewModel.syncWearable(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) { Text("Sync Now") }
                    }
                }
            }

            if (wearableState.status == WearableSyncStatus.SYNCING) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            wearableState.lastMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (wearableState.status == WearableSyncStatus.ERROR) MaterialTheme.colorScheme.error else GreenPrimary
                )
            }
        }
    }
}

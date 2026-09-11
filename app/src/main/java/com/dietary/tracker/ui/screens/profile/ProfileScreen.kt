package com.dietary.tracker.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.theme.GreenPrimary

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onWearablesClick: () -> Unit = {}, onRecipesClick: () -> Unit = {}, onChandraClick: () -> Unit = {}) {
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
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onWearablesClick, modifier = Modifier.fillMaxWidth()) { Text("⌚ Wearables & Health Connect") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRecipesClick, modifier = Modifier.fillMaxWidth()) { Text("🍽 My Meals / Recipe Builder") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onChandraClick, modifier = Modifier.fillMaxWidth()) { Text("🎙️ Chandra Voice Assistant") }
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
            Spacer(Modifier.height(40.dp))
        }
    }
}

package com.dietary.tracker.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.components.ProgressRing
import com.dietary.tracker.ui.components.StatCard
import com.dietary.tracker.ui.theme.BlueWater
import com.dietary.tracker.ui.theme.GreenPrimary
import com.dietary.tracker.ui.theme.OrangeAccent
import com.dietary.tracker.ui.theme.PurpleProtein
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddFoodClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFoodClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Food") },
                containerColor = GreenPrimary,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Today's Summary", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProgressRing(
                    progress = (state.caloriesIntake / state.caloriesGoal.coerceAtLeast(1)).toFloat(),
                    color = GreenPrimary,
                    label = "Calories",
                    valueText = "${state.caloriesIntake.roundToInt()}"
                )
                ProgressRing(
                    progress = (state.steps.toFloat() / state.stepGoal.coerceAtLeast(1)),
                    color = OrangeAccent,
                    label = "Steps",
                    valueText = "${state.steps}"
                )
                ProgressRing(
                    progress = (state.waterMl.toFloat() / state.waterGoalMl.coerceAtLeast(1)),
                    color = BlueWater,
                    label = "Water",
                    valueText = "${state.waterMl}ml"
                )
                ProgressRing(
                    progress = (state.proteinIntake / state.proteinGoal.coerceAtLeast(1)).toFloat(),
                    color = PurpleProtein,
                    label = "Protein",
                    valueText = "${state.proteinIntake.roundToInt()}g"
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Energy Balance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Calories In",
                    value = "${state.caloriesIntake.roundToInt()} kcal",
                    accentColor = GreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Calories Out (BMR+Steps)",
                    value = "${(state.tdee + state.caloriesBurntFromSteps).roundToInt()} kcal",
                    accentColor = OrangeAccent,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                val changeText = if (state.estimatedWeightChangeKg >= 0)
                    "+${"%.2f".format(state.estimatedWeightChangeKg)} kg"
                else
                    "${"%.2f".format(state.estimatedWeightChangeKg)} kg"
                StatCard(
                    title = "Net Balance",
                    value = "${state.netCalorieBalance.roundToInt()} kcal",
                    subtitle = if (state.netCalorieBalance >= 0) "Surplus" else "Deficit",
                    accentColor = if (state.netCalorieBalance >= 0) OrangeAccent else GreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Est. Weight Change Today",
                    value = changeText,
                    accentColor = PurpleProtein,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Quick Add Water", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(100, 250, 500).forEach { amount ->
                    OutlinedButton(onClick = { viewModel.addWater(amount) }) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = BlueWater)
                        Spacer(Modifier.width(6.dp))
                        Text("+${amount}ml")
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

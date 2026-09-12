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
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onAddFoodClick: () -> Unit,
    onChandraClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onChandraClick,
                    containerColor = OrangeAccent,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Chandra voice assistant")
                }
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = onAddFoodClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Food") },
                    containerColor = GreenPrimary,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            }
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

            Spacer(Modifier.height(24.dp))
            Text("Micronutrients Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "% of general adult daily reference intake",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    MicroRow("Vitamin C", state.micronutrients.vitaminCMg, com.dietary.tracker.util.NutrientRDA.VITAMIN_C_MG, "mg")
                    MicroRow("Calcium", state.micronutrients.calciumMg, com.dietary.tracker.util.NutrientRDA.CALCIUM_MG, "mg")
                    MicroRow("Iron", state.micronutrients.ironMg, com.dietary.tracker.util.NutrientRDA.IRON_MG, "mg")
                    MicroRow("Potassium", state.micronutrients.potassiumMg, com.dietary.tracker.util.NutrientRDA.POTASSIUM_MG, "mg")
                    MicroRow("Vitamin D", state.micronutrients.vitaminDMcg, com.dietary.tracker.util.NutrientRDA.VITAMIN_D_MCG, "mcg")
                    MicroRow("Vitamin B12", state.micronutrients.vitaminB12Mcg, com.dietary.tracker.util.NutrientRDA.VITAMIN_B12_MCG, "mcg", last = true)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("🔥 Streak & Badges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${state.streak.currentStreakDays}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Current streak (days)", style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            Text("${state.streak.longestStreakDays}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Longest streak (days)", style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            Text("${state.streak.totalEntries}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Total entries", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.streak.badges) { badge ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(78.dp)
                            ) {
                                Text(
                                    badge.emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier.graphicsLayer(alpha = if (badge.earned) 1f else 0.25f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    badge.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = if (badge.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MicroRow(label: String, value: Double, rda: Double, unit: String, last: Boolean = false) {
    val pct = ((value / rda) * 100).coerceIn(0.0, 999.0)
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${value.roundToInt()}$unit (${pct.roundToInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (pct / 100.0).coerceIn(0.0, 1.0).toFloat(),
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = GreenPrimary,
            trackColor = GreenPrimary.copy(alpha = 0.15f)
        )
    }
    if (!last) Divider(modifier = Modifier.padding(top = 4.dp))
}

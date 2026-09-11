package com.dietary.tracker.ui.screens.bmi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.components.AnimatedLineChart
import com.dietary.tracker.ui.components.LinePoint
import com.dietary.tracker.util.RangeType

@Composable
fun BmiScreen(viewModel: BmiViewModel) {
    val state by viewModel.uiState.collectAsState()
    var weightInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("BMI & Nutrition Plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("%.1f".format(state.bmi), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(state.bmiCategory, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Height: ${state.profile.heightCm.toInt()} cm  •  Weight: ${state.profile.currentWeightKg} kg",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Log today's weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("Weight (kg)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Button(onClick = {
                weightInput.toFloatOrNull()?.let {
                    viewModel.logWeight(it)
                    weightInput = ""
                }
            }) {
                Text("Log")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Weight Trend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(RangeType.WEEK, RangeType.MONTH, RangeType.YEAR)) { type ->
                FilterChip(
                    selected = state.rangeType == type,
                    onClick = { viewModel.setRange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        AnimatedLineChart(
            points = state.weightHistory.map { LinePoint(it.label, it.weightKg) }
        )

        Spacer(Modifier.height(28.dp))
        Text("Your Daily Nutrition Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            when (state.profile.goalType) {
                "Lose" -> "Goal: lose weight at ~${state.profile.goalRateKgPerWeek} kg/week" +
                    if (state.plan.weeksToGoal > 0) " • ~${state.plan.weeksToGoal} weeks to reach ${state.profile.goalWeightKg} kg" else ""
                "Gain" -> "Goal: gain weight at ~${state.profile.goalRateKgPerWeek} kg/week" +
                    if (state.plan.weeksToGoal > 0) " • ~${state.plan.weeksToGoal} weeks to reach ${state.profile.goalWeightKg} kg" else ""
                else -> "Goal: maintain current weight"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                PlanRow("Calories", "${state.plan.adjustedCalorieGoal} kcal/day")
                PlanRow("Water", "${state.plan.waterMl} ml/day (~${state.plan.waterMl / 250} glasses)")
                PlanRow("Protein", "${state.plan.proteinG} g/day")
                PlanRow("Carbohydrates", "${state.plan.carbsG} g/day")
                PlanRow("Fat", "${state.plan.fatG} g/day")
                PlanRow("Fiber", "${state.plan.fiberG} g/day")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("How to split it across the day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                TableHeaderRow(listOf("Meal", "% of day", "Calories"))
                Divider()
                state.plan.mealSplits.forEach { split ->
                    TableDataRow(listOf(split.mealType, "${split.percent}%", "${split.calories} kcal"))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's suggested meals (Veg vs Non-Veg)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.refreshMealPlan(state.plan.adjustedCalorieGoal) }) {
                Text("Refresh")
            }
        }
        Text(
            "Live suggestions from Spoonacular's meal-planning API, sized to your ${state.plan.adjustedCalorieGoal} kcal/day target.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))

        when (state.mealPlanStatus) {
            MealPlanStatus.LOADING -> {
                Card {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Fetching today's meal suggestions...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            MealPlanStatus.MISSING_KEY -> {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add a free Spoonacular API key to see live meal suggestions here.", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Get one free at spoonacular.com/food-api, then add SPOONACULAR_API_KEY=... " +
                                "to your local.properties file and rebuild.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            MealPlanStatus.ERROR -> {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Couldn't load meal suggestions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.mealPlanError ?: "Unknown error",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            MealPlanStatus.SUCCESS -> {
                val plan = state.mealPlan
                if (plan == null || (plan.vegetarian.isEmpty() && plan.standard.isEmpty())) {
                    Text("No suggestions returned - try Refresh.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Vegetarian", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            if (plan.vegetarian.isEmpty()) {
                                Text("No vegetarian suggestions returned.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                plan.vegetarian.forEach { meal -> MealSuggestionRow(meal) }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Regular (may include non-veg)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            if (plan.standard.isEmpty()) {
                                Text("No suggestions returned.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                plan.standard.forEach { meal -> MealSuggestionRow(meal) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MealSuggestionRow(meal: com.dietary.tracker.network.MealSuggestion) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(
                if (meal.sourceUrl != null) Modifier.clickableRow { uriHandler.openUri(meal.sourceUrl) }
                else Modifier
            )
    ) {
        Text(meal.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        val meta = listOfNotNull(
            meal.readyInMinutes?.let { "$it min" },
            meal.servings?.let { "$it serving(s)" }
        ).joinToString(" • ")
        if (meta.isNotBlank()) {
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
    Divider()
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))

@Composable
private fun PlanRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TableHeaderRow(cells: List<String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        cells.forEach {
            Text(it, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TableDataRow(cells: List<String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        cells.forEach {
            Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        }
    }
    Divider()
}

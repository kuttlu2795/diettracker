package com.dietary.tracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.components.AnimatedBarChart
import com.dietary.tracker.ui.components.BarChartEntry
import com.dietary.tracker.ui.theme.GreenPrimary
import com.dietary.tracker.util.RangeType
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RangeType.entries) { type ->
                FilterChip(
                    selected = state.rangeType == type,
                    onClick = { viewModel.setRange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(state.rangeLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryChip("Calories", "${state.totalCalories.roundToInt()}")
            SummaryChip("Protein", "${state.totalProtein.roundToInt()}g")
            SummaryChip("Sugar", "${state.totalSugar.roundToInt()}g")
            SummaryChip("Fat", "${state.totalFat.roundToInt()}g")
        }

        Spacer(Modifier.height(16.dp))
        if (state.dayBreakdown.isNotEmpty()) {
            AnimatedBarChart(
                entries = state.dayBreakdown.map { BarChartEntry(it.label, it.calories.toFloat()) },
                color = GreenPrimary
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Entries (${state.entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.name) },
                    supportingContent = { Text("${entry.mealType} • ${entry.quantityLabel}") },
                    trailingContent = { Text("${entry.calories.roundToInt()} kcal") }
                )
                Divider()
            }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        "No entries in this range yet.",
                        modifier = Modifier.padding(top = 24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

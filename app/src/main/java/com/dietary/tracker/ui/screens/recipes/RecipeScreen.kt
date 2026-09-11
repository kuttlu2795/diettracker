package com.dietary.tracker.ui.screens.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.FoodEntry
import com.dietary.tracker.data.entities.Recipe
import kotlinx.coroutines.launch

@Composable
fun RecipeScreen(repository: Repository, onBack: () -> Unit) {
    val recent by repository.recentFood(30).collectAsState(initial = emptyList())
    val recipes by repository.recipes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var message by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("My Meals / Recipes") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Build a reusable meal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Pick foods you have already logged. The app totals their nutrition and saves the combination for one-tap reuse.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Meal name") }, modifier = Modifier.fillMaxWidth())
            Text("Recent foods", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            recent.distinctBy { it.name }.forEach { food ->
                val checked = selected.contains(food.id)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(food.name, fontWeight = FontWeight.Medium); Text("${food.calories.toInt()} kcal • ${food.protein.toInt()}g protein", style = MaterialTheme.typography.labelSmall) }
                    Checkbox(checked, { selected = if (it) selected + food.id else selected - food.id })
                }
            }
            Button(onClick = {
                val chosen = recent.filter { selected.contains(it.id) }
                if (name.isBlank() || chosen.isEmpty()) { message = "Enter a meal name and select at least one food."; return@Button }
                scope.launch {
                    repository.addRecipe(Recipe(name = name.trim(), calories = chosen.sumOf { it.calories }, protein = chosen.sumOf { it.protein }, carbs = chosen.sumOf { it.carbs }, sugar = chosen.sumOf { it.sugar }, fat = chosen.sumOf { it.fat }, fiber = chosen.sumOf { it.fiber }, sodium = chosen.sumOf { it.sodium }))
                    name = ""; selected = emptySet(); message = "Saved to My Meals"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save My Meal") }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            Divider()
            Text("Saved meals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            recipes.forEach { recipe ->
                Card { Column(Modifier.padding(14.dp)) { Text(recipe.name, fontWeight = FontWeight.Bold); Text("${recipe.calories.toInt()} kcal • P ${recipe.protein.toInt()}g • C ${recipe.carbs.toInt()}g • F ${recipe.fat.toInt()}g", style = MaterialTheme.typography.bodySmall) } }
            }
        }
    }
}

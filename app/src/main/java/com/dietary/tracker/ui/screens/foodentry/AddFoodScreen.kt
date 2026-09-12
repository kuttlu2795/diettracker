package com.dietary.tracker.ui.screens.foodentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.data.entities.FavoriteFood
import com.dietary.tracker.data.entities.FoodEntry
import com.dietary.tracker.network.NutritionResult
import com.dietary.tracker.ui.theme.GreenPrimary
import com.dietary.tracker.ui.theme.OrangeAccent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    viewModel: AddFoodViewModel,
    onScanBarcode: () -> Unit,
    onScanPhoto: () -> Unit,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val recentFoods by viewModel.recentFoods.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.resetSaved()
            onDone()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Food", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onScanBarcode, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Scan Barcode")
            }
            OutlinedButton(onClick = onScanPhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Scan Label")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.selected != null) {
            SelectedFoodEditor(
                state = state,
                onGramsChange = viewModel::onGramsChange,
                onMealTypeChange = viewModel::onMealTypeChange,
                onMacroChange = viewModel::overrideMacro,
                onClear = { viewModel.clearSelection() },
                onSave = { viewModel.saveEntry("logged") },
                onSaveFavorite = { viewModel.saveCurrentAsFavorite() }
            )
        } else if (state.customizer == Customizer.EGG) {
            EggCustomizer(state.egg, viewModel::updateEgg, viewModel::confirmEggCustomizer, viewModel::cancelCustomizer)
        } else if (state.customizer == Customizer.JUICE) {
            JuiceCustomizer(state.juice, viewModel::updateJuice, viewModel::confirmJuiceCustomizer, viewModel::cancelCustomizer)
        } else if (state.customizer == Customizer.COFFEE) {
            CoffeeCustomizer(state.coffee, viewModel::updateCoffee, viewModel::confirmCoffeeCustomizer, viewModel::cancelCustomizer)
        } else if (state.customizer == Customizer.RICE) {
            RiceCustomizer(state.rice, viewModel::updateRice, viewModel::confirmRiceCustomizer, viewModel::cancelCustomizer)
        } else if (state.customizer == Customizer.ROTI) {
            RotiCustomizer(state.roti, viewModel::updateRoti, viewModel::confirmRotiCustomizer, viewModel::cancelCustomizer)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Type any food — e.g. \"2 idlis with sambar\"") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.search() }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
            Text(
                "Searches nutrition databases first, then AI can estimate anything it can't find.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )

            if (state.isSearching || state.aiLoading) {
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.aiLoading) "✨ Asking AI to estimate this…" else "Searching…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            if (state.showAiOffer) {
                Spacer(Modifier.height(8.dp))
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Couldn't find an exact match for \"${state.query}\"", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Let AI estimate its nutrition from the description, or enter the values yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { viewModel.enterManually() }, modifier = Modifier.weight(1f)) {
                                Text("Enter Manually")
                            }
                            Button(
                                onClick = { viewModel.askAi() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                            ) { Text("✨ Estimate with AI") }
                        }
                    }
                }
            }

            if (state.results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.results) { result -> FoodResultRow(result) { viewModel.selectResult(result) } }
                }
            } else if (!state.isSearching && !state.aiLoading && !state.showAiOffer) {
                val favorites by viewModel.favorites.collectAsState()
                if (favorites.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("⭐ Favorites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    favorites.forEach { fav ->
                        FavoriteFoodRow(fav, onClick = { viewModel.quickAddFavorite(fav) }, onDelete = { viewModel.deleteFavorite(fav) })
                    }
                }
                if (recentFoods.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(recentFoods) { entry ->
                            RecentFoodRow(entry) { viewModel.quickAddRecent(entry) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodResultRow(result: NutritionResult, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(result.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${result.caloriesPer100g.roundToInt()} kcal / 100g • ${result.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            SourceBadge(result.badge)
        }
    }
}

@Composable
private fun FavoriteFoodRow(favorite: FavoriteFood, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(favorite.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${favorite.mealType} • ${favorite.quantityLabel} • ${favorite.calories.roundToInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            IconButton(onClick = onDelete) {
                Text("✕", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun RecentFoodRow(entry: FoodEntry, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(entry.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${entry.mealType} • ${entry.quantityLabel} • ${entry.calories.roundToInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Text("Tap to add", style = MaterialTheme.typography.labelSmall, color = GreenPrimary)
        }
    }
}

@Composable
private fun SourceBadge(badge: String) {
    val (label, bg, fg) = when (badge) {
        "ai" -> Triple("AI Estimated", androidx.compose.ui.graphics.Color(0xFFFFF3E0), androidx.compose.ui.graphics.Color(0xFFE65100))
        "manual" -> Triple("Manual", androidx.compose.ui.graphics.Color(0xFFECEFF1), androidx.compose.ui.graphics.Color(0xFF37474F))
        "restaurant" -> Triple("Restaurant", androidx.compose.ui.graphics.Color(0xFFE3F2FD), androidx.compose.ui.graphics.Color(0xFF0D47A1))
        else -> Triple("Database", androidx.compose.ui.graphics.Color(0xFFE8F5E9), androidx.compose.ui.graphics.Color(0xFF1B5E20))
    }
    Surface(color = bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
        Text(label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun EggCustomizer(
    egg: EggOptions,
    onUpdate: ((EggOptions) -> EggOptions) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Text("🥚 Customize your eggs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "We'll turn these choices into a precise description and look up accurate nutrition for it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(16.dp))

        Text("How many eggs?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { onUpdate { it.copy(count = (it.count - 1).coerceAtLeast(1)) } }) { Text("-") }
            Text("${egg.count}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            OutlinedButton(onClick = { onUpdate { it.copy(count = (it.count + 1).coerceAtMost(12)) } }) { Text("+") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Yolk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = egg.withYolk, onClick = { onUpdate { it.copy(withYolk = true) } }, label = { Text("With Yolk") })
            FilterChip(selected = !egg.withYolk, onClick = { onUpdate { it.copy(withYolk = false) } }, label = { Text("Egg White Only") })
        }

        Spacer(Modifier.height(16.dp))
        Text("Salt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = egg.salt, onClick = { onUpdate { it.copy(salt = true) } }, label = { Text("Added") })
            FilterChip(selected = !egg.salt, onClick = { onUpdate { it.copy(salt = false) } }, label = { Text("None") })
        }

        Spacer(Modifier.height(16.dp))
        Text("Pepper", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = egg.pepper, onClick = { onUpdate { it.copy(pepper = true) } }, label = { Text("Added") })
            FilterChip(selected = !egg.pepper, onClick = { onUpdate { it.copy(pepper = false) } }, label = { Text("None") })
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Get Nutrition")
            }
        }
    }
}

@Composable
private fun JuiceCustomizer(
    juice: JuiceOptions,
    onUpdate: ((JuiceOptions) -> JuiceOptions) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Text("🧃 Customize your juice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "We'll turn these choices into a precise description and look up accurate nutrition for it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = juice.type,
            onValueChange = { v -> onUpdate { it.copy(type = v) } },
            label = { Text("Juice type") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Sugar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = !juice.addedSugar, onClick = { onUpdate { it.copy(addedSugar = false) } }, label = { Text("No Added Sugar") })
            FilterChip(selected = juice.addedSugar, onClick = { onUpdate { it.copy(addedSugar = true) } }, label = { Text("With Added Sugar") })
        }

        Spacer(Modifier.height(16.dp))
        Text("Serving size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(150, 250, 350).forEach { size ->
                FilterChip(selected = juice.volumeMl == size, onClick = { onUpdate { it.copy(volumeMl = size) } }, label = { Text("${size}ml") })
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Get Nutrition")
            }
        }
    }
}

@Composable
private fun CoffeeCustomizer(
    coffee: CoffeeOptions,
    onUpdate: ((CoffeeOptions) -> CoffeeOptions) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Text("☕ Customize your coffee/tea", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Milk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf("None", "Whole", "Skim", "Oat", "Soy").forEach { m ->
                FilterChip(selected = coffee.milk == m, onClick = { onUpdate { it.copy(milk = m) } }, label = { Text(m) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Sugar (teaspoons)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { onUpdate { it.copy(sugarTsp = (it.sugarTsp - 1).coerceAtLeast(0)) } }) { Text("-") }
            Text("${coffee.sugarTsp}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            OutlinedButton(onClick = { onUpdate { it.copy(sugarTsp = (it.sugarTsp + 1).coerceAtMost(6)) } }) { Text("+") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Cup size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(100, 150, 250).forEach { size ->
                FilterChip(selected = coffee.volumeMl == size, onClick = { onUpdate { it.copy(volumeMl = size) } }, label = { Text("${size}ml") })
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Get Nutrition")
            }
        }
    }
}

@Composable
private fun RiceCustomizer(
    rice: RiceOptions,
    onUpdate: ((RiceOptions) -> RiceOptions) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Text("🍚 Customize your rice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf("White", "Brown", "Basmati").forEach { t ->
                FilterChip(selected = rice.type == t, onClick = { onUpdate { it.copy(type = t) } }, label = { Text(t) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Cups (cooked)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { onUpdate { it.copy(cups = (it.cups - 1).coerceAtLeast(1)) } }) { Text("-") }
            Text("${rice.cups}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            OutlinedButton(onClick = { onUpdate { it.copy(cups = (it.cups + 1).coerceAtMost(6)) } }) { Text("+") }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Get Nutrition")
            }
        }
    }
}

@Composable
private fun RotiCustomizer(
    roti: RotiOptions,
    onUpdate: ((RotiOptions) -> RotiOptions) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Text("🫓 Customize your roti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("How many?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { onUpdate { it.copy(count = (it.count - 1).coerceAtLeast(1)) } }) { Text("-") }
            Text("${roti.count}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            OutlinedButton(onClick = { onUpdate { it.copy(count = (it.count + 1).coerceAtMost(10)) } }) { Text("+") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Ghee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = roti.ghee, onClick = { onUpdate { it.copy(ghee = true) } }, label = { Text("With Ghee") })
            FilterChip(selected = !roti.ghee, onClick = { onUpdate { it.copy(ghee = false) } }, label = { Text("Plain") })
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Get Nutrition")
            }
        }
    }
}

@Composable
private fun SelectedFoodEditor(
    state: AddFoodUiState,
    onGramsChange: (String) -> Unit,
    onMealTypeChange: (String) -> Unit,
    onMacroChange: (String, Double) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onSaveFavorite: () -> Unit
) {
    val result = state.selected!!
    val gramsValue = state.grams.toDoubleOrNull() ?: 0.0
    val scaled = result.scaled(gramsValue)
    val meals = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(result.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SourceBadge(result.badge)
        }
        Text("Source: ${result.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        if (result.badge == "ai") {
            Spacer(Modifier.height(8.dp))
            Surface(color = androidx.compose.ui.graphics.Color(0xFFFFF7E6), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) {
                Text(
                    "⚠️ AI-estimated values — please review and adjust the numbers below if they look off.",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color(0xFF8A6D00),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.grams,
            onValueChange = onGramsChange,
            label = { Text("Quantity (grams)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text("Meal type", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            meals.forEach { meal ->
                FilterChip(selected = state.mealType == meal, onClick = { onMealTypeChange(meal) }, label = { Text(meal) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Nutrition (tap any value to correct it)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                MacroField("Calories", "kcal", scaled.calories) { onMacroChange("calories", it) }
                MacroField("Protein", "g", scaled.protein) { onMacroChange("protein", it) }
                MacroField("Carbs", "g", scaled.carbs) { onMacroChange("carbs", it) }
                MacroField("Sugar", "g", scaled.sugar) { onMacroChange("sugar", it) }
                MacroField("Fat", "g", scaled.fat) { onMacroChange("fat", it) }
                MacroField("Fiber", "g", scaled.fiber) { onMacroChange("fiber", it) }
                MacroField("Sodium", "mg", scaled.sodium, last = true) { onMacroChange("sodium", it) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Back") }
            OutlinedButton(onClick = onSaveFavorite, modifier = Modifier.weight(1f)) { Text("⭐ Save Favorite") }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
            Text("Save Entry")
        }
    }
}

@Composable
private fun MacroField(label: String, unit: String, value: Double, last: Boolean = false, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(formatMacro(value)) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { v ->
                    text = v
                    v.toDoubleOrNull()?.let(onChange)
                },
                singleLine = true,
                modifier = Modifier.width(84.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
    if (!last) Divider()
}

private fun formatMacro(value: Double): String =
    if (value == value.roundToInt().toDouble()) value.roundToInt().toString() else "%.1f".format(value)

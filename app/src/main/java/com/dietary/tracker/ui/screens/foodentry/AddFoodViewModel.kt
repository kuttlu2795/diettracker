package com.dietary.tracker.ui.screens.foodentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.FavoriteFood
import com.dietary.tracker.data.entities.FoodEntry
import com.dietary.tracker.network.AiEstimateOutcome
import com.dietary.tracker.network.AiNutritionEstimator
import com.dietary.tracker.network.ApiKeys
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.network.NutritionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Customizer { NONE, EGG, JUICE, COFFEE, RICE, ROTI }

data class EggOptions(val count: Int = 2, val withYolk: Boolean = true, val salt: Boolean = false, val pepper: Boolean = false) {
    fun toDescription(): String =
        "$count egg${if (count > 1) "s" else ""}" +
            (if (withYolk) ", with yolk" else ", without yolk (egg white only)") +
            (if (salt) ", with salt" else ", no salt") +
            (if (pepper) ", with pepper" else ", no pepper")
}

data class JuiceOptions(val type: String = "Orange", val addedSugar: Boolean = false, val volumeMl: Int = 250) {
    fun toDescription(): String =
        "$volumeMl ml $type juice" + (if (addedSugar) ", with added sugar" else ", no added sugar (fresh)")
}

data class CoffeeOptions(val milk: String = "None", val sugarTsp: Int = 0, val volumeMl: Int = 150) {
    fun toDescription(): String =
        "$volumeMl ml coffee" +
            (if (milk == "None") ", no milk" else ", with $milk milk") +
            (if (sugarTsp == 0) ", no sugar" else ", $sugarTsp tsp sugar")
}

data class RiceOptions(val type: String = "White", val cups: Int = 1) {
    fun toDescription(): String = "$cups cup${if (cups > 1) "s" else ""} cooked $type rice"
}

data class RotiOptions(val count: Int = 2, val ghee: Boolean = false) {
    fun toDescription(): String =
        "$count roti${if (count > 1) "s" else ""}" + (if (ghee) ", with ghee" else ", plain (no ghee)")
}

data class AddFoodUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<NutritionResult> = emptyList(),
    val selected: NutritionResult? = null,
    val grams: String = "100",
    val mealType: String = "Snack",
    val saved: Boolean = false,
    val error: String? = null,
    val showAiOffer: Boolean = false,
    val aiLoading: Boolean = false,
    val customizer: Customizer = Customizer.NONE,
    val egg: EggOptions = EggOptions(),
    val juice: JuiceOptions = JuiceOptions(),
    val coffee: CoffeeOptions = CoffeeOptions(),
    val rice: RiceOptions = RiceOptions(),
    val roti: RotiOptions = RotiOptions()
)

class AddFoodViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val aiEstimator = AiNutritionEstimator()

    private val _state = MutableStateFlow(AddFoodUiState())
    val state: StateFlow<AddFoodUiState> = _state.asStateFlow()

    /** Recently logged foods, so the user can quickly re-add something they eat often. */
    val recentFoods: StateFlow<List<FoodEntry>> =
        repository.recentFood(10).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** User-saved favorites (MyFitnessPal-style quick add), independent of the log history. */
    val favorites: StateFlow<List<FavoriteFood>> =
        repository.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    /** Entry point from the search bar. Foods with common real-world variations (egg, juice,
     *  coffee, rice, roti) get a quick customizer first; everything else goes straight to search. */
    fun search() {
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        val lower = q.lowercase()
        when {
            Regex("\\begg(s)?\\b").containsMatchIn(lower) ->
                _state.value = _state.value.copy(customizer = Customizer.EGG)
            Regex("\\bjuice\\b").containsMatchIn(lower) -> {
                val guessedType = q.replace(Regex("juice", RegexOption.IGNORE_CASE), "").trim()
                    .replaceFirstChar { it.uppercase() }.ifBlank { "Fruit" }
                _state.value = _state.value.copy(
                    customizer = Customizer.JUICE,
                    juice = _state.value.juice.copy(type = guessedType)
                )
            }
            Regex("\\bcoffee\\b|\\btea\\b").containsMatchIn(lower) ->
                _state.value = _state.value.copy(customizer = Customizer.COFFEE)
            Regex("\\brice\\b").containsMatchIn(lower) ->
                _state.value = _state.value.copy(customizer = Customizer.RICE)
            Regex("\\brot(i|is)\\b|\\bchapat(i|is)\\b").containsMatchIn(lower) ->
                _state.value = _state.value.copy(customizer = Customizer.ROTI)
            else -> runSearch(q)
        }
    }

    fun updateEgg(transform: (EggOptions) -> EggOptions) {
        _state.value = _state.value.copy(egg = transform(_state.value.egg))
    }

    fun updateJuice(transform: (JuiceOptions) -> JuiceOptions) {
        _state.value = _state.value.copy(juice = transform(_state.value.juice))
    }

    fun updateCoffee(transform: (CoffeeOptions) -> CoffeeOptions) {
        _state.value = _state.value.copy(coffee = transform(_state.value.coffee))
    }

    fun updateRice(transform: (RiceOptions) -> RiceOptions) {
        _state.value = _state.value.copy(rice = transform(_state.value.rice))
    }

    fun updateRoti(transform: (RotiOptions) -> RotiOptions) {
        _state.value = _state.value.copy(roti = transform(_state.value.roti))
    }

    fun cancelCustomizer() {
        _state.value = _state.value.copy(customizer = Customizer.NONE)
    }

    fun confirmEggCustomizer() {
        val description = _state.value.egg.toDescription()
        _state.value = _state.value.copy(customizer = Customizer.NONE, query = description)
        runSearch(description)
    }

    fun confirmJuiceCustomizer() {
        val description = _state.value.juice.toDescription()
        _state.value = _state.value.copy(customizer = Customizer.NONE, query = description)
        runSearch(description)
    }

    fun confirmCoffeeCustomizer() {
        val description = _state.value.coffee.toDescription()
        _state.value = _state.value.copy(customizer = Customizer.NONE, query = description)
        runSearch(description)
    }

    fun confirmRiceCustomizer() {
        val description = _state.value.rice.toDescription()
        _state.value = _state.value.copy(customizer = Customizer.NONE, query = description)
        runSearch(description)
    }

    fun confirmRotiCustomizer() {
        val description = _state.value.roti.toDescription()
        _state.value = _state.value.copy(customizer = Customizer.NONE, query = description)
        runSearch(description)
    }

    /** Tries the real nutrition/restaurant databases (Open Food Facts + Edamam + Spoonacular menu
     *  items) for a query/description. If nothing comes back, offers the AI estimate / manual
     *  entry fallback instead of an error. */
    private fun runSearch(query: String) {
        _state.value = _state.value.copy(isSearching = true, error = null, showAiOffer = false, results = emptyList())
        viewModelScope.launch {
            try {
                val results = nutritionRepository.search(query)
                if (results.isEmpty()) {
                    _state.value = _state.value.copy(isSearching = false, showAiOffer = true)
                } else {
                    _state.value = _state.value.copy(isSearching = false, results = results)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSearching = false, error = "Search failed. Check your connection.")
            }
        }
    }

    /** Called when the user taps "Estimate with AI" after a database search comes up empty. */
    fun askAi() {
        val query = _state.value.query.trim()
        if (query.isBlank()) return
        _state.value = _state.value.copy(showAiOffer = false, aiLoading = true, error = null)
        viewModelScope.launch {
            when (val outcome = aiEstimator.estimate(query)) {
                is AiEstimateOutcome.Success -> {
                    _state.value = _state.value.copy(aiLoading = false, selected = outcome.result)
                }
                is AiEstimateOutcome.Error -> {
                    _state.value = _state.value.copy(aiLoading = false, error = outcome.message)
                }
                AiEstimateOutcome.MissingApiKey -> {
                    _state.value = _state.value.copy(
                        aiLoading = false,
                        error = "Add a free Gemini API key in local.properties to enable AI estimates " +
                            "for foods not found in the database (see local.properties.example)."
                    )
                }
            }
        }
    }

    /** Lets the user type in nutrition values themselves when neither the database nor AI can help. */
    fun enterManually() {
        val name = _state.value.query.trim().ifBlank { "Custom food" }
            .replaceFirstChar { it.uppercase() }
        _state.value = _state.value.copy(
            showAiOffer = false,
            selected = NutritionResult(
                name = name,
                caloriesPer100g = 0.0, proteinPer100g = 0.0, carbsPer100g = 0.0,
                sugarPer100g = 0.0, fatPer100g = 0.0, fiberPer100g = 0.0, sodiumMgPer100g = 0.0,
                source = "Manual entry", badge = "manual"
            )
        )
    }

    fun selectResult(result: NutritionResult) {
        _state.value = _state.value.copy(selected = result)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = null)
    }

    fun setResultDirectly(result: NutritionResult) {
        _state.value = _state.value.copy(selected = result, results = listOf(result))
    }

    /** Quickly re-log a food from the Recent list with its previously saved quantity/macros as-is. */
    fun quickAddRecent(entry: FoodEntry) {
        viewModelScope.launch {
            repository.addFood(entry.copy(id = 0, timestamp = System.currentTimeMillis()))
            _state.value = _state.value.copy(saved = true)
        }
    }

    /** Saves the currently selected food (at its current quantity) as a reusable Favorite. */
    fun saveCurrentAsFavorite() {
        val selected = _state.value.selected ?: return
        val grams = _state.value.grams.toDoubleOrNull() ?: 100.0
        val scaled = selected.scaled(grams)
        viewModelScope.launch {
            repository.addFavorite(
                FavoriteFood(
                    name = selected.name,
                    mealType = _state.value.mealType,
                    quantityLabel = "${grams.toInt()}g",
                    quantityGrams = grams,
                    calories = scaled.calories,
                    protein = scaled.protein,
                    carbs = scaled.carbs,
                    sugar = scaled.sugar,
                    fat = scaled.fat,
                    fiber = scaled.fiber,
                    sodium = scaled.sodium,
                    source = selected.badge
                )
            )
        }
    }

    /** Quickly logs a saved Favorite as a new food entry for right now. */
    fun quickAddFavorite(favorite: FavoriteFood) {
        viewModelScope.launch {
            repository.addFood(
                FoodEntry(
                    name = favorite.name,
                    timestamp = System.currentTimeMillis(),
                    mealType = favorite.mealType,
                    quantityLabel = favorite.quantityLabel,
                    quantityGrams = favorite.quantityGrams,
                    calories = favorite.calories,
                    protein = favorite.protein,
                    carbs = favorite.carbs,
                    sugar = favorite.sugar,
                    fat = favorite.fat,
                    fiber = favorite.fiber,
                    sodium = favorite.sodium,
                    source = "favorite"
                )
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun deleteFavorite(favorite: FavoriteFood) {
        viewModelScope.launch { repository.deleteFavorite(favorite) }
    }

    /** Looks up a scanned barcode via Open Food Facts and returns the result through the callback. */
    fun lookupBarcode(barcode: String, onResult: (NutritionResult?) -> Unit) {
        viewModelScope.launch {
            val result = try {
                nutritionRepository.byBarcode(barcode)
            } catch (e: Exception) {
                null
            }
            onResult(result)
        }
    }

    fun onGramsChange(value: String) {
        _state.value = _state.value.copy(grams = value)
    }

    fun onMealTypeChange(value: String) {
        _state.value = _state.value.copy(mealType = value)
    }

    /** Lets the editor override individual macro fields after an AI estimate or manual entry -
     *  updates the underlying per-100g result so the grams field keeps scaling correctly. */
    fun overrideMacro(field: String, absoluteValue: Double) {
        val selected = _state.value.selected ?: return
        val grams = _state.value.grams.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
        val per100g = absoluteValue / (grams / 100.0)
        val updated = when (field) {
            "calories" -> selected.copy(caloriesPer100g = per100g)
            "protein" -> selected.copy(proteinPer100g = per100g)
            "carbs" -> selected.copy(carbsPer100g = per100g)
            "sugar" -> selected.copy(sugarPer100g = per100g)
            "fat" -> selected.copy(fatPer100g = per100g)
            "fiber" -> selected.copy(fiberPer100g = per100g)
            "sodium" -> selected.copy(sodiumMgPer100g = per100g)
            else -> selected
        }
        _state.value = _state.value.copy(selected = updated)
    }

    fun saveEntry(source: String) {
        val selected = _state.value.selected ?: return
        val grams = _state.value.grams.toDoubleOrNull() ?: 100.0
        val scaled = selected.scaled(grams)

        viewModelScope.launch {
            repository.addFood(
                FoodEntry(
                    name = selected.name,
                    timestamp = System.currentTimeMillis(),
                    mealType = _state.value.mealType,
                    quantityLabel = "${grams.toInt()}g",
                    quantityGrams = grams,
                    calories = scaled.calories,
                    protein = scaled.protein,
                    carbs = scaled.carbs,
                    sugar = scaled.sugar,
                    fat = scaled.fat,
                    fiber = scaled.fiber,
                    sodium = scaled.sodium,
                    source = source,
                    vitaminCMg = scaled.vitaminC,
                    vitaminAMcg = scaled.vitaminA,
                    calciumMg = scaled.calcium,
                    ironMg = scaled.iron,
                    potassiumMg = scaled.potassium,
                    magnesiumMg = scaled.magnesium,
                    zincMg = scaled.zinc,
                    vitaminDMcg = scaled.vitaminD,
                    vitaminB12Mcg = scaled.vitaminB12,
                    folateMcg = scaled.folate
                )
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun resetSaved() {
        _state.value = _state.value.copy(
            saved = false, selected = null, results = emptyList(), query = "",
            showAiOffer = false, aiLoading = false, customizer = Customizer.NONE
        )
    }
}

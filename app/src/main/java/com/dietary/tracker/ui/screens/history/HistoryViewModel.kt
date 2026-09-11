package com.dietary.tracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.FoodEntry
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.util.RangeType
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class DayTotal(val label: String, val dateKey: String, val calories: Double, val sugar: Double, val protein: Double)

data class HistoryUiState(
    val rangeType: RangeType = RangeType.TODAY,
    val rangeLabel: String = "Today",
    val entries: List<FoodEntry> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalSugar: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val dayBreakdown: List<DayTotal> = emptyList()
)

class HistoryViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val rangeTypeFlow = MutableStateFlow(RangeType.TODAY)

    val uiState: StateFlow<HistoryUiState> = rangeTypeFlow.flatMapLatest { type ->
        val range = DateUtils.rangeFor(type)
        repository.foodEntriesBetween(range.start, range.end).map { entries ->
            HistoryUiState(
                rangeType = type,
                rangeLabel = range.label,
                entries = entries,
                totalCalories = entries.sumOf { it.calories },
                totalProtein = entries.sumOf { it.protein },
                totalSugar = entries.sumOf { it.sugar },
                totalCarbs = entries.sumOf { it.carbs },
                totalFat = entries.sumOf { it.fat },
                dayBreakdown = buildDayBreakdown(entries, type)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setRange(type: RangeType) {
        rangeTypeFlow.value = type
    }

    private fun buildDayBreakdown(entries: List<FoodEntry>, type: RangeType): List<DayTotal> {
        if (type == RangeType.TODAY || type == RangeType.YESTERDAY) {
            val total = entries.sumOf { it.calories }
            val sugar = entries.sumOf { it.sugar }
            val protein = entries.sumOf { it.protein }
            val label = if (type == RangeType.TODAY) "Today" else "Yest."
            return listOf(DayTotal(label, DateUtils.todayKey(), total, sugar, protein))
        }
        val grouped = entries.groupBy { DateUtils.dateKey(it.timestamp) }
        val fmt = SimpleDateFormat("dd MMM", Locale.US)
        return grouped.entries.sortedBy { it.key }.map { (key, list) ->
            val label = try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)
                if (d != null) fmt.format(d) else key
            } catch (e: Exception) {
                key
            }
            DayTotal(
                label = label,
                dateKey = key,
                calories = list.sumOf { it.calories },
                sugar = list.sumOf { it.sugar },
                protein = list.sumOf { it.protein }
            )
        }
    }
}

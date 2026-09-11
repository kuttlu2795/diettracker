package com.dietary.tracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.util.Calculations
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.util.RangeType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val caloriesIntake: Double = 0.0,
    val caloriesGoal: Int = 2000,
    val proteinIntake: Double = 0.0,
    val proteinGoal: Int = 60,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2500,
    val steps: Int = 0,
    val stepGoal: Int = 8000,
    val caloriesBurntFromSteps: Double = 0.0,
    val bmr: Double = 0.0,
    val tdee: Double = 0.0,
    val netCalorieBalance: Double = 0.0,
    val estimatedWeightChangeKg: Double = 0.0,
    val profile: UserProfile = UserProfile()
)

class DashboardViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeProfile(),
        foodTodayFlow(),
        waterTodayFlow(),
        repository.observeStepsForDate(DateUtils.todayKey())
    ) { profile, foodTotals, water, stepEntry ->
        val p = profile ?: UserProfile()
        val steps = stepEntry?.steps ?: 0
        val bmr = Calculations.bmr(p.currentWeightKg, p.heightCm, p.age, p.gender)
        val tdee = Calculations.tdee(bmr, p.activityLevel)
        val extraFromSteps = Calculations.caloriesFromSteps(steps, p.currentWeightKg)
        val totalBurnt = tdee + extraFromSteps
        val netBalance = foodTotals.first - totalBurnt

        DashboardUiState(
            caloriesIntake = foodTotals.first,
            caloriesGoal = p.dailyCalorieGoal,
            proteinIntake = foodTotals.second,
            proteinGoal = p.dailyProteinGoalG,
            waterMl = water,
            waterGoalMl = p.dailyWaterGoalMl,
            steps = steps,
            stepGoal = p.dailyStepGoal,
            caloriesBurntFromSteps = extraFromSteps,
            bmr = bmr,
            tdee = tdee,
            netCalorieBalance = netBalance,
            estimatedWeightChangeKg = Calculations.weightChangeKg(netBalance),
            profile = p
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun foodTodayFlow() = run {
        val range = DateUtils.rangeFor(RangeType.TODAY)
        repository.foodEntriesBetween(range.start, range.end).map { entries ->
            val cal = entries.sumOf { it.calories }
            val protein = entries.sumOf { it.protein }
            cal to protein
        }
    }

    private fun waterTodayFlow() = run {
        val range = DateUtils.rangeFor(RangeType.TODAY)
        repository.waterEntriesBetween(range.start, range.end).map { entries ->
            entries.sumOf { it.amountMl }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWater(amountMl, System.currentTimeMillis())
        }
    }
}

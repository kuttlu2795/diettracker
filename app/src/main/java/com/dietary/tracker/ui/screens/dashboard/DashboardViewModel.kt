package com.dietary.tracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.util.Calculations
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.util.RangeType
import com.dietary.tracker.util.StreakCalculator
import com.dietary.tracker.util.StreakInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MicronutrientTotals(
    val vitaminCMg: Double = 0.0,
    val vitaminAMcg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val magnesiumMg: Double = 0.0,
    val zincMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    val folateMcg: Double = 0.0
)

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
    val profile: UserProfile = UserProfile(),
    val micronutrients: MicronutrientTotals = MicronutrientTotals(),
    val streak: StreakInfo = StreakInfo(0, 0, 0, 0, emptyList())
)

class DashboardViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeProfile(),
        foodTodayFlow(),
        waterTodayFlow(),
        repository.observeStepsForDate(DateUtils.todayKey()),
        repository.observeAllFood()
    ) { profile, foodTotals, water, stepEntry, allFood ->
        val p = profile ?: UserProfile()
        val steps = stepEntry?.steps ?: 0
        val bmr = Calculations.bmr(p.currentWeightKg, p.heightCm, p.age, p.gender)
        val tdee = Calculations.tdee(bmr, p.activityLevel)
        val extraFromSteps = Calculations.caloriesFromSteps(steps, p.currentWeightKg)
        val totalBurnt = tdee + extraFromSteps
        val netBalance = foodTotals.calories - totalBurnt

        DashboardUiState(
            caloriesIntake = foodTotals.calories,
            caloriesGoal = p.dailyCalorieGoal,
            proteinIntake = foodTotals.protein,
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
            profile = p,
            micronutrients = foodTotals.micronutrients,
            streak = StreakCalculator.compute(allFood)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private data class FoodTotals(val calories: Double, val protein: Double, val micronutrients: MicronutrientTotals)

    private fun foodTodayFlow() = run {
        val range = DateUtils.rangeFor(RangeType.TODAY)
        repository.foodEntriesBetween(range.start, range.end).map { entries ->
            FoodTotals(
                calories = entries.sumOf { it.calories },
                protein = entries.sumOf { it.protein },
                micronutrients = MicronutrientTotals(
                    vitaminCMg = entries.sumOf { it.vitaminCMg },
                    vitaminAMcg = entries.sumOf { it.vitaminAMcg },
                    calciumMg = entries.sumOf { it.calciumMg },
                    ironMg = entries.sumOf { it.ironMg },
                    potassiumMg = entries.sumOf { it.potassiumMg },
                    magnesiumMg = entries.sumOf { it.magnesiumMg },
                    zincMg = entries.sumOf { it.zincMg },
                    vitaminDMcg = entries.sumOf { it.vitaminDMcg },
                    vitaminB12Mcg = entries.sumOf { it.vitaminB12Mcg },
                    folateMcg = entries.sumOf { it.folateMcg }
                )
            )
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

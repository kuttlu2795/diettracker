package com.dietary.tracker.ui.screens.bmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.network.DailyMealPlanResult
import com.dietary.tracker.network.MealPlanOutcome
import com.dietary.tracker.network.MealPlanRepository
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.util.Calculations
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.util.NutritionPlan
import com.dietary.tracker.util.NutritionPlanner
import com.dietary.tracker.util.RangeType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WeightPoint(val label: String, val weightKg: Float)

enum class MealPlanStatus { LOADING, SUCCESS, ERROR, MISSING_KEY }

private data class CoreState(
    val profile: UserProfile,
    val bmi: Double,
    val plan: NutritionPlan,
    val weightHistory: List<WeightPoint>,
    val rangeType: RangeType
)

data class BmiUiState(
    val profile: UserProfile = UserProfile(),
    val bmi: Double = 0.0,
    val bmiCategory: String = "-",
    val weightHistory: List<WeightPoint> = emptyList(),
    val rangeType: RangeType = RangeType.MONTH,
    val plan: NutritionPlan = NutritionPlanner.buildPlan(UserProfile()),
    val mealPlanStatus: MealPlanStatus = MealPlanStatus.LOADING,
    val mealPlanError: String? = null,
    val mealPlan: DailyMealPlanResult? = null
)

class BmiViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val mealPlanRepository = MealPlanRepository()
    private val rangeTypeFlow = MutableStateFlow(RangeType.MONTH)
    private val mealPlanStatusFlow = MutableStateFlow(MealPlanStatus.LOADING)
    private val mealPlanErrorFlow = MutableStateFlow<String?>(null)
    private val mealPlanFlow = MutableStateFlow<DailyMealPlanResult?>(null)

    private var lastFetchedCalorieTarget: Int? = null

    private val coreFlow: Flow<CoreState> = combine(
        repository.observeProfile(),
        rangeTypeFlow
    ) { profile, rangeType -> profile to rangeType }
        .flatMapLatest { (profile, rangeType) ->
            val p = profile ?: UserProfile()
            val range = DateUtils.rangeFor(rangeType)
            repository.weightEntriesBetween(range.start, range.end).map { entries ->
                val bmi = Calculations.bmi(p.currentWeightKg, p.heightCm)
                val plan = NutritionPlanner.buildPlan(p)
                maybeFetchMealPlan(plan.adjustedCalorieGoal)
                CoreState(
                    profile = p,
                    bmi = bmi,
                    plan = plan,
                    weightHistory = entries.map { WeightPoint(DateUtils.dayLabel(it.timestamp), it.weightKg) },
                    rangeType = rangeType
                )
            }
        }

    val uiState: StateFlow<BmiUiState> = combine(
        coreFlow, mealPlanStatusFlow, mealPlanErrorFlow, mealPlanFlow
    ) { core, status, error, mealPlan ->
        BmiUiState(
            profile = core.profile,
            bmi = core.bmi,
            bmiCategory = Calculations.bmiCategory(core.bmi),
            weightHistory = core.weightHistory,
            rangeType = core.rangeType,
            plan = core.plan,
            mealPlanStatus = status,
            mealPlanError = error,
            mealPlan = mealPlan
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BmiUiState())

    private fun maybeFetchMealPlan(targetCalories: Int) {
        if (lastFetchedCalorieTarget == targetCalories) return
        lastFetchedCalorieTarget = targetCalories
        refreshMealPlan(targetCalories)
    }

    fun refreshMealPlan(targetCalories: Int = lastFetchedCalorieTarget ?: 2000) {
        mealPlanStatusFlow.value = MealPlanStatus.LOADING
        viewModelScope.launch {
            when (val outcome = mealPlanRepository.fetchDailyPlan(targetCalories)) {
                is MealPlanOutcome.Success -> {
                    mealPlanFlow.value = outcome.plan
                    mealPlanStatusFlow.value = MealPlanStatus.SUCCESS
                    mealPlanErrorFlow.value = null
                }
                is MealPlanOutcome.Error -> {
                    mealPlanStatusFlow.value = MealPlanStatus.ERROR
                    mealPlanErrorFlow.value = outcome.message
                }
                MealPlanOutcome.MissingApiKey -> {
                    mealPlanStatusFlow.value = MealPlanStatus.MISSING_KEY
                }
            }
        }
    }

    fun setRange(type: RangeType) {
        rangeTypeFlow.value = type
    }

    fun logWeight(weightKg: Float) {
        viewModelScope.launch {
            repository.addWeight(weightKg, System.currentTimeMillis())
            val profile = repository.getProfile()
            repository.saveProfile(profile.copy(currentWeightKg = weightKg))
        }
    }
}

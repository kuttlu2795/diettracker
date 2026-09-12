package com.dietary.tracker.chandra

import android.content.Context
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.ExerciseEntry
import com.dietary.tracker.data.entities.FoodEntry
import com.dietary.tracker.network.AiEstimateOutcome
import com.dietary.tracker.network.AiNutritionEstimator
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.network.NutritionResult
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.util.ExerciseCalculator
import com.dietary.tracker.util.RangeType
import com.dietary.tracker.wearable.HealthConnectAvailability
import com.dietary.tracker.wearable.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * The single place that turns a [ChandraParsedCommand] into a real app action and a spoken
 * response. Every branch here calls the exact same [Repository] / [HealthConnectManager] /
 * [NutritionRepository] functions the on-screen UI uses - Chandra never has its own copy of
 * business logic, and never invents a value. Where data genuinely isn't available, it says so
 * (per the "no fake data" requirement) instead of guessing.
 */
class ChandraVoiceRepository(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) {
    private val aiEstimator = AiNutritionEstimator()

    // Typical single-unit weights, used only to size a voice-logged portion when the user says
    // a count ("2 eggs") rather than a gram amount. The actual nutrition per 100g still comes
    // from the real Open Food Facts / Edamam / AI lookup below - this table never supplies
    // nutrition values, only a plausible serving size.
    private val unitGrams = mapOf(
        "egg" to 50.0, "eggs" to 50.0,
        "banana" to 120.0, "bananas" to 120.0,
        "idli" to 40.0, "idlis" to 40.0,
        "roti" to 40.0, "rotis" to 40.0, "chapati" to 40.0,
        "apple" to 180.0, "apples" to 180.0,
        "orange" to 150.0, "oranges" to 150.0
    )

    suspend fun execute(command: ChandraParsedCommand, context: Context): ChandraResult {
        val action = command.actionId
            ?: return ChandraResult("Sorry, I didn't understand that. Please try again.")

        return try {
            when (action) {
                ChandraActionId.HEART_RATE -> heartRate(context)
                ChandraActionId.STEPS -> steps()
                ChandraActionId.WEIGHT -> weight()
                ChandraActionId.SLEEP -> sleep(context)
                ChandraActionId.SPO2 -> spo2(context)
                ChandraActionId.BLOOD_PRESSURE -> bloodPressure(context)

                ChandraActionId.ADD_FOOD -> addFood(command)
                ChandraActionId.CALORIES_STATUS -> caloriesStatus()
                ChandraActionId.PROTEIN_STATUS -> proteinStatus()
                ChandraActionId.WATER_STATUS -> waterStatus()
                ChandraActionId.ADD_WATER -> addWater(command)
                ChandraActionId.OPEN_DIARY -> ChandraResult("Here's your food diary.", navigateTo = "history")

                ChandraActionId.ADD_EXERCISE -> addExercise(command)

                ChandraActionId.START_FASTING -> startFasting()
                ChandraActionId.END_FASTING -> endFasting()
                ChandraActionId.FASTING_STATUS -> fastingStatus()

                ChandraActionId.CALORIE_GOAL -> calorieGoal(command)
                ChandraActionId.PROTEIN_GOAL -> proteinGoal(command)
                ChandraActionId.WATER_GOAL -> waterGoal(command)
                ChandraActionId.WEIGHT_GOAL -> weightGoal(command)

                ChandraActionId.DAILY_REPORT -> ChandraResult("Here's today's report.", navigateTo = "history")
                ChandraActionId.WEEKLY_REPORT -> ChandraResult("Opening your weekly report.", navigateTo = "history")
                ChandraActionId.PROGRESS -> ChandraResult("Here's your progress, including your weight trend.", navigateTo = "bmi")

                ChandraActionId.WATCH_SYNC -> watchSync(context)
            }
        } catch (e: Exception) {
            ChandraResult("Something went wrong trying to do that. Please try again.")
        }
    }

    // ---------- Health (Health Connect) ----------

    private suspend fun heartRate(context: Context): ChandraResult {
        val bpm = HealthConnectManager(context).readLatestHeartRateBpm()
        return if (bpm == null) {
            ChandraResult("I don't have your latest heart rate data.")
        } else {
            ChandraResult("Your current heart rate is $bpm beats per minute.")
        }
    }

    private suspend fun sleep(context: Context): ChandraResult {
        val hours = HealthConnectManager(context).readLatestSleepHours()
        return if (hours == null) {
            ChandraResult("I don't have your sleep data.")
        } else {
            ChandraResult("You slept $hours hours.")
        }
    }

    private suspend fun spo2(context: Context): ChandraResult {
        val pct = HealthConnectManager(context).readLatestSpO2()
        return if (pct == null) {
            ChandraResult("I don't have your oxygen level data.")
        } else {
            ChandraResult("Your oxygen level is ${pct.roundToInt()} percent.")
        }
    }

    private suspend fun bloodPressure(context: Context): ChandraResult {
        val bp = HealthConnectManager(context).readLatestBloodPressure()
        return if (bp == null) {
            ChandraResult("I don't have your blood pressure data.")
        } else {
            ChandraResult("Your blood pressure is ${bp.systolic.roundToInt()} over ${bp.diastolic.roundToInt()}.")
        }
    }

    private suspend fun watchSync(context: Context): ChandraResult {
        val manager = HealthConnectManager(context)
        if (manager.availability() != HealthConnectAvailability.AVAILABLE) {
            return ChandraResult("I couldn't sync your health data right now.")
        }
        if (!manager.hasAllPermissions()) {
            return ChandraResult("Please allow Health Connect access to read your health data.")
        }
        val result = manager.syncNow()
        var wroteAnything = false
        result.steps?.let { steps ->
            val today = DateUtils.todayKey()
            val existing = repository.getStepsForDate(today)
            val base = existing?.baseStepsAtBoot ?: 0
            repository.upsertSteps(com.dietary.tracker.data.entities.StepEntry(date = today, steps = steps.toInt(), baseStepsAtBoot = base))
            wroteAnything = true
        }
        result.weightKg?.let { w ->
            repository.addWeight(w, System.currentTimeMillis())
            val p = repository.getProfile()
            repository.saveProfile(p.copy(currentWeightKg = w))
            wroteAnything = true
        }
        return if (wroteAnything) {
            ChandraResult("Health data synced successfully.")
        } else {
            ChandraResult("I couldn't sync your health data right now.")
        }
    }

    // ---------- Steps / Weight (already tracked outside Health Connect too) ----------

    private suspend fun steps(): ChandraResult {
        val entry = repository.getStepsForDate(DateUtils.todayKey())
            ?: return ChandraResult("I don't have your step count yet today.")
        return ChandraResult("You have walked ${entry.steps} steps today.")
    }

    private suspend fun weight(): ChandraResult {
        val profile = repository.getProfile()
        return ChandraResult("Your current weight is ${profile.currentWeightKg} kilograms.")
    }

    // ---------- Nutrition status ----------

    private suspend fun caloriesStatus(): ChandraResult {
        val range = DateUtils.rangeFor(RangeType.TODAY)
        val entries = repository.foodEntriesBetween(range.start, range.end).firstValue()
        val eaten = entries.sumOf { it.calories }
        val goal = repository.getProfile().dailyCalorieGoal
        val remaining = (goal - eaten).roundToInt()
        return ChandraResult(
            "You've had ${eaten.roundToInt()} calories today, out of your $goal calorie goal. " +
                if (remaining >= 0) "$remaining calories remaining." else "That's ${-remaining} calories over your goal."
        )
    }

    private suspend fun proteinStatus(): ChandraResult {
        val range = DateUtils.rangeFor(RangeType.TODAY)
        val entries = repository.foodEntriesBetween(range.start, range.end).firstValue()
        val eaten = entries.sumOf { it.protein }
        val goal = repository.getProfile().dailyProteinGoalG
        return ChandraResult("You've had ${eaten.roundToInt()} grams of protein today, out of your $goal gram goal.")
    }

    private suspend fun waterStatus(): ChandraResult {
        val range = DateUtils.rangeFor(RangeType.TODAY)
        val entries = repository.waterEntriesBetween(range.start, range.end).firstValue()
        val drunk = entries.sumOf { it.amountMl }
        val goal = repository.getProfile().dailyWaterGoalMl
        val remaining = goal - drunk
        return ChandraResult(
            "You've had $drunk milliliters of water today, out of your $goal milliliter goal. " +
                if (remaining > 0) "$remaining milliliters remaining." else "You've reached your water goal!"
        )
    }

    private suspend fun addWater(command: ChandraParsedCommand): ChandraResult {
        val ml = (command.quantity ?: 250.0).roundToInt()
        repository.addWater(ml, System.currentTimeMillis())
        val range = DateUtils.rangeFor(RangeType.TODAY)
        val totalToday = repository.waterEntriesBetween(range.start, range.end).firstValue().sumOf { it.amountMl }
        return ChandraResult("Added ${ml}ml of water. You've now had ${totalToday}ml today.")
    }

    // ---------- Food ----------

    private fun pickBestResult(results: List<NutritionResult>): NutritionResult? =
        results.firstOrNull { it.badge == "db" } ?: results.firstOrNull { it.badge == "restaurant" } ?: results.firstOrNull()

    private fun inferMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 11 -> "Breakfast"
            hour < 16 -> "Lunch"
            hour < 20 -> "Dinner"
            else -> "Snack"
        }
    }

    private suspend fun addFood(command: ChandraParsedCommand): ChandraResult {
        val subject = command.subject?.trim()
        if (subject.isNullOrBlank()) {
            return ChandraResult("Please tell me what food to add, like 'add 2 eggs'.")
        }

        var result = pickBestResult(nutritionRepository.search(subject))
        if (result == null) {
            when (val outcome = aiEstimator.estimate(subject)) {
                is AiEstimateOutcome.Success -> result = outcome.result
                else -> { /* fall through to the not-found response below */ }
            }
        }
        if (result == null) {
            return ChandraResult("I couldn't find nutrition information for $subject. Please add it manually in the app.")
        }

        val count = command.quantity ?: 1.0
        val firstWord = subject.substringBefore(" ").lowercase()
        val perUnitGrams = unitGrams[firstWord]
        val grams = if (perUnitGrams != null) perUnitGrams * count else 100.0 * count

        val scaled = result.scaled(grams)
        repository.addFood(
            FoodEntry(
                name = result.name,
                timestamp = System.currentTimeMillis(),
                mealType = inferMealType(),
                quantityLabel = "${grams.roundToInt()}g",
                quantityGrams = grams,
                calories = scaled.calories,
                protein = scaled.protein,
                carbs = scaled.carbs,
                sugar = scaled.sugar,
                fat = scaled.fat,
                fiber = scaled.fiber,
                sodium = scaled.sodium,
                source = "voice",
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
        return ChandraResult("Added $subject to your diary. That's about ${scaled.calories.roundToInt()} calories.")
    }

    // ---------- Exercise ----------

    private suspend fun addExercise(command: ChandraParsedCommand): ChandraResult {
        val minutes = command.durationMinutes
            ?: return ChandraResult("Please tell me how long, like '30 minutes walking'.")
        val exerciseName = (command.subject ?: "exercise").trim().ifBlank { "exercise" }
            .replaceFirstChar { it.uppercase() }
        val weightKg = repository.getProfile().currentWeightKg
        val calories = ExerciseCalculator.caloriesBurned(exerciseName, minutes, weightKg)
        repository.addExercise(
            ExerciseEntry(
                name = exerciseName,
                durationMinutes = minutes,
                caloriesBurned = calories,
                timestamp = System.currentTimeMillis(),
                source = "voice"
            )
        )
        return ChandraResult("Added $minutes minutes of $exerciseName. That's about ${calories.roundToInt()} calories burned.")
    }

    // ---------- Fasting ----------

    private suspend fun startFasting(): ChandraResult {
        val active = repository.getActiveFasting()
        if (active != null) {
            return ChandraResult("You already have an active fast running.")
        }
        repository.startFasting(targetHours = 16)
        return ChandraResult("Started your fast. Target: 16 hours.")
    }

    private suspend fun endFasting(): ChandraResult {
        val active = repository.getActiveFasting()
            ?: return ChandraResult("You don't have an active fast right now.")
        repository.stopActiveFasting()
        val hours = (System.currentTimeMillis() - active.startTimestamp) / 3_600_000.0
        return ChandraResult("Ended your fast. You fasted for ${"%.1f".format(hours)} hours.")
    }

    private suspend fun fastingStatus(): ChandraResult {
        val active = repository.getActiveFasting()
            ?: return ChandraResult("You don't have an active fast right now.")
        val elapsedHours = (System.currentTimeMillis() - active.startTimestamp) / 3_600_000.0
        val remainingHours = active.targetHours - elapsedHours
        return if (remainingHours <= 0) {
            ChandraResult("You've reached your fasting target of ${active.targetHours} hours! You've fasted for ${"%.1f".format(elapsedHours)} hours.")
        } else {
            ChandraResult(
                "You've been fasting for ${"%.1f".format(elapsedHours)} hours. " +
                    "${"%.1f".format(remainingHours)} hours remaining until your ${active.targetHours}-hour target."
            )
        }
    }

    // ---------- Goals ----------

    private suspend fun calorieGoal(command: ChandraParsedCommand): ChandraResult {
        val profile = repository.getProfile()
        val newValue = command.quantity?.roundToInt()
        return if (newValue != null) {
            repository.saveProfile(profile.copy(dailyCalorieGoal = newValue))
            ChandraResult("Your calorie goal is now set to $newValue calories per day.")
        } else {
            ChandraResult("Your calorie goal is ${profile.dailyCalorieGoal} calories per day.")
        }
    }

    private suspend fun proteinGoal(command: ChandraParsedCommand): ChandraResult {
        val profile = repository.getProfile()
        val newValue = command.quantity?.roundToInt()
        return if (newValue != null) {
            repository.saveProfile(profile.copy(dailyProteinGoalG = newValue))
            ChandraResult("Your protein goal is now set to $newValue grams per day.")
        } else {
            ChandraResult("Your protein goal is ${profile.dailyProteinGoalG} grams per day.")
        }
    }

    private suspend fun waterGoal(command: ChandraParsedCommand): ChandraResult {
        val profile = repository.getProfile()
        val newValue = command.quantity?.roundToInt()
        return if (newValue != null) {
            repository.saveProfile(profile.copy(dailyWaterGoalMl = newValue))
            ChandraResult("Your water goal is now set to $newValue milliliters per day.")
        } else {
            ChandraResult("Your water goal is ${profile.dailyWaterGoalMl} milliliters per day.")
        }
    }

    private suspend fun weightGoal(command: ChandraParsedCommand): ChandraResult {
        val profile = repository.getProfile()
        val newValue = command.quantity?.toFloat()
        return if (newValue != null) {
            repository.saveProfile(profile.copy(goalWeightKg = newValue))
            ChandraResult("Your goal weight is now set to $newValue kilograms.")
        } else {
            ChandraResult("Your goal weight is ${profile.goalWeightKg} kilograms.")
        }
    }

    /** Small helper: takes the first emission of a Flow without a full collector at the call site. */
    private suspend fun <T> Flow<T>.firstValue(): T = first()
}

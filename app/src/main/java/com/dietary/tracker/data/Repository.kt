package com.dietary.tracker.data

import com.dietary.tracker.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Single access point the ViewModels talk to. Keeps Room details out of the UI layer.
 */
class Repository(private val db: AppDatabase) {

    // ---------- Food ----------
    fun foodEntriesBetween(start: Long, end: Long): Flow<List<FoodEntry>> =
        db.foodDao().getEntriesBetween(start, end)

    fun recentFood(limit: Int = 20): Flow<List<FoodEntry>> = db.foodDao().getRecent(limit)

    suspend fun addFood(entry: FoodEntry): Long = db.foodDao().insert(entry)

    suspend fun deleteFood(entry: FoodEntry) = db.foodDao().delete(entry)

    suspend fun searchPastFood(query: String) = db.foodDao().searchPastEntries(query)

    // ---------- Water ----------
    fun waterEntriesBetween(start: Long, end: Long): Flow<List<WaterEntry>> =
        db.waterDao().getEntriesBetween(start, end)

    suspend fun addWater(amountMl: Int, timestamp: Long) =
        db.waterDao().insert(WaterEntry(timestamp = timestamp, amountMl = amountMl))

    suspend fun deleteWater(entry: WaterEntry) = db.waterDao().delete(entry)

    // ---------- Weight ----------
    fun weightEntriesBetween(start: Long, end: Long): Flow<List<WeightEntry>> =
        db.weightDao().getEntriesBetween(start, end)

    fun latestWeight(): Flow<WeightEntry?> = db.weightDao().getLatest()

    suspend fun addWeight(weightKg: Float, timestamp: Long) =
        db.weightDao().insert(WeightEntry(timestamp = timestamp, weightKg = weightKg))

    // ---------- Steps ----------
    suspend fun getStepsForDate(date: String): StepEntry? = db.stepDao().getForDate(date)

    fun observeStepsForDate(date: String): Flow<StepEntry?> = db.stepDao().observeForDate(date)

    fun stepsBetween(start: String, end: String): Flow<List<StepEntry>> =
        db.stepDao().getEntriesBetween(start, end)

    suspend fun upsertSteps(entry: StepEntry) = db.stepDao().upsert(entry)

    // ---------- Profile ----------
    fun observeProfile(): Flow<UserProfile?> = db.userProfileDao().observeProfile()

    suspend fun getProfile(): UserProfile =
        db.userProfileDao().getProfile() ?: UserProfile().also { db.userProfileDao().upsert(it) }

    suspend fun saveProfile(profile: UserProfile) = db.userProfileDao().upsert(profile)

    // ---------- Water Reminder ----------
    fun observeWaterReminder(): Flow<WaterReminderSettings?> = db.waterReminderDao().observe()

    suspend fun getWaterReminder(): WaterReminderSettings =
        db.waterReminderDao().get() ?: WaterReminderSettings().also { db.waterReminderDao().upsert(it) }

    suspend fun saveWaterReminder(settings: WaterReminderSettings) = db.waterReminderDao().upsert(settings)

    // ---------- Recipes / My Meals ----------
    fun recipes(): Flow<List<Recipe>> = db.recipeDao().observeAll()
    suspend fun addRecipe(recipe: Recipe): Long = db.recipeDao().insert(recipe)
    suspend fun deleteRecipe(recipe: Recipe) = db.recipeDao().delete(recipe)
}

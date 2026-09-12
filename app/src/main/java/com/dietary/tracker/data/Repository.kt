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

    fun observeAllFood(): Flow<List<FoodEntry>> = db.foodDao().observeAll()

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

    // ---------- Favorites ----------
    fun observeFavorites(): Flow<List<FavoriteFood>> = db.favoriteFoodDao().observeAll()

    suspend fun addFavorite(favorite: FavoriteFood) = db.favoriteFoodDao().insert(favorite)

    suspend fun deleteFavorite(favorite: FavoriteFood) = db.favoriteFoodDao().delete(favorite)

    // ---------- One-shot reads for CSV export ----------
    suspend fun getAllFoodOnce(): List<FoodEntry> = db.foodDao().getAllOnce()

    suspend fun getAllWaterOnce(): List<WaterEntry> = db.waterDao().getAllOnce()

    suspend fun getAllWeightOnce(): List<WeightEntry> = db.weightDao().getAllOnce()

    // ---------- Exercise ----------
    suspend fun addExercise(entry: ExerciseEntry): Long = db.exerciseDao().insert(entry)

    fun exerciseEntriesBetween(start: Long, end: Long): Flow<List<ExerciseEntry>> =
        db.exerciseDao().getEntriesBetween(start, end)

    suspend fun totalExerciseCaloriesBetween(start: Long, end: Long): Double =
        db.exerciseDao().totalCaloriesBetween(start, end)

    // ---------- Fasting ----------
    suspend fun startFasting(targetHours: Int = 16): Long =
        db.fastingDao().insert(FastingSession(startTimestamp = System.currentTimeMillis(), targetHours = targetHours))

    suspend fun stopActiveFasting(): Boolean {
        val active = db.fastingDao().getActive() ?: return false
        db.fastingDao().update(active.copy(endTimestamp = System.currentTimeMillis()))
        return true
    }

    suspend fun getActiveFasting(): FastingSession? = db.fastingDao().getActive()

    fun observeActiveFasting(): Flow<FastingSession?> = db.fastingDao().observeActive()

    fun observeRecentFasting(): Flow<List<FastingSession>> = db.fastingDao().observeRecent()

    // ---------- Chandra ----------
    fun observeChandraCustomCommands(): Flow<List<ChandraCustomCommand>> = db.chandraCustomCommandDao().observeAll()

    suspend fun getAllChandraCustomCommandsOnce(): List<ChandraCustomCommand> = db.chandraCustomCommandDao().getAllOnce()

    suspend fun addChandraCustomCommand(command: ChandraCustomCommand): Long = db.chandraCustomCommandDao().insert(command)

    suspend fun deleteChandraCustomCommand(command: ChandraCustomCommand) = db.chandraCustomCommandDao().delete(command)

    fun observeChandraRecentCommands(): Flow<List<ChandraRecentCommand>> = db.chandraRecentCommandDao().observeRecent()

    suspend fun addChandraRecentCommand(entry: ChandraRecentCommand) = db.chandraRecentCommandDao().insert(entry)

    fun observeChandraSettings(): Flow<ChandraSettings?> = db.chandraSettingsDao().observe()

    suspend fun getChandraSettings(): ChandraSettings =
        db.chandraSettingsDao().get() ?: ChandraSettings().also { db.chandraSettingsDao().upsert(it) }

    suspend fun saveChandraSettings(settings: ChandraSettings) = db.chandraSettingsDao().upsert(settings)
}

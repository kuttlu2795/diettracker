package com.dietary.tracker.data.dao

import androidx.room.*
import com.dietary.tracker.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("SELECT * FROM food_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getEntriesBetween(start: Long, end: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE name LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 10")
    suspend fun searchPastEntries(query: String): List<FoodEntry>

    @Query("SELECT * FROM food_entries ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<FoodEntry>

    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<FoodEntry>>
}

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterEntry): Long

    @Delete
    suspend fun delete(entry: WaterEntry)

    @Query("SELECT * FROM water_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getEntriesBetween(start: Long, end: Long): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water_entries ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<WaterEntry>
}

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(entry: WeightEntry): Long

    @Delete
    suspend fun delete(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getEntriesBetween(start: Long, end: Long): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<WeightEntry>
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): StepEntry?

    @Query("SELECT * FROM step_entries WHERE date = :date LIMIT 1")
    fun observeForDate(date: String): Flow<StepEntry?>

    @Query("SELECT * FROM step_entries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getEntriesBetween(start: String, end: String): Flow<List<StepEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: StepEntry)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)
}

@Dao
interface WaterReminderDao {
    @Query("SELECT * FROM water_reminder_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<WaterReminderSettings?>

    @Query("SELECT * FROM water_reminder_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): WaterReminderSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: WaterReminderSettings)
}

@Dao
interface FavoriteFoodDao {
    @Query("SELECT * FROM favorite_foods ORDER BY name ASC")
    fun observeAll(): Flow<List<FavoriteFood>>

    @Insert
    suspend fun insert(favorite: FavoriteFood): Long

    @Delete
    suspend fun delete(favorite: FavoriteFood)
}

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(entry: ExerciseEntry): Long

    @Query("SELECT * FROM exercise_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getEntriesBetween(start: Long, end: Long): Flow<List<ExerciseEntry>>

    @Query("SELECT COALESCE(SUM(caloriesBurned),0) FROM exercise_entries WHERE timestamp BETWEEN :start AND :end")
    suspend fun totalCaloriesBetween(start: Long, end: Long): Double
}

@Dao
interface FastingDao {
    @Insert
    suspend fun insert(session: FastingSession): Long

    @Update
    suspend fun update(session: FastingSession)

    @Query("SELECT * FROM fasting_sessions WHERE endTimestamp IS NULL ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActive(): FastingSession?

    @Query("SELECT * FROM fasting_sessions WHERE endTimestamp IS NULL ORDER BY startTimestamp DESC LIMIT 1")
    fun observeActive(): Flow<FastingSession?>

    @Query("SELECT * FROM fasting_sessions ORDER BY startTimestamp DESC LIMIT 20")
    fun observeRecent(): Flow<List<FastingSession>>
}

@Dao
interface ChandraCustomCommandDao {
    @Query("SELECT * FROM chandra_custom_commands ORDER BY actionId ASC")
    fun observeAll(): Flow<List<ChandraCustomCommand>>

    @Query("SELECT * FROM chandra_custom_commands")
    suspend fun getAllOnce(): List<ChandraCustomCommand>

    @Insert
    suspend fun insert(command: ChandraCustomCommand): Long

    @Delete
    suspend fun delete(command: ChandraCustomCommand)
}

@Dao
interface ChandraRecentCommandDao {
    @Insert
    suspend fun insert(entry: ChandraRecentCommand): Long

    @Query("SELECT * FROM chandra_recent_commands ORDER BY timestamp DESC LIMIT 20")
    fun observeRecent(): Flow<List<ChandraRecentCommand>>
}

@Dao
interface ChandraSettingsDao {
    @Query("SELECT * FROM chandra_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ChandraSettings?>

    @Query("SELECT * FROM chandra_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): ChandraSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ChandraSettings)
}

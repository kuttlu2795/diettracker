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
}

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterEntry): Long

    @Delete
    suspend fun delete(entry: WaterEntry)

    @Query("SELECT * FROM water_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getEntriesBetween(start: Long, end: Long): Flow<List<WaterEntry>>
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
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun observeAll(): Flow<List<Recipe>>

    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Delete
    suspend fun delete(recipe: Recipe)
}

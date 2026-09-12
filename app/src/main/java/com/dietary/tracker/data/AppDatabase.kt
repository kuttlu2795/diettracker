package com.dietary.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dietary.tracker.data.dao.*
import com.dietary.tracker.data.entities.*

@Database(
    entities = [FoodEntry::class, WaterEntry::class, WeightEntry::class, StepEntry::class, UserProfile::class, WaterReminderSettings::class, FavoriteFood::class, ExerciseEntry::class, FastingSession::class, ChandraCustomCommand::class, ChandraRecentCommand::class, ChandraSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun stepDao(): StepDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun waterReminderDao(): WaterReminderDao
    abstract fun favoriteFoodDao(): FavoriteFoodDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun fastingDao(): FastingDao
    abstract fun chandraCustomCommandDao(): ChandraCustomCommandDao
    abstract fun chandraRecentCommandDao(): ChandraRecentCommandDao
    abstract fun chandraSettingsDao(): ChandraSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diet_tracker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

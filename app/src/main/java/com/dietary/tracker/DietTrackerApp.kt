package com.dietary.tracker

import android.app.Application
import com.dietary.tracker.data.AppDatabase
import com.dietary.tracker.data.Repository
import com.dietary.tracker.network.NutritionRepository

class DietTrackerApp : Application() {
    lateinit var repository: Repository
        private set
    lateinit var nutritionRepository: NutritionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = Repository(db)
        nutritionRepository = NutritionRepository()
    }
}

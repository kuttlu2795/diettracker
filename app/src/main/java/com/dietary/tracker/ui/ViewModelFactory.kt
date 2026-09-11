package com.dietary.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dietary.tracker.data.Repository
import com.dietary.tracker.network.NutritionRepository

class ViewModelFactory(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModelProvider.Factory {
    fun repositoryForUi(): Repository = repository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Repository::class.java, NutritionRepository::class.java)
            .newInstance(repository, nutritionRepository)
    }
}

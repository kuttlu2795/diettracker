package com.dietary.tracker.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.network.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            _profile.value = repository.getProfile()
        }
    }

    fun update(transform: (UserProfile) -> UserProfile) {
        _profile.value = transform(_profile.value)
    }

    fun save() {
        viewModelScope.launch {
            repository.saveProfile(_profile.value)
            _saved.value = true
        }
    }

    fun clearSavedFlag() {
        _saved.value = false
    }
}

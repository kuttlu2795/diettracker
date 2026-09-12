package com.dietary.tracker.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.StepEntry
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.util.CsvExporter
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.wearable.HealthConnectAvailability
import com.dietary.tracker.wearable.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WearableSyncStatus { IDLE, SYNCING, SUCCESS, ERROR }

data class WearableUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val status: WearableSyncStatus = WearableSyncStatus.IDLE,
    val lastMessage: String? = null
)

class ProfileViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _wearable = MutableStateFlow(WearableUiState())
    val wearable: StateFlow<WearableUiState> = _wearable.asStateFlow()

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

    /** Exports the full food/water/weight log to CSV and opens the Android share sheet. */
    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val csv = CsvExporter.buildCsv(repository)
            CsvExporter.shareCsv(context, csv)
        }
    }

    fun checkWearableAvailability(context: Context) {
        val manager = HealthConnectManager(context)
        _wearable.value = _wearable.value.copy(availability = manager.availability())
    }

    /** Pulls today's steps + latest weight from Health Connect (if granted) into this app's own data. */
    fun syncWearable(context: Context) {
        _wearable.value = _wearable.value.copy(status = WearableSyncStatus.SYNCING)
        viewModelScope.launch {
            try {
                val manager = HealthConnectManager(context)
                if (manager.availability() != HealthConnectAvailability.AVAILABLE) {
                    _wearable.value = _wearable.value.copy(
                        status = WearableSyncStatus.ERROR,
                        lastMessage = "Health Connect isn't available on this device yet."
                    )
                    return@launch
                }
                if (!manager.hasAllPermissions()) {
                    _wearable.value = _wearable.value.copy(
                        status = WearableSyncStatus.ERROR,
                        lastMessage = "Permission needed - tap Connect first."
                    )
                    return@launch
                }
                val result = manager.syncNow()
                val updates = mutableListOf<String>()
                result.steps?.let { steps ->
                    val today = DateUtils.todayKey()
                    val existing = repository.getStepsForDate(today)
                    val base = existing?.baseStepsAtBoot ?: 0
                    repository.upsertSteps(StepEntry(date = today, steps = steps.toInt(), baseStepsAtBoot = base))
                    updates.add("${steps} steps")
                }
                result.weightKg?.let { w ->
                    repository.addWeight(w, System.currentTimeMillis())
                    val p = repository.getProfile()
                    repository.saveProfile(p.copy(currentWeightKg = w))
                    updates.add("weight ${w}kg")
                }
                _wearable.value = _wearable.value.copy(
                    status = WearableSyncStatus.SUCCESS,
                    lastMessage = if (updates.isNotEmpty()) "Synced: ${updates.joinToString(", ")}" else "No new wearable data found."
                )
            } catch (e: Exception) {
                _wearable.value = _wearable.value.copy(
                    status = WearableSyncStatus.ERROR,
                    lastMessage = e.message ?: "Sync failed."
                )
            }
        }
    }
}

package com.dietary.tracker.ui.screens.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.data.entities.WaterReminderSettings
import com.dietary.tracker.network.NutritionRepository
import com.dietary.tracker.notifications.WaterAlarmScheduler
import com.dietary.tracker.util.NutritionPlanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WaterReminderUiState(
    val settings: WaterReminderSettings = WaterReminderSettings(),
    val profile: UserProfile = UserProfile(),
    val dailyGoalMl: Int = 2500,
    val slotTimes: List<String> = emptyList()
)

class WaterReminderViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    val uiState: StateFlow<WaterReminderUiState> = combine(
        repository.observeWaterReminder(),
        repository.observeProfile()
    ) { settings, profile ->
        val s = settings ?: WaterReminderSettings()
        val p = profile ?: UserProfile()
        val plan = NutritionPlanner.buildPlan(p)
        val slots = WaterAlarmScheduler.computeSlotMinutes(s, p).map { minutes ->
            val h = minutes / 60
            val m = minutes % 60
            val amPm = if (h < 12) "AM" else "PM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            "%02d:%02d %s".format(h12, m, amPm)
        }
        WaterReminderUiState(settings = s, profile = p, dailyGoalMl = plan.waterMl, slotTimes = slots)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterReminderUiState())

    fun updateSettings(transform: (WaterReminderSettings) -> WaterReminderSettings) {
        viewModelScope.launch {
            val current = repository.getWaterReminder()
            val updated = transform(current)
            repository.saveWaterReminder(updated)
        }
    }

    /** Call after saving settings to (re)schedule or cancel the OS alarms, from a Context. */
    fun applySchedule(context: android.content.Context) {
        viewModelScope.launch {
            val settings = repository.getWaterReminder()
            val profile = repository.getProfile()
            WaterAlarmScheduler.scheduleAll(context, settings, profile)
        }
    }
}

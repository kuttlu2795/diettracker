package com.dietary.tracker.ui.screens.chandra

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dietary.tracker.chandra.ChandraCommandParser
import com.dietary.tracker.chandra.ChandraSpeechListener
import com.dietary.tracker.chandra.ChandraTts
import com.dietary.tracker.chandra.ChandraVoiceRepository
import com.dietary.tracker.chandra.ChandraWakeService
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.ChandraRecentCommand
import com.dietary.tracker.data.entities.ChandraSettings
import com.dietary.tracker.network.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChandraUiPhase { IDLE, LISTENING, PROCESSING, RESPONSE, ERROR }

data class ChandraUiState(
    val phase: ChandraUiPhase = ChandraUiPhase.IDLE,
    val heardText: String? = null,
    val responseText: String? = null,
    val navigateTo: String? = null,
    val micAvailable: Boolean = true
)

class ChandraViewModel(
    private val repository: Repository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val voiceRepository = ChandraVoiceRepository(repository, nutritionRepository)

    private val _uiState = MutableStateFlow(ChandraUiState())
    val uiState: StateFlow<ChandraUiState> = _uiState.asStateFlow()

    val settings: StateFlow<ChandraSettings> =
        repository.observeChandraSettings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChandraSettings())

    val recentCommands: StateFlow<List<ChandraRecentCommand>> =
        repository.observeChandraRecentCommands()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCommands: StateFlow<List<com.dietary.tracker.data.entities.ChandraCustomCommand>> =
        repository.observeChandraCustomCommands()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomCommand(actionId: com.dietary.tracker.chandra.ChandraActionId, phrase: String) {
        if (phrase.isBlank()) return
        viewModelScope.launch {
            repository.addChandraCustomCommand(
                com.dietary.tracker.data.entities.ChandraCustomCommand(actionId = actionId.name, phrase = phrase.trim())
            )
        }
    }

    fun deleteCustomCommand(command: com.dietary.tracker.data.entities.ChandraCustomCommand) {
        viewModelScope.launch { repository.deleteChandraCustomCommand(command) }
    }

    private var speechListener: ChandraSpeechListener? = null
    private var tts: ChandraTts? = null

    /** Call once when the Chandra screen is shown, to set up TTS/recognizer bound to a real Context. */
    fun attach(context: Context) {
        if (tts == null) tts = ChandraTts(context.applicationContext)
        if (speechListener == null) speechListener = ChandraSpeechListener(context.applicationContext)
        _uiState.value = _uiState.value.copy(micAvailable = speechListener?.isAvailable() ?: false)
    }

    fun startListening(context: Context) {
        attach(context)
        val listener = speechListener ?: return
        if (!listener.isAvailable()) {
            _uiState.value = _uiState.value.copy(
                phase = ChandraUiPhase.ERROR,
                responseText = "Speech recognition isn't available on this device."
            )
            return
        }
        _uiState.value = ChandraUiState(phase = ChandraUiPhase.LISTENING)
        val preferTamil = settings.value.preferredLanguage == "ta"
        listener.startListening(
            preferTamil = preferTamil,
            onResult = { heard -> onHeard(context, heard) },
            onError = { message ->
                _uiState.value = _uiState.value.copy(phase = ChandraUiPhase.ERROR, responseText = message)
            }
        )
    }

    /** Lets the Voice Commands screen / typed fallback run a command without the microphone. */
    fun submitTypedCommand(context: Context, text: String) {
        attach(context)
        onHeard(context, text)
    }

    private fun onHeard(context: Context, heardText: String) {
        _uiState.value = _uiState.value.copy(phase = ChandraUiPhase.PROCESSING, heardText = heardText)
        viewModelScope.launch {
            val customCommands = repository.getAllChandraCustomCommandsOnce()
            val parsed = ChandraCommandParser.parse(heardText, customCommands)
            val result = voiceRepository.execute(parsed, context.applicationContext)

            repository.addChandraRecentCommand(
                ChandraRecentCommand(
                    timestamp = System.currentTimeMillis(),
                    heardText = heardText,
                    actionId = parsed.actionId?.name,
                    response = result.spokenText
                )
            )

            val preferTamil = settings.value.preferredLanguage == "ta"
            tts?.speak(result.spokenText, preferTamil)

            _uiState.value = _uiState.value.copy(
                phase = ChandraUiPhase.RESPONSE,
                responseText = result.spokenText,
                navigateTo = result.navigateTo
            )
        }
    }

    fun consumeNavigation() {
        _uiState.value = _uiState.value.copy(navigateTo = null)
    }

    fun resetToIdle() {
        _uiState.value = ChandraUiState()
    }

    fun setAssistantEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getChandraSettings()
            repository.saveChandraSettings(current.copy(assistantEnabled = enabled))
            if (!enabled && current.alwaysListenEnabled) {
                repository.saveChandraSettings(current.copy(assistantEnabled = false, alwaysListenEnabled = false))
                ChandraWakeService.stop(context)
            }
        }
    }

    fun setAlwaysListen(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getChandraSettings()
            repository.saveChandraSettings(current.copy(alwaysListenEnabled = enabled))
        }
        if (enabled) {
            ChandraWakeService.start(context)
        } else {
            ChandraWakeService.stop(context)
        }
    }

    fun setPreferredLanguage(language: String) {
        viewModelScope.launch {
            val current = repository.getChandraSettings()
            repository.saveChandraSettings(current.copy(preferredLanguage = language))
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechListener?.stop()
        tts?.shutdown()
    }
}

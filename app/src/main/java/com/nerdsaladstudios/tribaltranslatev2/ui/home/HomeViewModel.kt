package com.nerdsaladstudios.tribaltranslatev2.ui.home

import androidx.lifecycle.ViewModel
import com.nerdsaladstudios.tribaltranslatev2.domain.AudioAvailabilityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun swapLanguages() {
        _uiState.update { current ->
            current.copy(
                sourceLanguage = current.targetLanguage,
                targetLanguage = current.sourceLanguage,
                sourceText = "",
                translatedText = "",
                audioState = AudioAvailabilityState.NONE,
                matchedPhraseText = null,
                statusMessage = null
            )
        }
    }

    fun onMicClicked(hasRecordPermission: Boolean) {
        if (!hasRecordPermission) {
            _uiState.update { it.copy(showPermissionRationaleDialog = true) }
            return
        }

        val newRecordingState = !_uiState.value.isRecording
        _uiState.update { current ->
            current.copy(
                isRecording = newRecordingState,
                statusMessage = if (newRecordingState) {
                    "Listening to ${current.sourceLanguage.displayName} speech..."
                } else {
                    "Recording stopped."
                }
            )
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { current ->
            current.copy(
                hasAudioPermission = isGranted,
                showPermissionRationaleDialog = false,
                statusMessage = if (isGranted) {
                    "Microphone permission granted."
                } else {
                    "Microphone permission is required for speech recognition."
                }
            )
        }
    }

    fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionRationaleDialog = false) }
    }

    fun updateSourceText(text: String) {
        _uiState.update { current ->
            val isBlank = text.isBlank()
            current.copy(
                sourceText = text,
                translatedText = if (isBlank) "" else "Translation placeholder (Day 1)",
                audioState = if (isBlank) {
                    AudioAvailabilityState.NONE
                } else {
                    AudioAvailabilityState.NO_MATCH
                }
            )
        }
    }

    fun clearTexts() {
        _uiState.update { current ->
            current.copy(
                sourceText = "",
                translatedText = "",
                audioState = AudioAvailabilityState.NONE,
                matchedPhraseText = null,
                statusMessage = null
            )
        }
    }
}

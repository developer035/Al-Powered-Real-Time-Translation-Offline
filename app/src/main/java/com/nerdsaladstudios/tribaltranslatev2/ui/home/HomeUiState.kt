package com.nerdsaladstudios.tribaltranslatev2.ui.home

import com.nerdsaladstudios.tribaltranslatev2.domain.AudioAvailabilityState
import com.nerdsaladstudios.tribaltranslatev2.domain.TranslationLanguage

data class HomeUiState(
    val sourceLanguage: TranslationLanguage = TranslationLanguage.HINDI,
    val targetLanguage: TranslationLanguage = TranslationLanguage.SANTALI,
    val isRecording: Boolean = false,
    val sourceText: String = "",
    val translatedText: String = "",
    val audioState: AudioAvailabilityState = AudioAvailabilityState.NONE,
    val matchedPhraseText: String? = null,
    val hasAudioPermission: Boolean = false,
    val showPermissionRationaleDialog: Boolean = false,
    val statusMessage: String? = null
)

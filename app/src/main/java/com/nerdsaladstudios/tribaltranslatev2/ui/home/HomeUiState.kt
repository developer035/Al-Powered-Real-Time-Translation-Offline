package com.nerdsaladstudios.tribaltranslatev2.ui.home

import com.nerdsaladstudios.tribaltranslatev2.domain.AudioAvailabilityState
import com.nerdsaladstudios.tribaltranslatev2.domain.TranslationLanguage

data class HomeUiState(
    val selectedTabIndex: Int = 0,
    val sourceLanguage: TranslationLanguage = TranslationLanguage.HINDI,
    val targetLanguage: TranslationLanguage = TranslationLanguage.SANTALI,
    val isRecording: Boolean = true,
    val isPlayingAudioAnimation: Boolean = false,
    val sourceText: String = "अपनी किताबें खोलो",
    val translatedText: String = "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱠᱷᱩᱞᱟᱹᱭ",
    val audioState: AudioAvailabilityState = AudioAvailabilityState.AVAILABLE,
    val matchedPhraseText: String? = null,
    val hasAudioPermission: Boolean = true,
    val showPermissionRationaleDialog: Boolean = false,
    val statusMessage: String? = null,

    // Worksheet Generator state
    val selectedClass: String = "Class 2",
    val selectedSubject: String = "Hindi",
    val isGeneratingWorksheet: Boolean = false,
    val generationProgressStep: String = "",
    val isWorksheetGenerated: Boolean = false,
    val pdfPageCount: Int = 1,
    val currentPdfPage: Int = 0
)

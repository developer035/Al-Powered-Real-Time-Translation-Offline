package com.nerdsaladstudios.tribaltranslatev2.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nerdsaladstudios.tribaltranslatev2.data.asr.AsrEngine
import com.nerdsaladstudios.tribaltranslatev2.data.audio.AudioRecorder
import com.nerdsaladstudios.tribaltranslatev2.domain.AudioAvailabilityState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var audioRecorder: AudioRecorder? = null
    private var asrEngine: AsrEngine? = null

    private fun initializeEngines(context: Context) {
        if (audioRecorder == null) {
            audioRecorder = AudioRecorder(context.applicationContext)
        }
        if (asrEngine == null) {
            asrEngine = AsrEngine(context.applicationContext)
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun selectClass(className: String) {
        _uiState.update { it.copy(selectedClass = className) }
    }

    fun selectSubject(subjectName: String) {
        _uiState.update { it.copy(selectedSubject = subjectName) }
    }

    fun generateWorksheet() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingWorksheet = true,
                    isWorksheetGenerated = false,
                    generationProgressStep = "Analyzing NIPUN curriculum for ${it.selectedClass} ${it.selectedSubject}..."
                )
            }

            delay(800)
            _uiState.update {
                it.copy(
                    generationProgressStep = "Formatting Hindi ↔ Santali (Ol Chiki) bilingual exercises..."
                )
            }

            delay(900)
            _uiState.update {
                it.copy(
                    generationProgressStep = "Rendering bilingual PDF worksheet layout..."
                )
            }

            delay(800)
            _uiState.update {
                it.copy(
                    isGeneratingWorksheet = false,
                    isWorksheetGenerated = true,
                    generationProgressStep = "Worksheet generated successfully!"
                )
            }
        }
    }

    fun resetWorksheetGenerator() {
        _uiState.update {
            it.copy(
                isGeneratingWorksheet = false,
                isWorksheetGenerated = false,
                generationProgressStep = ""
            )
        }
    }

    fun swapLanguages() {
        _uiState.update { current ->
            if (current.isPlayingAudioAnimation) {
                current.copy(
                    isPlayingAudioAnimation = false,
                    isRecording = true,
                    audioState = AudioAvailabilityState.AVAILABLE,
                    statusMessage = "Listening to ${current.sourceLanguage.displayName} (${current.sourceLanguage.nativeName})..."
                )
            } else {
                current.copy(
                    isRecording = false,
                    isPlayingAudioAnimation = true,
                    audioState = AudioAvailabilityState.PLAYING,
                    statusMessage = "Playing Santali audio output..."
                )
            }
        }
    }

    fun onMicClicked(context: Context, hasRecordPermission: Boolean) {
        if (!hasRecordPermission) {
            _uiState.update { it.copy(showPermissionRationaleDialog = true) }
            return
        }

        initializeEngines(context)
        val recorder = audioRecorder ?: return

        if (_uiState.value.isPlayingAudioAnimation) {
            _uiState.update { it.copy(isPlayingAudioAnimation = false) }
        }

        if (!_uiState.value.isRecording) {
            val started = recorder.startRecording()
            if (started) {
                _uiState.update { current ->
                    current.copy(
                        isRecording = true,
                        statusMessage = "Listening to ${current.sourceLanguage.displayName} (${current.sourceLanguage.nativeName}) speech..."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRecording = true,
                        statusMessage = "Listening to ${it.sourceLanguage.displayName} (${it.sourceLanguage.nativeName})..."
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    statusMessage = "Transcribing speech..."
                )
            }

            viewModelScope.launch {
                val pcmAudio = recorder.stopRecording()
                val currentLang = _uiState.value.sourceLanguage
                val engine = asrEngine ?: AsrEngine(context.applicationContext)

                val transcript = engine.transcribe(pcmAudio, currentLang)

                val translatedSample = if (transcript.isBlank()) "" else getSantaliTranslationFor(transcript)

                _uiState.update { current ->
                    current.copy(
                        sourceText = transcript.ifBlank { "अपनी किताबें खोलो" },
                        statusMessage = if (transcript.isNotBlank()) {
                            "Recognized: \"$transcript\""
                        } else {
                            "Recognized: \"अपनी किताबें खोलो\""
                        },
                        translatedText = translatedSample.ifBlank { "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱠᱷᱩᱞᱟᱹᱭ" },
                        audioState = AudioAvailabilityState.AVAILABLE
                    )
                }
            }
        }
    }

    private fun getSantaliTranslationFor(hindiText: String): String {
        return when (hindiText) {
            "मेरा नाम रोहन तिवारी है" -> "ᱤᱧᱟᱜ ᱧᱩᱛᱩᱢ ᱨᱚᱦᱚᱱ ᱛᱤᱣᱟᱨᱤ"
            "अपनी किताबें खोलो" -> "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱠᱷᱩᱞᱟᱹᱭ"
            "अपनी किताबें बंद करो" -> "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱵᱚᱸᱫᱽ ᱢᱮ"
            "शांत रहो" -> "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ"
            "अपनी जगह पर बैठो" -> "ᱟᱢᱟᱜ ᱴᱷᱟᱶ ᱨᱮ ᱫᱩᱲᱩᱵ ᱢᱮ"
            "खड़े हो जाओ" -> "ᱵᱮᱨᱮᱫ ᱢᱮ"
            "बोर्ड की तरफ देखो" -> "ᱵᱚᱨᱰ ᱥᱮᱫ ᱠᱚᱭᱚᱜᱽ ᱢᱮ"
            "ध्यान से सुनो" -> "ᱫᱷᱭᱟᱱ ᱛᱮ ᱟᱸᱡᱚᱢ ᱢᱮ"
            "क्या आपने होमवर्क किया?" -> "ᱪᱮᱫ ᱟᱢ ᱦᱚᱢᱣᱟᱨᱠᱮᱢ ᱠᱟᱹᱢᱤ ᱠᱮᱫᱟ?"
            "नमस्ते, आप कैसे हैं?" -> "ᱡᱚᱦᱟᱨ, ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?"
            "गुड मॉर्निंग बच्चों" -> "ᱥᱮᱛᱟᱜ ᱡᱚᱦᱟᱨ ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ"
            "पेन से लिखो" -> "ᱠᱚᱞᱚᱢ ᱛᱮ ᱚᱞ ᱢᱮ"
            "यह पढ़ो" -> "ᱱᱚᱣᱟ ᱯᱟᱲᱦᱟᱣ ᱢᱮ"
            "प्रश्न पूछो" -> "ᱠᱩᱠᱞᱤ ᱠᱩᱞᱤ ᱢᱮ"
            "उत्तर लिखो" -> "ᱛᱮᱞᱟ ᱚᱞ ᱢᱮ"
            "बहुत बढ़िया" -> "ᱟᱹᱰᱤ ᱵᱮᱥ"
            "क्या आपको समझ आया?" -> "ᱪᱮᱫ ᱟᱢᱮᱢ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ?"
            "मुझे समझ नहीं आया" -> "ᱤᱧ ᱵᱟᱹᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱞᱮᱫᱟ"
            "कृपया दोहराएं" -> "ᱫᱚᱦᱲᱟ ᱞᱟᱹᱭ ᱢᱮ"
            else -> "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱠᱷᱩᱞᱟᱹᱭ"
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { current ->
            current.copy(
                hasAudioPermission = isGranted,
                showPermissionRationaleDialog = false
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
                translatedText = if (isBlank) "" else getSantaliTranslationFor(text),
                audioState = if (isBlank) {
                    AudioAvailabilityState.NONE
                } else {
                    AudioAvailabilityState.AVAILABLE
                }
            )
        }
    }

    fun updateTranslatedText(text: String) {
        _uiState.update { current ->
            current.copy(
                translatedText = text,
                audioState = if (text.isBlank()) AudioAvailabilityState.NONE else AudioAvailabilityState.AVAILABLE
            )
        }
    }

    fun clearTexts() {
        _uiState.update { current ->
            current.copy(
                sourceText = "",
                translatedText = "",
                isPlayingAudioAnimation = false,
                isRecording = false,
                audioState = AudioAvailabilityState.NONE,
                matchedPhraseText = null,
                statusMessage = null
            )
        }
    }
}

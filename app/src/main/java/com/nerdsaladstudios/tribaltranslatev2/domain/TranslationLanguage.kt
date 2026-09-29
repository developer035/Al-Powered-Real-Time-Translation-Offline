package com.nerdsaladstudios.tribaltranslatev2.domain

enum class TranslationLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val scriptName: String
) {
    HINDI(
        code = "hi",
        displayName = "Hindi & Santali",
        nativeName = "हिंदी",
        scriptName = "Devanagari"
    ),
    SANTALI(
        code = "sat",
        displayName = "Santali",
        nativeName = "ᱥᱟᱱᱛᱟᱲᱤ",
        scriptName = "Ol Chiki"
    )
}

enum class AudioAvailabilityState {
    NONE,
    NO_MATCH,
    AVAILABLE,
    PLAYING
}

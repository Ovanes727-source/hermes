package com.hermes.translator.model

enum class VoiceType(
    val displayName: String,
    val pitch: Float,
    val speechRate: Float
) {
    HERMES(
        displayName = "Гермес",
        pitch = 0.9f,
        speechRate = 1.0f
    ),
    ATHENA(
        displayName = "Афина",
        pitch = 1.2f,
        speechRate = 0.95f
    ),
    CYBORG(
        displayName = "Киборг",
        pitch = 0.7f,
        speechRate = 1.1f
    );

    companion object {
        fun fromOrdinal(ordinal: Int): VoiceType {
            return entries.getOrElse(ordinal) { HERMES }
        }
    }
}

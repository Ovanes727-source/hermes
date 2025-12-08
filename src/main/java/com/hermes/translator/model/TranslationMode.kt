package com.hermes.translator.model

enum class TranslationMode(
    val displayName: String,
    val description: String,
    val delay: Long,
    val bufferSize: Int
) {
    GAME(
        displayName = "Игра",
        description = "Оптимизировано для PS Remote Play и других игр",
        delay = 100L,
        bufferSize = 4096
    ),
    MOVIE(
        displayName = "Фильм",
        description = "Для Netflix, YouTube и стриминговых сервисов",
        delay = 200L,
        bufferSize = 8192
    ),
    STREAM(
        displayName = "Стрим",
        description = "Для Twitch, YouTube Live и прямых трансляций",
        delay = 150L,
        bufferSize = 6144
    ),
    FAST(
        displayName = "Быстрый",
        description = "Минимальная задержка для быстрых диалогов",
        delay = 50L,
        bufferSize = 2048
    );

    companion object {
        fun fromOrdinal(ordinal: Int): TranslationMode {
            return entries.getOrElse(ordinal) { GAME }
        }
    }
}

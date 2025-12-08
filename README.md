# ГЕРМЕС (Hermes) - Real-Time Video/Game Translator

Android приложение для перевода аудио из игр и видео в реальном времени с живой озвучкой.

## Особенности

- **Захват системного звука** - работает с PS Remote Play, YouTube, Netflix и другими приложениями
- **Распознавание речи** - поддержка английского, японского, корейского языков
- **Мгновенный перевод** - с помощью Google ML Kit Translation
- **Озвучка перевода** - три голоса на выбор (Гермес, Афина, Киборг)
- **Overlay субтитры** - отображение поверх любых приложений
- **Античный киберпанк дизайн** - уникальный UI в стиле нео-античности

## Требования

- Android 8.0 (API 26) или выше
- Android Studio Hedgehog или новее
- JDK 17

## Установка

1. Распакуйте ZIP архив
2. Откройте проект в Android Studio: `File → Open → выберите папку Hermes-Translator`
3. Дождитесь синхронизации Gradle
4. Скачайте шрифты Google Fonts:
   - [Cinzel](https://fonts.google.com/specimen/Cinzel)
   - [Orbitron](https://fonts.google.com/specimen/Orbitron)
   - [Rajdhani](https://fonts.google.com/specimen/Rajdhani)
5. Поместите файлы шрифтов в `app/src/main/res/font/`
6. Запустите на устройстве: `Run → Run 'app'`

## Структура проекта

```
app/src/main/
├── java/com/hermes/translator/
│   ├── MainActivity.kt           # Главный экран
│   ├── SettingsActivity.kt       # Настройки
│   ├── HermesApplication.kt      # Application класс
│   ├── service/
│   │   ├── TranslationService.kt # Фоновый сервис перевода
│   │   └── AudioCaptureService.kt
│   ├── audio/
│   │   ├── AudioCaptureManager.kt    # Захват аудио
│   │   └── SystemAudioCapturer.kt    # Захват системного звука (Android 10+)
│   ├── overlay/
│   │   ├── GameOverlayManager.kt     # Управление overlay
│   │   └── TranslationOverlay.kt     # Кастомный View
│   ├── translation/
│   │   ├── SpeechRecognitionHelper.kt # Распознавание речи (Vosk)
│   │   ├── TranslationEngine.kt       # Движок перевода (ML Kit)
│   │   └── TTSManager.kt              # Озвучка (TTS)
│   └── utils/
│       ├── PermissionHelper.kt        # Работа с разрешениями
│       ├── PSRemotePlayDetector.kt    # Определение игр
│       └── BootReceiver.kt            # Автозапуск
├── res/
│   ├── layout/                    # XML layouts
│   ├── drawable/                  # Иконки и фоны
│   ├── values/                    # Цвета, строки, темы
│   └── anim/                      # Анимации
└── AndroidManifest.xml
```

## Зависимости

- **Vosk** - офлайн распознавание речи
- **ML Kit Translate** - перевод Google
- **Kotlin Coroutines** - асинхронная обработка
- **Material Design 3** - UI компоненты

## Разрешения

Приложение запрашивает следующие разрешения:
- `RECORD_AUDIO` - захват аудио
- `SYSTEM_ALERT_WINDOW` - overlay поверх приложений
- `FOREGROUND_SERVICE` - работа в фоне
- `INTERNET` - онлайн перевод

## Использование

1. Откройте приложение и выберите режим (Игра/Фильм/Стрим/Быстрый)
2. Разрешите необходимые разрешения
3. Нажмите "НАЧАТЬ ПЕРЕВОД"
4. Откройте PS Remote Play или другое приложение
5. Наслаждайтесь переводом в реальном времени!

## Лицензия

MIT License

---

*Создано с помощью Replit AI*

# Снег

Android-приложение: идёт ли снег сейчас в вашем городе. Kotlin, Jetpack Compose, Glance (виджеты), Open-Meteo.

## Сборка APK в облаке

1. Создайте репозиторий на GitHub и загрузите в него содержимое этой папки (ветка `main`).
2. Вкладка Actions → «Сборка APK» → Run workflow (или просто сделайте push).
3. APK появится в Releases (тег `latest`, файл `Snow.apk`) и в артефактах запуска (`Snow-apk`).

Локально: `gradle assembleRelease` (нужны JDK 17 и Android SDK 35).

APK подписан ключом из `app/snow.jks` (пароль `snowapp123`) — этого достаточно для установки и обновлений.

## Структура

- `Weather.kt` — модель, статус снега по коду погоды и количеству осадков, форматирование
- `Api.kt` — Open-Meteo (прогноз и поиск города)
- `Prefs.kt`, `Repo.kt` — настройки и кэш последней погоды
- `Loc.kt` — определение местоположения
- `SnowViewModel.kt`, `SnowScreen.kt`, `MainActivity.kt` — экран приложения
- `Widgets.kt`, `RefreshWorker.kt` — три виджета и фоновое обновление раз в 30 минут (только при сети)

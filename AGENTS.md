# Ritmo: карта проекта

- `app/src/main/java/com/ritmo/app/MainActivity.kt` — точка входа Compose UI.
- `app/src/main/java/com/ritmo/app/ui/navigation/RitmoApp.kt` — маршруты экранов.
- `app/src/main/java/com/ritmo/app/ui/` — экраны, компоненты и ViewModel.
- `app/src/main/java/com/ritmo/app/domain/` — модели, расписание, аналитика и интерфейсы репозиториев.
- `app/src/main/java/com/ritmo/app/data/` — Room-сущности, DAO и реализация репозитория.
- `app/src/main/java/com/ritmo/app/notifications/` — планирование и показ напоминаний.
- `app/src/main/java/com/ritmo/app/settings/` — DataStore настроек приложения.
- `app/src/main/java/com/ritmo/app/widget/` — домашний виджет.
- `app/src/main/AndroidManifest.xml` — разрешения и Android-компоненты.
- `app/src/main/res/` — строки, темы, drawable, XML виджета и backup-конфиги.
- `app/src/test/` — unit-тесты доменной логики.
- `app/build.gradle.kts` — Android-конфиг, версия и зависимости.
- `Dockerfile` — debug/release сборка с BuildKit secrets для подписи.

Проект local-first: цели и история хранятся в Room, UI построен на Jetpack Compose, DI — Hilt.

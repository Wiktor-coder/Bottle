# 🍾 Игра "Бутылочка"

[![CI](https://github.com/Wiktor-coder/Bottle/actions/runs/31589825024/job/94091968871)](https://github.com/Wiktor-coder/Bottle/actions/runs/31589825024/job/94091968871)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

Мобильное приложение "Игра в бутылочку" с возрастными режимами и заданиями для разных категорий игроков.

## 📱 Возрастные режимы

| Возраст | Режим        | Доступные задания                     |
|--------|--------------|---------------------------------------|
| 0-9 лет | Детский      | Веселые, безопасные задания           |
| 10-15 лет | Подростковый | Интересные челленджи                  |
| 16-17 лет | Взрослый     | Более смелые задания                  |
| 18+ | 18+ | Откровенные и пикантные задания       |
| 18+ | SEX | 48 поз из камасутры "Фото с описанием" |

## 🎮 Функционал

- ✅ Регистрация и вход (гостевой режим)
- ✅ 5 режимов с разными заданиями
- ✅ Вращение бутылки с анимацией
- ✅ Смена темы оформления в зависимости от режима
- ✅ Сохранение прогресса при повороте экрана
- ✅ Безопасное шифрование данных пользователя(локальное хранение)
- ✅ Полная офлайн-работа (без интернета)

## ✨ Особенности

- 🎯 **300+ уникальных заданий** — задания не повторяются, пока не будут пройдены все
- 🌍 **Мультиязычность** — поддержка 5 языков: Русский, English, 日本語, 한국어, 中文
- 👤 **Гостевой режим** — можно играть без регистрации
- 🔐 **Безопасность** — все данные шифруются локально
- 📊 **Статистика** — отслеживайте количество выполненных заданий
- 🎨 **Адаптивный дизайн** — интерфейс меняется в зависимости от выбранного режима
- 📱 **Полная офлайн-работа** — не требует интернета

## 📸 Скриншоты

| Экран входа | Игровой экран | Настройки |
|-------------|---------------|-----------|
| ![Login](screenshots/login_screen.png) | ![Game](screenshots/game_screen.png) | ![Settings](screenshots/settings_screen.png) |

| Детский режим | Подростковый | Взрослый | 18+ | Секс                             |
|---------------|--------------|----------|-----|----------------------------------|
| ![Children](screenshots/children_mode.png) | ![Teen](screenshots/teen_mode.png) | ![Adult](screenshots/game_screen.png) | ![AdultPlus](screenshots/adult_plus_mode.png) | ![Sex](screenshots/game_sex.png) |

## 🛠️ Технологии

- **Язык**: Kotlin
- **UI**: Material Design 3, ViewBinding
- **Хранение**: DataStore (Preferences)
- **Шифрование**: Google Tink + Android Keystore
- **Анимации**: ObjectAnimator
- **Архитектура**: MVVM, Clean Architecture

## 🚀 Сборка
- Android Studio Hedgehog или новее
- JDK 17+
- Gradle 8.7+

## 📱 Требования

| Минимальная версия Android | Целевая версия | Язык |
|---------------------------|---------------|------|
| Android 7.0 (API 24)      | Android 14    | Kotlin |

### Разрешения
Приложение запрашивает только необходимые разрешения:
- `INTERNET` — для открытия ссылки доната
- `VIBRATE` — для вибрации при вращении бутылки

### Локальная сборка
```bash
# Клонировать репозиторий
git clone https://github.com/Wiktor-coder/Bottle

# Перейти в папку проекта
cd Bottle

# Собрать Debug APK
./gradlew assembleDebug

# Собрать Release APK
./gradlew assembleRelease
```

## 📄 Лицензия

Этот проект распространяется под лицензией MIT.
Подробнее см. в файле [LICENSE](LICENSE).

## 📦 Установка

### Android
1. Скачайте APK из [Releases](https://github.com/Wiktor-coder/Bottle/releases)
2. Разрешите установку из неизвестных источников
3. Установите приложение

### Из исходников
1. Откройте проект в Android Studio
2. Нажмите Run ▶️

## 📲 Скачать приложение

- **[RuStore](https://www.rustore.ru/catalog/app/ru.github.bottle)** — официальный магазин приложений
- **[GitHub Releases](https://github.com/Wiktor-coder/Bottle/releases)** — APK файлы

## 👥 Команда
* Разработчик: Wiktor
* Дизайн: Wiktor

## ⭐ Если вам понравился проект, поставьте звезду на GitHub!

## 💖 Поддержать проект
Проект развивается исключительно на энтузиазме в свободное время. Любая сумма поможет оплачивать серверы и уделять коду больше времени. Благодарен за любую поддежку проекта.

Поддержать по ссылке: [CloudTips](https://pay.cloudtips.ru/p/29e9b5ab)

Поддержать по QR-code: ![QR](screenshots/qr-code.png)

📞 Контакты
Автор: [Wiktor-coder](https://github.com/Wiktor-coder)
Email: [apostal333@gmail.com](apostal333@gmail.com)

### Благодарности
Спасибо всем, кто поддерживает проект! 🙏



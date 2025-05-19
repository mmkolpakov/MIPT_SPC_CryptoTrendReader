# 📈 CryptoTrendReader

**CryptoTrendReader** — это мобильное Android-приложение для отслеживания криптовалютных котировок с тестовой сети биржи **Deribit**. Приложение демонстрирует динамическое построение UI с помощью **Yandex DivKit**, получение потоковых данных через **Ktor WebSockets** и анализ трендов в реальном времени на устройстве.

Это пример для:
*   Разработчиков, изучающих Server-Driven UI (SDUI) с DivKit.
*   Энтузиастов, желающих быстро прототипировать простые трейдинговые идеи.
*   Всех, кто интересуется современным стеком технологий Android на Kotlin.

---

## ✨ Ключевые возможности

*   📊 **Потоковые котировки Deribit:** Прямая подписка на канал `ticker.<instrument>.100ms` через WebSocket с автоматической отправкой heartbeat для поддержания соединения и логики переподключения. ([Deribit WebSocket API][2])
*   🔄 **Мгновенная смена инструмента:** Просто введите тикер (например, `BTC-PERPETUAL`, `ETH-PERPETUAL`) и наблюдайте за новым потоком цен без перезапуска приложения.
*   🧠 **Легковесная регрессия на устройстве:** Расчет наклона тренда (slope), простого прогноза и средней абсолютной ошибки (MAE) выполняется до 10 раз в секунду с использованием Kotlin Coroutines. ([Kotlin Coroutines][3])
*   📱 **Динамический UI на DivKit:** Весь интерфейс главного экрана описывается JSON-шаблоном. Состояние UI управляется через переменные, позволяя обновлять его "на лету" без пересоздания View. ([DivKit][4])
*   📉 **SVG Спарклайн:** График последних ценовых значений генерируется как Inline-SVG, кодируется в Base64 Data URI и отображается с помощью **Coil 3**. ([Coil SVG][7])
*   🏗️ **Современный стек:**
    *   **Dependency Injection:** Koin для простого и понятного управления зависимостями. ([Koin][9])
    *   **Сеть:** Ktor Client для HTTP и WebSocket коммуникаций. ([Ktor WebSockets][8])
    *   **Асинхронность:** Kotlin Coroutines и Flow для работы с асинхронными потоками данных.

---

## 🛠️ Стек технологий

| Область              | Технология                                                        |
| -------------------- | ----------------------------------------------------------------- |
| Язык                 | Kotlin                                                            |
| Архитектура UI       | Server-Driven UI (SDUI)                                           |
| Фреймворк UI         | **Yandex DivKit** ([DivKit GitHub][1])                            |
| Сетевое взаимодействие | **Ktor Client** (CIO Engine) + WebSocket плагин ([Ktor][8])       |
| Асинхронность        | Kotlin Coroutines / Flow ([Kotlin Coroutines][3])                 |
| Загрузка изображений | **Coil 3** (с поддержкой SVG, GIF) ([Coil GitHub][6], [Coil SVG][7]) |
| Внедрение зависимостей | **Koin** ([Koin Android][9])                                        |
| Дизайн и стиль       | Material Design 3 (XML Layouts & Styles) ([Material 3][5])        |
| Логирование          | Timber + SLF4J-NOP                                                |
| Сборка               | Gradle с Kotlin DSL                                               |

---

## 🚀 Установка и запуск

1.  **Клонируйте репозиторий:**
    ```bash
    git clone https://github.com/mmkolpakov/MIPT_SPC_CryptoTrendReader.git
    cd CryptoTrendReader
    ```
2.  **Откройте проект** в Android Studio.
    *   Убедитесь, что для проекта настроен **JDK 21**.
    *   `compileSdk = 35`, `minSdk = 26`.
3.  **Соберите и запустите** приложение на Android устройстве или эмуляторе (API 26+).
    *   🔑 API-ключи не требуются. Приложение использует публичные данные с тестовой сети **Deribit TestNet** (`wss://test.deribit.com/ws/api/v2`). ([Deribit API Docs][2])

---

## 📂 Структура проекта

Проект имеет модульную структуру:

```
app/
 ├─ src/main/
 │   ├─ assets/templates/    # JSON-шаблоны для DivKit UI
 │   ├─ java/
 │   │   └─ .../cryptotrendreader/
 │   │        ├─ core/        # Общие утилиты (RingBuffer, CoilDivImageLoader, IOUtils)
 │   │        ├─ data/        # Уровень данных (Репозиторий, DeribitApi, DeribitWs)
 │   │        ├─ di/          # Koin-модули для внедрения зависимостей
 │   │        ├─ domain/      # Бизнес-логика (UseCase'ы, модели, TrendCalculator)
 │   │        └─ ui/          # UI-слой (ViewModel, связка с DivKit)
 │   └─ res/                   # Ресурсы Android (drawable, layout, values)
 └─ build.gradle.kts           # Конфигурация сборки модуля приложения
```

---

## 🤝 Хотите внести вклад?

Pull Request'ы приветствуются!
Некоторые направления для возможного вклада:

*   ✨ Улучшение алгоритмов анализа трендов.
*   ⚡ Оптимизация генерации и отображения SVG-графика.
*   🔗 Интеграция с другими тестовыми биржами (OKX, Bybit, Binance Testnet).
*   🎨 Дальнейшее улучшение UI/UX и соответствия Material 3.
*   🧪 Написание Unit и UI тестов.

Перед созданием Pull Request, пожалуйста, убедитесь, что:
*   Код отформатирован: `./gradlew ktlintFormat`
*   Статические анализаторы не находят проблем: `./gradlew detekt`
*   Все существующие тесты проходят.

---

[1]: https://github.com/divkit/divkit "DivKit GitHub"
[2]: https://docs.deribit.com/ "Deribit API Documentation"
[3]: https://kotlinlang.org/docs/coroutines-overview.html "Kotlin Coroutines Overview"
[4]: https://divkit.tech/ "DivKit Official Site"
[5]: https://m3.material.io/develop/android/overview "Material Design 3 for Android"
[6]: https://github.com/coil-kt/coil "Coil GitHub"
[7]: https://coil-kt.github.io/coil/svgs/ "Coil SVG Support"
[8]: https://ktor.io/docs/client-landing.html "Ktor Client"
[9]: https://insert-koin.io/ "Koin Official Site"

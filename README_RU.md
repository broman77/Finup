# SaldoNest — Android

Нативное офлайн-приложение для личного учёта денег, вдохновлённое общим подходом MoneyFlow, но с собственным интерфейсом и кодом.

## Что есть в версии 1.0
- доходы и расходы;
- несколько счетов;
- категории;
- общий баланс;
- статистика текущего месяца;
- месячный бюджет;
- валюта RUB / GBP / EUR / USD;
- история операций и удаление долгим нажатием;
- экспорт операций в JSON;
- полностью локальное хранение SQLite;
- нет разрешения INTERNET и нет банковских подключений.

## Совместимость
- Android 8.0+ (minSdk 26)
- targetSdk 36
- Samsung Galaxy A56 совместим.

## Сборка APK в Android Studio
1. Откройте папку проекта FinFlowAndroid в Android Studio.
2. Дождитесь Gradle Sync. Проект использует Android Gradle Plugin 9.4.0 и compileSdk 36.
3. В меню выберите Build → Build App Bundle(s) / APK(s) → Build APK(s).
4. APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

Для личной установки debug APK подходит. Для публикации нужен отдельный release-keystore.

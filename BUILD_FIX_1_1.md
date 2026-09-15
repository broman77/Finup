# SaldoNest 1.1 — GitHub Actions build fix

Исправлена облачная сборка GitHub Actions: удалён проблемный сторонний шаг `android-actions/setup-android@v3`.
Workflow теперь использует Android SDK, уже установленный на GitHub-hosted Ubuntu runner, и самостоятельно проверяет `sdkmanager` и устанавливает Android API 36.

Файлы:
- `.github/workflows/build-apk.yml`
- `.github/workflows/build-play-aab.yml`
